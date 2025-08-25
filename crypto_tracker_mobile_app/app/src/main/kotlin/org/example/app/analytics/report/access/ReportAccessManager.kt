package org.example.app.analytics.report.access

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportAccessManager @Inject constructor() {
    private val accessRules = MutableStateFlow<Map<String, ReportAccessRule>>(emptyMap())
    private val userPermissions = MutableStateFlow<Map<String, Set<ReportPermission>>>(emptyMap())

    fun setReportAccess(
        reportId: String,
        rule: ReportAccessRule
    ) {
        accessRules.value = accessRules.value + (reportId to rule)
    }

    fun setUserPermissions(
        userId: String,
        permissions: Set<ReportPermission>
    ) {
        userPermissions.value = userPermissions.value + (userId to permissions)
    }

    fun canAccessReport(
        reportId: String,
        userId: String
    ): Boolean {
        val rule = accessRules.value[reportId] ?: return false
        val userPerms = userPermissions.value[userId] ?: return false

        return when (rule.accessLevel) {
            AccessLevel.PUBLIC -> true
            AccessLevel.PRIVATE -> rule.ownerId == userId
            AccessLevel.RESTRICTED -> {
                rule.ownerId == userId || 
                (rule.allowedUsers.contains(userId) && 
                 userPerms.contains(ReportPermission.VIEW))
            }
        }
    }

    fun canModifyReport(
        reportId: String,
        userId: String
    ): Boolean {
        val rule = accessRules.value[reportId] ?: return false
        val userPerms = userPermissions.value[userId] ?: return false

        return rule.ownerId == userId || 
               (rule.allowedUsers.contains(userId) && 
                userPerms.contains(ReportPermission.MODIFY))
    }

    fun canShareReport(
        reportId: String,
        userId: String
    ): Boolean {
        val rule = accessRules.value[reportId] ?: return false
        val userPerms = userPermissions.value[userId] ?: return false

        return rule.ownerId == userId || 
               (rule.allowedUsers.contains(userId) && 
                userPerms.contains(ReportPermission.SHARE))
    }

    fun getAccessibleReports(userId: String): Flow<List<String>> {
        return accessRules.map { rules ->
            rules.filter { (reportId, rule) ->
                canAccessReport(reportId, userId)
            }.keys.toList()
        }
    }

    fun getReportCollaborators(reportId: String): Flow<List<String>> {
        return accessRules.map { rules ->
            rules[reportId]?.allowedUsers?.toList() ?: emptyList()
        }
    }
}

data class ReportAccessRule(
    val ownerId: String,
    val accessLevel: AccessLevel,
    val allowedUsers: Set<String> = emptySet(),
    val expirationTime: Long? = null
)

enum class AccessLevel {
    PUBLIC,
    PRIVATE,
    RESTRICTED
}

enum class ReportPermission {
    VIEW,
    MODIFY,
    SHARE,
    DELETE
}
