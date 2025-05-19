package net.refractored.joblistings.listeners

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.mail.Mail
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinListener : Listener {
    @EventHandler
    suspend fun onJoin(event: PlayerJoinEvent) {
        withContext(JobListings.instance.asyncDispatcher) {
            delay(1000L * JobListings.instance.config.getLong("mail.join-delay"))
            Mail.sendMail(event.player)
        }
    }
}
