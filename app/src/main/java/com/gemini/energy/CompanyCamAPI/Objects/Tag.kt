package CompanyCamAPI.Objects

data class Tag(
        val id: String,
        val company_id: String,
        val display_value: String,
        val value: String,
        val created_at: Int,
        val updated_at: Int,
        val tag_type: String
)