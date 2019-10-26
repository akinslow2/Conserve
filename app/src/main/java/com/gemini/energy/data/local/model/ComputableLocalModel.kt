package com.gemini.energy.data.local.model

data class ComputableLocalModel(
        var auditId: Long,
        var auditName: String,
        var zoneId: Long,
        var zoneName: String,
        var auditScopeId: Long,
        var auditScopeName: String,
        var auditScopeType: String,
        var auditScopeSubType: String
)
