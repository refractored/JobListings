package net.refractored.joblistings.order.tables

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import net.refractored.joblistings.order.impl.Expires
import net.refractored.joblistings.order.impl.Item
import net.refractored.joblistings.order.impl.Owner
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
    val id: UUID,
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    override var expireTime: LocalDateTime,
    @DatabaseField(persisterClass = ItemstackSerializers::class)
    override var item: ItemStack,
    @DatabaseField
    override var itemAmount: Int,
    @DatabaseField
    override var owner: UUID,
    /**
     * The time the order was finished.
     */
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var creation: LocalDateTime,
    /**
     * The amount of items that the [owner] has claimed from this order.
     *
     * This is out of how many in [itemAmount].
     */
    @DatabaseField
    var itemClaimedAmount: Int,
) : Owner,
    Item,
    Expires {
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
    )

    fun getStatusComponent() = MessageUtil.getMessage("OrderStatus.completed")
}
