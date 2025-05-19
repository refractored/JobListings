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
import net.refractored.joblistings.order.tables.ClaimedOrder
import net.refractored.joblistings.order.tables.PendingOrder
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import net.refractored.joblistings.util.Messages
import net.refractored.joblistings.util.Messages.fixItalics
import net.refractored.joblistings.util.Messages.miniToComponent
import net.refractored.joblistings.util.Messages.replace
import net.refractored.joblistings.util.Messages.toLegacy
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.ceil

class AllOrders(
    player: Player
) : OrdersGUI(player) {
    override val config = JobListings.instance.gui.getConfigurationSection("AllOrders")!!

    override fun getName(): Component = (config.getString("Title") ?: "Title")
        .replace("%current_page%", (orderPage + 1).toString())
        .replace("%max_pages%", pageCount.toString())
        .miniToComponent()

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

        JobListings.instance.launch {
            withContext(JobListings.instance.asyncDispatcher) {
                pageCount = ceil(Database.pendingOrderDao.countOf().toDouble() / orderSlots.count()).toInt().coerceAtLeast(1)

                withContext(JobListings.instance.minecraftDispatcher) {
                    gui.name = getName().toLegacy()
                }
            }
        }
    }

    /**
     * Clears all non-stickied slots, and loads the orders for the requested page.
     * @param page The page to load orders for.
     */
    override suspend fun loadOrders(page: Int) {
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
            Messages
                .getString("AllOrders.OrderItemLore")
                .replace("%reward%", order.reward.toString())
                .replace("%owner%", order.getOwner().name ?: "Unknown")
                .replace("%created%", createdDurationText)
                .replace("%expire%", expireDurationText)
                .replace("%amount%", order.itemAmount.toString())
                .lines()
                .map { it.miniToComponent().fixItalics() }

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
        order: PendingOrder
    ) {
        withContext(JobListings.instance.asyncDispatcher) {
            Database.pendingOrderDao.queryForId(order.id) ?: run {
                withContext(JobListings.instance.minecraftDispatcher) {
                    event.whoClicked.closeInventory()
                }
                event.whoClicked.sendMessage(
                    Messages.getStringPrefixed("General.OrderAlreadyClaimed").miniToComponent(),
                )
                return@withContext
            }
            if (order.owner == event.whoClicked.uniqueId) {
                withContext(JobListings.instance.minecraftDispatcher) {
                    event.whoClicked.closeInventory()
                }
                event.whoClicked.sendMessage(
                    Messages.getStringPrefixed("General.CannotAcceptOwnOrder").miniToComponent(),
                )
                return@withContext
            }
            if (order.isOrderExpired()) {
                withContext(JobListings.instance.minecraftDispatcher) {
                    event.whoClicked.closeInventory()
                }
                event.whoClicked.sendMessage(
                    Messages.getStringPrefixed("General.OrderExpired").miniToComponent(),
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
                            Messages.getStringPrefixed("General.Ignored"),
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
                refreshOpenGUIs()
            }
        }
    }

    companion object {
        val openGUIs = mutableListOf<AllOrders>()

        /**
         * Creates an instance of the AllOrders class, and returns a working gui.
         * @return The gui.
         */
        fun getGUI(player: Player): SGMenu {
            val allOrders = AllOrders(player)
            openGUIs.add(allOrders)
            return allOrders.gui
        }

        fun openGUI(player: Player) {
            player.openInventory(this.getGUI(player).inventory)
        }

        fun refreshOpenGUIs() {
            JobListings.instance.launch {
                for (gui in openGUIs) {
                    gui.loadOrders(gui.orderPage)
                }
            }
        }
    }
}
