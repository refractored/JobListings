package net.refractored.joblistings.mail

import com.earth2me.essentials.Console
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.table.DatabaseTable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.SECTION_CHAR
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.JobListings.Companion.essentials
import net.refractored.joblistings.database.Database.Companion.mailDao
import net.refractored.joblistings.serializers.ComponentSerializers
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.util.*

@DatabaseTable(tableName = "joblistings_mail")
data class Mail(
    @DatabaseField(id = true)
    val id: UUID,

    @DatabaseField
    var user: UUID,

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    var timeCreated: LocalDateTime,

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    var timeExpires: LocalDateTime,

    @DatabaseField(persisterClass = ComponentSerializers::class)
    var message: Component,

) {
    /**
     * This constructor should only be used for ORMLite
     */
    constructor() : this(
        UUID.randomUUID(),
        UUID.randomUUID(),
        LocalDateTime.now(),
        LocalDateTime.now().plusHours(JobListings.instance.config.getLong("Mail.ExpireTime")),
        MessageUtil.toComponent(""),
    )

    companion object {

        fun createMail(user: UUID, message: Component) {
            if (!JobListings.instance.config.getBoolean("Mail.Enabled")) return
            // If essentials is enabled, and config option is enabled, use essentials mail
            essentials?.let {
                if (!JobListings.instance.config.getBoolean("Essentials.UseEssentialsMail")) {
                    val essPlayer = it.userMap.getUser(user)
                    val expireTime = if (JobListings.instance.config.getLong("Mail.ExpireTime") < 1L) {
                        0L
                    } else {
                        (System.currentTimeMillis() + (24 * 3600 * JobListings.instance.config.getLong("Orders.MinOrdersTime")))
                    }
                    it.mail.sendMail(
                        essPlayer,
                        Console.getInstance(),
                        // Why doesn't this have a component serializer? Kill me
                        LegacyComponentSerializer.legacy(SECTION_CHAR).serialize(message),
                        expireTime
                    )
                    return
                }
            }
            // Otherwise use my mailing system
            val mail = Mail()
            val expireTime: Long = if (JobListings.instance.config.getLong("Mail.ExpireTime") < 1L) {
                30L
            } else {
                JobListings.instance.config.getLong("Mail.ExpireTime")
            }
            mail.user = user
            mail.message = message
            mail.timeCreated = LocalDateTime.now()
            mail.timeExpires = LocalDateTime.now().plusDays(expireTime)
            mailDao.create(mail)
        }

        fun purgeMail() {
            if (!JobListings.instance.config.getBoolean("Mail.Enabled")) return
            if (JobListings.instance.config.getLong("Mail.ExpireTime") < 1L) return
            essentials.let{
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

        fun sendMail(player: Player) {
            if (!JobListings.instance.config.getBoolean("Mail.Enabled")) return
            val queryBuilder: QueryBuilder<Mail, UUID> = mailDao.queryBuilder()
            queryBuilder.where().eq("user", player.uniqueId)
            val allMail = mailDao.query(queryBuilder.prepare())
            if (allMail.isEmpty()) return
            for (mail in allMail) {
                player.sendMessage(mail.message)
                mailDao.delete(mail)
            }
        }

    }
}
