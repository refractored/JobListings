package net.refractored.joblistings.gui

import com.github.shynixn.mccoroutine.bukkit.launch
import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.menu.SGMenu
import net.kyori.adventure.text.Component
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.messages.Messages.fixItalics
import net.refractored.joblistings.messages.Messages.miniToComponent
import net.refractored.joblistings.messages.Messages.toLegacy
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
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
        lore: List<Component>
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
            name.miniToComponent().fixItalics(),
        )
        item.itemMeta = itemMeta
        item.lore(
            lore,
        )
        return item
    }

    fun generateItem(subsection: ConfigurationSection): ItemStack = generateItem(
        Material.valueOf(
            subsection.getString("Material") ?: "BEDROCK",
        ),
        subsection.getInt("Amount"),
        subsection.getInt("ModelData"),
        subsection.getString("Name") ?: "null",
        subsection.getStringList("Lore").map { line ->
            line.miniToComponent().fixItalics()
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
            (fallbackConfig.getString("Name") ?: "null").miniToComponent().fixItalics(),
        )
        item.itemMeta = itemMeta
        item.lore(
            fallbackConfig.getStringList("Amount").map { line ->
                line.miniToComponent().fixItalics()
            },
        )
        return SGButton(item)
    }

    fun getOffset(
        page: Int,
        rows: Int
    ): Int = page * (rows * 9)

    fun loadNavButtons(
        config: ConfigurationSection,
        gui: SGMenu,
        pageCount: Int
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
        pageCount: Int
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

abstract class OrdersGUI(
    val player: Player
) {
    abstract val config: ConfigurationSection

    val rows: Int by lazy { config.getInt("Rows", 6) }

    val orderSlots: List<Int> by lazy { config.getIntegerList("OrderSlots") }

    var pageCount: Int = 1

    var orderPage: Int = 0

    val gui: SGMenu by lazy {
        JobListings.instance.spiGUI.create(
            getName().toLegacy(),
            rows,
        )
    }

    abstract fun getName(): Component

    abstract suspend fun loadOrders(page: Int)

    fun loadNavigation() {
        val navKeys =
            listOf(
                config.getConfigurationSection("NextPage")!!,
                config.getConfigurationSection("PreviousPage")!!,
            )
        navKeys.forEach { configKey ->
            val button =
                SGButton(
                    generateItem(configKey),
                )
            when (configKey.name) {
                "NextPage" -> {
                    button.setListener { event ->
                        val nextPage = orderPage + 1
                        if (nextPage > pageCount - 1) {
                            return@setListener
                        }
                        orderPage = nextPage
                        JobListings.instance.launch {
                            loadOrders(nextPage)
                        }
                    }
                }
                "PreviousPage" -> {
                    button.setListener { event ->
                        if (orderPage <= 0) {
                            return@setListener
                        }
                        orderPage--
                        JobListings.instance.launch {
                            loadOrders(gui.currentPage - 1)
                        }
                    }
                }
            }
            configKey.getIntegerList("Slots").forEach { slot ->
                gui.setButton(
                    slot,
                    button,
                )
                gui.stickSlot(slot)
            }
        }
    }

    /**
     * Get the Fallback Button
     * @return The Fallback Button
     */
    fun getFallbackButton(): SGButton {
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
            (fallbackConfig.getString("Name") ?: "null").miniToComponent().fixItalics(),
        )
        item.itemMeta = itemMeta
        item.lore(
            fallbackConfig.getStringList("Amount").map { line ->
                line.miniToComponent().fixItalics()
            },
        )
        return SGButton(item)
    }

    /**
     * Loads all the "cosmetic" items in the Items section of the config.
     */
    fun loadCosmeticItems() {
        val section = config.getConfigurationSection("Items")!!
        val keys = section.getKeys(false)
        for (key in keys) {
            val subsection = section.getConfigurationSection(key)!!
            section.getIntegerList("$key.Slots").forEach {
                gui.setButton(
                    it,
                    SGButton(
                        GuiHelper.generateItem(subsection),
                    ),
                )
                gui.stickSlot(it)
            }
        }
    }

    /**
     * Generate Item from data
     * @return The generated Itemstack
     */
    private fun generateItem(
        material: Material,
        amount: Int,
        modelData: Int,
        name: String,
        lore: List<Component>
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
            name.miniToComponent().fixItalics(),
        )
        item.itemMeta = itemMeta
        item.lore(
            lore,
        )
        return item
    }

    fun generateItem(subsection: ConfigurationSection): ItemStack = generateItem(
        Material.valueOf(
            subsection.getString("Material") ?: "BEDROCK",
        ),
        subsection.getInt("Amount"),
        subsection.getInt("ModelData"),
        subsection.getString("Name") ?: "null",
        subsection.getStringList("Lore").map { line ->
            (line.miniToComponent()).fixItalics()
        },
    )
}
