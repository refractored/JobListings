package net.refractored.joblistings.mail

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.table.DatabaseTable
import net.kyori.adventure.text.Component
import net.refractored.joblistings.JobListings
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
            val mail = Mail()
            mail.user = user
            mail.message = message
            mail.timeCreated = LocalDateTime.now()
            mail.timeExpires = LocalDateTime.now().plusHours(JobListings.instance.config.getLong("Mail.ExpireTime"))
            mailDao.create(mail)
        }

        fun purgeMail() {
            val queryBuilder: QueryBuilder<Mail, UUID> = mailDao.queryBuilder()
            val allMail = mailDao.query(queryBuilder.prepare())
            for (mail in allMail) {
                if (LocalDateTime.now().isAfter(mail.timeExpires)) {
                    mailDao.delete(mail)
                }
            }
        }

        fun sendMail(player: Player) {
            val queryBuilder: QueryBuilder<Mail, UUID> = mailDao.queryBuilder()
            queryBuilder.where().eq("user", player.uniqueId)
            val allMail = mailDao.query(queryBuilder.prepare())
            if (allMail.isEmpty()) {
                return
            }
            for (mail in allMail) {
                player.sendMessage(mail.message)
                mailDao.delete(mail)
            }
        }

    }
}
