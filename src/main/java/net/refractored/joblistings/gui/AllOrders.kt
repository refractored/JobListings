package net.refractored.joblistings.gui

import com.j256.ormlite.stmt.QueryBuilder
import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.item.ItemBuilder
import com.samjakob.spigui.menu.SGMenu
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.refractored.joblistings.JobListings
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.AMPERSAND_CHAR
import net.refractored.joblistings.JobListings.Companion.essentials
import net.refractored.joblistings.JobListings.Companion.spiGUI
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
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
    companion object{
        fun openAllOrders(actor: BukkitCommandActor) {
            val gui = spiGUI.create(
                // Me when no component support :((((
                LegacyComponentSerializer.legacy(AMPERSAND_CHAR).serialize(
                    MessageUtil.toComponent(
                        "<gradient:#7ECD71:#4CB13B><bold>All Orders</gradient> <#3e403f>(Page {currentPage}/{maxPage})"
                    )
                ),
                5)

            val pageCount = if (ceil(Database.orderDao.countOf().toDouble() / 21).toInt() > 0) {
                ceil(Database.orderDao.countOf().toDouble() / 21).toInt()
            } else {
                1
            }


            val borderItems = listOf(
                0,  1,  2,  3,  4,  5,  6,  7,  8,
                9,                              17,
                18,                             26,
                27,                             35,
                    37, 38, 39, 40, 41, 42, 43,
            )


            for (i in 0..< pageCount) {
                val pageSlot = 45 * i
                borderItems.forEach{
                    gui.setButton(
                        (it + pageSlot),
                        SGButton(
                            ItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
                            .name(" ")
                            .build()
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

        private fun loadItems(gui: SGMenu, actor: BukkitCommandActor){
            gui.setButton(
                ((gui.currentPage * 45) + 44),
                SGButton(
                    ItemBuilder(Material.ARROW)
                    .name("Next page")
                    .build()
                ).withListener { event: InventoryClickEvent ->
                    gui.nextPage(actor.player)
                },
            )
            gui.setButton(
                ((gui.currentPage * 45) + 36),
                SGButton(
                    ItemBuilder(Material.ARROW)
                    .name("Previous page")
                    .build()
                ).withListener { event: InventoryClickEvent ->
                    gui.previousPage(actor.player)
                },
            )
            Order.getPendingOrders(21, gui.currentPage * 21 ).forEachIndexed { index, order ->
                val item = order.item.clone()
                val itemMetaCopy = item.itemMeta
                val expireDuration = Duration.between(LocalDateTime.now(), order.timeExpires)
                val expireDurationText = "${expireDuration.toDays()} Days, ${expireDuration.toHoursPart()} Hours, ${expireDuration.toMinutesPart()} Minutes"
                val createdDuration = Duration.between(order.timeCreated, LocalDateTime.now())
                val createdDurationText = "${createdDuration.toDays()} Days ${createdDuration.toHoursPart()} Hours, ${createdDuration.toMinutesPart()} Minutes"
                val infoLore = listOf(
                    MessageUtil.toComponent("").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                    MessageUtil.toComponent("<#69b85c>Reward: <white>${order.cost}").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                    MessageUtil.toComponent("<#69b85c>User: <white>${Bukkit.getOfflinePlayer(order.user).name}").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                    MessageUtil.toComponent("<#69b85c>Created: <white>${createdDurationText} ago").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                    MessageUtil.toComponent("<#69b85c>Expires: <white>${expireDurationText}").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                    MessageUtil.toComponent(""),
                    MessageUtil.toComponent("<gray>(Click to accept order)").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                )

                if (itemMetaCopy.hasLore()) {
                    val itemLore = itemMetaCopy.lore()!!
                    itemLore.addAll(infoLore)
                    itemMetaCopy.lore(itemLore)
                } else {
                    itemMetaCopy.lore(infoLore)
                }

                item.itemMeta = itemMetaCopy

                val button = SGButton(
                    item
                ).withListener { event: InventoryClickEvent ->
                    if (order.user == actor.uniqueId) {
                        event.whoClicked.closeInventory()
                        actor.reply(
                            MessageUtil.toComponent("<red>You cannot accept your own order.")
                        )
                        return@withListener
                    }
                    if (order.status != OrderStatus.PENDING) {
                        event.whoClicked.closeInventory()
                        actor.reply(
                            MessageUtil.toComponent("<red>Order is not pending. Someone might have already accepted it.")
                        )
                        return@withListener
                    }
                    if (order.isOrderExpired()) {
                        event.whoClicked.closeInventory()
                        actor.reply(
                            MessageUtil.toComponent("<red>Order has expired.")
                        )
                        return@withListener
                    }
                    essentials?.let {
                        if (JobListings.instance.config.getBoolean("Essentials.UseIgnoreList")) {
                            val player = it.userMap.load(
                                actor.player
                            )
                            val owner = it.userMap.load(
                                Bukkit.getOfflinePlayer(order.user)
                            )
                            if (owner.isIgnoredPlayer(player) || player.isIgnoredPlayer(owner)) {
                                event.whoClicked.closeInventory()
                                actor.reply(
                                    MessageUtil.toComponent("<red>You cannot accept orders from players who have you ignored or you ignored yourself.")
                                )
                                return@withListener
                            }
                        }
                    }
                    val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
                    queryBuilder.where().eq("assignee", actor.uniqueId).and().eq("status", OrderStatus.CLAIMED)
                    val orders = orderDao.query(queryBuilder.prepare())
                    if (orders.count() > JobListings.instance.config.getInt("Orders.MaxOrdersAccepted") ) {
                        event.whoClicked.closeInventory()
                        actor.reply(
                            MessageUtil.toComponent("<red>You cannot have more than ${JobListings.instance.config.getInt("Orders.MaxOrdersAccepted")} claimed orders at once.")
                        )
                    }
                    val ownerMessage = Component.text()
                        .append(MessageUtil.toComponent(
                            "<green>One of your orders, <gray>"
                        ))
                        .append(order.getItemInfo())
                        .append(MessageUtil.toComponent(
                            "<green>, was accepted by "
                        ))
                        .append(actor.player.displayName())
                        .append(MessageUtil.toComponent(
                            "<green>!"
                        ))
                        .build()
                    order.messageOwner(ownerMessage)
                    actor.reply(
                        MessageUtil.toComponent("<green>Order accepted!")
                    )
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

                gui.setButton( slot , button)


            }
            gui.refreshInventory(actor.player)

        }
    }
}