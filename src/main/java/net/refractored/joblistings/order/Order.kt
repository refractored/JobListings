package net.refractored.joblistings.order

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import com.willfp.eco.core.items.Items
import net.kyori.adventure.text.Component
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.JobListings.Companion.eco
import net.refractored.joblistings.JobListings.Companion.ecoPlugin
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
import revxrsal.commands.bukkit.player
import java.time.LocalDateTime
import java.util.*

/**
 * Represents a order that has been placed on the job board
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

    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeCreated: LocalDateTime,

    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeExpires: LocalDateTime,

    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeClaimed: LocalDateTime?,

    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeDeadline: LocalDateTime?,

    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeCompleted: LocalDateTime?,

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
    fun getItemInfo(): Component {
        return MessageUtil.getMessage(
            "Orders.OrderInfo",
            listOf(
                MessageReplacement(item.displayName()),
                MessageReplacement(itemAmount.toString()),
                MessageReplacement(cost.toString()),
            )
        )
    }

    /**
     * Get the OfflinePlayer owner of the order
     * @return The owner of the order
     */
    fun getOwner() : OfflinePlayer {
        return Bukkit.getOfflinePlayer(user)
    }

    /**
     * Get the OfflinePlayer assignee of the order
     * @return The assignee of the order, or null if there is no assignee
     */
    fun getAssignee() : OfflinePlayer? {
        assignee.let {
            it ?: return null
            return Bukkit.getOfflinePlayer(it)
        }
    }

    /**
     * Sends a message to the owner if they are online, otherwise it will be sent as a mail
     * @param message The message to send
     * @throws IllegalStateException if the order does not have an assignee
     */
    fun messageOwner(message: Component) {
        getOwner().player?.sendMessage(message)
            ?: run {
                Mail.createMail(user, message)
                return
            }
    }

    /**
     * Sends a message to the assignee if they are online, otherwise it will be sent as a mail
     * @param message The message to send
     * @throws IllegalStateException if the order does not have an assignee
     */
    fun messageAssignee(message: Component) {
        getAssignee().let {
            it ?: throw IllegalStateException("Order does not have an assignee")
            it.player?.sendMessage(message) ?: run {
                Mail.createMail(it.uniqueId, message)
                return
            }
        }
    }

    /**
     * Accept the order and assign it to a player
     * @param assigneePlayer The player who accepted the order
     * @param notify Whether to notify the user and assignee, default is true
     * @throws IllegalArgumentException if the order already has an assignee
     * @throws IllegalArgumentException if the assignee is the same as the user
     * @throws IllegalArgumentException if the order is not pending
     */
    fun acceptOrder(assigneePlayer: Player, notify: Boolean = true) {
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
            )
        )
    }

    /**
     * Remove the order from the database and refund the user
     * @throws IllegalStateException if the order is not pending
     */
    fun removeOrder() {
        if (status != OrderStatus.PENDING) {
            throw IllegalStateException("Order cannot be removed if it is not pending")
        }
        eco.depositPlayer(getOwner(), cost)
        orderDao.delete(this)
    }


    /**
     * Complete the order and pay the assignee
     * @param pay Whether to pay the assignee, default is true
     * @param notify Whether to notify the user and assignee, default is true
     */
    fun completeOrder(pay: Boolean = true, notify: Boolean = true) {
        itemCompleted = itemAmount
        status = OrderStatus.COMPLETED
        timeCompleted = LocalDateTime.now()
        timePickup = LocalDateTime.now().plusHours(JobListings.instance.config.getLong("Orders.PickupDeadline"))
        orderDao.update(this)
        if (pay) {
            getAssignee()?.let {
                eco.depositPlayer(
                    it,
                    cost
                )
            }
        }
        if (!notify) return
        val assigneeMessage = MessageUtil.getMessage(
            "OrderComplete.CompletedMessageAssignee",
            listOf(
                MessageReplacement(getItemInfo()),
                MessageReplacement(cost.toString()),
            )
        )
        messageAssignee(assigneeMessage)
        val ownerMessage = MessageUtil.getMessage(
            "OrderComplete.CompletedMessageOwner",
            listOf(
                MessageReplacement(getItemInfo()),
                MessageReplacement(getAssignee()?.name ?: "Unknown"),
            )
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
        eco.depositPlayer(getOwner(), cost)
        orderDao.delete(this)
        if (notify) {
            val message = Component.text()
                .append(MessageUtil.toComponent(
                    "<red>Your order, <gray>"
                ))
                .append(getItemInfo())
                .append(MessageUtil.toComponent(
                    "<red>has expired and you were refunded!"
                ))
                .build()
            messageOwner(message)
        }
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
        eco.depositPlayer(getOwner(), cost)
        if (itemCompleted == 0) {
            // No point of keeping the order if no items were turned in
            orderDao.delete(this)
        } else {
            orderDao.update(this)
        }
        if (!notify) return
        val ownerMessage = Component.text()
            .append(MessageUtil.toComponent(
                "<red>One of your orders, <gray>"
            ))
            .append(getItemInfo())
            .append(MessageUtil.toComponent(
                "<red>could not be completed in time and you were refunded!"
            ))
            .build()
        messageOwner(ownerMessage)
        if (assignee == null) return // This should never be null, but just in case
        val assigneeMessage = Component.text()
            .append(MessageUtil.toComponent(
                "<red>You were unable to complete your order, <gray>"
            ))
            .append(getItemInfo())
            .append(MessageUtil.toComponent(
                "<red>in time!"
            ))
            .build()
        messageAssignee(assigneeMessage)
    }

    /**
     * Checks if itemstack matches the order itemstack
     * @param itemArg The itemstack to compare
     * @return Whether the itemstack matches the order itemstack
     */
     fun itemMatches(itemArg: ItemStack): Boolean {
        ecoPlugin.let{
            Items.getCustomItem(item)?.let { customItem ->
                return customItem.matches(itemArg)
            }
        }
        return item.isSimilar(itemArg)
    }

    fun isOrderExpired(): Boolean {
        return LocalDateTime.now().isAfter(timeExpires)
    }

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
        ) : Order {
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
            val order = Order(
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
            return order
        }

        /**
         * Get a specific page of the newest orders from the database
         * @param limit Number of orders per page
         * @param offset Starting point for the current page
         * @return List of newest orders for the current page
         */
        fun getPendingOrders(limit: Int, offset: Int): List<Order> {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder.where().eq("status", OrderStatus.PENDING)
            queryBuilder.limit(limit.toLong())
            queryBuilder.offset(offset.toLong())
            return orderDao.query(queryBuilder.prepare()).sortedByDescending { it.timeCreated }
        }

        /**
         * Get a specific page of a player's created orders from the database
         * @param limit Number of orders per page
         * @param offset Starting point for the current page
         * @param playerUUID Player UUID to get orders for
         * @return List of newest orders for the current page
         */
        fun getPlayerCreatedOrders(limit: Int, offset: Int, playerUUID: UUID): List<Order> {
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
        fun getPlayerAcceptedOrders(limit: Int, offset: Int, playerUUID: UUID): List<Order> {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder
                .where().eq("assignee", playerUUID)
                .and().eq("status", OrderStatus.CLAIMED)
                .or().eq("status", OrderStatus.INCOMPLETE)
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