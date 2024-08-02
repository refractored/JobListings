package net.refractored.joblistings.commands

import com.j256.ormlite.stmt.QueryBuilder
import com.samjakob.spigui.item.ItemBuilder
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.Order.Companion.getMaxOrders
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Material
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Optional
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.bukkit.player
import java.util.*

class CreateOrderMaterial {
    @CommandPermission("joblistings.create.material")
    @Description("Create an order from the specified material.")
    @Command("joblistings create material")
    fun createOrderMaterial(
        actor: BukkitCommandActor,
        material: Material,
        cost: Double,
        @Optional amount: Int = 1,
        @Optional hours: Long = JobListings.instance.config.getLong("Orders.MaxOrdersTime"),
    ) {
        if (actor.isConsole) {
            throw net.refractored.joblistings.exceptions.CommandErrorException(
                MessageUtil.getMessage("General.IsNotPlayer"),
            )
        }

        if (amount < 1) {
            throw net.refractored.joblistings.exceptions.CommandErrorException(
                MessageUtil.getMessage("CreateOrder.LessThanOneItem"),
            )
        }

        if (hours < 1) {
            throw net.refractored.joblistings.exceptions.CommandErrorException(
                MessageUtil.getMessage("CreateOrder.LessThanOneHour"),
            )
        }

        if (hours > JobListings.instance.config.getLong("Orders.MaxOrdersTime")) {
            throw net.refractored.joblistings.exceptions.CommandErrorException(
                MessageUtil.getMessage(
                    "CreateOrder.MoreThanMaxHoursConfig",
                    listOf(
                        MessageReplacement(
                            JobListings.instance.config
                                .getLong("Orders.MaxOrdersTime")
                                .toString(),
                        ),
                    ),
                ),
            )
        }

        if (hours < JobListings.instance.config.getLong("Orders.MinOrdersTime")) {
            throw net.refractored.joblistings.exceptions.CommandErrorException(
                MessageUtil.getMessage(
                    "CreateOrder.MoreThanMinHoursConfig",
                    listOf(
                        MessageReplacement(
                            JobListings.instance.config
                                .getLong("Orders.MinOrdersTime")
                                .toString(),
                        ),
                    ),
                ),
            )
        }

        if (cost < 1) {
            throw CommandErrorException(
                MessageUtil.getMessage("CreateOrder.LessThanOneCost"),
            )
        }

        if (JobListings.instance.eco.getBalance(actor.player) < cost) {
            throw CommandErrorException(
                MessageUtil.getMessage("CreateOrder.NotEnoughMoney"),
            )
        }

        val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
        queryBuilder.orderBy("timeCreated", false)
        queryBuilder
            .where()
            .eq("status", OrderStatus.PENDING)
            .and()
            .eq("user", actor.uniqueId)
        val orders = orderDao.query(queryBuilder.prepare())
        val maxOrders = getMaxOrders(actor.player)

        if (orders.count() >= maxOrders) {
            throw CommandErrorException(
                MessageUtil.getMessage(
                    "CreateOrder.MaxOrdersReached",
                    listOf(MessageReplacement("$maxOrders")),
                ),
            )
        }

        if (material == Material.AIR) {
            throw CommandErrorException(
                MessageUtil.getMessage("CreateOrder.MaterialSetToAir"),
            )
        }

        val item = ItemBuilder(material).build()

        val maxItems = JobListings.instance.config.getInt("Orders.MaximumItems")

        when {
            maxItems == -1 && amount > item.maxStackSize -> {
                throw net.refractored.joblistings.exceptions.CommandErrorException(
                    MessageUtil.getMessage(
                        "CreateOrder.StackSizeExceeded",
                        listOf(
                            MessageReplacement(item.maxStackSize.toString()),
                        ),
                    ),
                )
            }
            maxItems != 0 && amount >= maxItems -> {
                throw net.refractored.joblistings.exceptions.CommandErrorException(
                    MessageUtil.getMessage(
                        "CreateOrder.MaxOrdersExceeded",
                        listOf(
                            MessageReplacement(maxItems.toString()),
                        ),
                    ),
                )
            }
        }

        item.amount = 1

        JobListings.instance.eco.withdrawPlayer(actor.player, cost)

        val orderInfo =
            Order
                .createOrder(
                    actor.uniqueId,
                    cost,
                    item,
                    amount,
                    hours,
                ).getItemInfo()

        actor.player.sendMessage(
            MessageUtil.getMessage(
                "CreateOrder.OrderCreated",
                listOf(
                    MessageReplacement(orderInfo),
                    MessageReplacement(cost.toString()),
                    MessageReplacement(hours.toString()),
                ),
            ),
        )
    }
}
