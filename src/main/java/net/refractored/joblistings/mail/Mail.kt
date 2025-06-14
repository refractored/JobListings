package net.refractored.joblistings.mail

import com.earth2me.essentials.Console
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.table.DatabaseTable
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.SECTION_CHAR
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database.mailDao
import net.refractored.joblistings.messages.Messages.miniToComponent
import net.refractored.joblistings.messages.Messages.toLegacy
import net.refractored.joblistings.serializers.ComponentSerializers
import net.refractored.joblistings.serializers.LocalDateTimeSerializers
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.util.*

@DatabaseTable(tableName = "joblistings_mail")
data class Mail(
    @DatabaseField(id = true)
    val id: UUID = UUID.randomUUID(),
    @DatabaseField
    var user: UUID = UUID.randomUUID(),
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeCreated: LocalDateTime = LocalDateTime.now(),
    @DatabaseField(persisterClass = LocalDateTimeSerializers::class)
    var timeExpires: LocalDateTime = LocalDateTime.now().plusHours(JobListings.instance.config.getLong("mail.expiration")),
    @DatabaseField(persisterClass = ComponentSerializers::class)
    var message: Component = "".miniToComponent()
) {

    companion object {
        /**
         * Creates a mail message for a user in the database.
         */
        fun createMail(
            user: UUID,
            message: Component
        ) {
            if (!JobListings.instance.config.getBoolean("mail.enabled")) return
            // If essentials is enabled, and config option is enabled, use essentials mail
            JobListings.instance.essentials?.let { essentials ->
                if (JobListings.instance.config.getBoolean("Essentials.UseEssentialsMail")) {
                    val essPlayer = essentials.users.getUser(user)
                    val expireTime =
                        if (JobListings.instance.config.getLong("mail.expiration") < 1L) {
                            0L
                        } else {
                            (System.currentTimeMillis() + (24 * 3600 * JobListings.instance.config.getLong("orders.min-order-time")))
                        }
                    essentials.mail.sendMail(
                        essPlayer,
                        Console.getInstance(),
                        // Why doesn't this take components? Kill me.
                        message.toLegacy(SECTION_CHAR),
                        expireTime,
                    )
                    return
                }
            }
            // Otherwise use my mailing system
            val expireTime: Long =
                if (JobListings.instance.config.getLong("mail.expiration") < 1L) {
                    30L
                } else {
                    JobListings.instance.config.getLong("mail.expiration")
                }
            val mail = Mail(
                user = user,
                message = message,
                timeExpires = LocalDateTime.now().plusDays(expireTime),
            )
            mailDao.create(mail)
        }

        fun purgeMail() {
            if (!JobListings.instance.config.getBoolean("mail.enabled")) return
            if (JobListings.instance.config.getLong("mail.expiration") < 1L) return
            JobListings.instance.essentials.let {
                if (JobListings.instance.config.getBoolean("Essentials.UseEssentialsMail")) return
            }
            val queryBuilder: QueryBuilder<Mail, UUID> = mailDao.queryBuilder()
            val allMail = mailDao.query(queryBuilder.prepare())
            for (mail in allMail) {
                if (LocalDateTime.now().isAfter(mail.timeExpires)) {
                    mailDao.delete(mail)
                }
            }
        }

        suspend fun sendMail(player: Player) {
            if (!JobListings.instance.config.getBoolean("mail.enabled")) return
            val queryBuilder: QueryBuilder<Mail, UUID> = mailDao.queryBuilder()
            queryBuilder.where().eq("user", player.uniqueId)
            val allMail = mailDao.query(queryBuilder.prepare())
            if (allMail.isEmpty()) return
            for (mail in allMail) {
                player.sendMessage(mail.message)
                mailDao.delete(mail)
                delay(1000L)
            }
        }
    }
}
