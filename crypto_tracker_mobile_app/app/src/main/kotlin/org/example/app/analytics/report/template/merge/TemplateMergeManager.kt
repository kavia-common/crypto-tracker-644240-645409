package org.example.app.analytics.report.template.merge

import org.example.app.analytics.report.template.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class TemplateMergeManager @Inject constructor() {
    private val _mergeState = MutableStateFlow<MergeState>(MergeState.Idle)
    val mergeState: StateFlow<MergeState> = _mergeState

    fun mergeTemplates(
        base: ReportTemplate,
        local: ReportTemplate,
        remote: ReportTemplate
    ): MergeResult {
        try {
            _mergeState.value = MergeState.InProgress

            // Detect conflicts
            val conflicts = detectConflicts(base, local, remote)
            if (conflicts.isNotEmpty()) {
                _mergeState.value = MergeState.Conflict(conflicts)
                return MergeResult.Conflicts(conflicts)
            }

            // Perform three-way merge
            val mergedTemplate = performMerge(base, local, remote)
            _mergeState.value = MergeState.Completed

            return MergeResult.Success(mergedTemplate)
        } catch (e: Exception) {
            _mergeState.value = MergeState.Failed(e.message ?: "Merge failed")
            return MergeResult.Error(e.message ?: "Failed to merge templates")
        }
    }

    private fun detectConflicts(
        base: ReportTemplate,
        local: ReportTemplate,
        remote: ReportTemplate
    ): List<MergeConflict> {
        val conflicts = mutableListOf<MergeConflict>()

        // Check for section conflicts
        val baseIds = base.sections.map { it.id }.toSet()
        val localIds = local.sections.map { it.id }.toSet()
        val remoteIds = remote.sections.map { it.id }.toSet()

        // Detect deleted sections
        val localDeleted = baseIds - localIds
        val remoteDeleted = baseIds - remoteIds

        localDeleted.intersect(remoteIds).forEach { id ->
            conflicts.add(
                MergeConflict(
                    type = ConflictType.SECTION_DELETION,
                    elementId = id,
                    description = "Section deleted locally but modified in remote",
                    localValue = null,
                    remoteValue = remote.sections.find { it.id == id }
                )
            )
        }

        // Detect modified sections
        base.sections.forEach { baseSection ->
            val localSection = local.sections.find { it.id == baseSection.id }
            val remoteSection = remote.sections.find { it.id == baseSection.id }

            if (localSection != null && remoteSection != null &&
                localSection != baseSection && remoteSection != baseSection &&
                localSection != remoteSection) {
                conflicts.add(
                    MergeConflict(
                        type = ConflictType.SECTION_MODIFICATION,
                        elementId = baseSection.id,
                        description = "Section modified differently in local and remote",
                        localValue = localSection,
                        remoteValue = remoteSection
                    )
                )
            }
        }

        // Check for style conflicts
        if (local.colorScheme != base.colorScheme && 
            remote.colorScheme != base.colorScheme &&
            local.colorScheme != remote.colorScheme) {
            conflicts.add(
                MergeConflict(
                    type = ConflictType.STYLE_CONFLICT,
                    elementId = "colorScheme",
                    description = "Color scheme modified differently in local and remote",
                    localValue = local.colorScheme,
                    remoteValue = remote.colorScheme
                )
            )
        }

        return conflicts
    }

    private fun performMerge(
        base: ReportTemplate,
        local: ReportTemplate,
        remote: ReportTemplate
    ): ReportTemplate {
        // Start with the local template as base
        return local.copy(
            sections = mergeSections(base.sections, local.sections, remote.sections),
            colorScheme = mergeColorScheme(base.colorScheme, local.colorScheme, remote.colorScheme),
            fonts = mergeFonts(base.fonts, local.fonts, remote.fonts),
            charts = mergeChartConfig(base.charts, local.charts, remote.charts)
        )
    }

    private fun mergeSections(
        base: List<SectionTemplate>,
        local: List<SectionTemplate>,
        remote: List<SectionTemplate>
    ): List<SectionTemplate> {
        val mergedSections = mutableListOf<SectionTemplate>()
        val processedIds = mutableSetOf<String>()

        // Add sections from local that weren't modified in remote
        local.forEach { localSection ->
            val baseSection = base.find { it.id == localSection.id }
            val remoteSection = remote.find { it.id == localSection.id }

            when {
                remoteSection == null -> {
                    // Section only exists in local
                    mergedSections.add(localSection)
                }
                baseSection == remoteSection -> {
                    // Remote hasn't modified this section
                    mergedSections.add(localSection)
                }
                baseSection == localSection -> {
                    // Local hasn't modified this section
                    mergedSections.add(remoteSection)
                }
            }
            processedIds.add(localSection.id)
        }

        // Add new sections from remote
        remote.forEach { remoteSection ->
            if (remoteSection.id !in processedIds) {
                mergedSections.add(remoteSection)
            }
        }

        return mergedSections
    }

    private fun mergeColorScheme(
        base: ColorScheme,
        local: ColorScheme,
        remote: ColorScheme
    ): ColorScheme {
        // Prefer local changes unless remote has newer changes
        return when {
            local == base -> remote
            remote == base -> local
            else -> local // In case of conflict, prefer local
        }
    }

    private fun mergeFonts(
        base: FontConfiguration,
        local: FontConfiguration,
        remote: FontConfiguration
    ): FontConfiguration {
        return when {
            local == base -> remote
            remote == base -> local
            else -> local // In case of conflict, prefer local
        }
    }

    private fun mergeChartConfig(
        base: ChartConfiguration,
        local: ChartConfiguration,
        remote: ChartConfiguration
    ): ChartConfiguration {
        return when {
            local == base -> remote
            remote == base -> local
            else -> local // In case of conflict, prefer local
        }
    }

    fun resolveConflict(
        conflict: MergeConflict,
        resolution: ConflictResolution
    ): ResolvedConflict {
        return when (resolution) {
            ConflictResolution.KEEP_LOCAL -> ResolvedConflict(
                conflict = conflict,
                resolution = resolution,
                resolvedValue = conflict.localValue
            )
            ConflictResolution.KEEP_REMOTE -> ResolvedConflict(
                conflict = conflict,
                resolution = resolution,
                resolvedValue = conflict.remoteValue
            )
            ConflictResolution.MERGE -> {
                val mergedValue = when (conflict.type) {
                    ConflictType.SECTION_MODIFICATION -> mergeSectionConflict(
                        conflict.localValue as SectionTemplate,
                        conflict.remoteValue as SectionTemplate
                    )
                    ConflictType.STYLE_CONFLICT -> mergeStyleConflict(
                        conflict.localValue as ColorScheme,
                        conflict.remoteValue as ColorScheme
                    )
                    else -> conflict.localValue
                }
                ResolvedConflict(
                    conflict = conflict,
                    resolution = resolution,
                    resolvedValue = mergedValue
                )
            }
        }
    }

    private fun mergeSectionConflict(
        local: SectionTemplate,
        remote: SectionTemplate
    ): SectionTemplate {
        return local.copy(
            metrics = (local.metrics + remote.metrics).distinct()
        )
    }

    private fun mergeStyleConflict(
        local: ColorScheme,
        remote: ColorScheme
    ): ColorScheme {
        return local // For simplicity, prefer local colors
    }
}

sealed class MergeState {
    object Idle : MergeState()
    object InProgress : MergeState()
    data class Conflict(val conflicts: List<MergeConflict>) : MergeState()
    object Completed : MergeState()
    data class Failed(val error: String) : MergeState()
}

sealed class MergeResult {
    data class Success(val template: ReportTemplate) : MergeResult()
    data class Conflicts(val conflicts: List<MergeConflict>) : MergeResult()
    data class Error(val message: String) : MergeResult()
}

data class MergeConflict(
    val type: ConflictType,
    val elementId: String,
    val description: String,
    val localValue: Any?,
    val remoteValue: Any?
)

enum class ConflictType {
    SECTION_DELETION,
    SECTION_MODIFICATION,
    STYLE_CONFLICT
}

enum class ConflictResolution {
    KEEP_LOCAL,
    KEEP_REMOTE,
    MERGE
}

data class ResolvedConflict(
    val conflict: MergeConflict,
    val resolution: ConflictResolution,
    val resolvedValue: Any?
)
