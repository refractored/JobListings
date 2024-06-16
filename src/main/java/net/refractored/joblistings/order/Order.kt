package net.refractored.joblistings.order

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import net.kyori.adventure.text.Component
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

    fun getOwner() : OfflinePlayer {
        return Bukkit.getOfflinePlayer(user)
    }

    fun getAssignee() : OfflinePlayer? {
        assignee.let {
            it ?: return null
            return Bukkit.getOfflinePlayer(it)
        }
    }

    fun messageOwner(message: Component) {
        getOwner().player?.sendMessage(message)
            ?: run {
                Mail.createMail(user, message)
                return
            }
    }

    fun messageAssignee(message: Component) {
        getAssignee().let {
            it ?: throw IllegalStateException("Order does not have an assignee")
            it.player?.sendMessage(message) ?: run {
                Mail.createMail(it.uniqueId, message)
                return
            }
        }
    }

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

    fun completeOrder(){
        itemCompleted = itemAmount
        status = OrderStatus.COMPLETED
        timeCompleted = LocalDateTime.now()
        orderDao.update(this)
    }

    fun expireOrder(notify: Boolean = true) {
        if (status == OrderStatus.EXPIRED) {
            throw IllegalStateException("Order is already expired")
        }
        if (status == OrderStatus.INCOMPLETE) {
            throw IllegalStateException("Order cannot be marked expired if its status is INCOMPLETE")
        }
        status = OrderStatus.EXPIRED
        orderDao.update(this)
        if (notify) {
            val message = Component.text()
                .append(MessageUtil.toComponent(
                    "<red>Your order, <gray>"
                ))
                .append(getItemInfo())
                .append(MessageUtil.toComponent(
                    "<red>has expired!"
                ))
                .build()
            messageOwner(message)
        }
    }

    fun incompleteOrder(notify: Boolean = true) {
        if (status == OrderStatus.INCOMPLETE) {
            throw IllegalStateException("Order is already marked incomplete")
        }
        if (status == OrderStatus.EXPIRED) {
            throw IllegalStateException("Order cannot be marked incomplete if it is expired")
        }
        status = OrderStatus.INCOMPLETE
        orderDao.update(this)
        if (!notify) return
        val ownerMessage = Component.text()
            .append(MessageUtil.toComponent(
                "<red>One of your orders, <gray>"
            ))
            .append(getItemInfo())
            .append(MessageUtil.toComponent(
                "<red>could not be completed in time!"
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

    fun isOrderExpired(): Boolean {
        return LocalDateTime.now().isAfter(timeExpires)
    }

    fun isOrderDeadlinePassed(): Boolean {
        val deadline = timeDeadline ?: return false
        return LocalDateTime.now().isAfter(deadline)
    }

    companion object {

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

        fun updateDeadlineOrders() {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder.orderBy("timeClaimed", true)
            queryBuilder.where().eq("status", OrderStatus.CLAIMED)
            orderDao.query(queryBuilder.prepare()).forEach { order ->
                if (!order.isOrderDeadlinePassed()) return
                order.incompleteOrder()
            }
        }
    }
}