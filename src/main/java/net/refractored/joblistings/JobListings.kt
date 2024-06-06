package net.refractored.joblistings

import com.earth2me.essentials.Essentials
import com.samjakob.spigui.SpiGUI
import net.milkbowl.vault.economy.Economy
import net.refractored.joblistings.commands.*
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.listeners.PlayerJoinListener
import net.refractored.joblistings.mail.Mail
import net.refractored.joblistings.order.Order
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import revxrsal.commands.bukkit.BukkitCommandHandler

/**
 * The main plugin class
 */
class JobListings : JavaPlugin() {
    /**
     * The command handler
     */
    private lateinit var handler: BukkitCommandHandler

    /**
     * The messages configuration
     */
    lateinit var messages: FileConfiguration
        private set

    private lateinit var cleanDatabase: BukkitTask

    override fun onEnable() {
        // Set the instance
        instance = this

        spiGUI = SpiGUI(this);

        // Save default configs
        saveDefaultConfig()
        saveResource("messages.yml", false)

        // Load messages config
        messages = YamlConfiguration.loadConfiguration(dataFolder.resolve("messages.yml"))

        // Initialize the database
        Database.init()

        server.servicesManager.getRegistration(Economy::class.java)?.let {
            eco = it.provider
        } ?: run {
            logger.warning("Economy plugin not found! Disabling plugin.")
            server.pluginManager.disablePlugin(this)
            return
        }

        server.pluginManager.getPlugin("Essentials")?.let {
            essentials = (it as Essentials)
            logger.info("Hooked into Essentials!")
        }

        // Create command handler
        handler = BukkitCommandHandler.create(this)

        // Register commands
        handler.register(CreateOrderHand())
        handler.register(CreateOrderMaterial())
        handler.register(ViewOrder())
        handler.register(GetOrders())
        handler.register(ClaimedOrders())
        handler.register(CompleteOrders())
        handler.register(HelpCommand())
        handler.registerBrigadier()

        // Register listeners
        server.pluginManager.registerEvents(PlayerJoinListener(), this)

        cleanDatabase = server.scheduler.runTaskTimer(this, Runnable {
            Order.updateExpiredOrders()
            Order.updateDeadlineOrders()
            Mail.purgeMail()
        }, 20L, 20L)

        logger.info("JobListings has been enabled!")
    }

    override fun onDisable() {
        handler.unregisterAllCommands()
        cleanDatabase.cancel()
        logger.info("JobListings has been disabled!")
    }

    /**
     * Reload the plugin configuration
     */
    fun reload() {
        reloadConfig()
        messages = YamlConfiguration.loadConfiguration(dataFolder.resolve("messages.yml"))
    }

    companion object {

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
         * The plugin's instance
         */
        lateinit var instance: JobListings
            private set

        /**
         * The plugin's GUI manager
         */
        lateinit var spiGUI: SpiGUI
            private set
    }
}