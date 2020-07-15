package CompanyCamAPI.Objects

data class Comment(
        val id: String,
        val creator_id: String,
        val commentable_id: String,
        val commentable_type: String,
        val creator_name: String,
        val status: String,
        val content: String,
        val created_at: Int,
        val updated_at: Int
)