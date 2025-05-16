package net.refractored.joblistings.commands.annotations

import net.refractored.joblistings.JobListings
import revxrsal.commands.annotation.Range
import revxrsal.commands.annotation.dynamic.AnnotationReplacer
import revxrsal.commands.annotation.dynamic.Annotations
import java.lang.reflect.AnnotatedElement

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
/**
 * Grabs the config value for the min and max range of a config value.
 */
annotation class ConfigRange(
    val minPath: String,
    val maxPath: String,
)

class ConfigRangeReplacer : AnnotationReplacer<ConfigRange> {
    override fun replaceAnnotation(
        element: AnnotatedElement,
        annotation: ConfigRange,
    ): Collection<Annotation> {
        val config = JobListings.instance.config
        val minValue = config.getDouble(annotation.minPath)
        val maxValue = config.getDouble(annotation.maxPath)
        val commandAnnotation = Annotations.create(Range::class.java, "min", minValue, "max", maxValue)

        return listOf(commandAnnotation)
    }
}
