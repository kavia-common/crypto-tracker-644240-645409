package org.example.app.analytics.report.template.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.example.app.analytics.report.template.ReportTemplate
import org.example.app.analytics.report.template.validation.TemplateValidator
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupVerificationManager @Inject constructor(
    private val context: Context,
    private val templateValidator: TemplateValidator
) {
    private val gson = Gson()
    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState: StateFlow<VerificationState> = _verificationState

    suspend fun verifyBackup(uri: Uri): VerificationResult = withContext(Dispatchers.IO) {
        try {
            _verificationState.value = VerificationState.InProgress(0f)

            val verificationResults = mutableListOf<TemplateVerificationResult>()
            var manifest: BackupManifest? = null
            var templatesFound = 0
            var assetsFound = 0

            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == "manifest.json" -> {
                                manifest = verifyManifest(zipIn.bufferedReader().readText())
                            }
                            entry.name.startsWith("templates/") -> {
                                val template = verifyTemplate(zipIn.bufferedReader().readText())
                                verificationResults.add(template)
                                templatesFound++
                                
                                _verificationState.value = VerificationState.InProgress(
                                    templatesFound.toFloat() / (manifest?.templateCount ?: 1)
                                )
                            }
                            entry.name.startsWith("assets/") -> {
                                verifyAsset(entry.name)
                                assetsFound++
                            }
                        }
                        entry = zipIn.nextEntry
                    }
                }
            }

            // Verify manifest consistency
            manifest?.let { m ->
                if (m.templateCount != templatesFound) {
                    return@withContext VerificationResult.Error(
                        "Template count mismatch: expected ${m.templateCount}, found $templatesFound"
                    )
                }
            } ?: return@withContext VerificationResult.Error("Missing manifest")

            // Check for template validation errors
            val failedValidations = verificationResults.filter { it.validationResult is ValidationResult.Failure }
            if (failedValidations.isNotEmpty()) {
                return@withContext VerificationResult.ValidationErrors(failedValidations)
            }

            _verificationState.value = VerificationState.Completed

            VerificationResult.Success(
                manifest = manifest,
                templateCount = templatesFound,
                assetCount = assetsFound
            )
        } catch (e: Exception) {
            _verificationState.value = VerificationState.Failed(e.message ?: "Verification failed")
            VerificationResult.Error(e.message ?: "Failed to verify backup")
        }
    }

    private fun verifyManifest(manifestJson: String): BackupManifest {
        try {
            val manifest = gson.fromJson(manifestJson, BackupManifest::class.java)
            if (manifest.version > TemplateBackupManager.BACKUP_VERSION) {
                throw IllegalStateException("Unsupported backup version: ${manifest.version}")
            }
            return manifest
        } catch (e: JsonSyntaxException) {
            throw IllegalStateException("Invalid manifest format", e)
        }
    }

    private fun verifyTemplate(templateJson: String): TemplateVerificationResult {
        try {
            val template = gson.fromJson(templateJson, ReportTemplate::class.java)
            val validationResult = templateValidator.validateTemplate(template)
            
            return TemplateVerificationResult(
                template = template,
                parseSuccess = true,
                validationResult = validationResult
            )
        } catch (e: JsonSyntaxException) {
            return TemplateVerificationResult(
                template = null,
                parseSuccess = false,
                validationResult = ValidationResult.Failure(listOf(
                    ValidationError("Failed to parse template: ${e.message}")
                ))
            )
        }
    }

    private fun verifyAsset(assetPath: String) {
        // Verify asset path format and allowed extensions
        if (!isValidAssetPath(assetPath)) {
            throw IllegalStateException("Invalid asset path: $assetPath")
        }
    }

    private fun isValidAssetPath(path: String): Boolean {
        val allowedExtensions = setOf(".png", ".jpg", ".jpeg", ".svg")
        return path.startsWith("assets/") && 
               allowedExtensions.any { path.lowercase().endsWith(it) }
    }
}

sealed class VerificationState {
    object Idle : VerificationState()
    data class InProgress(val progress: Float) : VerificationState()
    object Completed : VerificationState()
    data class Failed(val error: String) : VerificationState()
}

sealed class VerificationResult {
    data class Success(
        val manifest: BackupManifest,
        val templateCount: Int,
        val assetCount: Int
    ) : VerificationResult()
    
    data class ValidationErrors(
        val failedTemplates: List<TemplateVerificationResult>
    ) : VerificationResult()
    
    data class Error(val message: String) : VerificationResult()
}

data class TemplateVerificationResult(
    val template: ReportTemplate?,
    val parseSuccess: Boolean,
    val validationResult: ValidationResult
)

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val errors: List<ValidationError>) : ValidationResult()
}

data class ValidationError(
    val message: String,
    val field: String? = null
)
