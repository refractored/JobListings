package net.refractored.joblistings.order.tables

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import net.refractored.joblistings.database.Database
import net.refractored.joblistings.order.impl.Assignee
import net.refractored.joblistings.order.impl.Creation
import net.refractored.joblistings.order.impl.Expires
import net.refractored.joblistings.order.impl.Item
import net.refractored.joblistings.serializers.ItemstackSerializers
import net.refractored.joblistings.serializers.LocalDateTimeSerializers
import net.refractored.joblistings.util.Messages
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.LocalDateTime
import java.util.*

/**
 *  Represents an order that was not completed successfully.
 *  This entry is only created in the database if the player has items that were turned in.
 */
@DatabaseTable(tableName = "joblistings_failed_orders")
data class FailedOrder(
    @DatabaseField(id = true)
    val id: UUID,
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    override var expireTime: LocalDateTime,
    @DatabaseField(persisterClass = ItemstackSerializers::class)
    override var item: ItemStack,
    @DatabaseField
    override var itemAmount: Int,
    @DatabaseField
    override var assignee: UUID,
    /**
     * The time the order was switched from claimed to incomplete.
     */
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    override var creation: LocalDateTime,
    /**
     * The amount of items that the [assignee] has turned in, and needs to reclaim.
     *
     * This is out of how many in [itemAmount].
     */
    @DatabaseField
    var amountTurnedIn: Int,
    @DatabaseField
    val status: FailureType
) : Item,
    Assignee,
    Expires,
    Creation {
    /**
     * This constructor should only be used for ORMLite
     */
    constructor() : this(
        UUID.randomUUID(),
        LocalDateTime.now().plusHours(1),
        ItemBuilder(Material.STONE).amount(1).build(),
        0,
        UUID.randomUUID(),
        LocalDateTime.now(),
        0,
        FailureType.INCOMPLETE,
    )

    fun getStatusComponent() = Messages.getString("OrderStatus.incomplete")

    enum class FailureType {
        /**
         * The order was not completed in time.
         */
        INCOMPLETE,

        /**
         * The order was canceled by the owner.
         */
        CANCELED
    }

    companion object {
        /**
         * Get a specific page of the newest orders from the database
         * @param limit Number of orders per page
         * @param offset Starting point for the current page
         * @return List of newest orders for the current page
         */
        fun getOrders(
            limit: Int,
            offset: Int,
            player: Player,
            failureType: FailureType? = null
        ): List<ClaimedOrder> {
            val queryBuilder: QueryBuilder<ClaimedOrder, UUID> = Database.claimedOrderDao.queryBuilder()
            queryBuilder.orderBy("creation", false)
            queryBuilder.where().eq("assignee", player.uniqueId)
                .also {
                    if (failureType != null) {
                        it.and().eq("status", failureType)
                    }
                }
            queryBuilder.limit(limit.toLong())
            queryBuilder.offset(offset.toLong())
            return Database.claimedOrderDao.query(queryBuilder.prepare())
        }
    }
}
