package net.refractored.joblistings.listeners

import net.refractored.joblistings.JobListings
import net.refractored.joblistings.mail.Mail
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinListener : Listener {
    @EventHandler(priority = EventPriority.LOW)
    fun onJoin(event: PlayerJoinEvent) {
        Bukkit.getScheduler().runTaskLater(
            JobListings.instance,
            Runnable {
                Mail.sendMail(event.player)
            },
            20L * JobListings.instance.config.getInt("Mail.JoinDelay"),
        )
    }
}
