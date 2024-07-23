package net.refractored.joblistings.gui

import com.j256.ormlite.stmt.QueryBuilder
import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.item.ItemBuilder
import com.samjakob.spigui.menu.SGMenu
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.AMPERSAND_CHAR
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.JobListings.Companion.essentials
import net.refractored.joblistings.JobListings.Companion.spiGUI
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.player
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.math.ceil

class AllOrders {
    companion object {
        fun openAllOrders(actor: BukkitCommandActor) {
            val gui =
                spiGUI.create(
                    // Me when no component support :((((
                    LegacyComponentSerializer.legacy(AMPERSAND_CHAR).serialize(
                        MessageUtil.getMessage(
                            "AllOrders.Title",
                            listOf(
                                // I only did this for consistency in the messages.yml
                                MessageReplacement("{currentPage}"),
                                MessageReplacement("{maxPage}"),
                            ),
                        ),
                    ),
                    5,
                )

            val pageCount =
                if (ceil(orderDao.countOf().toDouble() / 21).toInt() > 0) {
                    ceil(orderDao.countOf().toDouble() / 21).toInt()
                } else {
                    1
                }

            val borderItems =
                listOf(
                    0,
                    1,
                    2,
                    3,
                    4,
                    5,
                    6,
                    7,
                    8,
                    9,
                    17,
                    18,
                    26,
                    27,
                    35,
                    37,
                    38,
                    39,
                    40,
                    41,
                    42,
                    43,
                )

            for (i in 0..<pageCount) {
                val pageSlot = 45 * i
                borderItems.forEach {
                    gui.setButton(
                        (it + pageSlot),
                        SGButton(
                            ItemBuilder(
                                Material.valueOf(
                                    MessageUtil.getMessageUnformatted("AllOrders.BorderItem"),
                                ),
                            ).name(" ")
                                .build(),
                        ),
                    )
                }
            }

            gui.setOnPageChange { inventory ->
                if (gui.getButton(44 + (inventory.currentPage * 45)) == null) {
                    loadItems(inventory, actor)
                }
            }

            actor.player.openInventory(gui.inventory)
            loadItems(gui, actor)
        }

        private fun loadItems(
            gui: SGMenu,
            actor: BukkitCommandActor,
        ) {
            gui.setButton(
                ((gui.currentPage * 45) + 44),
                SGButton(
                    ItemBuilder(Material.ARROW)
                        .name(
                            LegacyComponentSerializer.legacy(AMPERSAND_CHAR).serialize(
                                MessageUtil.getMessage("AllOrders.NextPage"),
                            ),
                        ).build(),
                ).withListener { event: InventoryClickEvent ->
                    gui.nextPage(actor.player)
                },
            )
            gui.setButton(
                ((gui.currentPage * 45) + 36),
                SGButton(
                    ItemBuilder(Material.ARROW)
                        .name(
                            LegacyComponentSerializer.legacy(AMPERSAND_CHAR).serialize(
                                MessageUtil.getMessage("AllOrders.PreviousPage"),
                            ),
                        ).build(),
                ).withListener { event: InventoryClickEvent ->
                    gui.previousPage(actor.player)
                },
            )
            Order.getPendingOrders(21, gui.currentPage * 21).forEachIndexed { index, order ->
                val item = order.item.clone()
                item.amount =
                    if (order.itemAmount <= item.maxStackSize) {
                        order.itemAmount
                    } else {
                        item.maxStackSize
                    }
                val itemMetaCopy = item.itemMeta
                val expireDuration = Duration.between(LocalDateTime.now(), order.timeExpires)
                val expireDurationText =
                    MessageUtil.getMessage(
                        "AllOrders.ExpireDuration",
                        listOf(
                            MessageReplacement(expireDuration.toDays().toString()),
                            MessageReplacement(expireDuration.toHoursPart().toString()),
                            MessageReplacement(expireDuration.toMinutesPart().toString()),
                        ),
                    )
                val createdDuration = Duration.between(order.timeCreated, LocalDateTime.now())
                val createdDurationText =
                    MessageUtil.getMessage(
                        "AllOrders.CreatedDuration",
                        listOf(
                            MessageReplacement(createdDuration.toDays().toString()),
                            MessageReplacement(createdDuration.toHoursPart().toString()),
                            MessageReplacement(createdDuration.toMinutesPart().toString()),
                        ),
                    )

                val OrderItemLore =
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
                    itemLore.addAll(OrderItemLore)
                    itemMetaCopy.lore(itemLore)
                } else {
                    itemMetaCopy.lore(OrderItemLore)
                }

                item.itemMeta = itemMetaCopy

                val button =
                    SGButton(
                        item,
                    ).withListener { event: InventoryClickEvent ->
                        if (order.user == actor.uniqueId) {
                            event.whoClicked.closeInventory()
                            actor.reply(
                                MessageUtil.getMessage("General.CannotAcceptOwnOrder"),
                            )
                            return@withListener
                        }
                        if (order.status != OrderStatus.PENDING) {
                            event.whoClicked.closeInventory()
                            actor.reply(
                                MessageUtil.getMessage("General.OrderAlreadyClaimed"),
                            )
                            return@withListener
                        }
                        if (order.isOrderExpired()) {
                            event.whoClicked.closeInventory()
                            actor.reply(
                                MessageUtil.getMessage("General.OrderExpired"),
                            )
                            return@withListener
                        }
                        essentials?.let {
                            if (JobListings.instance.config.getBoolean("Essentials.UseIgnoreList")) {
                                val player =
                                    it.userMap.load(
                                        actor.player,
                                    )
                                val owner =
                                    it.userMap.load(
                                        Bukkit.getOfflinePlayer(order.user),
                                    )
                                if (owner.isIgnoredPlayer(player) || player.isIgnoredPlayer(owner)) {
                                    event.whoClicked.closeInventory()
                                    actor.reply(
                                        MessageUtil.getMessage("General.Ignored"),
                                    )
                                    return@withListener
                                }
                            }
                        }
                        val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
                        queryBuilder
                            .where()
                            .eq("assignee", actor.uniqueId)
                            .and()
                            .eq("status", OrderStatus.CLAIMED)
                        val orders = orderDao.query(queryBuilder.prepare())
                        if (orders.count() > JobListings.instance.config.getInt("Orders.MaxOrdersAccepted")) {
                            event.whoClicked.closeInventory()
                            actor.reply(
                                MessageUtil.getMessage(
                                    "AllOrders.OrderItemLore",
                                    listOf(
                                        MessageReplacement(
                                            JobListings.instance.config
                                                .getInt("Orders.MaxOrdersAccepted")
                                                .toString(),
                                        ),
                                    ),
                                ),
                            )
                        }

                        order.acceptOrder(actor.player)
                        event.whoClicked.closeInventory()
                    }
                val baseSlot = (index + 10) + (gui.currentPage * 45)

                var slot = baseSlot

                while (gui.getButton(slot) != null) {
                    if (slot >= 44) {
                        throw IndexOutOfBoundsException("No more slots available in page ${gui.currentPage}")
                    }
                    slot++
                }

                gui.setButton(slot, button)
            }
            gui.refreshInventory(actor.player)
        }
    }
}
