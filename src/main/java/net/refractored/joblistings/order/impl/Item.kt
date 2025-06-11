package net.refractored.joblistings.order.impl

import net.kyori.adventure.text.Component
import net.refractored.joblistings.messages.Messages
import net.refractored.joblistings.messages.Messages.replace
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
    fun getItemInfo(): Component = Messages.getMessage("Orders.OrderInfo")
        .replace("%1", item.displayName())
        .replace("%2", itemAmount.toString())
}
