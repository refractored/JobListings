package net.refractored.joblistings.commands

import com.j256.ormlite.stmt.QueryBuilder
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.commands.autocomplete.MaterialSuggesstion
import net.refractored.joblistings.database.Database.orderDao
import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.order.tables.PendingOrder
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import net.refractored.joblistings.util.Messages
import net.refractored.joblistings.util.Messages.miniToComponent
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Named
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.SuggestWith
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import java.util.*

class CreateOrderMaterial {
    @CommandPermission("joblistings.create.material")
    @Description("Create an order from the specified material.")
    @Command("joblistings create material")
    fun createOrderMaterial(
        actor: BukkitCommandActor,
        @SuggestWith(MaterialSuggesstion::class) @Named("type")stackName: String,
        cost: Double,
        @Optional amount: Int = 1,
        @Optional hours: Long = JobListings.instance.config.getLong("orders.max-order-time"),
    ) {
        val player = actor.requirePlayer()

        if (amount < 1) {
            throw CommandErrorException(
                Messages.getString("CreateOrder.LessThanOneItem").miniToComponent(),
            )
        }

        if (hours < 1) {
            throw CommandErrorException(
                Messages.getString("CreateOrder.LessThanOneHour").miniToComponent(),
            )
        }

        if (hours > JobListings.instance.config.getLong("orders.max-order-time")) {
            throw CommandErrorException(
                MessageUtil.getMessage(
                    "CreateOrder.MoreThanMaxHoursConfig",
                    listOf(
                        MessageReplacement(
                            JobListings.instance.config
                                .getLong("orders.max-order-time")
                                .toString(),
                        ),
                    ),
                ),
            )
        }

        if (hours < JobListings.instance.config.getLong("orders.min-order-time")) {
            throw CommandErrorException(
                MessageUtil.getMessage(
                    "CreateOrder.MoreThanMinHoursConfig",
                    listOf(
                        MessageReplacement(
                            JobListings.instance.config
                                .getLong("orders.min-order-time")
                                .toString(),
                        ),
                    ),
                ),
            )
        }

        if (cost < 1) {
            throw CommandErrorException(
                Messages.getString("CreateOrder.LessThanOneCost").miniToComponent(),
            )
        }

        if (JobListings.instance.eco.getBalance(player) < cost) {
            throw CommandErrorException(
                Messages.getString("CreateOrder.NotEnoughMoney").miniToComponent(),
            )
        }

        val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
        queryBuilder.orderBy("timeCreated", false)
        queryBuilder
            .where()
            .eq("status", OrderStatus.PENDING)
            .and()
            .eq("user", actor.uniqueId())
        val orders = orderDao.query(queryBuilder.prepare())
        val maxOrders = PendingOrder.getMaxOrders(player)

        if (orders.count() >= maxOrders) {
            throw CommandErrorException(
                MessageUtil.getMessage(
                    "CreateOrder.MaxOrdersReached",
                    listOf(MessageReplacement("$maxOrders")),
                ),
            )
        }

        val item: ItemStack =
            /* Presets.getPreset(stackName)
                ?:*/
            Material.getMaterial(stackName.uppercase())?.let { ItemStack(it) }
                ?: throw CommandErrorException(
                    Messages.getString("CreateOrder.MaterialNotFound").miniToComponent(),
                )

        item.amount = 1

        if (item.type == Material.AIR) {
            throw CommandErrorException(
                Messages.getString("CreateOrder.MaterialSetToAir").miniToComponent(),
            )
        }

        if (blacklistedMaterial(item.type.name)) {
            throw CommandErrorException(
                Messages.getString("CreateOrder.BlacklistedMaterial").miniToComponent(),
            )
        }

        val maxItems = JobListings.instance.config.getInt("orders.max-items")

        when {
            maxItems == -1 && amount > item.maxStackSize -> {
                throw CommandErrorException(
                    MessageUtil.getMessage(
                        "CreateOrder.StackSizeExceeded",
                        listOf(
                            MessageReplacement(item.maxStackSize.toString()),
                        ),
                    ),
                )
            }
            maxItems != 0 && amount >= maxItems -> {
                throw CommandErrorException(
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

        JobListings.instance.eco.withdrawPlayer(player, cost)

        val order =
            PendingOrder.create(
                actor.uniqueId(),
                cost,
                item,
                amount,
                hours,
            )

        val orderInfo = order.getItemInfo()

        player.sendMessage(
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

    private fun blacklistedMaterial(arg: String): Boolean {
        val blacklistedMaterials = JobListings.instance.config.getStringList("orders.BlacklistedMaterials")
        blacklistedMaterials.addAll(
            JobListings.instance.config.getStringList("orders.BlacklistedCreateMaterials"),
        )
        return blacklistedMaterials.any { it.equals(arg, true) }
    }
}
