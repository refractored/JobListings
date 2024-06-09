package net.refractored.joblistings.gui

import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.item.ItemBuilder
import com.samjakob.spigui.menu.SGMenu
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.refractored.joblistings.JobListings.Companion.eco
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
import kotlin.math.ceil

class MyOrders {
    companion object {
        fun openMyOrders(actor: BukkitCommandActor) {
            val gui = spiGUI.create("&9&lMy Orders &9(Page {currentPage}/{maxPage})", 5)

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
                            ItemBuilder(Material.BLUE_STAINED_GLASS_PANE)
                                .name(" ")
                                .build()
                        ),
                    )
                    gui.stickSlot(it + pageSlot)
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

            Order.getPlayerCreatedOrders(21, gui.currentPage * 21, actor.uniqueId).forEachIndexed { index, order ->
                val item = order.item.clone()
                val itemMetaCopy = item.itemMeta
                val createdDuration = Duration.between(order.timeCreated, LocalDateTime.now())
                val createdDurationText = "${createdDuration.toDays()} Days, ${createdDuration.toHoursPart()} Hours, ${createdDuration.toMinutesPart()} Minutes"
                val infoLore = mutableListOf(
                    MessageUtil.toComponent(""),
                    MessageUtil.toComponent("<reset><red>Cost: <white>${order.cost}"),
                    MessageUtil.toComponent("<reset><red>User: <white>${Bukkit.getOfflinePlayer(order.user).name}"),
                    MessageUtil.toComponent("<reset><red>Created: <white>${createdDurationText} ago"),
                )

                // God spare my soul for this
                when (order.status) {
                    OrderStatus.PENDING -> {
                        val expireDuration = Duration.between(LocalDateTime.now(), order.timeExpires)
                        val expireDurationText = "${expireDuration.toDays()} Days, ${expireDuration.toHoursPart()} Hours, ${expireDuration.toMinutesPart()} Minutes"
                        infoLore.add(MessageUtil.toComponent("<reset><red>Expires in: <white>${expireDurationText}"))
                        infoLore.add(MessageUtil.toComponent("<reset><red>Status: <white>${order.status}"))
                        infoLore.add(MessageUtil.toComponent(""))
                        infoLore.add(MessageUtil.toComponent("<reset><red>(Click to cancel order)"))
                    }
                    OrderStatus.CLAIMED -> {
                        val deadlineDuration = Duration.between(LocalDateTime.now(), order.timeDeadline)
                        val deadlineDurationText = "${deadlineDuration.toDays()} Days, ${deadlineDuration.toHoursPart()} Hours, ${deadlineDuration.toMinutesPart()} Minutes"
                        infoLore.add(MessageUtil.toComponent("<reset><red>Deadline in: <white>${deadlineDurationText}"))
                        order.assignee?.let {
                            infoLore.add(MessageUtil.toComponent("<reset><red>Assignee: <white>${Bukkit.getOfflinePlayer(it).name}"))
                        }
                        infoLore.add(MessageUtil.toComponent("<reset><red>Status: <white>${order.status}"),)
                        infoLore.add(MessageUtil.toComponent(""))
                        infoLore.add(MessageUtil.toComponent("<reset><gray>In-progress orders only return half the payment."))
                        infoLore.add(MessageUtil.toComponent("<reset><yellow>(Click to remove order)"))
                    }
                    OrderStatus.COMPLETED -> {
                        val completedDuration = Duration.between(order.timeCompleted, LocalDateTime.now())
                        val completedDurationText = "${completedDuration.toDays()} Days, ${completedDuration.toHoursPart()} Hours, ${completedDuration.toMinutesPart()} Minutes"
                        infoLore.add(MessageUtil.toComponent("<reset><red>Completed: <white>${completedDurationText} ago"))
                        order.assignee?.let {
                            infoLore.add(MessageUtil.toComponent("<reset><red>Assignee: <white>${Bukkit.getOfflinePlayer(it).name}"))
                        }
                        infoLore.add(MessageUtil.toComponent("<reset><red>Status: <white>${order.status}"),)
                        infoLore.add(MessageUtil.toComponent(""))
                        infoLore.add(MessageUtil.toComponent("<reset><green>(Click to claim order)"))
                    }
                    OrderStatus.INCOMPLETE -> {
                        order.assignee?.let {
                            infoLore.add(MessageUtil.toComponent("<reset><red>Assignee: <white>${Bukkit.getOfflinePlayer(it).name}"))
                        }
                        infoLore.add(MessageUtil.toComponent("<reset><red>Status: <white>${order.status}"),)
                        infoLore.add(MessageUtil.toComponent(""))
                        infoLore.add(MessageUtil.toComponent("<reset><blue>(Click to refund order)"))
                    }
                    OrderStatus.EXPIRED -> {
                        infoLore.add(MessageUtil.toComponent("<reset><red>Status: <white>${order.status}"),)
                        infoLore.add(MessageUtil.toComponent(""))
                        infoLore.add(MessageUtil.toComponent("<reset><blue>(Click to refund order)"))
                    }
                }

                for (lore in infoLore){
                    lore.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                }

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
                    when (order.status) {
                        OrderStatus.PENDING -> {
                            event.whoClicked.sendMessage(
                                MessageUtil.toComponent("<green>Order Cancelled!")
                            )
                            gui.removeButton((index + 10) + (gui.currentPage * 45))
                            eco.depositPlayer(actor.player, order.cost)
                            Database.orderDao.delete(order)
                            reloadItems(gui, actor)
                            gui.refreshInventory(actor.player)
                        }
                        OrderStatus.CLAIMED -> {
                            event.whoClicked.sendMessage(
                                MessageUtil.toComponent("<green>Order Cancelled!")
                            )
                            gui.removeButton((index + 10) + (gui.currentPage * 45))
                            eco.depositPlayer(actor.player, (order.cost / 2) )
                            order.status = OrderStatus.INCOMPLETE
                            // No need to refund items to assignee, if no items were ever turned in
                            if (order.itemCompleted == 0){
                                Database.orderDao.delete(order)
                            } else {
                                Database.orderDao.update(order)
                            }
                            val assigneeMessage = Component.text()
                                .append(MessageUtil.toComponent(
                                    "<green>One of your orders, <gray>"
                                ))
                                .append(order.getItemInfo())
                                .append(MessageUtil.toComponent(
                                    "<gray>, was canceled by the owner!"
                                ))
                                .build()
                            order.messageAssignee(assigneeMessage)
                            reloadItems(gui, actor)
                            gui.refreshInventory(actor.player)
                        }
                        OrderStatus.COMPLETED -> {
                            gui.removeButton((index + 10) + (gui.currentPage * 45))
                            val inventoryFull = actor.player.inventory.storageContents.none { it == null }
                            if (inventoryFull) {
                                actor.player.closeInventory()
                                event.whoClicked.sendMessage(
                                    MessageUtil.toComponent("<red>Your inventory is full!")
                                )
                                return@withListener
                            }
                            actor.player.inventory.addItem(order.item)
                            Database.orderDao.delete(order)
                            reloadItems(gui, actor)
                            gui.refreshInventory(actor.player)
                            event.whoClicked.sendMessage(
                                MessageUtil.toComponent("<green>Order claimed!")
                            )
                            val inventorySpaces = actor.player.inventory.storageContents.count{
                                it == null || (it.isSimilar(order.item) && it.amount < it.maxStackSize)
                            }
                            if (inventorySpaces == 0) {
                                actor.player.closeInventory()
                                event.whoClicked.sendMessage(
                                    MessageUtil.toComponent("<red>Your inventory is full!")
                                )
                                return@withListener
                            }
                            if (order.itemCompleted == order.itemsReturned) {
                                actor.player.closeInventory()
                                event.whoClicked.sendMessage(
                                    MessageUtil.toComponent("<red>You have already refunded this order!")
                                )
                                return@withListener
                            }
                            giveOrderItems(order, actor)
                        }

                        OrderStatus.INCOMPLETE -> {
                            event.whoClicked.sendMessage(
                                MessageUtil.toComponent("<green>Order refunded!")
                            )
                            gui.removeButton((index + 10) + (gui.currentPage * 45))
                            eco.depositPlayer(actor.player, (order.cost) )
                            Database.orderDao.delete(order)
                            reloadItems(gui, actor)
                            gui.refreshInventory(actor.player)
                        }
                        OrderStatus.EXPIRED -> {
                            event.whoClicked.sendMessage(
                                MessageUtil.toComponent("<green>Order refunded!")
                            )
                            gui.removeButton((index + 10) + (gui.currentPage * 45))
                            eco.depositPlayer(actor.player, (order.cost) )
                            Database.orderDao.delete(order)
                            reloadItems(gui, actor)
                            gui.refreshInventory(actor.player)
                        }
                    }
                }
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

