package net.refractored.joblistings.commands.annotations

import net.refractored.joblistings.util.Messages
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.dynamic.AnnotationReplacer
import revxrsal.commands.annotation.dynamic.Annotations
import java.lang.reflect.AnnotatedElement

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
/**
 * Grabs the config value for the min and max range of a config value.
 */
annotation class ConfigDescription(
    val path: String,
)

class ConfigDescriptionReplacer : AnnotationReplacer<ConfigDescription> {
    override fun replaceAnnotation(
        element: AnnotatedElement,
        annotation: ConfigDescription,
    ): Collection<Annotation> {
        val commandAnnotation =
            Annotations.create(
                Description::class.java,
                "value",
                Messages.getString(annotation.path),
            )

        return listOf(commandAnnotation)
    }
}
