package org.example.app.analytics.report.template.merge.workflow

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.app.analytics.report.template.*
import org.example.app.analytics.report.template.merge.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MergeWorkflowManager @Inject constructor(
    private val mergeManager: TemplateMergeManager,
    private val historyManager: MergeHistoryManager
) {
    private val _workflowState = MutableStateFlow<WorkflowState>(WorkflowState.Initial)
    val workflowState: StateFlow<WorkflowState> = _workflowState

    private var currentSession: MergeSession? = null

    fun startMergeWorkflow(
        base: ReportTemplate,
        local: ReportTemplate,
        remote: ReportTemplate
    ) {
        val mergeResult = mergeManager.mergeTemplates(base, local, remote)
        when (mergeResult) {
            is MergeResult.Success -> {
                _workflowState.value = WorkflowState.Completed(mergeResult.template)
                recordSuccessfulMerge(base, local, remote, mergeResult.template)
            }
            is MergeResult.Conflicts -> {
                currentSession = MergeSession(
                    base = base,
                    local = local,
                    remote = remote,
                    unresolvedConflicts = mergeResult.conflicts.toMutableList(),
                    resolvedConflicts = mutableListOf()
                )
                _workflowState.value = WorkflowState.ConflictResolution(
                    conflicts = mergeResult.conflicts,
                    progress = MergeProgress(0, mergeResult.conflicts.size)
                )
            }
            is MergeResult.Error -> {
                _workflowState.value = WorkflowState.Failed(mergeResult.message)
            }
        }
    }

    fun resolveConflict(
        conflict: MergeConflict,
        resolution: ConflictResolution
    ) {
        val session = currentSession ?: return
        val resolvedConflict = mergeManager.resolveConflict(conflict, resolution)
        
        session.unresolvedConflicts.remove(conflict)
        session.resolvedConflicts.add(resolvedConflict)

        if (session.unresolvedConflicts.isEmpty()) {
            // All conflicts resolved, perform final merge
            completeMerge(session)
        } else {
            // Update progress
            _workflowState.value = WorkflowState.ConflictResolution(
                conflicts = session.unresolvedConflicts,
                progress = MergeProgress(
                    resolved = session.resolvedConflicts.size,
                    total = session.resolvedConflicts.size + session.unresolvedConflicts.size
                )
            )
        }
    }

    fun resolveAllConflicts(resolution: ConflictResolution) {
        val session = currentSession ?: return
        val resolvedConflicts = session.unresolvedConflicts.map { conflict ->
            mergeManager.resolveConflict(conflict, resolution)
        }
        
        session.resolvedConflicts.addAll(resolvedConflicts)
        session.unresolvedConflicts.clear()

        completeMerge(session)
    }

    private fun completeMerge(session: MergeSession) {
        try {
            val mergedTemplate = applyResolutions(
                session.base,
                session.local,
                session.remote,
                session.resolvedConflicts
            )

            _workflowState.value = WorkflowState.Completed(mergedTemplate)
            recordSuccessfulMerge(
                session.base,
                session.local,
                session.remote,
                mergedTemplate,
                session.resolvedConflicts
            )
        } catch (e: Exception) {
            _workflowState.value = WorkflowState.Failed(e.message ?: "Failed to complete merge")
        }
    }

    private fun applyResolutions(
        base: ReportTemplate,
        local: ReportTemplate,
        remote: ReportTemplate,
        resolutions: List<ResolvedConflict>
    ): ReportTemplate {
        var result = local.copy()

        resolutions.forEach { resolution ->
            when (resolution.conflict.type) {
                ConflictType.SECTION_MODIFICATION -> {
                    result = applySectionResolution(result, resolution)
                }
                ConflictType.SECTION_DELETION -> {
                    result = applySectionDeletionResolution(result, resolution)
                }
                ConflictType.STYLE_CONFLICT -> {
                    result = applyStyleResolution(result, resolution)
                }
            }
        }

        return result
    }

    private fun applySectionResolution(
        template: ReportTemplate,
        resolution: ResolvedConflict
    ): ReportTemplate {
        val sectionId = resolution.conflict.elementId
        val resolvedSection = resolution.resolvedValue as? SectionTemplate ?: return template

        return template.copy(
            sections = template.sections.map { 
                if (it.id == sectionId) resolvedSection else it 
            }
        )
    }

    private fun applySectionDeletionResolution(
        template: ReportTemplate,
        resolution: ResolvedConflict
    ): ReportTemplate {
        val sectionId = resolution.conflict.elementId
        return when (resolution.resolution) {
            ConflictResolution.KEEP_LOCAL -> template
            ConflictResolution.KEEP_REMOTE -> {
                val remoteSection = resolution.resolvedValue as? SectionTemplate
                if (remoteSection != null) {
                    template.copy(
                        sections = template.sections + remoteSection
                    )
                } else {
                    template.copy(
                        sections = template.sections.filter { it.id != sectionId }
                    )
                }
            }
            ConflictResolution.MERGE -> template // No merge for deletion
        }
    }

    private fun applyStyleResolution(
        template: ReportTemplate,
        resolution: ResolvedConflict
    ): ReportTemplate {
        return when (resolution.resolution) {
            ConflictResolution.KEEP_LOCAL -> template
            ConflictResolution.KEEP_REMOTE -> {
                template.copy(
                    colorScheme = resolution.resolvedValue as? ColorScheme ?: template.colorScheme
                )
            }
            ConflictResolution.MERGE -> {
                // For style conflicts, merge means taking specific properties from each version
                val mergedStyle = mergeStyles(
                    template.colorScheme,
                    resolution.resolvedValue as? ColorScheme
                )
                template.copy(colorScheme = mergedStyle)
            }
        }
    }

    private fun mergeStyles(
        local: ColorScheme,
        remote: ColorScheme?
    ): ColorScheme {
        if (remote == null) return local
        return ColorScheme(
            primary = local.primary,
            secondary = remote.secondary,
            accent = local.accent,
            background = remote.background,
            text = local.text
        )
    }

    private fun recordSuccessfulMerge(
        base: ReportTemplate,
        local: ReportTemplate,
        remote: ReportTemplate,
        result: ReportTemplate,
        resolutions: List<ResolvedConflict> = emptyList()
    ) {
        historyManager.recordMerge(
            base = base,
            local = local,
            remote = remote,
            result = result,
            conflicts = resolutions.map { it.conflict },
            resolutions = resolutions
        )
    }
}

sealed class WorkflowState {
    object Initial : WorkflowState()
    data class ConflictResolution(
        val conflicts: List<MergeConflict>,
        val progress: MergeProgress
    ) : WorkflowState()
    data class Completed(val result: ReportTemplate) : WorkflowState()
    data class Failed(val error: String) : WorkflowState()
}

data class MergeProgress(
    val resolved: Int,
    val total: Int
) {
    val percentage: Float
        get() = (resolved.toFloat() / total.toFloat()) * 100
}

data class MergeSession(
    val base: ReportTemplate,
    val local: ReportTemplate,
    val remote: ReportTemplate,
    val unresolvedConflicts: MutableList<MergeConflict>,
    val resolvedConflicts: MutableList<ResolvedConflict>
)
