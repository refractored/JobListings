package net.refractored.joblistings.order

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
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

    @DatabaseField
    var cost: Double,

    @DatabaseField
    var user: UUID,

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

    @DatabaseField
    var status: OrderStatus,

    @DatabaseField(persisterClass = ItemstackSerializers::class)
    var item: ItemStack,

    @DatabaseField
    val userClaimed: Boolean

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
        LocalDateTime.now().plusHours(12),
        null,
        null,
        null,
        OrderStatus.PENDING,
        (ItemBuilder(Material.STONE).build()),
        false,
    )

    companion object {
        /**
         * Get a specific page of the newest orders from the database
         * @param limit Number of orders per page
         * @param offset Starting point for the current page
         * @return List of newest orders for the current page
         */
        fun getPendingOrders(limit: Int, offset: Int): List<Order> {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder.orderBy("timeCreated", false)
            queryBuilder.limit(limit.toLong())
            queryBuilder.offset(offset.toLong())
            queryBuilder.where().eq("status", OrderStatus.PENDING)
            return orderDao.query(queryBuilder.prepare())
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
            queryBuilder.orderBy("timeCreated", false)
            queryBuilder.limit(limit.toLong())
            queryBuilder.offset(offset.toLong())
            queryBuilder.where().eq("user", playerUUID)
            return orderDao.query(queryBuilder.prepare())
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
            queryBuilder.orderBy("timeCreated", false)
            queryBuilder.limit(limit.toLong())
            queryBuilder.offset(offset.toLong())
            queryBuilder.where().eq("status", OrderStatus.CLAIMED)
            queryBuilder.where().eq("asignee", playerUUID)
            return orderDao.query(queryBuilder.prepare())
        }

        fun isOrderExpired(order: Order): Boolean {
            return (order.timeExpires <= LocalDateTime.now())
        }

        fun updateExpiredOrders() {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder.orderBy("timeCreated", true)
            queryBuilder.where().eq("status", OrderStatus.PENDING)
            val orders = orderDao.query(queryBuilder.prepare())
            for (order in orders) {
                if (isOrderExpired(order)) {
                    order.status = OrderStatus.EXPIRED
                    orderDao.update(order)
                    val item = order.item
                    val orderInfo = "${PlainTextComponentSerializer.plainText().serialize(item.displayName())}  x${item.amount}"
                    val message = MessageUtil.toComponent(
                        "<red>One of your orders <gray>\"${orderInfo}\"</gray> expired!"
                    )
                    Bukkit.getPlayer(order.user)?.sendMessage(message)
                        ?: run {
                            // TODO: Check if player exists.
                            Mail.createMail(order.user, message)
                        }

                }
            }
        }


        fun isOrderDeadlinePassed(order: Order): Boolean {
            val deadline = order.timeDeadline ?: return false
            return LocalDateTime.now() >= deadline
        }

        fun updateDeadlineOrders() {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder.orderBy("timeClaimed", true)
            queryBuilder.where().eq("status", OrderStatus.CLAIMED)
            orderDao.query(queryBuilder.prepare()).forEach { order ->
                if (!isOrderDeadlinePassed(order)) return
                order.status = OrderStatus.INCOMPLETE
                orderDao.update(order)
                val item = order.item

                val orderInfo = "${PlainTextComponentSerializer.plainText().serialize(item.displayName())}  x${item.amount}"
                val ownerMessage = MessageUtil.toComponent(
                    "<red>One of your orders, <gray>\"${orderInfo}\"</gray>, could not be completed in time!"
                )
                Bukkit.getPlayer(order.user)?.sendMessage(ownerMessage)
                    ?: run {
                        Mail.createMail(order.user, ownerMessage)
                    }

                if (order.assignee == null) return // This should never be null, but just in case

                val asigneeMessage = MessageUtil.toComponent(
                    "<red>You were unable to complete your order, <gray>\"${orderInfo}\"</gray>, in time!"
                )
                Bukkit.getPlayer(order.user)?.sendMessage(asigneeMessage)
                    ?: run {
                        Mail.createMail(order.assignee!!, asigneeMessage)
                    }
            }
        }

        fun acceptOrder(order: Order, assignee: Player) {
            if (order.assignee != null) {
                throw IllegalArgumentException("Order already has an assignee")
            }
            if (order.user == assignee.uniqueId) {
                throw IllegalArgumentException("Cannot accept your own order")
            }
            if (order.status != OrderStatus.PENDING) {
                throw IllegalArgumentException("Order is not pending")
            }
            order.assignee = assignee.uniqueId
            order.timeClaimed = LocalDateTime.now()
            order.timeDeadline = LocalDateTime.now().plusHours(12)
            order.status = OrderStatus.CLAIMED
            orderDao.update(order)
        }
    }
}