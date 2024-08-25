package net.refractored.joblistings.gui

import com.j256.ormlite.stmt.QueryBuilder
import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.menu.SGMenu
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.AMPERSAND_CHAR
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.gui.GuiHelper.loadCosmeticItems
import net.refractored.joblistings.gui.GuiHelper.loadNavButtons
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.Order.Companion.getMaxOrdersAccepted
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.math.ceil

class AllOrders {
    private val config = JobListings.instance.gui.getConfigurationSection("AllOrders")!!

    private val rows = config.getInt("Rows", 6)

    private val orderSlots: List<Int> = config.getIntegerList("OrderSlots")

    private var pageCount: Int = ceil(orderDao.countOf().toDouble() / orderSlots.count()).toInt().coerceAtLeast(1)

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
            JobListings.instance.gui.getInt("AllOrders.Rows", 6),
        )

    init {
        gui.setOnPageChange { inventory ->
            inventory.clearAllButStickiedSlots()
            loadOrders(inventory.currentPage)
        }

        loadNavButtons(config, gui, pageCount)
        loadCosmeticItems(config, gui, pageCount)
        loadOrders(0)
    }

    /**
     * Clears all non-stickied slots, and loads the orders for the requested page.
     * @param page The page to load orders for.
     */
    private fun loadOrders(page: Int) {
        gui.clearAllButStickiedSlots()
        val orders = Order.getPendingOrders(orderSlots.count(), page * orderSlots.count())
        for ((index, slot) in orderSlots.withIndex()) {
            val button: SGButton = orders.getOrNull(index)?.let { getOrderButton(it) } ?: GuiHelper.getFallbackButton(config)
            gui.setButton(slot + GuiHelper.getOffset(page, rows), button)
        }
    }

    private fun getOrderButton(order: Order): SGButton {
        val item = order.item.clone()
        item.amount = minOf(order.itemAmount, item.maxStackSize)
        val itemMetaCopy = item.itemMeta
        val expireDuration = Duration.between(LocalDateTime.now(), order.timeExpires)
        val expireDurationText =
            MessageUtil.getMessage(
                "General.DateFormat",
                listOf(
                    MessageReplacement(expireDuration.toDays().toString()),
                    MessageReplacement(expireDuration.toHoursPart().toString()),
                    MessageReplacement(expireDuration.toMinutesPart().toString()),
                ),
            )
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

        val orderItemLore =
            MessageUtil.getMessageList(
                "AllOrders.OrderItemLore",
                listOf(
                    MessageReplacement(order.cost.toString()),
                    MessageReplacement(order.getOwner().name ?: "Unknown"),
                    MessageReplacement(createdDurationText),
                    MessageReplacement(expireDurationText),
                    MessageReplacement(order.itemAmount.toString()),
                ),
            )

        if (itemMetaCopy.hasLore()) {
            val itemLore = itemMetaCopy.lore()!!
            itemLore.addAll(orderItemLore)
            itemMetaCopy.lore(itemLore)
        } else {
            itemMetaCopy.lore(orderItemLore)
        }

        item.itemMeta = itemMetaCopy

        val button = SGButton(item)

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
        if (order.user == event.whoClicked.uniqueId) {
            event.whoClicked.closeInventory()
            event.whoClicked.sendMessage(
                MessageUtil.getMessage("General.CannotAcceptOwnOrder"),
            )
            return
        }
        if (order.status != OrderStatus.PENDING) {
            event.whoClicked.closeInventory()
            event.whoClicked.sendMessage(
                MessageUtil.getMessage("General.OrderAlreadyClaimed"),
            )
            return
        }
        if (order.isOrderExpired()) {
            event.whoClicked.closeInventory()
            event.whoClicked.sendMessage(
                MessageUtil.getMessage("General.OrderExpired"),
            )
            return
        }
        JobListings.instance.essentials?.let {
            if (JobListings.instance.config.getBoolean("Essentials.UseIgnoreList")) {
                val player =
                    it.users.load(
                        event.whoClicked.uniqueId,
                    )
                val owner =
                    it.users.load(
                        Bukkit.getOfflinePlayer(order.user).uniqueId,
                    )
                if (owner.isIgnoredPlayer(player) || player.isIgnoredPlayer(owner)) {
                    event.whoClicked.closeInventory()
                    event.whoClicked.sendMessage(
                        MessageUtil.getMessage("General.Ignored"),
                    )
                    return
                }
            }
        }
        val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
        queryBuilder
            .where()
            .eq("assignee", event.whoClicked.uniqueId)
            .and()
            .eq("status", OrderStatus.CLAIMED)
        val orders = orderDao.query(queryBuilder.prepare())
        val maxOrdersAccepted = getMaxOrdersAccepted(event.whoClicked as Player)
        if (orders.count() > maxOrdersAccepted) {
            event.whoClicked.closeInventory()
            event.whoClicked.sendMessage(
                MessageUtil.getMessage(
                    "AllOrders.OrderItemLore",
                    listOf(
                        MessageReplacement(
                            "$maxOrdersAccepted",
                        ),
                    ),
                ),
            )
        }

        order.acceptOrder(event.whoClicked as Player)
        event.whoClicked.closeInventory()
    }

    companion object {
        /**
         * Creates an instance of the AllOrders class, and returns a working gui.
         * @return The gui.
         */
        fun getGUI(): SGMenu {
            val allOrders = AllOrders()
            return allOrders.gui
        }
    }
}
