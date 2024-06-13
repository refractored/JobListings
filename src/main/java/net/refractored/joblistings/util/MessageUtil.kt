package net.refractored.joblistings.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.refractored.joblistings.JobListings

class MessageUtil {

    companion object {

        fun toComponent(miniMessage: String): Component {
            return MiniMessage.miniMessage().deserialize(miniMessage)
        }

        fun getMessage(key: String): Component {
            return toComponent(JobListings.instance.messages.getString(key) ?: (key))
        }

        fun getMessageUnformatted(key: String): String {
            return JobListings.instance.messages.getString(key) ?: (key)
        }

        fun getMessageList(key: String, replacements: List<MessageReplacement>): List<Component> {
            var replacedMessage = getMessageUnformatted(key)

            for ((index, replacement) in replacements.withIndex()) {
                if (replacement.string != null) {
                    replacedMessage = replacedMessage.replace("%$index", replacement.string)
                } else if (replacement.component != null) {
                    replacedMessage = replacedMessage.replace(
                        "%$index", MiniMessage.miniMessage()
                            .serialize(replacement.component)
                    )
                }
            }

            return replacedMessage.lines().map { line ->
                toComponent(line).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            }
        }

        fun getMessage(key: String, replacements: List<MessageReplacement>): Component {
            var replacedMessage = getMessageUnformatted(key)

            for ((index, replacement) in replacements.withIndex()) {
                if (replacement.string != null) {
                    replacedMessage = replacedMessage.replace("%$index", replacement.string)
                } else if (replacement.component != null) {
                    replacedMessage = replacedMessage.replace("%$index", MiniMessage.miniMessage()
                        .serialize(replacement.component))
                }
            }

            return toComponent(replacedMessage)
        }

    }
}

class MessageReplacement(val string: String?, val component: Component?) {
    constructor(string: String) : this(string, null)
    constructor(component: Component) : this(null, component)
}