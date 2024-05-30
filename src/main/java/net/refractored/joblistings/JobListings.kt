package net.refractored.joblistings

import com.samjakob.spigui.SpiGUI
import net.refractored.joblistings.commands.CreateOrder
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

//        handler.autoCompleter.registerParameterSuggestions(Card::class.java, "cards")

        // Register Listeners

        // Register Cards



        // Create command handler
        handler = BukkitCommandHandler.create(this)

        // Register commands
        handler.register(CreateOrder())
        handler.register(ViewOrder())


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

    companion object {

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