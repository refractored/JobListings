package net.refractored.joblistings.commands

import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.item.ItemBuilder
import com.samjakob.spigui.menu.SGMenu
import net.refractored.joblistings.JobListings.Companion.spiGUI
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.serializers.ItemstackSerializers
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Optional
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player
import kotlin.math.ceil


class GetOrders {
    @CommandPermission("joblistings.order.create")
    @Description("Views order ingame")
    @Command("joblistings orders")
    fun getOrders(actor: BukkitCommandActor, @Optional page: Int = 1) {

        val rows = 5

        val gui = spiGUI.create("&c&lOrders &c(Page {currentPage}/{maxPage})", rows)

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
                    SGButton(ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .name(" ")
                    .build()
                    ),
                )
            }
        }

        gui.setOnPageChange { inventory ->
            if (gui.getButton(10 + (inventory.currentPage * 45)) == null) {
                loadItems(inventory, actor)
            }
        }


        actor.player.openInventory(gui.inventory)
        loadItems(gui, actor)
    }

    private fun loadItems(gui: SGMenu, actor: BukkitCommandActor){
        gui.setButton(
            ((gui.currentPage * 45) + 44),
            SGButton(ItemBuilder(Material.ARROW)
                .name("Next page")
                .build()
            ).withListener { event: InventoryClickEvent ->
                gui.nextPage(actor.player)
            },
        )
        gui.setButton(
            ((gui.currentPage * 45) + 36),
            SGButton(ItemBuilder(Material.ARROW)
                .name("Previous page")
                .build()
            ).withListener { event: InventoryClickEvent ->
                gui.previousPage(actor.player)
            },
        )
        Order.getOrders(21, gui.currentPage * 21 ).forEachIndexed { index, order ->
            val item = ItemstackSerializers.deserialize(order.item)!!.clone()
            val itemMetaCopy = item.itemMeta
            val infoLore = listOf(
                MessageUtil.toComponent(""),
                MessageUtil.toComponent("<reset><red>Pay: <white>${order.cost}"),
                MessageUtil.toComponent("<reset><red>User: <white>${Bukkit.getOfflinePlayer(order.user).name}"),
                MessageUtil.toComponent("<reset><red>Created: <white>${order.timeCreated}"),
                MessageUtil.toComponent(""),
                MessageUtil.toComponent("<reset><gray>(Click to accept order)"),
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
                event.whoClicked.sendMessage("Order Accepted (Broken!)")
                event.whoClicked.closeInventory()
                TODO()
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