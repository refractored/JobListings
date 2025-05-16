package net.refractored.joblistings.listeners

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.mail.Mail
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinListener : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        JobListings.instance.launch {
            withContext(JobListings.instance.asyncDispatcher) {
                delay(1000L * JobListings.instance.config.getLong("mail.join-delay"))
                Mail.sendMail(event.player)
            }
        }
    }
}
