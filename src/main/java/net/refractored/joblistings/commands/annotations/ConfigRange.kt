package net.refractored.joblistings.commands.annotations

import net.refractored.joblistings.JobListings
import revxrsal.commands.annotation.Range
import revxrsal.commands.annotation.dynamic.AnnotationReplacer
import java.lang.reflect.AnnotatedElement

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
/**
 * Grabs the config value for the min and max range of a config value.
 */
annotation class ConfigRange(
    val minPath: String = "",
    val maxPath: String = "",
)

class ConfigRangeReplacer : AnnotationReplacer<ConfigRange> {
    override fun replaceAnnotation(
        element: AnnotatedElement,
        annotation: ConfigRange,
    ): Collection<Annotation> {
        val config = JobListings.instance.config

        var minValue = 1.0

        if (!annotation.minPath.isBlank()) {
            minValue = config.getDouble(annotation.minPath)
        }

        var maxValue = Double.MAX_VALUE

        if (!annotation.maxPath.isBlank() || config.getDouble(annotation.maxPath) > 0) {
            maxValue = config.getDouble(annotation.maxPath)
        }

        val range = Range(minValue, maxValue)

        return listOf(range)
    }
}
