package net.refractored.joblistings.order.tables

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.order.BaseOrder
import net.refractored.joblistings.serializers.ItemstackSerializers
import net.refractored.joblistings.serializers.LocalDateTimeSerializers
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.LocalDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

//                              ┌-> IncompleteOrder
// PendingOrder -> ClaimedOrder ┼-> CanceledOrder
//                              └-> CompletedOrder

/**
 * Represents an order that has been placed on the job board
 */
@DatabaseTable(tableName = "joblistings_pending_orders")
data class PendingOrder(
    @DatabaseField(id = true)
    override val id: UUID,
    /**
     * The reward of the order if completed
     */
    @DatabaseField
    override var reward: Double,
    /**
     * The player's uuid who created the order
     */
    @DatabaseField
    override var user: UUID,
    /**
     * The item
     *
     * This ItemStack is not representative of the amount of items required to complete it.
     * @see itemAmount
     */
    @DatabaseField(persisterClass = ItemstackSerializers::class)
    override var item: ItemStack,
    /**
     * The amount of items required to complete the order
     */
    @DatabaseField
    override var itemAmount: Int,
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeCreated: LocalDateTime,
) : BaseOrder {
    /**
     * This constructor should only be used for ORMLite
     */
    constructor() : this(
        UUID.randomUUID(),
        0.0,
        UUID.randomUUID(),
        (ItemBuilder(Material.STONE).amount(1).build()),
        69,
        LocalDateTime.now(),
    )

    fun isOrderDeadlinePassed(): Boolean {
        val deadline =
            timeCreated.plusHours(
                JobListings.Companion.instance.config
                    .getLong("orders.order-deadline"),
            )
        return LocalDateTime.now().isAfter(deadline)
    }

    private fun toClaimedOrder(
        player: Player,
        localDateTime: LocalDateTime = LocalDateTime.now(),
    ): ClaimedOrder =
        ClaimedOrder(
            id,
            reward,
            user,
            item,
            itemAmount,
            player.uniqueId,
            0,
            localDateTime,
        )

    /**
     * Remove the order from the database and refund the user
     */
    fun refundOrder() {
        JobListings.Companion.instance.eco
            .depositPlayer(getOwner(), reward)
        Database.pendingOrderDao.delete(this)
    }

    override fun getStatusComponent() = MessageUtil.getMessage("OrderStatus.pending")

    /**
     * Mark the order as expired and refund the user.
     * Order is marked incomplete whenever it is not done in time by its assignee.
     * @param notify Whether to notify the user, default is true
     */
    fun expireOrder(notify: Boolean = true) {
        JobListings.instance.eco.depositPlayer(getOwner(), reward)
        Database.pendingOrderDao.delete(this)
        if (notify) {
            val message =
                MessageUtil.getMessage(
                    "AllOrders.OrderExpired",
                    listOf(
                        MessageReplacement(getItemInfo()),
                    ),
                )
            messageOwner(message)
        }
    }

    /**
     * Mark the order as claimed and move it to the claimed orders table.
     *
     * @return The claimed order
     */
    fun markClaimed(
        assignee: Player,
        notify: Boolean = true,
    ): ClaimedOrder {
        val claimedOrder = toClaimedOrder(assignee)
        Database.pendingOrderDao.delete(this)
        Database.claimedOrderDao.create(claimedOrder)
        if (!notify) return claimedOrder
        val ownerMessage =
            MessageUtil.Companion.getMessage(
                "AllOrders.OrderAcceptedNotification",
                listOf(
                    MessageReplacement(getItemInfo()),
                    MessageReplacement(assignee.displayName()),
                ),
            )
        messageOwner(ownerMessage)
        assignee.sendMessage(
            MessageUtil.Companion.getMessage(
                "AllOrders.OrderAccepted",
            ),
        )
        return claimedOrder
    }

    companion object {
        /**
         * Create a new order and insert it into the database
         * @param user The user who created the order
         * @param cost The reward for completing the order
         * @param item The itemstack required to complete the order
         * @param amount The amount of items required to complete the order
         * @param hours The amount of hours the order will be available for
         * @return The created order
         */
        fun create(
            user: UUID,
            cost: Double,
            item: ItemStack,
            amount: Int,
            hours: Long,
            announce: Boolean = true,
        ): PendingOrder {
            val maxItems =
                JobListings.Companion.instance.config
                    .getInt("orders.max-items")
            when {
                maxItems == -1 && amount > item.maxStackSize -> {
                    throw IllegalArgumentException("Item stack size exceeded")
                }
                maxItems != 0 && amount >= maxItems -> {
                    throw IllegalArgumentException("Max orders exceeded")
                }
            }
            if (hours >
                JobListings.Companion.instance.config
                    .getLong("orders.max-order-time")
            ) {
                throw IllegalArgumentException("Order time exceeds maximum")
            }
            if (hours <
                JobListings.Companion.instance.config
                    .getLong("orders.min-order-time")
            ) {
                throw IllegalArgumentException("Order time exceeds maximum")
            }
            item.amount = 1

            val pendingOrder =
                PendingOrder(
                    UUID.randomUUID(),
                    cost,
                    user,
                    item,
                    amount,
                    LocalDateTime.now(),
                )
            Database.pendingOrderDao.create(pendingOrder)

            if (announce &&
                JobListings.Companion.instance.config
                    .getBoolean("orders.AnnounceOnOrderCreate", false)
            ) {
                val message =
                    MessageUtil.Companion.getMessage(
                        "orders.Announcement",
                        listOf(
                            MessageReplacement(pendingOrder.getOwner().name ?: "Unknown"),
                            MessageReplacement(pendingOrder.getItemInfo()),
                            MessageReplacement(pendingOrder.reward.toString()),
                        ),
                    )
                if (JobListings.Companion.instance.redisChat != null &&
                    JobListings.Companion.instance.config
                        .getBoolean("Redischat.RedisChatAnnounce", false)
                ) {
                    JobListings.Companion.instance.redisChat!!.broadcastMessage(
                        JobListings.Companion.instance.redisChat!!
                            .getChannel(
                                JobListings.Companion.instance.config
                                    .getString("Redischat.RedisChatChannel"),
                            ).getOrNull()
                            ?: throw IllegalStateException("Channel not found"),
                        MiniMessage.miniMessage().serialize(message),
                    )
                } else {
                    JobListings.Companion.instance.server
                        .broadcast(message)
                }
            }
            return pendingOrder
        }
    }
}
