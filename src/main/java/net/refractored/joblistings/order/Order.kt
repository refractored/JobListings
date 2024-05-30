package net.refractored.joblistings.order

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import com.samjakob.spigui.item.ItemBuilder
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

    @DatabaseField
    var item: ItemStack

) {
    /**
     * This constructor should only be used for ORMLite
     */
    constructor() : this(UUID.randomUUID(), 0.0, UUID.randomUUID(), null, java.util.Date(), OrderStatus.PENDING, ItemBuilder(Material.STONE).build())

    fun serializeItemStack(itemStack: ItemStack): String {
        return Gson().toJson(itemStack.serialize())
    }

    fun deserializeItemStack(serializedItemStack: String): ItemStack {
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        val itemMap: Map<String, Any> = Gson().fromJson(serializedItemStack, mapType)
        return ItemStack.deserialize(itemMap)
    }
}