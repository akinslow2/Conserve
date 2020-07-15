package CompanyCamAPI.Types

data class ProjectIntegration(
        // name of integration
        val type: String,
        // record id provided by the integration
        val relation_id: String
)