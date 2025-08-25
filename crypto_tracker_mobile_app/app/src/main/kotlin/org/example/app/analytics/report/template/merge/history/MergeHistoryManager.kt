package org.example.app.analytics.report.template.merge.history

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.app.analytics.report.template.ReportTemplate
import org.example.app.analytics.report.template.merge.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import java.util.*

@Singleton
class MergeHistoryManager @Inject constructor(
    private val context: Context
) {
    private val gson = Gson()
    private val historyDir = File(context.filesDir, "merge_history")
    private val _mergeHistory = MutableStateFlow<List<MergeRecord>>(emptyList())
    val mergeHistory: StateFlow<List<MergeRecord>> = _mergeHistory

    init {
        historyDir.mkdirs()
        loadHistory()
    }

    fun recordMerge(
        base: ReportTemplate,
        local: ReportTemplate,
        remote: ReportTemplate,
        result: ReportTemplate,
        conflicts: List<MergeConflict>,
        resolutions: List<ResolvedConflict>
    ) {
        val record = MergeRecord(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            baseTemplateId = base.id,
            localTemplateId = local.id,
            remoteTemplateId = remote.id,
            resultTemplateId = result.id,
            conflicts = conflicts,
            resolutions = resolutions,
            metadata = MergeMetadata(
                localChanges = countChanges(base, local),
                remoteChanges = countChanges(base, remote),
                conflictCount = conflicts.size,
                resolvedConflictCount = resolutions.size
            )
        )

        saveMergeRecord(record)
        _mergeHistory.value = _mergeHistory.value + record
    }

    fun getMergeDetails(mergeId: String): MergeRecord? {
        return _mergeHistory.value.find { it.id == mergeId }
    }

    fun getMergesByTemplate(templateId: String): List<MergeRecord> {
        return _mergeHistory.value.filter { record ->
            record.baseTemplateId == templateId ||
            record.localTemplateId == templateId ||
            record.remoteTemplateId == templateId ||
            record.resultTemplateId == templateId
        }
    }

    fun getRecentMerges(limit: Int = 10): List<MergeRecord> {
        return _mergeHistory.value
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    fun getMergeStats(): MergeStats {
        return MergeStats(
            totalMerges = _mergeHistory.value.size,
            totalConflicts = _mergeHistory.value.sumOf { it.conflicts.size },
            resolvedConflicts = _mergeHistory.value.sumOf { it.resolutions.size },
            averageConflictsPerMerge = _mergeHistory.value.map { it.conflicts.size }.average(),
            conflictsByType = _mergeHistory.value
                .flatMap { it.conflicts }
                .groupBy { it.type }
                .mapValues { it.value.size }
        )
    }

    private fun loadHistory() {
        val records = historyDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    gson.fromJson(file.readText(), MergeRecord::class.java)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

        _mergeHistory.value = records.sortedByDescending { it.timestamp }
    }

    private fun saveMergeRecord(record: MergeRecord) {
        val file = File(historyDir, "${record.id}.json")
        file.writeText(gson.toJson(record))
    }

    private fun countChanges(base: ReportTemplate, modified: ReportTemplate): ChangeCount {
        return ChangeCount(
            sectionChanges = countSectionChanges(base, modified),
            styleChanges = countStyleChanges(base, modified),
            metricChanges = countMetricChanges(base, modified)
        )
    }

    private fun countSectionChanges(base: ReportTemplate, modified: ReportTemplate): Int {
        val baseIds = base.sections.map { it.id }.toSet()
        val modifiedIds = modified.sections.map { it.id }.toSet()
        
        val added = (modifiedIds - baseIds).size
        val removed = (baseIds - modifiedIds).size
        val modified = base.sections.count { baseSection ->
            modified.sections.find { it.id == baseSection.id }?.let { modifiedSection ->
                baseSection != modifiedSection
            } ?: false
        }
        
        return added + removed + modified
    }

    private fun countStyleChanges(base: ReportTemplate, modified: ReportTemplate): Int {
        var changes = 0
        if (base.colorScheme != modified.colorScheme) changes++
        if (base.fonts != modified.fonts) changes++
        return changes
    }

    private fun countMetricChanges(base: ReportTemplate, modified: ReportTemplate): Int {
        val baseMetrics = base.sections.flatMap { it.metrics }.toSet()
        val modifiedMetrics = modified.sections.flatMap { it.metrics }.toSet()
        
        return (baseMetrics + modifiedMetrics).size - (baseMetrics intersect modifiedMetrics).size
    }
}

data class MergeRecord(
    val id: String,
    val timestamp: Long,
    val baseTemplateId: String,
    val localTemplateId: String,
    val remoteTemplateId: String,
    val resultTemplateId: String,
    val conflicts: List<MergeConflict>,
    val resolutions: List<ResolvedConflict>,
    val metadata: MergeMetadata
)

data class MergeMetadata(
    val localChanges: ChangeCount,
    val remoteChanges: ChangeCount,
    val conflictCount: Int,
    val resolvedConflictCount: Int
)

data class ChangeCount(
    val sectionChanges: Int,
    val styleChanges: Int,
    val metricChanges: Int
) {
    val total: Int
        get() = sectionChanges + styleChanges + metricChanges
}

data class MergeStats(
    val totalMerges: Int,
    val totalConflicts: Int,
    val resolvedConflicts: Int,
    val averageConflictsPerMerge: Double,
    val conflictsByType: Map<ConflictType, Int>
)
