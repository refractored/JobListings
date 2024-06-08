package net.refractored.joblistings.order

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.mail.Mail
import net.refractored.joblistings.serializers.ItemstackSerializers
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
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

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    var timeCreated: LocalDateTime,

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    var timeExpires: LocalDateTime,

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    var timeClaimed: LocalDateTime?,

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    var timeDeadline: LocalDateTime?,

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    var timeCompleted: LocalDateTime?,

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

    /**
     * True if the order has been expired, and the user has been refunded
     */
    @DatabaseField
    var moneyReturned: Boolean,

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
        OrderStatus.PENDING,
        (ItemBuilder(Material.STONE).amount(1).build()),
        69,
        0,
        0,
        0,
        false
    )

    fun getItemInfo(): Component {
        return Component.text()
            .append(
                item.displayName()
            )
            .append(
                MessageUtil.toComponent(" x${itemAmount}<reset>")
            )
            .build()
    }

    fun messageOwner(message: Component) {
        Bukkit.getPlayer(user)?.sendMessage(message)
            ?: run {
                Mail.createMail(user, message)
                return
            }
    }

    fun messageAssignee(message: Component) {
        val player = assignee
            ?: throw IllegalStateException("Order does not have an assignee")
        Bukkit.getPlayer(player)?.sendMessage(message)
            ?: run {
                Mail.createMail(player, message)
                return
            }
    }

    fun isOrderDeadlinePassed(): Boolean {
        val deadline = timeDeadline ?: return false
        return LocalDateTime.now().isAfter(deadline)
    }

    fun acceptOrder(assigneePlayer: Player) {
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
    }

    fun isOrderExpired(): Boolean {
        return LocalDateTime.now().isAfter(timeExpires)
    }

    companion object {
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
                order.status = OrderStatus.EXPIRED
                orderDao.update(order)
                val message = Component.text()
                    .append(MessageUtil.toComponent(
                    "<red>One of your orders, <gray>"
                    ))
                    .append(order.getItemInfo())
                    .append(MessageUtil.toComponent(
                        "<red>expired!"
                    ))
                    .build()
                order.messageOwner(message)
            }
        }

        fun updateDeadlineOrders() {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder.orderBy("timeClaimed", true)
            queryBuilder.where().eq("status", OrderStatus.CLAIMED)
            orderDao.query(queryBuilder.prepare()).forEach { order ->
                if (!order.isOrderDeadlinePassed()) return
                order.status = OrderStatus.INCOMPLETE
                orderDao.update(order)
                val ownerMessage = Component.text()
                .append(MessageUtil.toComponent(
                    "<red>One of your orders, <gray>"
                ))
                .append(order.getItemInfo())
                .append(MessageUtil.toComponent(
                    "<red>could not be completed in time!"
                ))
                .build()
                order.messageOwner(ownerMessage)
                if (order.assignee == null) return // This should never be null, but just in case
                val assigneeMessage = Component.text()
                    .append(MessageUtil.toComponent(
                        "<red>You were unable to complete your order, <gray>"
                    ))
                    .append(order.getItemInfo())
                    .append(MessageUtil.toComponent(
                        "<red>in time!"
                    ))
                    .build()
                order.messageAssignee(assigneeMessage)
            }
        }
    }
}