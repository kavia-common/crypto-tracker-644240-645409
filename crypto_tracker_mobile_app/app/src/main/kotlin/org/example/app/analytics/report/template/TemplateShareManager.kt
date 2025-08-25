package org.example.app.analytics.report.template

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateShareManager @Inject constructor(
    private val context: Context,
    private val templateCustomizer: ReportTemplateCustomizer
) {
    private val gson = Gson()
    private val _sharedTemplates = MutableStateFlow<List<SharedTemplate>>(emptyList())
    val sharedTemplates: StateFlow<List<SharedTemplate>> = _sharedTemplates

    suspend fun shareTemplate(
        templateId: String,
        recipients: List<String>? = null
    ): Uri = withContext(Dispatchers.IO) {
        val template = templateCustomizer.currentTemplate.value
        val sharedTemplate = SharedTemplate(
            id = templateId,
            template = template,
            version = 1,
            sharedAt = System.currentTimeMillis(),
            sharedWith = recipients ?: emptyList()
        )

        val templateJson = gson.toJson(sharedTemplate)
        val templateFile = File(context.cacheDir, "${templateId}.json")
        templateFile.writeText(templateJson)

        _sharedTemplates.value = _sharedTemplates.value + sharedTemplate

        Uri.fromFile(templateFile)
    }

    suspend fun importTemplate(uri: Uri): SharedTemplate = withContext(Dispatchers.IO) {
        val content = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readText()
        } ?: throw IllegalArgumentException("Unable to read template file")

        val sharedTemplate = gson.fromJson(content, SharedTemplate::class.java)
        _sharedTemplates.value = _sharedTemplates.value + sharedTemplate

        sharedTemplate
    }

    suspend fun updateSharedTemplate(
        templateId: String,
        update: (ReportTemplate) -> ReportTemplate
    ) {
        val existingTemplate = _sharedTemplates.value.find { it.id == templateId }
        if (existingTemplate != null) {
            val updatedTemplate = update(existingTemplate.template)
            val updatedSharedTemplate = existingTemplate.copy(
                template = updatedTemplate,
                version = existingTemplate.version + 1,
                lastModified = System.currentTimeMillis()
            )

            _sharedTemplates.value = _sharedTemplates.value.map {
                if (it.id == templateId) updatedSharedTemplate else it
            }
        }
    }

    fun getTemplateHistory(templateId: String): List<TemplateVersion> {
        return templateHistory.getOrDefault(templateId, emptyList())
    }

    private val templateHistory = mutableMapOf<String, List<TemplateVersion>>()

    private fun addToHistory(templateId: String, template: ReportTemplate) {
        val history = templateHistory.getOrDefault(templateId, emptyList())
        templateHistory[templateId] = history + TemplateVersion(
            version = history.size + 1,
            template = template,
            timestamp = System.currentTimeMillis()
        )
    }
}

data class SharedTemplate(
    val id: String,
    val template: ReportTemplate,
    val version: Int,
    val sharedAt: Long,
    val lastModified: Long = sharedAt,
    val sharedWith: List<String> = emptyList()
)

data class TemplateVersion(
    val version: Int,
    val template: ReportTemplate,
    val timestamp: Long
)
