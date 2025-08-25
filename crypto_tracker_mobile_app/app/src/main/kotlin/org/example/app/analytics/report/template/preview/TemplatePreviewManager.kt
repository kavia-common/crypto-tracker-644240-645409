package org.example.app.analytics.report.template.preview

import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.example.app.analytics.report.template.ReportTemplate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplatePreviewManager @Inject constructor(
    private val context: Context
) {
    private val _previewState = MutableStateFlow<PreviewState>(PreviewState.Initial)
    val previewState: StateFlow<PreviewState> = _previewState

    suspend fun generatePreview(template: ReportTemplate) {
        _previewState.value = PreviewState.Loading

        try {
            val preview = withContext(Dispatchers.Default) {
                // Generate sample data for preview
                val sampleData = generateSampleData()
                
                // Generate previews for different sections
                val sectionPreviews = template.sections.map { section ->
                    SectionPreview(
                        sectionId = section.id,
                        name = section.name,
                        preview = generateSectionPreview(section, template, sampleData)
                    )
                }

                TemplatePreview(
                    template = template,
                    sections = sectionPreviews,
                    colorPalette = generateColorPalette(template),
                    typographyPreview = generateTypographyPreview(template)
                )
            }

            _previewState.value = PreviewState.Success(preview)
        } catch (e: Exception) {
            _previewState.value = PreviewState.Error(e.message ?: "Failed to generate preview")
        }
    }

    private suspend fun generateSectionPreview(
        section: org.example.app.analytics.report.template.SectionTemplate,
        template: ReportTemplate,
        sampleData: SampleData
    ): Bitmap = withContext(Dispatchers.Main) {
        val html = generatePreviewHtml(section, template, sampleData)
        renderHtmlPreview(html)
    }

    private fun generateSampleData(): SampleData {
        return SampleData(
            metrics = listOf(
                SampleMetric("Active Users", 1500, 0.05),
                SampleMetric("Session Duration", 25.5, -0.02),
                SampleMetric("Retention Rate", 0.75, 0.03)
            ),
            timeSeriesData = generateSampleTimeSeriesData(),
            distributions = generateSampleDistributions()
        )
    }

    private fun generatePreviewHtml(
        section: org.example.app.analytics.report.template.SectionTemplate,
        template: ReportTemplate,
        sampleData: SampleData
    ): String {
        return buildString {
            append("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body {
                            font-family: ${template.fonts.bodyFont};
                            color: ${template.colorScheme.text};
                            background-color: ${template.colorScheme.background};
                            margin: 0;
                            padding: 16px;
                        }
                        h1 {
                            font-family: ${template.fonts.titleFont};
                            color: ${template.colorScheme.primary};
                            font-size: ${template.fonts.titleSize}px;
                        }
                        .metric {
                            background-color: ${template.colorScheme.secondary}10;
                            padding: 16px;
                            border-radius: 8px;
                            margin-bottom: 8px;
                        }
                    </style>
                </head>
                <body>
            """.trimIndent())

            append("<h1>${section.name}</h1>")

            // Add sample metrics
            sampleData.metrics.forEach { metric ->
                append("""
                    <div class="metric">
                        <h3>${metric.name}</h3>
                        <p style="font-size: 24px; color: ${template.colorScheme.primary}">
                            ${formatValue(metric.value)}
                        </p>
                        <p style="color: ${if (metric.change >= 0) "#2dd36f" else "#eb445a"}">
                            ${formatChange(metric.change)}
                        </p>
                    </div>
                """.trimIndent())
            }

            append("</body></html>")
        }
    }

    private suspend fun renderHtmlPreview(html: String): Bitmap = withContext(Dispatchers.Main) {
        val webView = WebView(context)
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        // Implementation to convert WebView to Bitmap
        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888) // Placeholder
    }

    private fun generateColorPalette(template: ReportTemplate): List<ColorSwatch> {
        return listOf(
            ColorSwatch("Primary", template.colorScheme.primary),
            ColorSwatch("Secondary", template.colorScheme.secondary),
            ColorSwatch("Accent", template.colorScheme.accent),
            ColorSwatch("Background", template.colorScheme.background),
            ColorSwatch("Text", template.colorScheme.text)
        )
    }

    private fun generateTypographyPreview(template: ReportTemplate): TypographyPreview {
        return TypographyPreview(
            titleFont = template.fonts.titleFont,
            bodyFont = template.fonts.bodyFont,
            samples = listOf(
                TextSample("Title", template.fonts.titleSize),
                TextSample("Subtitle", template.fonts.subtitleSize),
                TextSample("Body", template.fonts.bodySize)
            )
        )
    }

    private fun formatValue(value: Double): String {
        return when {
            value >= 1000000 -> String.format("%.1fM", value / 1000000)
            value >= 1000 -> String.format("%.1fK", value / 1000)
            else -> String.format("%.1f", value)
        }
    }

    private fun formatChange(change: Double): String {
        return String.format("%+.1f%%", change * 100)
    }

    private fun generateSampleTimeSeriesData(): List<TimeSeriesPoint> {
        // Generate sample time series data
        return (0..6).map { day ->
            TimeSeriesPoint(
                timestamp = System.currentTimeMillis() - (day * 24 * 60 * 60 * 1000),
                value = (100..500).random().toDouble()
            )
        }
    }

    private fun generateSampleDistributions(): Map<String, Double> {
        return mapOf(
            "Category A" to 0.3,
            "Category B" to 0.25,
            "Category C" to 0.2,
            "Category D" to 0.15,
            "Others" to 0.1
        )
    }
}

sealed class PreviewState {
    object Initial : PreviewState()
    object Loading : PreviewState()
    data class Success(val preview: TemplatePreview) : PreviewState()
    data class Error(val message: String) : PreviewState()
}

data class TemplatePreview(
    val template: ReportTemplate,
    val sections: List<SectionPreview>,
    val colorPalette: List<ColorSwatch>,
    val typographyPreview: TypographyPreview
)

data class SectionPreview(
    val sectionId: String,
    val name: String,
    val preview: Bitmap
)

data class ColorSwatch(
    val name: String,
    val color: String
)

data class TypographyPreview(
    val titleFont: String,
    val bodyFont: String,
    val samples: List<TextSample>
)

data class TextSample(
    val text: String,
    val size: Float
)

data class SampleData(
    val metrics: List<SampleMetric>,
    val timeSeriesData: List<TimeSeriesPoint>,
    val distributions: Map<String, Double>
)

data class SampleMetric(
    val name: String,
    val value: Double,
    val change: Double
)

data class TimeSeriesPoint(
    val timestamp: Long,
    val value: Double
)
