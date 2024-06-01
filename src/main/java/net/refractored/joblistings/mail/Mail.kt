package net.refractored.joblistings.mail

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.table.DatabaseTable
import net.refractored.joblistings.database.Database.Companion.mailDao
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.awt.Component
import java.time.LocalDateTime
import java.util.*

@DatabaseTable(tableName = "joblistings_mail")
data class Mail(
    @DatabaseField(id = true)
    val id: UUID,

    @DatabaseField
    var user: UUID,

    @DatabaseField
    var timeCreated: LocalDateTime,

    @DatabaseField
    var timeExpires: LocalDateTime,

    @DatabaseField
    var message: String,

) {
    /**
     * This constructor should only be used for ORMLite
     */
    constructor() : this(
        UUID.randomUUID(),
        UUID.randomUUID(),
        LocalDateTime.now(),
        LocalDateTime.now().plusHours(12),
        "",
    )

    companion object {

        fun createMail(user: UUID, message: Component) {
            val mail = Mail()
            mail.user = user
            mail.message = message.toString()
            mail.timeCreated = LocalDateTime.now()
            mail.timeExpires = LocalDateTime.now().plusHours(12)
            mailDao.create(mail)
        }

        fun sendMail(player: Player) {
            val queryBuilder: QueryBuilder<Mail, UUID> = mailDao.queryBuilder()
            queryBuilder.orderBy("timeCreated", true)
            queryBuilder.where().eq("user", player.uniqueId)
            val allMail = mailDao.query(queryBuilder.prepare())
            if (allMail.isEmpty()) {
                return
            }
            for (mail in allMail) {
                player.sendMessage(MessageUtil.toComponent(mail.message))
                mailDao.delete(mail)
            }
        }

    }
}
