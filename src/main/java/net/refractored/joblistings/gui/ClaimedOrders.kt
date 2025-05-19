package net.refractored.joblistings.gui

import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.menu.SGMenu
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database.orderDao
import net.refractored.joblistings.gui.GuiHelper.getFallbackButton
import net.refractored.joblistings.gui.GuiHelper.getOffset
import net.refractored.joblistings.gui.GuiHelper.loadCosmeticItems
import net.refractored.joblistings.gui.GuiHelper.loadNavButtons
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import net.refractored.joblistings.util.Messages
import net.refractored.joblistings.util.Messages.miniToComponent
import net.refractored.joblistings.util.Messages.replace
import net.refractored.joblistings.util.Messages.toLegacy
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.ceil

class ClaimedOrders(
    player: Player
) {
    private val config = JobListings.instance.gui.getConfigurationSection("ClaimedOrders")!!

    private val rows = config.getInt("Rows", 6)

    private val orderSlots: List<Int> = config.getIntegerList("OrderSlots")

    private val pageCount: Int =
        ceil(
            orderDao
                .queryBuilder()
                .where()
                .eq("assignee", player.uniqueId)
                .and()
                .eq("status", OrderStatus.CLAIMED)
                .or()
                .eq("status", OrderStatus.INCOMPLETE)
                .countOf()
                .toDouble() / orderSlots.count(),
        ).toInt().coerceAtLeast(1)

    val gui: SGMenu =
        JobListings.instance.spiGUI.create(
            // Me when no component support :((((
            (config.getString("Title") ?: "Title")
                .replace("%0", "{currentPage}")
                .replace("%1", "{maxPage}")
                .miniToComponent()
                .toLegacy(),
            config.getInt("Rows", 6),
        )

    init {
        gui.setOnPageChange { inventory ->
            inventory.clearAllButStickiedSlots()
            loadOrders(inventory.currentPage, player)
        }
        loadNavButtons(config, gui, pageCount)
        loadCosmeticItems(config, gui, pageCount)
        loadOrders(0, player)
    }

    /**
     * Clears all non-stickied slots, and loads the orders for the requested page.
     * @param page The page to load orders for.
     */
    private fun loadOrders(
        page: Int,
        player: Player
    ) {
        gui.clearAllButStickiedSlots()
        val orders = Order.getPlayerAcceptedOrders(orderSlots.count(), page * orderSlots.count(), player.uniqueId)
        for ((index, slot) in orderSlots.withIndex()) {
            val button: SGButton = orders.getOrNull(index)?.let { getOrderButton(it) } ?: getFallbackButton(config)
            gui.setButton(slot + getOffset(page, rows), button)
        }
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
                Messages
                    .getString("ClaimedOrders.OrderItemLore")
                    .replace("%0", order.cost.toString())
                    .replace("%1", Bukkit.getOfflinePlayer(order.user).name ?: "Unknown")
                    .replace("%2", createdDurationText)
                    .replace("%3", deadlineDurationText)
                    .replace("%4", order.itemAmount.toString())
                    .replace("%5", order.itemCompleted.toString())
                    .lines()
                    .map { it.miniToComponent() }
            } else {
                Messages
                    .getString("ClaimedOrders.OrderItemLoreIncomplete")
                    .replace("%0", order.cost.toString())
                    .replace("%1", Bukkit.getOfflinePlayer(order.user).name ?: "Unknown")
                    .replace("%2", createdDurationText)
                    .replace("%4", order.itemAmount.toString())
                    .replace("%5", order.itemsReturned.toString())
                    .lines()
                    .map { it.miniToComponent() }
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
        order: Order
    ) {
        when (order.status) {
            OrderStatus.CLAIMED -> {
                gui.removeButton(event.slot + getOffset(gui.currentPage, rows))
                loadOrders(gui.currentPage, event.whoClicked as Player)
                order.status = OrderStatus.INCOMPLETE
                order.incompleteOrder()
            }
            OrderStatus.INCOMPLETE -> {
                val inventorySpaces =
                    event.whoClicked.inventory.storageContents.count {
                        it == null || (it.isSimilar(order.item) && it.amount < it.maxStackSize)
                    }
                if (inventorySpaces == 0) {
                    event.whoClicked.closeInventory()
                    event.whoClicked.sendMessage(
                        Messages.getStringPrefixed("General.InventoryFull"),
                    )
                    return
                }
                if (order.itemCompleted == order.itemsReturned) {
                    event.whoClicked.closeInventory()
                    event.whoClicked.sendMessage(
                        Messages.getStringPrefixed("ClaimedOrders.OrderAlreadyRefunded"),
                    )
                    return
                }
                if (giveRefundableItems(order, (event.whoClicked as Player))) {
                    event.whoClicked.closeInventory()
                    event.whoClicked.sendMessage(
                        Messages.getStringPrefixed("ClaimedOrders.OrderFullyRefunded"),
                    )
                    gui.removeButton(event.slot + getOffset(gui.currentPage, rows))
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
        player: Player
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

    companion object {
        /**
         * Creates an instance of the ClaimedOrders class, and returns a working gui.
         * @return The gui.
         */
        fun getGUI(player: Player): SGMenu {
            val claimedOrders = ClaimedOrders(player)
            return claimedOrders.gui
        }
    }
}
