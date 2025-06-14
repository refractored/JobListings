package net.refractored.joblistings.order.tables

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
import net.refractored.joblistings.messages.Messages
import net.refractored.joblistings.order.impl.Expires
import net.refractored.joblistings.order.impl.Item
import net.refractored.joblistings.order.impl.Owner
import net.refractored.joblistings.serializers.ItemstackSerializers
import net.refractored.joblistings.serializers.LocalDateTimeSerializers
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.time.LocalDateTime
import java.util.*

@DatabaseTable(tableName = "joblistings_completed_orders")
data class CompletedOrder(
    @DatabaseField(id = true)
    val id: UUID = UUID.randomUUID(),
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    override var expireTime: LocalDateTime = LocalDateTime.now().plusHours(1),
    @DatabaseField(persisterClass = ItemstackSerializers::class)
    override var item: ItemStack = ItemBuilder(Material.STONE).amount(1).build(),
    @DatabaseField
    override var itemAmount: Int = 0,
    @DatabaseField
    override var owner: UUID = UUID.randomUUID(),
    /**
     * The time the order was finished.
     */
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var creation: LocalDateTime = LocalDateTime.now(),
    /**
     * The amount of items that the [owner] has claimed from this order.
     *
     * This is out of how many in [itemAmount].
     */
    @DatabaseField
    var itemClaimedAmount: Int = 0
) : Owner,
    Item,
    Expires {

    fun getStatusComponent() = Messages.getString("OrderStatus.completed")
}
