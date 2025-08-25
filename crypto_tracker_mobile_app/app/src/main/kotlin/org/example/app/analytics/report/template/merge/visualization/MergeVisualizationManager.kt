package org.example.app.analytics.report.template.merge.visualization

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import org.example.app.analytics.report.template.*
import org.example.app.analytics.report.template.merge.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MergeVisualizationManager @Inject constructor(
    private val context: Context
) {
    fun createMergeVisualization(
        base: ReportTemplate,
        local: ReportTemplate,
        remote: ReportTemplate,
        conflicts: List<MergeConflict>
    ): MergeVisualization {
        val sections = visualizeSections(base, local, remote, conflicts)
        val styles = visualizeStyles(base, local, remote, conflicts)
        val metrics = visualizeMetrics(base, local, remote)

        return MergeVisualization(
            sections = sections,
            styles = styles,
            metrics = metrics,
            conflictHighlights = generateConflictHighlights(conflicts)
        )
    }

    private fun visualizeSections(
        base: ReportTemplate,
        local: ReportTemplate,
        remote: ReportTemplate,
        conflicts: List<MergeConflict>
    ): List<SectionVisualization> {
        val allSectionIds = (base.sections + local.sections + remote.sections)
            .map { it.id }
            .distinct()

        return allSectionIds.map { sectionId ->
            val baseSection = base.sections.find { it.id == sectionId }
            val localSection = local.sections.find { it.id == sectionId }
            val remoteSection = remote.sections.find { it.id == sectionId }
            val conflict = conflicts.find { 
                it.type in listOf(ConflictType.SECTION_DELETION, ConflictType.SECTION_MODIFICATION) && 
                it.elementId == sectionId 
            }

            SectionVisualization(
                sectionId = sectionId,
                status = when {
                    conflict != null -> ChangeStatus.CONFLICT
                    localSection == null -> ChangeStatus.DELETED_LOCAL
                    remoteSection == null -> ChangeStatus.DELETED_REMOTE
                    localSection != baseSection && remoteSection != baseSection -> ChangeStatus.MODIFIED_BOTH
                    localSection != baseSection -> ChangeStatus.MODIFIED_LOCAL
                    remoteSection != baseSection -> ChangeStatus.MODIFIED_REMOTE
                    else -> ChangeStatus.UNCHANGED
                },
                localVersion = localSection,
                remoteVersion = remoteSection,
                baseVersion = baseSection
            )
        }
    }

    private fun visualizeStyles(
        base: ReportTemplate,
        local: ReportTemplate,
        remote: ReportTemplate,
        conflicts: List<MergeConflict>
    ): StyleVisualization {
        val colorSchemeConflict = conflicts.find { it.type == ConflictType.STYLE_CONFLICT }

        return StyleVisualization(
            colorScheme = ColorSchemeVisualization(
                status = when {
                    colorSchemeConflict != null -> ChangeStatus.CONFLICT
                    local.colorScheme != base.colorScheme && 
                    remote.colorScheme != base.colorScheme -> ChangeStatus.MODIFIED_BOTH
                    local.colorScheme != base.colorScheme -> ChangeStatus.MODIFIED_LOCAL
                    remote.colorScheme != base.colorScheme -> ChangeStatus.MODIFIED_REMOTE
                    else -> ChangeStatus.UNCHANGED
                },
                localVersion = local.colorScheme,
                remoteVersion = remote.colorScheme,
                baseVersion = base.colorScheme
            ),
            typography = TypographyVisualization(
                status = when {
                    local.fonts != base.fonts && 
                    remote.fonts != base.fonts -> ChangeStatus.MODIFIED_BOTH
                    local.fonts != base.fonts -> ChangeStatus.MODIFIED_LOCAL
                    remote.fonts != base.fonts -> ChangeStatus.MODIFIED_REMOTE
                    else -> ChangeStatus.UNCHANGED
                },
                localVersion = local.fonts,
                remoteVersion = remote.fonts,
                baseVersion = base.fonts
            )
        )
    }

    private fun visualizeMetrics(
        base: ReportTemplate,
        local: ReportTemplate,
        remote: ReportTemplate
    ): List<MetricVisualization> {
        val allMetrics = base.sections.flatMap { it.metrics } +
                        local.sections.flatMap { it.metrics } +
                        remote.sections.flatMap { it.metrics }

        return allMetrics.distinct().map { metric ->
            val baseHasMetric = base.sections.any { it.metrics.contains(metric) }
            val localHasMetric = local.sections.any { it.metrics.contains(metric) }
            val remoteHasMetric = remote.sections.any { it.metrics.contains(metric) }

            MetricVisualization(
                metricId = metric,
                status = when {
                    !baseHasMetric && localHasMetric && remoteHasMetric -> ChangeStatus.ADDED_BOTH
                    !baseHasMetric && localHasMetric -> ChangeStatus.ADDED_LOCAL
                    !baseHasMetric && remoteHasMetric -> ChangeStatus.ADDED_REMOTE
                    baseHasMetric && !localHasMetric && !remoteHasMetric -> ChangeStatus.DELETED_BOTH
                    baseHasMetric && !localHasMetric -> ChangeStatus.DELETED_LOCAL
                    baseHasMetric && !remoteHasMetric -> ChangeStatus.DELETED_REMOTE
                    else -> ChangeStatus.UNCHANGED
                }
            )
        }
    }

    private fun generateConflictHighlights(
        conflicts: List<MergeConflict>
    ): List<ConflictHighlight> {
        return conflicts.map { conflict ->
            ConflictHighlight(
                type = conflict.type,
                elementId = conflict.elementId,
                severity = when (conflict.type) {
                    ConflictType.SECTION_DELETION -> HighlightSeverity.HIGH
                    ConflictType.SECTION_MODIFICATION -> HighlightSeverity.MEDIUM
                    ConflictType.STYLE_CONFLICT -> HighlightSeverity.LOW
                }
            )
        }
    }

    fun createMergeVisualizationView(visualization: MergeVisualization): View {
        return MergeVisualizationView(context, visualization)
    }
}

