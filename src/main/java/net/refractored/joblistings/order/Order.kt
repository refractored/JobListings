package net.refractored.joblistings.order

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
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

}