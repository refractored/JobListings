package net.refractored.joblistings.order

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import net.refractored.joblistings.serializers.ItemstackSerializers
import net.refractored.joblistings.serializers.LocalDateTimeSerializers
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.time.LocalDateTime
import java.util.*

/**
 * Represents an order that has been placed on the job board
 */
@DatabaseTable(tableName = "joblistings_completed_orders")
data class CompletedOrder(
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
    /**
     * The person who claimed the order.
     */
    @DatabaseField
    var assignee: UUID,
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeFinished: LocalDateTime,
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
        UUID.randomUUID(),
        LocalDateTime.now(),
    )
}
