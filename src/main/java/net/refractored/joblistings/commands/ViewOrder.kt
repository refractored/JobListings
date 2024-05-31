package net.refractored.joblistings.commands

import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.item.ItemBuilder
import com.samjakob.spigui.menu.SGMenu
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.serializers.ItemstackSerializers
import org.bukkit.event.inventory.InventoryClickEvent
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player
import net.refractored.joblistings.JobListings.Companion.spiGUI
import net.refractored.joblistings.gui.MyOrders
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import kotlin.math.ceil


class ViewOrder {
    @CommandPermission("joblistings.order.view")
    @Description("View your order.")
    @Command("joblistings view")
    fun ViewOrder(actor: BukkitCommandActor) {
        MyOrders.openMyOrders(actor)
    }
}