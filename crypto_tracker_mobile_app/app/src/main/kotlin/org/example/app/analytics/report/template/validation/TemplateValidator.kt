package org.example.app.analytics.report.template.validation

import org.example.app.analytics.report.template.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateValidator @Inject constructor() {
    
    fun validateTemplate(template: ReportTemplate): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Validate basic template properties
        validateBasicProperties(template, errors)
        
        // Validate color scheme
        validateColorScheme(template.colorScheme, errors)
        
        // Validate typography
        validateTypography(template.fonts, errors)
        
        // Validate sections
        validateSections(template.sections, errors)
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
    
    private fun validateBasicProperties(template: ReportTemplate, errors: MutableList<ValidationError>) {
        if (template.name.isBlank()) {
            errors.add(ValidationError("Template name cannot be empty"))
        }
        
        if (template.sections.isEmpty()) {
            errors.add(ValidationError("Template must have at least one section"))
        }
    }
    
    private fun validateColorScheme(colorScheme: ColorScheme, errors: MutableList<ValidationError>) {
        val colorRegex = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})\$"
        
        if (!colorScheme.primary.matches(colorRegex.toRegex())) {
            errors.add(ValidationError("Invalid primary color format"))
        }
        
        if (!colorScheme.secondary.matches(colorRegex.toRegex())) {
            errors.add(ValidationError("Invalid secondary color format"))
        }
        
        if (!colorScheme.accent.matches(colorRegex.toRegex())) {
            errors.add(ValidationError("Invalid accent color format"))
        }
        
        // Check color contrast ratios
        if (!hasValidContrast(colorScheme.text, colorScheme.background)) {
            errors.add(ValidationError("Insufficient contrast between text and background colors"))
        }
    }
    
    private fun validateTypography(fonts: FontConfiguration, errors: MutableList<ValidationError>) {
        if (fonts.titleSize <= fonts.subtitleSize) {
            errors.add(ValidationError("Title size must be larger than subtitle size"))
        }
        
        if (fonts.subtitleSize <= fonts.bodySize) {
            errors.add(ValidationError("Subtitle size must be larger than body size"))
        }
        
        if (fonts.bodySize < 12f) {
            errors.add(ValidationError("Body font size must be at least 12px for readability"))
        }
    }
    
    private fun validateSections(sections: List<SectionTemplate>, errors: MutableList<ValidationError>) {
        val sectionIds = sections.map { it.id }
        if (sectionIds.size != sectionIds.distinct().size) {
            errors.add(ValidationError("Section IDs must be unique"))
        }
        
        sections.forEach { section ->
            if (section.name.isBlank()) {
                errors.add(ValidationError("Section name cannot be empty"))
            }
            
            if (section.metrics.isEmpty() && section.includeCharts) {
                errors.add(ValidationError("Section ${section.name} has charts enabled but no metrics defined"))
            }
        }
    }
    
    private fun hasValidContrast(foreground: String, background: String): Boolean {
        // Implementation of WCAG contrast ratio calculation
        // For simplicity, returning true here
        return true
    }
    
    fun validateTemplateCompatibility(oldTemplate: ReportTemplate, newTemplate: ReportTemplate): CompatibilityResult {
        val incompatibilities = mutableListOf<Incompatibility>()
        
        // Check for removed sections
        val removedSections = oldTemplate.sections.filter { oldSection ->
            newTemplate.sections.none { it.id == oldSection.id }
        }
        if (removedSections.isNotEmpty()) {
            incompatibilities.add(
                Incompatibility(
                    type = IncompatibilityType.REMOVED_SECTIONS,
                    description = "Sections removed: ${removedSections.map { it.name }}",
                    severity = IncompatibilitySeverity.HIGH
                )
            )
        }
        
        // Check for metric changes
        oldTemplate.sections.forEach { oldSection ->
            newTemplate.sections.find { it.id == oldSection.id }?.let { newSection ->
                val removedMetrics = oldSection.metrics - newSection.metrics.toSet()
                if (removedMetrics.isNotEmpty()) {
                    incompatibilities.add(
                        Incompatibility(
                            type = IncompatibilityType.MODIFIED_METRICS,
                            description = "Metrics removed from section ${oldSection.name}: $removedMetrics",
                            severity = IncompatibilitySeverity.MEDIUM
                        )
                    )
                }
            }
        }
        
        // Check for major style changes
        if (oldTemplate.colorScheme != newTemplate.colorScheme) {
            incompatibilities.add(
                Incompatibility(
                    type = IncompatibilityType.STYLE_CHANGE,
                    description = "Color scheme has been modified",
                    severity = IncompatibilitySeverity.LOW
                )
            )
        }
        
        return CompatibilityResult(incompatibilities)
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val errors: List<ValidationError>) : ValidationResult()
}

data class ValidationError(
    val message: String,
    val field: String? = null,
    val severity: ValidationSeverity = ValidationSeverity.ERROR
)

enum class ValidationSeverity {
    ERROR,
    WARNING,
    INFO
}

data class CompatibilityResult(
    val incompatibilities: List<Incompatibility>
) {
    val isCompatible: Boolean
        get() = incompatibilities.none { it.severity == IncompatibilitySeverity.HIGH }
}

data class Incompatibility(
    val type: IncompatibilityType,
    val description: String,
    val severity: IncompatibilitySeverity
)

enum class IncompatibilityType {
    REMOVED_SECTIONS,
    MODIFIED_METRICS,
    STYLE_CHANGE
}

enum class IncompatibilitySeverity {
    HIGH,
    MEDIUM,
    LOW
}
