package net.refractored.joblistings

import com.earth2me.essentials.Essentials
import com.samjakob.spigui.SpiGUI
import com.tchristofferson.configupdater.ConfigUpdater
import dev.unnm3d.redischat.api.RedisChatAPI
import io.papermc.lib.PaperLib
import net.milkbowl.vault.economy.Economy
import net.refractored.joblistings.commands.*
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.exceptions.CommandErrorHandler
import net.refractored.joblistings.listeners.PlayerJoinListener
import net.refractored.joblistings.mail.Mail
import net.refractored.joblistings.order.Order
import org.bstats.bukkit.Metrics
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import revxrsal.commands.bukkit.BukkitCommandHandler
import revxrsal.commands.command.CommandActor
import revxrsal.commands.command.ExecutableCommand
import java.io.File
import java.io.IOException
import java.util.*

/**
 * The main plugin class
 */
class JobListings : JavaPlugin() {
    /**
     * The plugin's GUI manager
     */
    lateinit var spiGUI: SpiGUI
        private set

    /**
     * Economy Provider
     */
    lateinit var eco: Economy
        private set

    /**
     * Essentials
     */
    var essentials: Essentials? = null
        private set

    /**
     * Returns api if RedisChat is loaded
     */
    var redisChat: RedisChatAPI? = null
        private set

    /**
     * The command handler
     */
    private lateinit var handler: BukkitCommandHandler

    /**
     * The messages configuration
     */
    lateinit var messages: FileConfiguration
        private set

    /**
     * The gui configuration
     */
    lateinit var gui: FileConfiguration
        private set

    /**
     * The preset configuration
     */
    lateinit var presets: FileConfiguration
        private set

    private lateinit var cleanDatabase: BukkitTask

