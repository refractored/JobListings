package net.refractored.joblistings

import com.earth2me.essentials.Essentials
import com.samjakob.spigui.SpiGUI
import net.milkbowl.vault.economy.Economy
import net.refractored.joblistings.commands.*
import net.refractored.joblistings.commands.autocompletion.MaterialResolver
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
import java.io.File

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
     * Returns true if eco is loaded
     */
    var ecoPlugin: Boolean = false
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
        // Set the instance
        instance = this

        spiGUI = SpiGUI(this)

        val pluginId = 22844
        val metrics: Metrics = Metrics(this, pluginId)

        // Save default configs
        saveDefaultConfig()

        if (!File(dataFolder, "messages.yml").exists()) {
            saveResource("messages.yml", false)
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
            // This probably would never get ran as the plugin.yml requires vault.
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

        server.pluginManager.getPlugin("eco")?.let {
            ecoPlugin = true
            logger.info("Hooked into eco")
        }

        // Create command handler
        handler = BukkitCommandHandler.create(this)

        // Register autocompletions
        val materialResolver = MaterialResolver()

        handler.autoCompleter.registerParameterSuggestions(Material::class.java, materialResolver)
        handler.registerBrigadier()

        // Register the command exception handler
        handler.setExceptionHandler(CommandErrorHandler())

        if (!instance.config.getBoolean("Orders.CreateHand", true) && !instance.config.getBoolean("Orders.CreateMaterial", true)) {
            logger.warning("You have disabled both order creation methods!")
            logger.warning("Please double check your config!")
        }
        // Register commands
        if (instance.config.getBoolean("Orders.CreateHand", true)) {
            handler.register(CreateOrderHand())
        }
        if (instance.config.getBoolean("Orders.CreateMaterial", true)) {
            handler.register(CreateOrderMaterial())
        }
        handler.register(OwnedOrders())
        handler.register(GetOrders())
        handler.register(ClaimedOrders())
        handler.register(CompleteOrders())
        handler.register(HelpCommand())
        handler.register(ReloadCommand())
        handler.register(CreatePreset())

        // Register listeners
        server.pluginManager.registerEvents(PlayerJoinListener(), this)

        cleanDatabase =
            server.scheduler.runTaskTimer(
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
    }

    companion object {
        /**
         * The plugin's instance
         */
        lateinit var instance: JobListings
            private set
    }
}
