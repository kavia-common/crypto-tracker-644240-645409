package org.example.app.analytics.report.template

import android.content.Context
import android.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportTemplateCustomizer @Inject constructor(
    private val context: Context
) {
    private val _currentTemplate = MutableStateFlow(ReportTemplate())
    val currentTemplate: StateFlow<ReportTemplate> = _currentTemplate

    fun updateTemplate(update: (ReportTemplate) -> ReportTemplate) {
        _currentTemplate.value = update(_currentTemplate.value)
    }

    fun applyTemplate(templateId: String) {
        // Load predefined template
        val template = predefinedTemplates[templateId] ?: return
        _currentTemplate.value = template
    }

    fun saveCustomTemplate(name: String): String {
        val templateId = "custom_${System.currentTimeMillis()}"
        val template = _currentTemplate.value.copy(
            id = templateId,
            name = name
        )
        customTemplates[templateId] = template
        return templateId
    }

    companion object {
        private val predefinedTemplates = mapOf(
            "default" to ReportTemplate(
                id = "default",
                name = "Default Template",
                colorScheme = ColorScheme(
                    primary = "#3880ff",
                    secondary = "#2c2c54",
                    accent = "#ffb142",
                    background = "#ffffff",
                    text = "#000000"
                ),
                fonts = FontConfiguration(
                    titleFont = "Arial",
                    bodyFont = "Helvetica",
                    titleSize = 24f,
                    subtitleSize = 18f,
                    bodySize = 14f
                ),
                sections = defaultSections
            ),
            "dark" to ReportTemplate(
                id = "dark",
                name = "Dark Theme",
                colorScheme = ColorScheme(
                    primary = "#61dafb",
                    secondary = "#282c34",
                    accent = "#ff6b6b",
                    background = "#1a1a1a",
                    text = "#ffffff"
                ),
                fonts = FontConfiguration(
                    titleFont = "Roboto",
                    bodyFont = "Open Sans",
                    titleSize = 24f,
                    subtitleSize = 18f,
                    bodySize = 14f
                ),
                sections = defaultSections
            )
        )

        private val customTemplates = mutableMapOf<String, ReportTemplate>()

        private val defaultSections = listOf(
            SectionTemplate(
                id = "summary",
                name = "Executive Summary",
                order = 0,
                includeCharts = true,
                metrics = listOf("activeUsers", "retention", "engagement")
            ),
            SectionTemplate(
                id = "performance",
                name = "Performance Metrics",
                order = 1,
                includeCharts = true,
                metrics = listOf("responseTime", "crashes", "errors")
            ),
            SectionTemplate(
                id = "user",
                name = "User Analytics",
                order = 2,
                includeCharts = true,
                metrics = listOf("userSessions", "userFlow", "demographics")
            )
        )
    }
}

data class ReportTemplate(
    val id: String = "custom",
    val name: String = "Custom Template",
    val colorScheme: ColorScheme = ColorScheme(),
    val fonts: FontConfiguration = FontConfiguration(),
    val sections: List<SectionTemplate> = emptyList(),
    val headerLogo: String? = null,
    val footerText: String? = null,
    val pageOrientation: PageOrientation = PageOrientation.PORTRAIT,
    val charts: ChartConfiguration = ChartConfiguration()
)

data class ColorScheme(
    val primary: String = "#3880ff",
    val secondary: String = "#2c2c54",
    val accent: String = "#ffb142",
    val background: String = "#ffffff",
    val text: String = "#000000"
)

data class FontConfiguration(
    val titleFont: String = "Arial",
    val bodyFont: String = "Helvetica",
    val titleSize: Float = 24f,
    val subtitleSize: Float = 18f,
    val bodySize: Float = 14f
)

data class SectionTemplate(
    val id: String,
    val name: String,
    val order: Int,
    val includeCharts: Boolean = true,
    val metrics: List<String> = emptyList()
)

data class ChartConfiguration(
    val showLegend: Boolean = true,
    val showGrid: Boolean = true,
    val animationDuration: Long = 1000,
    val colors: List<String> = listOf("#3880ff", "#2c2c54", "#ffb142", "#ff6b6b", "#61dafb")
)

enum class PageOrientation {
    PORTRAIT,
    LANDSCAPE
}
