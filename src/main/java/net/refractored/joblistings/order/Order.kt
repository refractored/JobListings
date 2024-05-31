package net.refractored.joblistings.order

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.serializers.ItemstackSerializers
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
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
    var timeCreated: Date,

    @DatabaseField
    var status: OrderStatus,

    @DatabaseField(dataType = DataType.LONG_STRING)
    var item: String

) {
    /**
     * This constructor should only be used for ORMLite
     */
    constructor() : this(UUID.randomUUID(), 0.0, UUID.randomUUID(), null, java.util.Date(), OrderStatus.PENDING, ItemstackSerializers.serialize(ItemBuilder(Material.STONE).build()))

    companion object{
        /**
         * Get a specific page of the newest orders from the database
         * @param limit Number of orders per page
         * @param offset Starting point for the current page
         * @return List of newest orders for the current page
         */
        fun getOrdersPage(limit: Int, offset: Int): List<Order> {
            val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
            queryBuilder.orderBy("timeCreated", false)
            queryBuilder.limit(limit.toLong())
            queryBuilder.offset(offset.toLong())
            return orderDao.query(queryBuilder.prepare())
        }
    }
}