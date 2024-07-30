package net.refractored.joblistings.gui

import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.menu.SGMenu
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.AMPERSAND_CHAR
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import revxrsal.commands.bukkit.player
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.math.ceil

class ClaimedOrders(
    player: Player,
) {
    val gui: SGMenu =
        JobListings.instance.spiGUI.create(
            // Me when no component support :((((
            LegacyComponentSerializer.legacy(AMPERSAND_CHAR).serialize(
                MessageUtil.replaceMessage(
                    config.getString("Title")!!,
                    listOf(
                        // I only did this for consistency in the messages.yml
                        MessageReplacement("{currentPage}"),
                        MessageReplacement("{maxPage}"),
                    ),
                ),
            ),
            config.getInt("Rows", 6),
        )

    init {
        gui.setOnPageChange { inventory ->
            inventory.clearAllButStickiedSlots()
            loadOrders(inventory.currentPage, player)
        }
        loadNavButtons()
        loadCosmeticItems()
        loadOrders(0, player)
    }

    private val config
        get() = JobListings.instance.gui.getConfigurationSection("ClaimedOrders")!!

    private val rows
        get() = config.getInt("Rows", 6)

    private val orderSlots: List<Int>
        get() = config.getIntegerList("OrderSlots")

    private val pageCount
        get() =
            ceil(orderDao.countOf().toDouble() / orderSlots.count()).toInt().let {
                if (it > 0) it else 1
            }

    /**
     * Gets the offset for the page number, based on the rows set in config.
     * @param page The page.
     * @return The number of slots required for the page.
     */
    private fun getOffset(page: Int): Int = page * (rows * 9)

    /**
     * Clears all non-stickied slots, and loads the orders for the requested page.
     * @param page The page to load orders for.
     */
    private fun loadOrders(
        page: Int,
        player: Player,
    ) {
        gui.clearAllButStickiedSlots()
        val orders = Order.getPlayerAcceptedOrders(orderSlots.count(), page * orderSlots.count(), player.uniqueId)
        for ((index, slot) in orderSlots.withIndex()) {
            val button: SGButton = orders.getOrNull(index)?.let { getOrderButton(it) } ?: getFallbackButton()
            gui.setButton(slot + getOffset(page), button)
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

    private fun getFallbackButton(): SGButton {
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

    private fun getOrderButton(order: Order): SGButton {
        val displayItem = order.item.clone()
        displayItem.amount =
            if (order.itemAmount <= displayItem.maxStackSize) {
                order.itemAmount
            } else {
                displayItem.maxStackSize
            }
        val itemMetaCopy = displayItem.itemMeta
        val deadlineDuration = Duration.between(LocalDateTime.now(), order.timeDeadline)
        val createdDuration = Duration.between(order.timeCreated, LocalDateTime.now())
        val createdDurationText =
            MessageUtil.getMessage(
                "General.DatePastTense",
                listOf(
                    MessageReplacement(createdDuration.toDays().toString()),
                    MessageReplacement(createdDuration.toHoursPart().toString()),
                    MessageReplacement(createdDuration.toMinutesPart().toString()),
                ),
            )
        val deadlineDurationText =
            MessageUtil.getMessage(
                "General.DateFormat",
                listOf(
                    MessageReplacement(deadlineDuration.toDays().toString()),
                    MessageReplacement(deadlineDuration.toHoursPart().toString()),
                    MessageReplacement(deadlineDuration.toMinutesPart().toString()),
                ),
            )
        val infoLore =
            if (order.status != OrderStatus.INCOMPLETE) {
                MessageUtil.getMessageList(
                    "AllOrders.OrderItemLore",
                    listOf(
                        MessageReplacement(order.cost.toString()),
                        MessageReplacement(Bukkit.getOfflinePlayer(order.user).name ?: "Unknown"),
                        MessageReplacement(createdDurationText),
                        MessageReplacement(deadlineDurationText),
                        MessageReplacement(order.itemAmount.toString()),
                        MessageReplacement(order.itemCompleted.toString()),
                    ),
                )
            } else {
                MessageUtil.getMessageList(
                    "AllOrders.OrderItemLoreIncomplete",
                    listOf(
                        MessageReplacement(order.cost.toString()),
                        MessageReplacement(Bukkit.getOfflinePlayer(order.user).name ?: "Unknown"),
                        MessageReplacement(createdDurationText),
                        MessageReplacement(order.itemAmount.toString()),
                        MessageReplacement(order.itemsReturned.toString()),
                    ),
                )
            }

        if (itemMetaCopy.hasLore()) {
            val itemLore = itemMetaCopy.lore()!!
            itemLore.addAll(infoLore)
            itemMetaCopy.lore(itemLore)
        } else {
            itemMetaCopy.lore(infoLore)
        }

        displayItem.itemMeta = itemMetaCopy

        val button = SGButton(displayItem)

        button.setListener { event: InventoryClickEvent ->
            clickOrder(event, order)
        }
        return button
    }

    /**
     * Handles the click event for an order.
     * @param event The click event.
     * @param order The order.
     */
    private fun clickOrder(
        event: InventoryClickEvent,
        order: Order,
    ) {
        when (order.status) {
            OrderStatus.INCOMPLETE -> {
                val inventorySpaces =
                    event.whoClicked.inventory.storageContents.count {
                        it == null || (it.isSimilar(order.item) && it.amount < it.maxStackSize)
                    }
                if (inventorySpaces == 0) {
                    event.whoClicked.closeInventory()
                    event.whoClicked.sendMessage(
                        MessageUtil.getMessage("General.InventoryFull"),
                    )
                    return
                }
                if (order.itemCompleted == order.itemsReturned) {
                    event.whoClicked.closeInventory()
                    event.whoClicked.sendMessage(
                        MessageUtil.getMessage("ClaimedOrders.OrderAlreadyRefunded"),
                    )
                    return
                }
                if (giveRefundableItems(order, (event.whoClicked as Player))) {
                    event.whoClicked.closeInventory()
                    event.whoClicked.sendMessage(
                        MessageUtil.getMessage("ClaimedOrders.OrderFullyRefunded"),
                    )
                    gui.removeButton(event.slot + getOffset(gui.currentPage))
                    orderDao.delete(order)
                    gui.refreshInventory(event.whoClicked)
                } else {
                    event.whoClicked.closeInventory()
                    event.whoClicked.sendMessage(
                        MessageUtil.getMessage(
                            "ClaimedOrders.OrderPartiallyRefunded",
                            listOf(
                                MessageReplacement((order.itemCompleted - order.itemsReturned).toString()),
                            ),
                        ),
                    )
                }
            }

            else -> return
        }
        event.whoClicked.closeInventory()
    }

    /**
     * Refunds the items to the assignee of the order
     * This also updates the order itemsReturned
     * @return true if all items have been refunded
     */
    private fun giveRefundableItems(
        order: Order,
        player: Player,
    ): Boolean {
        var itemsLeft = order.itemCompleted - order.itemsReturned
        while (itemsLeft > 0) {
            if (player.inventory.storageContents.count {
                    it == null || (it.isSimilar(order.item) && it.amount < it.maxStackSize)
                } == 0
            ) {
                break
            }
            val itemAmount =
                if (itemsLeft < order.item.maxStackSize) {
                    itemsLeft
                } else {
                    order.item.maxStackSize
                }
            itemsLeft -= itemAmount
            val item =
                order.item.clone().apply {
                    amount = itemAmount
                }
            val excessItems = player.inventory.addItem(item)
            itemsLeft += excessItems.values.sumOf { it.amount }
        }
        order.itemsReturned = order.itemCompleted - itemsLeft
        orderDao.update(order)
        return (order.itemsReturned == order.itemCompleted)
    }

    private fun loadNavButtons() {
        val navKeys =
            listOf(
                config.getConfigurationSection("NextPage")!!,
                config.getConfigurationSection("PreviousPage")!!,
            )
        navKeys.forEach {
            val button =
                SGButton(
                    generateItem(
                        Material.valueOf(
                            it.getString("Material", "BEDROCK")!!,
                        ),
                        it.getInt("Amount"),
                        it.getInt("ModelData"),
                        it.getString("Name") ?: "null",
                        it.getStringList("Amount").map { line ->
                            MessageUtil.toComponent(line).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                        },
                    ),
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
                val offset = getOffset(i)
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
    private fun loadCosmeticItems() {
        val section = config.getConfigurationSection("Items")!!
        val keys = section.getKeys(false)
        for (key in keys) {
            val subsection = section.getConfigurationSection(key)!!
            for (i in 0..<pageCount) {
                val offset = getOffset(i)
                section.getIntegerList("$key.Slots").forEach {
                    gui.setButton(
                        it + offset,
                        SGButton(
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
                            ),
                        ),
                    )
                    gui.stickSlot(it + offset)
                }
            }
        }
    }

    companion object {
        /**
         * Creates an instance of the AllOrders class, and returns a working gui.
         * @return The gui.
         */
        fun getGUI(player: Player): SGMenu {
            val claimedOrders = ClaimedOrders(player)
            return claimedOrders.gui
        }
    }
}
