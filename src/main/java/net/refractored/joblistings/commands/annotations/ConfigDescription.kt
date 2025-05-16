package net.refractored.joblistings.commands.annotations

import net.refractored.joblistings.util.Messages
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.dynamic.AnnotationReplacer
import revxrsal.commands.annotation.dynamic.Annotations
import java.lang.reflect.AnnotatedElement

class CommandPrefixConfigReplacer : AnnotationReplacer<ConfigCommand> {
    override fun replaceAnnotation(
        element: AnnotatedElement,
        annotation: ConfigCommand,
    ): Collection<Annotation> {
        val prefix = Messages.getString("messages.command-prefix")
        val commands: Array<String> =
            if (prefix.isEmpty() || prefix.isBlank()) {
                annotation.value
            } else {
                annotation.value
                    .map {
                        if (it == "") {
                            prefix
                        } else {
                            "$prefix $it"
                        }
                    }.toTypedArray()
            }
        val commandAnnotation =
            Annotations.create(
                Command::class.java,
                "value",
                commands,
            )

        return listOf(commandAnnotation)
    }
}