data class MergeVisualization(
    val sections: List<SectionVisualization>,
    val styles: StyleVisualization,
    val metrics: List<MetricVisualization>,
    val conflictHighlights: List<ConflictHighlight>
)

data class SectionVisualization(
    val sectionId: String,
    val status: ChangeStatus,
    val localVersion: SectionTemplate?,
    val remoteVersion: SectionTemplate?,
    val baseVersion: SectionTemplate?
)

data class StyleVisualization(
    val colorScheme: ColorSchemeVisualization,
    val typography: TypographyVisualization
)

data class ColorSchemeVisualization(
    val status: ChangeStatus,
    val localVersion: ColorScheme,
    val remoteVersion: ColorScheme,
    val baseVersion: ColorScheme
)

data class TypographyVisualization(
    val status: ChangeStatus,
    val localVersion: FontConfiguration,
    val remoteVersion: FontConfiguration,
    val baseVersion: FontConfiguration
)

data class MetricVisualization(
    val metricId: String,
    val status: ChangeStatus
)

data class ConflictHighlight(
    val type: ConflictType,
    val elementId: String,
    val severity: HighlightSeverity
)

enum class ChangeStatus {
    UNCHANGED,
    MODIFIED_LOCAL,
    MODIFIED_REMOTE,
    MODIFIED_BOTH,
    ADDED_LOCAL,
    ADDED_REMOTE,
    ADDED_BOTH,
    DELETED_LOCAL,
    DELETED_REMOTE,
    DELETED_BOTH,
    CONFLICT
}

enum class HighlightSeverity {
    LOW,
    MEDIUM,
    HIGH
}

private class MergeVisualizationView(
    context: Context,
    private val visualization: MergeVisualization
) : View(context) {
    private val paint = Paint().apply {
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawSections(canvas)
        drawStyles(canvas)
        drawMetrics(canvas)
        drawConflicts(canvas)
    }

    private fun drawSections(canvas: Canvas) {
        // Implementation of section visualization drawing
    }

    private fun drawStyles(canvas: Canvas) {
        // Implementation of style visualization drawing
    }

    private fun drawMetrics(canvas: Canvas) {
        // Implementation of metrics visualization drawing
    }

    private fun drawConflicts(canvas: Canvas) {
        // Implementation of conflict highlights drawing
    }
}
