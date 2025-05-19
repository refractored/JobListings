package net.refractored.joblistings

import com.earth2me.essentials.Essentials
import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.samjakob.spigui.SpiGUI
import com.tchristofferson.configupdater.ConfigUpdater
import dev.unnm3d.redischat.api.RedisChatAPI
import io.papermc.lib.PaperLib
import kotlinx.coroutines.*
import net.milkbowl.vault.economy.Economy
import net.refractored.joblistings.commands.*
import net.refractored.joblistings.commands.annotations.*
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.listeners.PlayerJoinListener
import net.refractored.joblistings.mail.Mail
import net.refractored.joblistings.order.Order
import org.bstats.bukkit.Metrics
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import revxrsal.commands.Lamp
import revxrsal.commands.bukkit.BukkitLamp
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import java.io.File
import java.io.IOException

/**
 * The main plugin class
 */
class JobListings : SuspendingJavaPlugin() {
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
    private lateinit var lamp: Lamp<BukkitCommandActor>

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

    private lateinit var cleanDatabase: Job

    override suspend fun onEnableAsync() {
        if (!PaperLib.isPaper()) {
            if (config.getBoolean("BypassPaperWarning")) {
                logger.warning("The paper warning has been bypassed.")
            } else {
                logger.severe("-----------------------------------")
                logger.severe("This plugin recommends paper!")
                logger.severe("Paper is a high-performance fork of Spigot.")
                logger.severe("Everything that works on Spigot works on Paper.")
                logger.severe("Learn more here: https://papermc.io/")
                logger.severe("")
                logger.severe("This warning can be disabled but is NOT recommended.")
                logger.severe("Check the wiki: https://plugins.refractored.net")
                logger.severe("-----------------------------------")
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

        // Load messages config
        messages = YamlConfiguration.loadConfiguration(dataFolder.resolve("messages.yml"))
        // Load gui config
        gui = YamlConfiguration.loadConfiguration(dataFolder.resolve("gui.yml"))

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

        lamp =
            BukkitLamp
                .builder(this)
                // TODO: Setup command error handler.
                // .exceptionHandler(CommandErrorHandler())
                .annotationReplacer(ConfigCommand::class.java, CommandPrefixConfigReplacer())
                .annotationReplacer(ConfigDescription::class.java, ConfigDescriptionReplacer())
                .annotationReplacer(ConfigRange::class.java, ConfigRangeReplacer())
                .build()

        // Register commands
        lamp.register(CreateOrder())
        lamp.register(OwnedOrders())
        lamp.register(GetOrders())
        if (messages.getBoolean("messages.orders.enable-prefix-command")) {
            lamp.register(GetOrdersBlank())
        }
        lamp.register(ClaimedOrders())
        lamp.register(CompleteOrders())
        lamp.register(HelpCommand())
        lamp.register(ReloadCommand())

        // Register listeners
        server.pluginManager.registerSuspendingEvents(PlayerJoinListener(), this)

        cleanDatabase = launch { runDatabaseCleaner() }

        logger.info("JobListings has been enabled!")
    }

    suspend fun runDatabaseCleaner() {
        withContext(Dispatchers.IO) {
            while (coroutineContext.isActive) {
                Order.updateExpiredOrders()
                yield()
                Order.updateDeadlineOrders()
                yield()
                Order.updatePickupDeadline()
                yield()
                Mail.purgeMail()
                delay(1000L * 5L)
            }
        }
    }

    override suspend fun onDisableAsync() {
        if (this::lamp.isInitialized) {
            lamp.unregisterAllCommands()
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
