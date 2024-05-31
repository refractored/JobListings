package net.refractored.joblistings.commands

import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.item.ItemBuilder
import com.samjakob.spigui.menu.SGMenu
import net.refractored.joblistings.JobListings.Companion.spiGUI
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.gui.AllOrders
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
    fun getOrders(actor: BukkitCommandActor) {
        AllOrders.openAllOrders(actor)
    }
}