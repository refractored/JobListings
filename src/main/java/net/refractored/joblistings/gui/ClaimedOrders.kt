package net.refractored.joblistings.gui

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.menu.SGMenu
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import net.kyori.adventure.text.Component
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database.orderDao
import net.refractored.joblistings.gui.AllOrders.Companion.openGUIs
import net.refractored.joblistings.gui.GuiHelper.getFallbackButton
import net.refractored.joblistings.gui.GuiHelper.loadCosmeticItems
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.tables.ClaimedOrder
import net.refractored.joblistings.order.tables.FailedOrder
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import net.refractored.joblistings.util.Messages
import net.refractored.joblistings.util.Messages.miniToComponent
import net.refractored.joblistings.util.Messages.replace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import java.time.Duration
import java.time.LocalDateTime
import kotlin.text.replace

class ClaimedOrders(
    player: Player
) : OrdersGUI(player) {
    override val config = JobListings.instance.gui.getConfigurationSection("ClaimedOrders")!!

    override fun getName(): Component = (config.getString("Title") ?: "Title")
        .miniToComponent()
        .replace("%current_page%", (orderPage + 1).toString())
        .replace("%max_pages%", pageCount.toString())

    init {
        loadNavigation()
        loadCosmeticItems()

        JobListings.instance.launch {
            loadOrders(0)
            withContext(JobListings.instance.minecraftDispatcher) {
                gui.refreshInventory(player)
            }
        }

        gui.setOnClose {
            val player = this.player
            JobListings.instance.server.scheduler.runTaskLater(
                JobListings.instance,
                Runnable {
                    if (player.openInventory.topInventory.holder != gui.inventory.holder) {
                        openGUIs.remove(this)
                    }
                },
                1L,
            )
        }
    }

    /**
     * Clears all non-stickied slots, and loads the orders for the requested page.
     * @param page The page to load orders for.
     */
    override suspend fun loadOrders(page: Int) {
        gui.clearAllButStickiedSlots()

        val claimedOrders = ClaimedOrder.getOrders(orderSlots.count(), page * orderSlots.count(), player)
        for ((index, slot) in orderSlots.withIndex()) {
            val button: SGButton = claimedOrders.getOrNull(index)?.let { getOrderButton(it) } ?: getFallbackButton()
            gui.setButton(slot, button)
        }
        yield()
        if (!claimedOrders.isEmpty()) return
        val orders = FailedOrder.getOrders(orderSlots.count(), page * orderSlots.count(), player, FailedOrder.FailureType.INCOMPLETE)
        for ((index, slot) in orderSlots.withIndex()) {
            val button: SGButton = orders.getOrNull(index)?.let { getOrderButton(it) } ?: getFallbackButton()
            gui.setButton(slot, button)
        }
        // TODO: INCOMPLETE ORDERS
    }

    private fun getOrderButton(order: ClaimedOrder): SGButton {
        val displayItem = order.item.clone()
        displayItem.amount =
            if (order.itemAmount <= displayItem.maxStackSize) {
                order.itemAmount
            } else {
                displayItem.maxStackSize
            }
        val itemMetaCopy = displayItem.itemMeta
        val deadlineDuration = Duration.between(LocalDateTime.now(), order.expireTime)
        val createdDuration = Duration.between(order.creation, LocalDateTime.now())
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
            Messages
                .getString("ClaimedOrders.OrderItemLore")
                .replace("%0", order.reward.toString())
                .replace("%1", order.getOwner().toString())
                .replace("%2", createdDurationText)
                .replace("%3", deadlineDurationText)
                .replace("%4", order.itemAmount.toString())
                .replace("%5", order.amountTurnedIn.toString())
                .lines()
                .map { it.miniToComponent() }
//            } else {
//                Messages
//                    .getString("ClaimedOrders.OrderItemLoreIncomplete")
//                    .replace("%0", order.cost.toString())
//                    .replace("%1", Bukkit.getOfflinePlayer(order.user).name ?: "Unknown")
//                    .replace("%2", createdDurationText)
//                    .replace("%4", order.itemAmount.toString())
//                    .replace("%5", order.itemsReturned.toString())
//                    .lines()
//                    .map { it.miniToComponent() }
//            }

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
        order: ClaimedOrder
    ) {
        // TODO
        player.sendMessage { "<pink>:3".miniToComponent() }
//        when (order.status) {
//            OrderStatus.CLAIMED -> {
//                gui.removeButton(event.slot + getOffset(gui.currentPage, rows))
//                loadOrders(gui.currentPage)
//                order.status = OrderStatus.INCOMPLETE
//                order.incompleteOrder()
//            }
//            OrderStatus.INCOMPLETE -> {
//                val inventorySpaces =
//                    event.whoClicked.inventory.storageContents.count {
//                        it == null || (it.isSimilar(order.item) && it.amount < it.maxStackSize)
//                    }
//                if (inventorySpaces == 0) {
//                    event.whoClicked.closeInventory()
//                    event.whoClicked.sendMessage(
//                        Messages.getStringPrefixed("General.InventoryFull"),
//                    )
//                    return
//                }
//                if (order.itemCompleted == order.itemsReturned) {
//                    event.whoClicked.closeInventory()
//                    event.whoClicked.sendMessage(
//                        Messages.getStringPrefixed("ClaimedOrders.OrderAlreadyRefunded"),
//                    )
//                    return
//                }
//                if (giveRefundableItems(order, (event.whoClicked as Player))) {
//                    event.whoClicked.closeInventory()
//                    event.whoClicked.sendMessage(
//                        Messages.getStringPrefixed("ClaimedOrders.OrderFullyRefunded"),
//                    )
//                    gui.removeButton(event.slot + getOffset(gui.currentPage, rows))
//                    orderDao.delete(order)
//                    gui.refreshInventory(event.whoClicked)
//                } else {
//                    event.whoClicked.closeInventory()
//                    event.whoClicked.sendMessage(
//                        MessageUtil.getMessage(
//                            "ClaimedOrders.OrderPartiallyRefunded",
//                            listOf(
//                                MessageReplacement((order.itemCompleted - order.itemsReturned).toString()),
//                            ),
//                        ),
//                    )
//                }
//            }
//
//            else -> return
//        }
//        event.whoClicked.closeInventory()
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

        val openGUIs = mutableListOf<ClaimedOrders>()

        /**
         * Creates an instance of the AllOrders class, and returns a working gui.
         * @return The gui.
         */
        fun getGUI(player: Player): SGMenu {
            val claimedOrders = ClaimedOrders(player)
            openGUIs.add(claimedOrders)
            return claimedOrders.gui
        }

        fun openGUI(player: Player) {
            player.openInventory(this.getGUI(player).inventory)
        }

        fun refreshOpenGUIs() {
            JobListings.instance.launch {
                for (gui in AllOrders.Companion.openGUIs) {
                    gui.loadOrders(gui.orderPage)
                }
            }
        }
    }
}
