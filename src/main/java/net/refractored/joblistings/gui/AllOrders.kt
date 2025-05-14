package net.refractored.joblistings.gui

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.menu.SGMenu
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.gui.GuiHelper.generateItem
import net.refractored.joblistings.order.tables.ClaimedOrder
import net.refractored.joblistings.order.tables.PendingOrder
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import net.refractored.joblistings.util.Messages.toLegacy
import org.bukkit.configuration.ConfigurationSection
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

    private var pageCount: Int = 1

    private var orderPage: Int = 0

    private fun getName(): Component =
        MessageUtil.replaceMessage(
            config.getString("Title")!!,
            listOf(
                MessageReplacement((orderPage + 1).toString()),
                MessageReplacement(pageCount.toString()),
            ),
        )

    val gui: SGMenu =
        JobListings.instance.spiGUI.create(
            getName().toLegacy(),
            rows,
        )

    init {
        experimentLoadNavButtons(config, gui)
        GuiHelper.loadCosmeticItems(config, gui, 1)

        JobListings.instance.launch {
            loadOrders(0)
            withContext(JobListings.instance.minecraftDispatcher) {
                gui.refreshInventory(player)
            }
        }

        JobListings.instance.launch {
            withContext(JobListings.instance.asyncDispatcher) {
                pageCount = ceil(Database.pendingOrderDao.countOf().toDouble() / orderSlots.count()).toInt().coerceAtLeast(1)

                withContext(JobListings.instance.minecraftDispatcher) {
                    gui.name = getName().toLegacy()
                }
            }
        }
    }

    fun experimentLoadNavButtons(
        config: ConfigurationSection,
        gui: SGMenu,
    ) {
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
     * Clears all non-stickied slots, and loads the orders for the requested page.
     * @param page The page to load orders for.
     */
    private suspend fun loadOrders(page: Int) {
        withContext(JobListings.instance.asyncDispatcher) {
            val orders = PendingOrder.getOrders(orderSlots.count(), page * orderSlots.count())

            withContext(JobListings.instance.minecraftDispatcher) {
                gui.clearAllButStickiedSlots()
                gui.name = getName().toLegacy()

                for ((index, slot) in orderSlots.withIndex()) {
                    val button: SGButton = orders.getOrNull(index)?.let { getOrderButton(it) } ?: GuiHelper.getFallbackButton(config)
                    gui.setButton(slot, button)
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
            JobListings.instance.launch {
                clickOrder(event, order)
            }
        }
        return button
    }

    /**
     * Handles the click event for an order.
     * @param event The click event.
     * @param order The order.
     */
    private suspend fun clickOrder(
        event: InventoryClickEvent,
        order: PendingOrder,
    ) {
        withContext(JobListings.instance.asyncDispatcher) {
            Database.pendingOrderDao.queryForId(order.id) ?: run {
                withContext(JobListings.instance.minecraftDispatcher) {
                    event.whoClicked.closeInventory()
                }
                event.whoClicked.sendMessage(
                    MessageUtil.getMessage("General.OrderAlreadyClaimed"),
                )
                return@withContext
            }
            if (order.owner == event.whoClicked.uniqueId) {
                withContext(JobListings.instance.minecraftDispatcher) {
                    event.whoClicked.closeInventory()
                }
                event.whoClicked.sendMessage(
                    MessageUtil.getMessage("General.CannotAcceptOwnOrder"),
                )
                return@withContext
            }
            if (order.isOrderExpired()) {
                withContext(JobListings.instance.minecraftDispatcher) {
                    event.whoClicked.closeInventory()
                }
                event.whoClicked.sendMessage(
                    MessageUtil.getMessage("General.OrderExpired"),
                )
                return@withContext
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
                        withContext(JobListings.instance.minecraftDispatcher) {
                            event.whoClicked.closeInventory()
                        }
                        event.whoClicked.sendMessage(
                            MessageUtil.getMessage("General.Ignored"),
                        )
                        return@withContext
                    }
                }
            }
            val maxOrdersAccepted = ClaimedOrder.getMaxOrdersAccepted(event.whoClicked as Player)
            if (ClaimedOrder.countClaimedOrders(event.whoClicked as Player) > maxOrdersAccepted) {
                withContext(JobListings.instance.minecraftDispatcher) {
                    event.whoClicked.closeInventory()
                }
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

            withContext(JobListings.instance.minecraftDispatcher) {
                event.whoClicked.closeInventory()
            }
        }
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
