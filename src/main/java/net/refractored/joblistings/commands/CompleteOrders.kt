package net.refractored.joblistings.commands

import com.j256.ormlite.stmt.QueryBuilder
import com.willfp.eco.core.items.Items
import net.kyori.adventure.text.Component
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
        var ordersUpdated = 0
        var ordersCompleted = 0
        for (order in orders) {
            for (item in actor.player.inventory.storageContents){
                item ?: continue
                if (!isMatchingItem(item, order)) continue
                if (order.item is Damageable && item.itemMeta is Damageable) {
                    if ((order.item as Damageable).damage != (item.itemMeta as Damageable).damage) {
                        actor.reply( Component.text()
                            .append(MessageUtil.toComponent("<red>One of your orders call for a,"))
                            .append(item.displayName())
                            .append(MessageUtil.toComponent("<red>,but the item you have is damaged and cannot be delivered."))
                        )
                        continue
                    }
                }
                if (order.itemCompleted + item.amount >= order.itemAmount) {
                    // Order completed YIPPEE
                    val itemsLeft = (order.itemCompleted + item.amount) - order.itemAmount
                    order.itemCompleted = order.itemAmount
                    order.status = OrderStatus.COMPLETED
                    order.timeCompleted = LocalDateTime.now()
                    orderDao.update(order)
                    item.amount = itemsLeft
                    ordersCompleted++
                    messageCompletion(actor, order)
                    continue
                }
                // Order not completed :(
                order.itemCompleted += item.amount
                orderDao.update(order)
                item.amount = 0
                ordersUpdated++
                messageProgress(actor, order)

            }
        }
        if (ordersUpdated == 0) {
            throw CommandErrorException("None your claimed order's requirements were met.")
        }
        if (ordersCompleted == orderCount) {
            actor.reply(MessageUtil.toComponent("<green>All your orders have been completed."))
            return
        }
        actor.reply(
            Component.text()
                .append(MessageUtil.toComponent("<green>Completed <gold>$ordersCompleted</gold> out of <gold>$orderCount</gold> orders."))
                .appendNewline()
                .append(MessageUtil.toComponent("<green>Updated <gold>$ordersUpdated</gold> out of <gold>$orderCount</gold> orders."))
        )
    }

    /**
     * Returns whether the given item matches the order
     */
    private fun isMatchingItem(item: ItemStack,order: Order): Boolean {
        ecoPlugin.let{
            if (Items.isCustomItem(item)) {
                return !Items.getCustomItem(order.item)!!.matches(item)
            }
        }
        return order.item.isSimilar(item)
    }

    private fun messageCompletion(actor: BukkitCommandActor, order: Order) {
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

    private fun messageProgress(actor: BukkitCommandActor, order: Order){
        val assigneeMessage =  Component.text()
            .append(MessageUtil.toComponent(
                "<green>You have made progress on the order <gray>"
            ))
            .append(order.getItemInfo())
            .append(MessageUtil.toComponent(
                "<green>. You have turned in <gold>${order.itemCompleted}</gold> out of <gold>${order.itemAmount}</gold> items!"
            ))
            .build()
        actor.reply(assigneeMessage)
        val ownerMessage = Component.text()
            .append(MessageUtil.toComponent(
                "<green>One of your orders, <gray>"
            ))
            .append(order.getItemInfo())
            .append(MessageUtil.toComponent(
                "<green>, was updated by "
            ))
            .append(actor.player.displayName())
            .append(MessageUtil.toComponent(
                "<green>!"
            ))
            .appendNewline()
            .append(MessageUtil.toComponent(
                "<green>They have turned in <gold>${order.itemCompleted}</gold> out of <gold>${order.itemAmount}</gold> items!"
            ))
            .build()
        order.messageOwner(ownerMessage)
    }
}