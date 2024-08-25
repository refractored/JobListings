package net.refractored.joblistings.order

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import dev.lone.itemsadder.api.ItemsAdder
import dev.unnm3d.redischat.chat.objects.ChannelAudience
import dev.unnm3d.redischat.chat.objects.ChatMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.mail.Mail
import net.refractored.joblistings.serializers.ItemstackSerializers
import net.refractored.joblistings.serializers.LocalDateTimeSerializers
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.LocalDateTime
import java.util.*

/**
 * Represents an order that has been placed on the job board
 */
@DatabaseTable(tableName = "joblistings_orders")
data class Order(
    @DatabaseField(id = true)
    val id: UUID,
    /**
     * The reward of the order if completed
     */
    @DatabaseField
    var cost: Double,
    /**
     * The player's uuid who created the order
     */
    @DatabaseField
    var user: UUID,
    /**
     * The player's uuid who accepted the order
     */
    @DatabaseField
    var assignee: UUID?,
    /**
     * The time the order was created
     */
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeCreated: LocalDateTime,
    /**
     * The time the order expires
     * This is only used if the order was never claimed
     */
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeExpires: LocalDateTime,
    /**
     * The time the order was claimed
     */
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeClaimed: LocalDateTime?,
    /**
     * The time the order is due
     * This is only used if the order was claimed
     * If this is not completed in time, the order will be marked as incomplete
     */
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeDeadline: LocalDateTime?,
    /**
     * The time the order was completed
     */
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeCompleted: LocalDateTime?,
    /**
     * The time the order gets permanently removed
     */
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timePickup: LocalDateTime?,
    /**
     * The status of the order
     * @see OrderStatus
     */
    @DatabaseField
    var status: OrderStatus,
    /**
     * The item
     *
     * This ItemStack is not representative of the amount of items required to complete it.
     * @see itemAmount
     */
    @DatabaseField(persisterClass = ItemstackSerializers::class)
    var item: ItemStack,
    /**
     * The amount of items required to complete the order
     */
    @DatabaseField
    var itemAmount: Int,
    /**
     * The amount of items the assignee has turned in
     */
    @DatabaseField
    var itemCompleted: Int,
    /**
     * The amount of items has been returned to the assignee
     * ONLY if the order was not completed in time, or cancelled.
     */
    @DatabaseField
    var itemsReturned: Int,
    /**
     * The amount of items has been obtained by the user
     * ONLY if the order was completed in time.
     */
    @DatabaseField
    var itemsObtained: Int,
) {
    /**
     * This constructor should only be used for ORMLite
     */
    constructor() : this(
        UUID.randomUUID(),
        0.0,
        UUID.randomUUID(),
        null,
        LocalDateTime.now(),
        LocalDateTime.now().plusHours(JobListings.instance.config.getLong("Orders.MinOrdersTime")),
        null,
        null,
        null,
        null,
        OrderStatus.PENDING,
        (ItemBuilder(Material.STONE).amount(1).build()),
        69,
        0,
        0,
        0,
    )

    /**
     * Get the display name of the item
     * @return The display name of the item
     */
    fun getItemInfo(): Component =
        MessageUtil.getMessage(
            "Orders.OrderInfo",
            listOf(
                MessageReplacement(item.displayName()),
                MessageReplacement(itemAmount.toString()),
                MessageReplacement(cost.toString()), // Optional
            ),
        )

    /**
     * Get the OfflinePlayer of the owner of the order
     * @return The owner of the order
     */
    fun getOwner(): OfflinePlayer = Bukkit.getOfflinePlayer(user)

    /**
     * Get the OfflinePlayer of the assignee of the order
     * @return The assignee of the order, or null if there is no assignee
     */
    fun getAssignee(): OfflinePlayer? {
        val assigneeUUID = assignee ?: return null
        return Bukkit.getOfflinePlayer(assigneeUUID)
    }

    /**
     * Sends a message to the owner if they are online, otherwise it will be sent as a mail
     * @param message The message to send
     * @throws IllegalStateException if the order does not have an assignee
     */
    fun messageOwner(message: Component) {
        getOwner().player?.sendMessage(message) ?: Mail.createMail(user, message)
    }

    /**
     * Sends a message to the assignee if they are online, otherwise it will be sent as a mail
     * @param message The message to send
     * @throws IllegalStateException if the order does not have an assignee
     */
    fun messageAssignee(message: Component) {
        val offlineAssignee = getAssignee() ?: throw IllegalStateException("Order does not have an assignee")
        offlineAssignee.player?.sendMessage(message) ?: Mail.createMail(offlineAssignee.uniqueId, message)
    }

    /**
     * Accept the order and assign it to a player
     * @param assigneePlayer The player who accepted the order
     * @param notify Whether to notify the user and assignee, default is true
     * @throws IllegalArgumentException if the order already has an assignee
     * @throws IllegalArgumentException if the assignee is the same as the user
     * @throws IllegalArgumentException if the order is not pending
     */
    fun acceptOrder(
        assigneePlayer: Player,
        notify: Boolean = true,
    ) {
        if (assignee != null) {
            throw IllegalArgumentException("Order already has an assignee")
        }
        if (user == assigneePlayer.uniqueId) {
            throw IllegalArgumentException("Assignee cannot be the same as the user")
        }
        if (status != OrderStatus.PENDING) {
            throw IllegalArgumentException("Order is not pending")
        }
        assignee = assigneePlayer.uniqueId
        timeClaimed = LocalDateTime.now()
        timeDeadline = LocalDateTime.now().plusHours(JobListings.instance.config.getLong("Orders.OrderDeadline"))
        status = OrderStatus.CLAIMED
        orderDao.update(this)
        if (!notify) return
        val ownerMessage =
            MessageUtil.getMessage(
                "AllOrders.OrderAcceptedNotification",
                listOf(
                    MessageReplacement(getItemInfo()),
                    MessageReplacement(assigneePlayer.displayName()),
                ),
            )
        messageOwner(ownerMessage)
        messageAssignee(
            MessageUtil.getMessage(
                "AllOrders.OrderAccepted",
            ),
        )
    }

    /**
     * Remove the order from the database and refund the user
     * @throws IllegalStateException if the order is not pending
     */
    fun removeOrder() {
        if (status != OrderStatus.PENDING) {
            throw IllegalStateException("Order is not pending.")
        }
        JobListings.instance.eco.depositPlayer(getOwner(), cost)
        orderDao.delete(this)
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
        val assigneePlayer = getAssignee() ?: throw IllegalStateException("Order does not have an assignee")
        itemCompleted = itemAmount
        status = OrderStatus.COMPLETED
        timeCompleted = LocalDateTime.now()
        timePickup = LocalDateTime.now().plusHours(JobListings.instance.config.getLong("Orders.PickupDeadline"))
        orderDao.update(this)
        if (pay) {
            JobListings.instance.eco.depositPlayer(
                assigneePlayer,
                cost,
            )
        }
        if (!notify) return
        val assigneeMessage =
            MessageUtil.getMessage(
                "OrderComplete.CompletedMessageAssignee",
                listOf(
                    MessageReplacement(getItemInfo()),
                    MessageReplacement(cost.toString()),
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

    /**
     * Mark the order as expired and refund the user
     * @param notify Whether to notify the user, default is true
     */
    fun expireOrder(notify: Boolean = true) {
        if (status == OrderStatus.INCOMPLETE) {
            throw IllegalStateException("Order cannot be marked expired if its status is INCOMPLETE")
        }
        JobListings.instance.eco.depositPlayer(getOwner(), cost)
        orderDao.delete(this)
        if (notify) {
            val message =
                Component
                    .text()
                    .append(
                        MessageUtil.toComponent(
                            "<red>Your order, <gray>",
                        ),
                    ).append(getItemInfo())
                    .append(
                        MessageUtil.toComponent(
                            "<red>has expired and you were refunded!",
                        ),
                    ).build()
            messageOwner(message)
        }
    }

    /**
     * Mark the order as canceled and notifies the assignee.
     * @param notify Whether to notify the user and assignee, default is true
     */
    fun cancelOrder(
        notify: Boolean = true,
        fullRefund: Boolean = false,
    ) {
        if (status == OrderStatus.INCOMPLETE) {
            throw IllegalStateException("Order is already marked incomplete")
        }
        status = OrderStatus.CANCELLED
        if (fullRefund) {
            JobListings.instance.eco.depositPlayer(getOwner(), cost)
        } else {
            JobListings.instance.eco.depositPlayer(getOwner(), (cost / 2))
        }
        if (itemCompleted == 0) {
            // No point of keeping the order if no items were turned in
            orderDao.delete(this)
        } else {
            orderDao.update(this)
        }
        if (!notify) return
        if (assignee == null) return // This should never be null, but just in case
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
     * Mark the order as incomplete and refund the user
     * @param notify Whether to notify the user and assignee, default is true
     */
    fun incompleteOrder(notify: Boolean = true) {
        if (status == OrderStatus.INCOMPLETE) {
            throw IllegalStateException("Order is already marked incomplete")
        }
        status = OrderStatus.INCOMPLETE
        JobListings.instance.eco.depositPlayer(getOwner(), cost)
        if (itemCompleted == 0) {
            // No point of keeping the order if no items were turned in
            orderDao.delete(this)
        } else {
            timePickup = LocalDateTime.now().plusHours(JobListings.instance.config.getLong("Orders.PickupDeadline"))
            orderDao.update(this)
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
        if (assignee == null) return // This should never be null, but just in case
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
     * Checks if itemstack matches the order's itemstack
     * @param itemArg The itemstack to compare
     * @return Whether the itemstack matches the order itemstack
     */
    fun itemMatches(itemArg: ItemStack): Boolean {
        if (JobListings.instance.ecoPlugin) {
            Items.getCustomItem(item)?.let { customItem ->
                return customItem.matches(itemArg)
            }
        }
        return item.isSimilar(itemArg)
    }

    fun isOrderExpired(): Boolean = LocalDateTime.now().isAfter(timeExpires)

    fun isOrderDeadlinePassed(): Boolean {
        val deadline = timeDeadline ?: return false
        return LocalDateTime.now().isAfter(deadline)
    }

    fun isOrderPickupPassed(): Boolean {
        val pickupTime = timePickup ?: return false
        return LocalDateTime.now().isAfter(pickupTime)
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
        fun createOrder(
            user: UUID,
            cost: Double,
            item: ItemStack,
            amount: Int,
            hours: Long,
            announce: Boolean = true,
        ): Order {
            val maxItems = JobListings.instance.config.getInt("Orders.MaximumItems")
            when {
                maxItems == -1 && amount > item.maxStackSize -> {
                    throw IllegalArgumentException("Item stack size exceeded")
                }
                maxItems != 0 && amount >= maxItems -> {
                    throw IllegalArgumentException("Max orders exceeded")
                }
            }
            if (hours > JobListings.instance.config.getLong("Orders.MaxOrdersTime")) {
                throw IllegalArgumentException("Order time exceeds maximum")
            }
            if (hours < JobListings.instance.config.getLong("Orders.MinOrdersTime")) {
                throw IllegalArgumentException("Order time exceeds maximum")
            }
            item.amount = 1
            val order =
                Order(
                    UUID.randomUUID(),
                    cost,
                    user,
                    null,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(hours),
                    null,
                    null,
                    null,
                    null,
                    OrderStatus.PENDING,
                    item,
                    amount,
                    0,
                    0,
                    0,
                )
            orderDao.create(order)

            if (announce && JobListings.instance.config.getBoolean("Orders.AnnounceOnOrderCreate", false)) {
                val message =
                    MessageUtil.getMessage(
                        "Orders.Announcement",
                        listOf(
                            MessageReplacement(order.getOwner().name ?: "Unknown"),
                            MessageReplacement(order.getItemInfo()),
                            MessageReplacement(order.cost.toString()),
                        ),
                    )
                if (JobListings.instance.redisChat != null && JobListings.instance.config.getBoolean("Redischat.RedisChatAnnounce", false)) {
                    JobListings.instance.redisChat!!
                        .dataManager
                        .sendChatMessage(
                            ChatMessage(
                                ChannelAudience(),
                                "",
                                MiniMessage.miniMessage().serialize(message),
                            ChannelAudience(JobListings.instance.config.getString("Redischat.Channel", "public")),
                        )
                    )
                } else {
                    JobListings.instance.server.broadcast(message)
                }
            }
            return order
        }

        /**
         * Get a specific page of the newest orders from the database
         * @param limit Number of orders per page
         * @param offset Starting point for the current page
         * @return List of newest orders for the current page
         */
        fun getPendingOrders(
            limit: Int,
            offset: Int,
        ): List<Order> {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder.where().eq("status", OrderStatus.PENDING)
            queryBuilder.limit(limit.toLong())
            queryBuilder.offset(offset.toLong())
            return orderDao.query(queryBuilder.prepare()).sortedByDescending { it.timeCreated }
        }

        /**
         * Gets the max orders a player can create, if a player has a permission node it will be grabbed instead.
         * If they don't have one, the config option will be used instead.
         * If the config isn't set, it will default to 1.
         * @return The max order amount.
         */
        fun getMaxOrders(player: Player): Int {
            val maxOrderAmount =
                player.effectivePermissions
                    .filter {
                        it.permission.startsWith("joblistings.create.max.")
                    }.mapNotNull { it.permission.substringAfter("joblistings.create.max.").toIntOrNull() }
                    .maxOrNull()
                    ?: JobListings.instance.config.getInt("Orders.MaxOrders", 1)

            return maxOrderAmount.coerceAtLeast(0)
        }

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
                    ?: JobListings.instance.config.getInt("Orders.MaxOrdersAccepted", 1)

            return maxOrdersAccepted.coerceAtLeast(0)
        }

        /**
         * Get a specific page of a player's created orders from the database
         * @param limit Number of orders per page
         * @param offset Starting point for the current page
         * @param playerUUID Player UUID to get orders for
         * @return List of newest orders for the current page
         */
        fun getPlayerCreatedOrders(
            limit: Int,
            offset: Int,
            playerUUID: UUID,
        ): List<Order> {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder.where().eq("user", playerUUID)
            queryBuilder.limit(limit.toLong())
            queryBuilder.offset(offset.toLong())
            return orderDao.query(queryBuilder.prepare()).sortedByDescending { it.timeCreated }
        }

        /**
         * Get a specific page of a player's accepted orders from the database
         * @param limit Number of orders per page
         * @param offset Starting point for the current page
         * @param playerUUID Player UUID to get orders for
         * @return List of newest orders for the current page
         */
        fun getPlayerAcceptedOrders(
            limit: Int,
            offset: Int,
            playerUUID: UUID,
        ): List<Order> {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder
                .where()
                .eq("assignee", playerUUID)
                .and()
                .eq("status", OrderStatus.CLAIMED)
                .or()
                .eq("status", OrderStatus.INCOMPLETE)
                .or()
                .eq("status", OrderStatus.CANCELLED)
            queryBuilder.limit(limit.toLong())
            queryBuilder.offset(offset.toLong())
            return orderDao.query(queryBuilder.prepare()).sortedByDescending { it.timeCreated }
        }

        /**
         * If the order was never claimed, and the order expire date has passed it will be marked as expired
         */
        fun updateExpiredOrders() {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder.orderBy("timeCreated", true)
            queryBuilder.where().eq("status", OrderStatus.PENDING)
            val orders = orderDao.query(queryBuilder.prepare())
            for (order in orders) {
                if (!order.isOrderExpired()) return
                order.expireOrder()
            }
        }

        /**
         * Updates all orders that have passed their deadline
         */
        fun updateDeadlineOrders() {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder.orderBy("timeClaimed", true)
            queryBuilder.where().eq("status", OrderStatus.CLAIMED)
            orderDao.query(queryBuilder.prepare()).forEach { order ->
                if (!order.isOrderDeadlinePassed()) return
                order.incompleteOrder()
            }
        }

        /**
         * Deletes all orders that were not picked up in time
         */
        fun updatePickupDeadline() {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder.orderBy("timeClaimed", true)
            queryBuilder.where().eq("status", OrderStatus.CLAIMED)
            orderDao.query(queryBuilder.prepare()).forEach { order ->
                if (!order.isOrderPickupPassed()) return
                orderDao.delete(order)
            }
        }
    }
}
