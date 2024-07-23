package net.refractored.joblistings.gui

import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.item.ItemBuilder
import com.samjakob.spigui.menu.SGMenu
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.AMPERSAND_CHAR
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
import kotlin.math.ceil

class ClaimedOrders {
    companion object {
        fun openMyClaimedOrders(actor: BukkitCommandActor) {
            val gui =
                spiGUI.create(
                    LegacyComponentSerializer.legacy(AMPERSAND_CHAR).serialize(
                        MessageUtil.getMessage(
                            "ClaimedOrders.Title",
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
                                    MessageUtil.getMessageUnformatted("ClaimedOrders.BorderItem"),
                                ),
                            ).name(" ")
                                .build(),
                        ),
                    )
                    gui.stickSlot(it + pageSlot)
                }
            }

            gui.setOnPageChange { inventory ->
//                if (gui.getButton(44 + (inventory.currentPage * 45)) == null) {
//                    loadItems(inventory, actor)
//                }
                reloadItems(gui, actor)
                gui.refreshInventory(actor.player)
            }

            actor.player.openInventory(gui.inventory)
            loadItems(gui, actor)
        }

        private fun reloadItems(
            gui: SGMenu,
            actor: BukkitCommandActor,
        ) {
            gui.clearAllButStickiedSlots()
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
                                MessageUtil.getMessage("ClaimedOrders.NextPage"),
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
                                MessageUtil.getMessage("ClaimedOrders.PreviousPage"),
                            ),
                        ).build(),
                ).withListener { event: InventoryClickEvent ->
                    gui.previousPage(actor.player)
                },
            )

            Order.getPlayerAcceptedOrders(21, gui.currentPage * 21, actor.uniqueId).forEachIndexed { index, order ->
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
                        MessageUtil.getMessageList(
                            "AllOrders.OrderItemLore",
                            listOf(
                                MessageReplacement(order.cost.toString()),
                                MessageReplacement(Bukkit.getOfflinePlayer(order.user).name ?: "Unknown"),
                                MessageReplacement(createdDurationText),
                                MessageReplacement(deadlineDurationText),
                                MessageReplacement(order.itemAmount.toString()),
                                MessageReplacement(order.itemCompleted.toString()),
                            ),
                        )
                    } else {
                        MessageUtil.getMessageList(
                            "AllOrders.OrderItemLoreIncomplete",
                            listOf(
                                MessageReplacement(order.cost.toString()),
                                MessageReplacement(Bukkit.getOfflinePlayer(order.user).name ?: "Unknown"),
                                MessageReplacement(createdDurationText),
                                MessageReplacement(order.itemAmount.toString()),
                                MessageReplacement(order.itemsReturned.toString()),
                            ),
                        )
                    }

                if (itemMetaCopy.hasLore()) {
                    val itemLore = itemMetaCopy.lore()!!
                    itemLore.addAll(infoLore)
                    itemMetaCopy.lore(itemLore)
                } else {
                    itemMetaCopy.lore(infoLore)
                }

                displayItem.itemMeta = itemMetaCopy

                val button =
                    SGButton(
                        displayItem,
                    ).withListener { event: InventoryClickEvent ->
                        when (order.status) {
                            OrderStatus.INCOMPLETE -> {
                                val inventorySpaces =
                                    actor.player.inventory.storageContents.count {
                                        it == null || (it.isSimilar(order.item) && it.amount < it.maxStackSize)
                                    }
                                if (inventorySpaces == 0) {
                                    actor.player.closeInventory()
                                    event.whoClicked.sendMessage(
                                        MessageUtil.getMessage("General.InventoryFull"),
                                    )
                                    return@withListener
                                }
                                if (order.itemCompleted == order.itemsReturned) {
                                    actor.player.closeInventory()
                                    event.whoClicked.sendMessage(
                                        MessageUtil.getMessage("ClaimedOrders.OrderAlreadyRefunded"),
                                    )
                                    return@withListener
                                }
                                if (giveRefundableItems(order, actor)) {
                                    actor.player.closeInventory()
                                    event.whoClicked.sendMessage(
                                        MessageUtil.getMessage("ClaimedOrders.OrderFullyRefunded"),
                                    )
                                    gui.removeButton((index + 10) + (gui.currentPage * 45))
                                    orderDao.delete(order)
                                    reloadItems(gui, actor)
                                    gui.refreshInventory(actor.player)
                                } else {
                                    actor.player.closeInventory()
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

                            else -> return@withListener
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

        /**
         * Refunds the items to the assignee of the order
         * This also updates the order itemsReturned
         * @return true if all items have been refunded
         */
        private fun giveRefundableItems(
            order: Order,
            actor: BukkitCommandActor,
        ): Boolean {
            var itemsLeft = order.itemCompleted - order.itemsReturned
            while (itemsLeft > 0) {
                if (actor.player.inventory.storageContents.count {
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
                val excessItems = actor.player.inventory.addItem(item)
                itemsLeft += excessItems.values.sumOf { it.amount }
            }
            order.itemsReturned = order.itemCompleted - itemsLeft
            orderDao.update(order)
            return (order.itemsReturned == order.itemCompleted)
        }
    }
}
