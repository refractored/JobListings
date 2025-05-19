package net.refractored.joblistings.order.impl

import net.kyori.adventure.text.Component
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.inventory.ItemStack

interface Item {
    /**
     * The item
     *
     * This ItemStack is not representative of the amount of items required to complete it.
     * @see itemAmount
     */
    var item: ItemStack

    /**
     * The amount of items required to complete the order
     */
    var itemAmount: Int

    /**
     * Get the display name of the item
     * @return The display name of the item
     */
    fun getItemInfo(): Component = MessageUtil.getMessage(
        "Orders.OrderInfo",
        listOf(
            MessageReplacement(item.displayName()),
            MessageReplacement(itemAmount.toString()),
        ),
    )
}
