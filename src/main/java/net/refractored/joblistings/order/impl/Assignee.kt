package net.refractored.joblistings.order.impl

import net.kyori.adventure.text.Component
import net.refractored.joblistings.mail.Mail
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.UUID

/**
 * Interface representing an order that has an assignee.
 */
interface Assignee {
    var assignee: UUID

    /**
     * Get the OfflinePlayer of the owner of the order
     * @return The owner of the order
     */
    fun getAssignee(): OfflinePlayer = Bukkit.getOfflinePlayer(assignee)

    /**
     * Sends a message to the owner if they are online, otherwise it will be sent as a mail
     * @param message The message to send
     * @throws IllegalStateException if the order does not have an assignee
     */
    fun messageAssignee(message: Component) {
        getAssignee().player?.sendMessage(message) ?: Mail.createMail(assignee, message)
    }
}
