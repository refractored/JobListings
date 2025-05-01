package net.refractored.joblistings.order

import net.kyori.adventure.text.Component
import net.refractored.joblistings.mail.Mail
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import java.util.UUID

/**
 * This interface represents the base properties of an order.
 */
interface BaseOrder {
    val id: UUID

    /**
     * The reward of the order if completed
     */
    var reward: Double

    /**
     * The player's uuid who created the order
     */
    var user: UUID

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
     * Get the OfflinePlayer of the owner of the order
     * @return The owner of the order
     */
    fun getOwner(): OfflinePlayer = Bukkit.getOfflinePlayer(user)

    /**
     * Get the display name of the item
     * @return The display name of the item
     */
    fun getItemInfo(): Component =
        MessageUtil.getMessage(
            "orders.OrderInfo",
            listOf(
                MessageReplacement(item.displayName()),
                MessageReplacement(itemAmount.toString()),
                MessageReplacement(reward.toString()), // Optional
            ),
        )

    /**
     * Sends a message to the owner if they are online, otherwise it will be sent as a mail
     * @param message The message to send
     * @throws IllegalStateException if the order does not have an assignee
     */
    fun messageOwner(message: Component) {
        getOwner().player?.sendMessage(message) ?: Mail.createMail(user, message)
    }

    /**
     * Gets the status of the order as a component
     */
    fun getStatusComponent(): Component
}