        private fun giveOrderItems(order: Order ,actor: BukkitCommandActor){
            val inventory = actor.player.inventory.storageContents
            for ((inventoryIndex, inventoryItem) in inventory.withIndex()) {
                if (order.itemCompleted == order.itemsObtained) break
                if (inventoryItem == null) {
                    val inventoryNewItem = order.item.clone().apply {
                        amount = if (order.itemsObtained + maxStackSize >= order.itemCompleted) {
                            order.itemCompleted - order.itemsObtained
                        } else {
                            maxStackSize
                        }
                    }
                    order.itemsObtained += inventoryNewItem.amount
                    actor.player.inventory.storageContents[inventoryIndex] = inventoryNewItem
                    break
                } else if (inventoryItem.isSimilar(order.item)) {
                    if (inventoryItem.amount == inventoryItem.maxStackSize) continue
                    val itemsNeeded = inventoryItem.maxStackSize - inventoryItem.amount
                    val itemsToAdd = if (order.itemsObtained + itemsNeeded >= order.itemCompleted) {
                        order.itemCompleted - order.itemsObtained
                    } else {
                        itemsNeeded
                    }
                    inventoryItem.amount += itemsToAdd
                    order.itemsObtained += itemsToAdd
                    break
                }
            }
            orderDao.update(order)
            if (order.itemsObtained == order.itemCompleted) {
                MessageUtil.toComponent("<green>Order fully obtained!")
                actor.player.closeInventory()
                orderDao.delete(order)
            }
        }
    }
}