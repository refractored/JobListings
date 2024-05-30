package net.refractored.joblistings.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.refractored.joblistings.JobListings

class MessageUtil {
    companion object {
        fun getMessage(key: String): String {
            return JobListings.instance.messages.getString(key) ?: (key)
        }

        fun toComponent(miniMessage: String): Component {
            return MiniMessage.miniMessage().deserialize(miniMessage)
        }
    }
}