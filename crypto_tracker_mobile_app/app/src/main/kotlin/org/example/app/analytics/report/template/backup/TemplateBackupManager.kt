package org.example.app.analytics.report.template.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.example.app.analytics.report.template.ReportTemplate
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import java.util.zip.ZipInputStream

@Singleton
class TemplateBackupManager @Inject constructor(
    private val context: Context
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val backupDir = File(context.filesDir, "template_backups")
    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState

    init {
        backupDir.mkdirs()
    }

    suspend fun createBackup(
        templates: List<ReportTemplate>,
        includeAssets: Boolean = true
    ): Uri = withContext(Dispatchers.IO) {
        try {
            _backupState.value = BackupState.InProgress(0f)

            val backupFile = File(backupDir, generateBackupFileName())
            val manifest = BackupManifest(
                version = BACKUP_VERSION,
                timestamp = System.currentTimeMillis(),
                templateCount = templates.size
            )

            ZipOutputStream(backupFile.outputStream()).use { zipOut ->
                // Write manifest
                zipOut.putNextEntry(ZipEntry("manifest.json"))
                zipOut.write(gson.toJson(manifest).toByteArray())
                zipOut.closeEntry()

                // Write templates
                templates.forEachIndexed { index, template ->
                    zipOut.putNextEntry(ZipEntry("templates/${template.id}.json"))
                    zipOut.write(gson.toJson(template).toByteArray())
                    zipOut.closeEntry()

                    _backupState.value = BackupState.InProgress(
                        (index + 1).toFloat() / templates.size
                    )

                    // Backup associated assets if requested
                    if (includeAssets) {
                        backupTemplateAssets(template, zipOut)
                    }
                }
            }

            _backupState.value = BackupState.Completed(Uri.fromFile(backupFile))
            Uri.fromFile(backupFile)
        } catch (e: Exception) {
            _backupState.value = BackupState.Failed(e.message ?: "Backup failed")
            throw e
        }
    }

    suspend fun restoreBackup(uri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        try {
            _backupState.value = BackupState.InProgress(0f)
            val restoredTemplates = mutableListOf<ReportTemplate>()
            var manifest: BackupManifest? = null

            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == "manifest.json" -> {
                                manifest = gson.fromJson(
                                    zipIn.bufferedReader().readText(),
                                    BackupManifest::class.java
                                )
                            }
                            entry.name.startsWith("templates/") -> {
                                val template = gson.fromJson(
                                    zipIn.bufferedReader().readText(),
                                    ReportTemplate::class.java
                                )
                                restoredTemplates.add(template)

                                _backupState.value = BackupState.InProgress(
                                    restoredTemplates.size.toFloat() / (manifest?.templateCount ?: 1)
                                )
                            }
                            entry.name.startsWith("assets/") -> {
                                restoreTemplateAsset(entry.name, zipIn)
                            }
                        }
                        entry = zipIn.nextEntry
                    }
                }
            }

            if (manifest == null) {
                throw IllegalStateException("Invalid backup file: missing manifest")
            }

            if (manifest.version > BACKUP_VERSION) {
                return@withContext RestoreResult.IncompatibleVersion(manifest.version)
            }

            _backupState.value = BackupState.Completed(uri)
            RestoreResult.Success(restoredTemplates)
        } catch (e: Exception) {
            _backupState.value = BackupState.Failed(e.message ?: "Restore failed")
            RestoreResult.Error(e.message ?: "Failed to restore backup")
        }
    }

    private fun backupTemplateAssets(template: ReportTemplate, zipOut: ZipOutputStream) {
        template.headerLogo?.let { logoPath ->
            val logoFile = File(context.filesDir, logoPath)
            if (logoFile.exists()) {
                zipOut.putNextEntry(ZipEntry("assets/$logoPath"))
                logoFile.inputStream().use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        }
    }

    private fun restoreTemplateAsset(assetPath: String, zipIn: ZipInputStream) {
        val targetFile = File(context.filesDir, assetPath.removePrefix("assets/"))
        targetFile.parentFile?.mkdirs()
        targetFile.outputStream().use { output ->
            zipIn.copyTo(output)
        }
    }

    private fun generateBackupFileName(): String {
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
            .format(java.util.Date())
        return "template_backup_$timestamp.zip"
    }

    companion object {
        const val BACKUP_VERSION = 1
    }
}

data class BackupManifest(
    val version: Int,
    val timestamp: Long,
    val templateCount: Int
)

sealed class BackupState {
    object Idle : BackupState()
    data class InProgress(val progress: Float) : BackupState()
    data class Completed(val uri: Uri) : BackupState()
    data class Failed(val error: String) : BackupState()
}

sealed class RestoreResult {
    data class Success(val templates: List<ReportTemplate>) : RestoreResult()
    data class IncompatibleVersion(val version: Int) : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}
