package net.refractored.joblistings.config

import net.refractored.joblistings.JobListings
import org.bukkit.inventory.ItemStack

class Presets(
    /**
     * The name of the Gamemode
     */
    val name: String,
    /**
     * The itemstack data
     */
    val item: ItemStack,
) {
    companion object {
        @JvmStatic
        val gamemodes = mutableMapOf<String, Presets>()

        /**
         * Create a new queue and add it to the map of queues
         * @return The gamemode that was created
         * @param name The name of the queue
         * @param matchData The data that will be applied to any match created from this queue
         */
        @JvmStatic
        fun createPreset(
            name: String,
            item: ItemStack,
        ): Presets {
            val preset = Presets(name, item)
            gamemodes[name] = preset
            return preset
        }

        /**
         * Replaces and deletes all gamemodes with the ones in the config.
         * This does the same with queues.
         * If "create-queue" is true, a queue will be created for that gamemode.
         */
        fun refreshPresets() {
            gamemodes.clear()
            val config = JobListings.instance.presets
            val section = config.getConfigurationSection("")
            val keys = section!!.getKeys(false)
            for (key in keys) {
                createPreset(key, config.getItemStack(key)!!)
            }
        }
    }
}
