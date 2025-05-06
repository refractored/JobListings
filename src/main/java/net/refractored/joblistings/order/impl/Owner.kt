package net.refractored.joblistings.order.impl

import net.kyori.adventure.text.Component
import net.refractored.joblistings.mail.Mail
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.UUID

interface Owner {
    /**
     * The player's uuid who created the order
     */
    var owner: UUID

    /**
     * Get the OfflinePlayer of the owner of the order
     * @return The owner, as an [OfflinePlayer].
     */
    fun getOwner(): OfflinePlayer = Bukkit.getOfflinePlayer(owner)

    /**
     * Sends a message to the owner if they are online, otherwise it will be sent as a mail
     * @param message The message to send
     * @throws IllegalStateException if the order does not have an assignee
     */
    fun messageOwner(message: Component) {
        getOwner().player?.sendMessage(message) ?: Mail.createMail(owner, message)
    }
}
