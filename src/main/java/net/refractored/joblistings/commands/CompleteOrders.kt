package net.refractored.joblistings.commands

import com.j256.ormlite.stmt.QueryBuilder
import com.willfp.eco.core.items.Items
import net.kyori.adventure.text.Component
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.JobListings.Companion.eco
import net.refractored.joblistings.JobListings.Companion.ecoPlugin
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player
import revxrsal.commands.exception.CommandErrorException
import java.time.LocalDateTime
import java.util.*

class CompleteOrders {
    @CommandPermission("joblistings.completeorders")
    @Description("Scans your inventory for items to complete an order")
        @Command("joblistings complete")
    fun completeOrders(actor: BukkitCommandActor) {
        val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
        queryBuilder.where().eq("assignee", actor.uniqueId).and().eq("status", OrderStatus.CLAIMED)
        val orders = orderDao.query(queryBuilder.prepare()).sortedByDescending { it.timeCreated }
        if (orders.isEmpty()) {
            throw CommandErrorException("You have no orders to complete.")
        }
        val orderCount = orders.count()
        var completionCount = 0
        for (order in orders) {
            // TODO: Fix if item is split into multiple stacks, it will not be detected.
            val itemStack = getMatchingItem(order, actor) ?: continue
            Items.getItem(order.item).matches(actor.player.inventory.itemInMainHand)
            if (order.item is Damageable && itemStack.itemMeta is Damageable) {
                if ((order.item as Damageable).damage != (itemStack.itemMeta as Damageable).damage) {
                    actor.reply( Component.text()
                        .append(MessageUtil.toComponent("<red>One of your orders call for a,"))
                        .append(itemStack.displayName())
                        .append(MessageUtil.toComponent("<red>,but the item you have is damaged and cannot be delivered."))
                    )
                    continue
                }
            }

            itemStack.amount -= order.item.amount
            order.status = OrderStatus.COMPLETED
            order.timeCompleted = LocalDateTime.now()
            orderDao.update(order)
            eco.depositPlayer(actor.player, order.cost)
            completionCount++
            val assigneeMessage =  Component.text()
                .append(MessageUtil.toComponent(
                    "<green>You have completed the order <gray>"
                ))
                .append(order.getItemInfo())
                .append(MessageUtil.toComponent(
                    "<green> and received <gold>${order.cost}</gold>."
                ))
                .build()
            actor.reply(assigneeMessage)
            val ownerMessage = Component.text()
                .append(MessageUtil.toComponent(
                "<green>One of your orders, <gray>"
            ))
                .append(order.getItemInfo())
                .append(MessageUtil.toComponent(
                    "<green>, was completed by "
                ))
                .append(actor.player.displayName())
                .append(MessageUtil.toComponent(
                    "<green>!"
                ))
                .build()
            order.messageOwner(ownerMessage)
        }
        if (completionCount == 0) {
            throw CommandErrorException("None your claimed order's requirements were met.")
        }
        if (completionCount == orderCount) {
            actor.reply(MessageUtil.toComponent("<green>All your orders have been completed."))
            return
        }
        actor.reply(
            MessageUtil.toComponent("<green><gold>$completionCount</gold> orders have been completed out of <gold>$orderCount</gold>." +
                    "\nYou now have <gold>${(orderCount - completionCount)}</gold> orders left")
        )
    }

    /**
     * Gets the matching item for the order in the player's inventory
     * @return The ItemStack, or null if none exists.
     */
    private fun getMatchingItem(order: Order, actor: BukkitCommandActor): ItemStack? {
        ecoPlugin.let{
            if (Items.isCustomItem(order.item)) {
                for (items in actor.player.inventory.storageContents) {
                    if (items == null) continue
                    if (!Items.getCustomItem(order.item)!!.matches(items)) continue
                    if (items.amount < order.item.amount) continue
                    return items
                }
                return null
            }
        }
        return actor.player.inventory.storageContents
            .firstOrNull{ (it?.isSimilar(order.item) ?: false) && ((it?.amount ?: 0) >= order.item.amount) }
    }
}