    override fun onEnable() {
        if (!PaperLib.isPaper()) {
            if (config.getBoolean("BypassPaperWarning")) {
                logger.warning("The paper warning has been bypassed.")
            } else {
                logger.severe("-----------------------------------")
                logger.severe("This plugin requires Paper to run!")
                logger.severe("Paper is a high-performance fork of Spigot.")
                logger.severe("Everything that works on Spigot works on Paper.")
                logger.severe("Learn more here: https://papermc.io/")
                logger.severe("")
                logger.severe("This warning can be bypassed but is NOT recommended.")
                logger.severe("Check the wiki: https://plugins.refractored.net")
                logger.severe("")
                logger.severe("The plugin will now disable itself.")
                logger.severe("-----------------------------------")
                throw IllegalStateException("Server is not running Paper.")
            }
        }

        // Set the instance
        instance = this

        spiGUI = SpiGUI(this)

        val pluginId = 22844
        val metrics: Metrics = Metrics(this, pluginId)

        saveDefaultConfig()

        val configFile = File(dataFolder, "config.yml")

        try {
            ConfigUpdater.update(this, "config.yml", configFile, listOf())
        } catch (e: IOException) {
            e.printStackTrace()
        }

        reloadConfig()

        if (!File(dataFolder, "messages.yml").exists()) {
            saveResource("messages.yml", false)
        }

        val messagesFile = File(dataFolder, "messages.yml")

        try {
            ConfigUpdater.update(this, "messages.yml", messagesFile, listOf())
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (!File(dataFolder, "gui.yml").exists()) {
            saveResource("gui.yml", false)
        }

        if (!File(dataFolder, "presets.yml").exists()) {
            saveResource("presets.yml", false)
        }

        // Load messages config
        messages = YamlConfiguration.loadConfiguration(dataFolder.resolve("messages.yml"))
        // Load gui config
        gui = YamlConfiguration.loadConfiguration(dataFolder.resolve("gui.yml"))
        // Load preset config
        presets = YamlConfiguration.loadConfiguration(dataFolder.resolve("presets.yml"))

        // Initialize the database
        Database.init()

        server.servicesManager.getRegistration(Economy::class.java)?.let {
            eco = it.provider
        } ?: run {
            logger.warning("A economy plugin not found! Disabling plugin.")
            server.pluginManager.disablePlugin(this)
            return
        }

        server.pluginManager.getPlugin("Essentials")?.let {
            essentials = (it as Essentials)
            logger.info("Hooked into Essentials")
        } ?: run {
            if (instance.config.getBoolean("Essentials.UseEssentialsMail") || instance.config.getBoolean("Essentials.UseIgnoreList")) {
                logger.warning("Essentials config options are enabled but Essentials is not found!")
                logger.warning("Please install Essentials or disable these options in the config.yml.")
                logger.warning("https://essentialsx.net/downloads.html")
            }
        }

        server.pluginManager.getPlugin("RedisChat")?.let {
            redisChat = RedisChatAPI.getAPI()
            logger.info("Hooked into RedisChat")
        }

        // Create command handler
        handler = BukkitCommandHandler.create(this)
        logger.info("I am aware of this deprecation message, it will be fixed in a future update.")

        // Register the command exception handler
        handler.setExceptionHandler(CommandErrorHandler())

//        handler.autoCompleter.registerSuggestion(
//            "presets",
//        ) { args: List<String?>?, sender: CommandActor?, command: ExecutableCommand? ->
//            return@registerSuggestion Presets
//                .getPresets()
//                .keys
//        }

        handler.autoCompleter.registerSuggestion(
            "materials",
        ) { args: List<String>, sender: CommandActor?, command: ExecutableCommand? ->
            val stringArgs = args.joinToString(" ").lowercase()

            val config = instance.config
            val blacklistedMaterials = config.getStringList("orders.BlacklistedMaterials")
            val additionalBlacklistedMaterials = config.getStringList("orders.BlacklistedCreateMaterials")
            val blacklist =
                (blacklistedMaterials + additionalBlacklistedMaterials)
                    .map { it.lowercase() }
                    .toSet()

            val materialSuggestions =
                Material.entries
                    .asSequence()
                    .map { it.name.lowercase() }
                    .filterNot { name -> name in blacklist.map { it.lowercase() } }
                    .toMutableSet()

//            val presetSuggestions = Presets.getPresets().keys

            return@registerSuggestion (materialSuggestions /*+ presetSuggestions*/)
                .filter { it.startsWith(stringArgs, ignoreCase = true) }
                .toMutableSet()
        }

        if (!instance.config.getBoolean("orders.CreateHand", true) && !instance.config.getBoolean("orders.CreateMaterial", true)) {
            logger.warning("You have disabled both order creation methods!")
            logger.warning("Please double check your config!")
        }
        // Register commands
        if (instance.config.getBoolean("orders.CreateHand", true)) {
            handler.register(CreateOrderHand())
        }
        if (instance.config.getBoolean("orders.CreateMaterial", true)) {
            handler.register(CreateOrderMaterial())
        }
        handler.register(OwnedOrders())
        handler.register(GetOrders())
        handler.register(ClaimedOrders())
        handler.register(CompleteOrders())
        handler.register(HelpCommand())
        handler.register(ReloadCommand())

        handler.enableAdventure()
//        handler.registerBrigadier()

        // Register listeners
        server.pluginManager.registerEvents(PlayerJoinListener(), this)

        cleanDatabase =
            server.scheduler.runTaskTimerAsynchronously(
                this,
                Runnable {
                    Order.updateExpiredOrders()
                    Order.updateDeadlineOrders()
                    Order.updatePickupDeadline()
                    Mail.purgeMail()
                },
                20L,
                40L,
            )

        logger.info("JobListings has been enabled!")
    }

    override fun onDisable() {
        if (this::handler.isInitialized) {
            handler.unregisterAllCommands()
        }
        if (this::cleanDatabase.isInitialized) {
            cleanDatabase.cancel()
        }

        logger.info("JobListings has been disabled!")
    }

    /**
     * Reload the plugin configuration
     */
    fun reload() {
        reloadConfig()
        messages = YamlConfiguration.loadConfiguration(dataFolder.resolve("messages.yml"))
        gui = YamlConfiguration.loadConfiguration(dataFolder.resolve("gui.yml"))
//        Presets.refreshPresets()
    }

    companion object {
        /**
         * The plugin's instance
         */
        lateinit var instance: JobListings
            private set
    }
}
