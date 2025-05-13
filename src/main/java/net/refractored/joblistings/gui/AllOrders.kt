package net.refractored.joblistings.gui

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.j256.ormlite.stmt.QueryBuilder
import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.menu.SGMenu
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.AMPERSAND_CHAR
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.gui.GuiHelper.loadCosmeticItems
import net.refractored.joblistings.gui.GuiHelper.loadNavButtons
import net.refractored.joblistings.order.tables.ClaimedOrder
import net.refractored.joblistings.order.tables.PendingOrder
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.math.ceil
import kotlin.times

class AllOrders(
    val player: Player,
) {
    private val config = JobListings.instance.gui.getConfigurationSection("AllOrders")!!

    private val rows = config.getInt("Rows", 6)

    private val orderSlots: List<Int> = config.getIntegerList("OrderSlots")

    private var pageCount: Int = ceil(Database.pendingOrderDao.countOf().toDouble() / orderSlots.count()).toInt().coerceAtLeast(1)

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
            JobListings.instance.launch {
                loadOrders(inventory.currentPage)
            }
        }

        loadNavButtons(config, gui, pageCount)
        loadCosmeticItems(config, gui, pageCount)

        JobListings.instance.launch {
            loadOrders(0)
        }
    }

    /**
     * Clears all non-stickied slots, and loads the orders for the requested page.
     * @param page The page to load orders for.
     */
    private suspend fun loadOrders(page: Int) {
        withContext(JobListings.instance.asyncDispatcher) {
            val orders = PendingOrder.getOrders(orderSlots.count(), page * orderSlots.count())

            withContext(JobListings.instance.minecraftDispatcher) {
                gui.clearAllButStickiedSlots()

                for ((index, slot) in orderSlots.withIndex()) {
                    val button: SGButton = orders.getOrNull(index)?.let { getOrderButton(it) } ?: GuiHelper.getFallbackButton(config)
                    gui.setButton(slot + GuiHelper.getOffset(page, rows), button)
                }

                gui.refreshInventory(player)
            }
        }
    }

    private fun getOrderButton(order: PendingOrder): SGButton {
        val item = order.item.clone()
        item.amount = minOf(order.itemAmount, item.maxStackSize)
        val itemMetaCopy = item.itemMeta
        val expireDuration = Duration.between(LocalDateTime.now(), order.expireTime)
        val expireDurationText =
            MessageUtil.getMessage(
                "General.DateFormat",
                listOf(
                    MessageReplacement(expireDuration.toDays().toString()),
                    MessageReplacement(expireDuration.toHoursPart().toString()),
                    MessageReplacement(expireDuration.toMinutesPart().toString()),
                ),
            )
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

        val orderItemLore =
            MessageUtil.getMessageList(
                "AllOrders.OrderItemLore",
                listOf(
                    MessageReplacement(order.reward.toString()),
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
        order: PendingOrder,
    ) {
        val existingOrder = Database.pendingOrderDao.queryForId(order.id)
        if (existingOrder == null) {
            event.whoClicked.closeInventory()
            event.whoClicked.sendMessage(
                MessageUtil.getMessage("General.OrderAlreadyClaimed"),
            )
            return
        }
        if (order.owner == event.whoClicked.uniqueId) {
            event.whoClicked.closeInventory()
            event.whoClicked.sendMessage(
                MessageUtil.getMessage("General.CannotAcceptOwnOrder"),
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
                        order.owner,
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
        val queryBuilder: QueryBuilder<ClaimedOrder, UUID> = Database.claimedOrderDao.queryBuilder()
        queryBuilder
            .where()
            .eq("assignee", event.whoClicked.uniqueId)
        val orders = Database.claimedOrderDao.query(queryBuilder.prepare())
        val maxOrdersAccepted = ClaimedOrder.getMaxOrdersAccepted(event.whoClicked as Player)
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

        order.markClaimed(event.whoClicked as Player)
        event.whoClicked.closeInventory()
    }

    companion object {
        /**
         * Creates an instance of the AllOrders class, and returns a working gui.
         * @return The gui.
         */
        fun getGUI(player: Player): SGMenu {
            val allOrders = AllOrders(player)
            return allOrders.gui
        }

        fun openGUI(player: Player) {
            player.openInventory(this.getGUI(player).inventory)
        }
    }
}
