package net.refractored.joblistings.listeners

import net.refractored.joblistings.database.Database
import net.refractored.joblistings.mail.Mail
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinListener : Listener {
    @EventHandler(priority = EventPriority.LOW)
    fun onJoin(event: PlayerJoinEvent){
        Mail.sendMail(event.player)
    }
}