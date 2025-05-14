package net.refractored.joblistings.order.tables

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.order.impl.Assignee
import net.refractored.joblistings.order.impl.Creation
import net.refractored.joblistings.order.impl.Expires
import net.refractored.joblistings.order.impl.Item
import net.refractored.joblistings.order.impl.Owner
import net.refractored.joblistings.order.impl.Rewardable
import net.refractored.joblistings.serializers.ItemstackSerializers
import net.refractored.joblistings.serializers.LocalDateTimeSerializers
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import net.refractored.joblistings.util.Messages
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents an order that has been placed on the job board
 */
@DatabaseTable(tableName = "joblistings_claimed_orders")
data class ClaimedOrder(
    @DatabaseField(id = true)
    val id: UUID,
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    override var expireTime: LocalDateTime,
    @DatabaseField(persisterClass = ItemstackSerializers::class)
    override var item: ItemStack,
    @DatabaseField
    override var itemAmount: Int,
    @DatabaseField
    override var owner: UUID,
    @DatabaseField
    override var assignee: UUID,
    @DatabaseField
    override var reward: Double,
    /**
     * The time the database entry was created.
     *
     * In this case, it represents the time the order was claimed.
     */
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    override var creation: LocalDateTime,
    /**
     * The amount of items that the [assignee] has turned in.
     *
     * This is out of how many in [itemAmount].
     */
    @DatabaseField
    var amountTurnedIn: Int,
) : Owner,
    Rewardable,
    Item,
    Assignee,
    Expires,
    Creation {
    /**
     * This constructor should only be used for ORMLite
     */
    constructor() : this(
        UUID.randomUUID(),
        LocalDateTime.now().plusHours(
            JobListings.Companion.instance.config
                .getLong("orders.max-order-time"),
        ),
        (ItemBuilder(Material.STONE).amount(1).build()),
        0,
        UUID.randomUUID(),
        UUID.randomUUID(),
        0.0,
        LocalDateTime.now(),
        0,
    )

    private fun toCompleteOrder(timeCompleted: LocalDateTime = LocalDateTime.now()): CompletedOrder {
        //
        return CompletedOrder(
            id,
            timeCompleted,
            item,
            itemAmount,
            owner,
            timeCompleted,
            0,
        )
    }

    private fun toFailedOrder(
        failureType: FailedOrder.FailureType,
        timeIncompleted: LocalDateTime = LocalDateTime.now(),
    ) = FailedOrder(
        id,
        timeIncompleted,
        item,
        itemAmount,
        assignee,
        timeIncompleted,
        0,
        failureType,
    )

    /**
     * Mark the order as incomplete and refund the user.
     * This is used when the assignee did not complete the order in time.
     * @param notify Whether to notify the user and assignee, default is true
     */
    fun incompleteOrder(notify: Boolean = true) {
        JobListings.instance.eco.depositPlayer(getOwner(), reward)
        if (amountTurnedIn == 0) {
            // No point of keeping the order if no items were turned in
            Database.claimedOrderDao.delete(this)
        } else {
            Database.failedOrderDao.create(toFailedOrder(FailedOrder.FailureType.INCOMPLETE))
            Database.claimedOrderDao.delete(this)
        }
        if (!notify) return
        val ownerMessage =
            MessageUtil.getMessage(
                "ClaimedOrders.OrderIncomplete",
                listOf(
                    MessageReplacement(getItemInfo()),
                ),
            )
        messageOwner(ownerMessage)
        val assigneeMessage =
            MessageUtil.getMessage(
                "ClaimedOrders.OrderIncompleteAssignee",
                listOf(
                    MessageReplacement(getItemInfo()),
                ),
            )
        messageAssignee(assigneeMessage)
    }

    /**
     * Mark the order as canceled and notifies the assignee.
     * Used whenever the order is cancelled by the owner.
     * @param notify Whether to notify the user and assignee, default is true
     */
    fun cancelOrder(
        notify: Boolean = true,
        fullRefund: Boolean = false,
    ) {
        if (fullRefund) {
            JobListings.instance.eco.depositPlayer(getOwner(), reward)
        } else {
            JobListings.instance.eco.depositPlayer(getOwner(), (reward / 2))
        }
        if (amountTurnedIn == 0) {
            // No point of keeping the order if no items were turned in
            Database.claimedOrderDao.delete(this)
        } else {
            Database.failedOrderDao.create(toFailedOrder(FailedOrder.FailureType.CANCELED))
            Database.claimedOrderDao.delete(this)
        }
        if (!notify) return
        val assigneeMessage =
            MessageUtil.getMessage(
                "MyOrders.AssigneeMessage",
                listOf(
                    MessageReplacement(getItemInfo()),
                ),
            )
        messageAssignee(assigneeMessage)
    }

    /**
     * Complete the order and pay the assignee
     * @param pay Whether to pay the assignee, default is true
     * @param notify Whether to notify the user and assignee, default is true
     */
    fun completeOrder(
        pay: Boolean = true,
        notify: Boolean = true,
    ) {
        Database.completedOrderDao.create(toCompleteOrder())
        Database.claimedOrderDao.delete(this)
        if (pay) {
            JobListings.instance.eco.depositPlayer(
                getOwner(),
                reward,
            )
        }
        if (!notify) return
        val assigneeMessage =
            MessageUtil.getMessage(
                "OrderComplete.CompletedMessageAssignee",
                listOf(
                    MessageReplacement(getItemInfo()),
                    MessageReplacement(reward.toString()),
                ),
            )
        messageAssignee(assigneeMessage)
        val ownerMessage =
            MessageUtil.getMessage(
                "OrderComplete.CompletedMessageOwner",
                listOf(
                    MessageReplacement(getItemInfo()),
                    MessageReplacement(getAssignee()?.name ?: "Unknown"),
                ),
            )
        messageOwner(ownerMessage)
    }

    fun getStatusComponent() = Messages.getString("OrderStatus.claimed")

    companion object {
        /**
         * Gets the max claimed orders a player can claim, if a player has a permission node it will be grabbed instead.
         * If they don't have one, the config option will be used instead.
         * If the config isn't set, it will default to 1.
         * @return The max order amount.
         */
        fun getMaxOrdersAccepted(player: Player): Int {
            val maxOrdersAccepted =
                player.effectivePermissions
                    .filter {
                        it.permission.startsWith("joblistings.accepted.max.")
                    }.mapNotNull { it.permission.substringAfter("joblistings.accepted.max.").toIntOrNull() }
                    .maxOrNull()
                    ?: JobListings.instance.config.getInt("orders.max-accepted-orders", 1)

            return maxOrdersAccepted.coerceAtLeast(0)
        }

        /**
         * Gets the claimed orders for a player.
         * @param player The player to get the claimed orders for.
         * @return A list of claimed orders for the player.
         */
        fun getClaimedOrders(player: Player): List<ClaimedOrder> {
            val queryBuilder: QueryBuilder<ClaimedOrder, UUID> = Database.claimedOrderDao.queryBuilder()
            queryBuilder
                .where()
                .eq("assignee", player.uniqueId)
            return Database.claimedOrderDao.query(queryBuilder.prepare())
        }

        /**
         * Count the amount of claimed orders for a player.
         * @param player The player to count the claimed orders for.
         * @return The amount of claimed orders for the player.
         */
        fun countClaimedOrders(player: Player): Long {
            val queryBuilder: QueryBuilder<ClaimedOrder, UUID> = Database.claimedOrderDao.queryBuilder()
            queryBuilder
                .where()
                .eq("assignee", player.uniqueId)
            return Database.claimedOrderDao.countOf(queryBuilder.prepare())
        }
    }
}
