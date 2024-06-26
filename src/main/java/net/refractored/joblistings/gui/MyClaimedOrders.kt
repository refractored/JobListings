package net.refractored.joblistings.gui

import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.item.ItemBuilder
import com.samjakob.spigui.menu.SGMenu
import net.kyori.adventure.text.format.TextDecoration
import net.refractored.joblistings.JobListings.Companion.spiGUI
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.player
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.ceil

class MyClaimedOrders {
    companion object {
        fun openMyClaimedOrders(actor: BukkitCommandActor) {
            val gui = spiGUI.create("&5&lMy Claimed Orders &5(Page {currentPage}/{maxPage})", 5)

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


            for (i in 0..<pageCount) {
                val pageSlot = 45 * i
                borderItems.forEach {
                    gui.setButton(
                        (it + pageSlot),
                        SGButton(
                            ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE)
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
        private fun reloadItems(gui: SGMenu, actor: BukkitCommandActor) {
            gui.clearAllButStickiedSlots()
            loadItems(gui, actor)
        }

        private fun loadItems(gui: SGMenu, actor: BukkitCommandActor) {
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

            Order.getPlayerAcceptedOrders(21, gui.currentPage * 21, actor.uniqueId).forEachIndexed { index, order ->
                val item = order.item.clone()
                val itemMetaCopy = item.itemMeta
                val deadlineDuration = Duration.between(LocalDateTime.now(), order.timeDeadline)
                val deadlineDurationText = "${deadlineDuration.toDays()} Days, ${deadlineDuration.toHoursPart()} Hours, ${deadlineDuration.toMinutesPart()} Minutes"
                val createdDuration = Duration.between(order.timeCreated, LocalDateTime.now())
                val createdDurationText = "${createdDuration.toDays()} Days, ${createdDuration.toHours()} Hours, ${createdDuration.toMinutesPart()} Minutes"
                val infoLore = listOf(
                    MessageUtil.toComponent("").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                    MessageUtil.toComponent("<reset><red>Reward: <white>${order.cost}").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                    MessageUtil.toComponent("<reset><red>User: <white>${Bukkit.getOfflinePlayer(order.user).name}").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                    MessageUtil.toComponent("<reset><red>Created: <white>${createdDurationText}").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                    MessageUtil.toComponent("<reset><red>Deadline in: <white>${deadlineDurationText}").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                    MessageUtil.toComponent("<reset><red>Status: <white>${order.status}").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                    MessageUtil.toComponent(""),
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
                )
//                    .withListener { event: InventoryClickEvent ->
//                    when (order.status) {
//                        OrderStatus.PENDING -> {
//                            event.whoClicked.sendMessage("Order Deleted!")
//                            gui.removeButton((index + 10) + (gui.currentPage * 45))
//                            eco.depositPlayer(actor.player, order.cost)
//                            Database.orderDao.delete(order)
//                            reloadItems(gui, actor)
//                            gui.refreshInventory(actor.player)
//                        }
//                        OrderStatus.CLAIMED -> {
//                            event.whoClicked.sendMessage("Order Removed!")
//                            gui.removeButton((index + 10) + (gui.currentPage * 45))
//                            eco.depositPlayer(actor.player, (order.cost / 2) )
//                            Database.orderDao.delete(order)
//                            reloadItems(gui, actor)
//                            gui.refreshInventory(actor.player)
//                        }
//                        OrderStatus.COMPLETED -> {
//                            TODO()
//                        }
//
//                        OrderStatus.INCOMPLETE -> TODO()
//                        OrderStatus.EXPIRED -> TODO()
//                    }
//                }
                val baseSlot = (index + 10) + (gui.currentPage * 45)

                var slot = baseSlot

                while (gui.getButton(slot) != null) {
                    if (slot >= 46) {
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