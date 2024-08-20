package net.refractored.joblistings.commands.autocompletion

import net.refractored.joblistings.config.Presets
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class OrderStack(
    name: String,
) {
    val item: ItemStack? = Presets.getPreset(name) ?: Material.getMaterial(name)?.let { ItemStack(it) }
}
