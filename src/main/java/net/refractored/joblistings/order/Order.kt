package net.refractored.joblistings.order

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.refractored.joblistings.database.Database.Companion.mailDao
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.mail.Mail
import net.refractored.joblistings.serializers.ItemstackSerializers
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Material
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

    @DatabaseField
    var timeCreated: LocalDateTime,

    @DatabaseField
    var timeExpires: LocalDateTime,

    @DatabaseField
    var timeClaimed: LocalDateTime?,

    @DatabaseField
    var timeDeadline: LocalDateTime?,

    @DatabaseField
    var timeCompleted: LocalDateTime?,

    @DatabaseField
    var status: OrderStatus,

    @DatabaseField(dataType = DataType.LONG_STRING)
    var item: String,

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
        ItemstackSerializers.serialize(ItemBuilder(Material.STONE).build()),
        false,
    )

    companion object {
        /**
         * Get a specific page of the newest orders from the database
         * @param limit Number of orders per page
         * @param offset Starting point for the current page
         * @return List of newest orders for the current page
         */
        fun getOrders(limit: Int, offset: Int): List<Order> {
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
            queryBuilder.where().eq("asignee", playerUUID)
            return orderDao.query(queryBuilder.prepare())
        }

        fun isOrderExpired(order: Order): Boolean {
            return (order.timeExpires > LocalDateTime.now())
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
                    var message = MessageUtil.toComponent(
                        "<red>One of your orders ${
                            ItemstackSerializers.deserialize(order.item)!!.displayName()
                        } expired!"
                    )
                    Bukkit.getPlayer(order.user)?.sendMessage(message)
                        ?: run {
                            mailDao.create(
                                Mail(
                                    id = UUID.randomUUID(),
                                    user = order.user,
                                    timeCreated = LocalDateTime.now(),
                                    timeExpires = LocalDateTime.now().plusDays(7),
                                    message = message.toString(),
                                )
                            )
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
                    if (isOrderDeadlinePassed(order)) {
                        order.status = OrderStatus.INCOMPLETE
                        orderDao.update(order)
                    }
                }
            }

        }
    }
