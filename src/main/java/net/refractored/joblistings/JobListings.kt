package net.refractored.joblistings

import com.samjakob.spigui.SpiGUI
import net.milkbowl.vault.economy.Economy
import net.refractored.joblistings.commands.CreateOrderHand
import net.refractored.joblistings.commands.CreateOrderMaterial
import net.refractored.joblistings.commands.GetOrders
import net.refractored.joblistings.commands.ViewOrder
import net.refractored.joblistings.database.Database
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
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

        if (!setupEconomy()) {
            logger.warning("Economy plugin not found! Disabling plugin.")
            server.pluginManager.disablePlugin(this)
            return
        }

        // Create command handler
        handler = BukkitCommandHandler.create(this)

        // Register commands
        handler.register(CreateOrderHand())
        handler.register(CreateOrderMaterial())
        handler.register(ViewOrder())
        handler.register(GetOrders())
        handler.registerBrigadier()


        logger.info("JobListings has been enabled!")
    }

    override fun onDisable() {
        handler.unregisterAllCommands()
        logger.info("Shuffled has been disabled!")
    }

    /**
     * Reload the plugin configuration
     */
    fun reload() {
        reloadConfig()
        messages = YamlConfiguration.loadConfiguration(dataFolder.resolve("messages.yml"))
    }

    private fun setupEconomy(): Boolean {
        val rsp = server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            logger.warning("Economy service is not registered! Make sure an economy plugin is installed.")
            return false
        }

        eco = rsp.provider
        return true
    }

    companion object {

        /**
         * Economy Provider
         */
        lateinit var eco: Economy

        /**
         * The plugin's instance
         */
        lateinit var instance: JobListings
            private set
        /**
         * The plugin's instance
         */
        lateinit var spiGUI: SpiGUI
            private set
    }
}