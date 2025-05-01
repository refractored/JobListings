package net.refractored.joblistings.order.tables

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.order.Assignee
import net.refractored.joblistings.order.BaseOrder
import net.refractored.joblistings.serializers.ItemstackSerializers
import net.refractored.joblistings.serializers.LocalDateTimeSerializers
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents an order that has been placed on the job board
 */
@DatabaseTable(tableName = "joblistings_claimed_orders")
data class ClaimedOrder(
    @DatabaseField(id = true)
    override val id: UUID,
    @DatabaseField
    override var reward: Double,
    @DatabaseField
    override var user: UUID,
    @DatabaseField(persisterClass = ItemstackSerializers::class)
    override var item: ItemStack,
    @DatabaseField
    override var itemAmount: Int,
    @DatabaseField
    override var assignee: UUID,
    @DatabaseField
    var amountTurnedIn: Int,
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeClaimed: LocalDateTime,
) : BaseOrder,
    Assignee {
    /**
     * This constructor should only be used for ORMLite
     */
    constructor() : this(
        UUID.randomUUID(),
        0.0,
        UUID.randomUUID(),
        (ItemBuilder(Material.STONE).amount(1).build()),
        69,
        UUID.randomUUID(),
        0,
        LocalDateTime.now(),
    )

    private fun toCompleteOrder(timeCompleted: LocalDateTime = LocalDateTime.now()) =
        CompletedOrder(
            id,
            reward,
            user,
            item,
            itemAmount,
            amountTurnedIn,
            timeCompleted,
        )

    private fun toFailedOrder(
        failureType: FailedOrder.FailureStatus,
        timeIncompleted: LocalDateTime = LocalDateTime.now(),
    ) = FailedOrder(
        id,
        assignee,
        item,
        amountTurnedIn,
        0,
        timeIncompleted,
        failureType,
    )

    /**
     * Mark the order as incomplete and refund the user.
     * This is used when the assignee did not complete the order in time.
     * @param notify Whether to notify the user and assignee, default is true
     */
    fun incompleteOrder(notify: Boolean = true) {
        JobListings.instance.eco.depositPlayer(getOwner(), reward)
        Database.failedOrderDao.create(toFailedOrder(FailedOrder.FailureStatus.INCOMPLETE))
        Database.claimedOrderDao.delete(this)
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
        Database.failedOrderDao.create(toFailedOrder(FailedOrder.FailureStatus.CANCELED))
        Database.claimedOrderDao.delete(this)
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

    override fun getStatusComponent() = MessageUtil.getMessage("OrderStatus.claimed")
}
