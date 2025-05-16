package net.refractored.joblistings.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.AMPERSAND_CHAR
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.util.Messages.miniToComponent
import org.bukkit.configuration.file.FileConfiguration

class MessageUtil {
    companion object {
        fun getMessage(
            key: String,
            replacements: List<MessageReplacement>,
        ): Component {
            var replacedMessage = Messages.getString(key)

            for ((index, replacement) in replacements.withIndex()) {
                if (replacement.string != null) {
                    replacedMessage = replacedMessage.replace("%$index", replacement.string)
                } else if (replacement.component != null) {
                    replacedMessage =
                        replacedMessage.replace(
                            "%$index",
                            MiniMessage
                                .miniMessage()
                                .serialize(replacement.component),
                        )
                }
            }

            return (replacedMessage).miniToComponent()
        }
    }
}

object Messages {
    /**
     * The configuration of the messages.
     */
    private val config: FileConfiguration
        get() = JobListings.instance.messages

    /**
     * @return a string from the messages.yml.
     */
    fun getStringOrNull(path: String): String? = config.getString(path)

    /**
     * @return a string from the messages.yml.
     *
     * If the path is not found, the path itself is returned.
     */
    fun getString(path: String): String = getStringOrNull(path) ?: path

    fun getPrefix(): String = getString("messages.prefix")

    /**
     * @return a string from the messages.yml, with the prefix.
     */
    fun getStringPrefixed(path: String): String = getPrefix() + getString(path)

    /**
     * @return a list of strings from the messages.ym.
     */
    fun getStrings(path: String): List<String> = config.getStringList(path)

    /**
     * Converts a [Component] to a legacy string using the specified character.
     */
    fun Component.toLegacy(char: Char = AMPERSAND_CHAR): String =
        LegacyComponentSerializer.legacy(char).serialize(
            this,
        )

    /**
     * Converts this [Component] to a string using minimessage.
     */
    fun Component.toMinimessage(): String = MiniMessage.miniMessage().serialize(this)

    /**
     * Converts this string to a component using minimessage.
     */
    fun String.miniToComponent(): Component = MiniMessage.miniMessage().deserialize(this)

    fun Component.toPlaintext(): String = PlainTextComponentSerializer.plainText().serialize(this)

    /**
     * Returns a new string obtained by replacing all occurrences of the [oldValue] substring in this string
     * with the specified [newValue] component formatted as minimessage.
     *
     * If the object has a method to get the raw minimessage,
     * it's recommended to use that instead as this just converts it back to minimessage,
     */
    fun String.replace(
        oldValue: String,
        newValue: Component,
        ignoreCase: Boolean = false,
    ): String = this.replace(oldValue, newValue.toMinimessage(), ignoreCase)

    /**
     * Disables the italic decoration on this component if it is not present.
     *
     * This is useful because by default, lore is italicized in Minecraft by default
     *
     * @return The component with the italic decoration disabled.
     */
    fun Component.fixItalics(): Component = this.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
}

class MessageReplacement(
    val string: String?,
    val component: Component?,
) {
    constructor(string: String) : this(string, null)
    constructor(component: Component) : this(null, component)
}
