package net.refractored.joblistings.order.tables

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import net.refractored.joblistings.serializers.ItemstackSerializers
import net.refractored.joblistings.serializers.LocalDateTimeSerializers
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.time.LocalDateTime
import java.util.UUID

/**
 *  Represents an order that was not completed successfully.
 *  This entry is only created in the database if the player has items that were turned in.
 */
@DatabaseTable(tableName = "joblistings_failed_orders")
data class FailedOrder(
    @DatabaseField(id = true)
    val id: UUID,
    @DatabaseField
    var assignee: UUID,
    @DatabaseField(persisterClass = ItemstackSerializers::class)
    var item: ItemStack,
    @DatabaseField
    var amountTurnedIn: Int,
    @DatabaseField
    var amountReclaimed: Int = 0,
    /**
     * The time the order was switched from claimed to incomplete.
     */
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeIncompleted: LocalDateTime,
    @DatabaseField
    val status: FailureStatus,
) {
    /**
     * This constructor should only be used for ORMLite
     */
    constructor() : this(
        UUID.randomUUID(),
        UUID.randomUUID(),
        (ItemBuilder(Material.STONE).amount(1).build()),
        0,
        0,
        LocalDateTime.now(),
        FailureStatus.INCOMPLETE,
    )

    fun getStatusComponent() = MessageUtil.getMessage("OrderStatus.incomplete")

    enum class FailureStatus {
        /**
         * The order was not completed in time.
         */
        INCOMPLETE,

        /**
         * The order was canceled by the owner.
         */
        CANCELED,
    }
}
