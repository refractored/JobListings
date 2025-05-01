package net.refractored.joblistings.order.tables

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import net.refractored.joblistings.order.BaseOrder
import net.refractored.joblistings.serializers.ItemstackSerializers
import net.refractored.joblistings.serializers.LocalDateTimeSerializers
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.time.LocalDateTime
import java.util.UUID

@DatabaseTable(tableName = "joblistings_completed_orders")
data class CompletedOrder(
    @DatabaseField(id = true)
    override val id: UUID,
    @DatabaseField
    override var reward: Double,
    @DatabaseField
    override var user: UUID,
    @DatabaseField(persisterClass = ItemstackSerializers::class)
    override var item: ItemStack,
    @DatabaseField
    override var itemAmount: Int,
    /**
     * The amount of items that the [user] has claimed from this order.
     *
     * This is out of how many in [itemAmount].
     */
    @DatabaseField
    var itemClaimedAmount: Int,
    /**
     * The time the order was finished.
     */
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
        0,
        LocalDateTime.now(),
    )

    override fun getStatusComponent() = MessageUtil.getMessage("OrderStatus.completed")
}
