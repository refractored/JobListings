package net.refractored.joblistings.gui

import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.menu.SGMenu
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

object GuiHelper {
    /**
     * Generate Item from data
     * @return The generated Itemstack
     */
    private fun generateItem(
        material: Material,
        amount: Int,
        modelData: Int,
        name: String,
        lore: List<Component>,
    ): ItemStack {
        val item =
            ItemStack(
                material,
            )
        if (item.type == Material.AIR) return item
        item.amount = amount
        val itemMeta = item.itemMeta
        itemMeta.setCustomModelData(
            modelData,
        )
        itemMeta.displayName(
            MessageUtil
                .toComponent(
                    name,
                ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
        )
        item.itemMeta = itemMeta
        item.lore(
            lore,
        )
        return item
    }

    private fun generateItem(subsection: ConfigurationSection): ItemStack =
        generateItem(
            Material.valueOf(
                subsection.getString("Material", "BEDROCK")!!,
            ),
            subsection.getInt("Amount"),
            subsection.getInt("ModelData"),
            subsection.getString("Name") ?: "null",
            subsection.getStringList("Amount").map { line ->
                MessageUtil.toComponent(line).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            },
        )

    /**
     * Get the Fallback Button
     * @return The Fallback Button
     */
    fun getFallbackButton(config: ConfigurationSection): SGButton {
        val fallbackConfig = config.getConfigurationSection("FallbackItem")!!
        val item =
            ItemStack(
                Material.valueOf(
                    fallbackConfig.getString("Material") ?: "BEDROCK",
                ),
            )
        if (item.type == Material.AIR) return SGButton(item)
        item.amount = fallbackConfig.getInt("Amount")
        val itemMeta = item.itemMeta
        itemMeta.setCustomModelData(
            fallbackConfig.getInt("ModelData"),
        )
        itemMeta.displayName(
            MessageUtil
                .toComponent(
                    fallbackConfig.getString("Name") ?: "null",
                ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
        )
        item.itemMeta = itemMeta
        item.lore(
            fallbackConfig.getStringList("Amount").map { line ->
                MessageUtil.toComponent(line).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            },
        )
        return SGButton(item)
    }

    fun getOffset(
        page: Int,
        rows: Int,
    ): Int = page * (rows * 9)

    fun loadNavButtons(
        config: ConfigurationSection,
        gui: SGMenu,
        pageCount: Int,
    ) {
        val navKeys =
            listOf(
                config.getConfigurationSection("NextPage")!!,
                config.getConfigurationSection("PreviousPage")!!,
            )
        navKeys.forEach {
            val button =
                SGButton(
                    generateItem(it),
                )
            when (it.name) {
                "NextPage" -> {
                    button.setListener { event -> gui.nextPage(event.whoClicked) }
                }
                "PreviousPage" -> {
                    button.setListener { event -> gui.previousPage(event.whoClicked) }
                }
            }
            for (i in 0..<pageCount) {
                val offset = getOffset(i, config.getInt("Rows", 6))
                it.getIntegerList("Slots").forEach { slot ->
                    gui.setButton(
                        slot + offset,
                        button,
                    )
                    gui.stickSlot(slot + offset)
                }
            }
        }
    }

    /**
     * Loads all the "cosmetic" items in the Items section of the config.
     */
    fun loadCosmeticItems(
        config: ConfigurationSection,
        gui: SGMenu,
        pageCount: Int,
    ) {
        val section = config.getConfigurationSection("Items")!!
        val keys = section.getKeys(false)
        for (key in keys) {
            val subsection = section.getConfigurationSection(key)!!
            for (i in 0..<pageCount) {
                val offset = getOffset(i, config.getInt("Rows", 6))
                section.getIntegerList("$key.Slots").forEach {
                    gui.setButton(
                        it + offset,
                        SGButton(
                            generateItem(subsection),
                        ),
                    )
                    gui.stickSlot(it + offset)
                }
            }
        }
    }
}
