package net.refractored.joblistings.commands.annotations

import net.refractored.joblistings.util.Messages
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.dynamic.AnnotationReplacer
import revxrsal.commands.annotation.dynamic.Annotations
import java.lang.reflect.AnnotatedElement

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ConfigCommand(
    val path: String,
)

class CommandPrefixConfigReplacer : AnnotationReplacer<ConfigCommand> {
    override fun replaceAnnotation(
        element: AnnotatedElement,
        annotation: ConfigCommand,
    ): Collection<Annotation> {
        val commandPrefix = Messages.getStringOrNull("messages.command-prefix").orEmpty()

        val command = Messages.getStringOrNull(annotation.path + ".command").orEmpty()

        val result =
            when {
                command.isBlank() -> commandPrefix
                commandPrefix.isBlank() -> command
                else -> "$commandPrefix $command"
            }

        val commandAnnotation =
            Annotations.create(
                Command::class.java,
                "value",
                arrayOf(result),
            )

        if (annotation.path.isBlank()) {
            return listOf(commandAnnotation)
        }
        val descriptionAnnotation =
            Annotations.create(
                Description::class.java,
                "value",
                Messages.getString(annotation.path + ".description"),
            )

        return listOf(commandAnnotation, descriptionAnnotation)
    }
}
