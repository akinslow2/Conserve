package CompanyCamAPI.Objects

import CompanyCamAPI.Types.ImageUri

data class User(
        val id: String,
        val company_id: String,
        val email_address: String,
        val status: String,
        val first_name: String,
        val last_name: String,
        val profile_image: Array<ImageUri>,
        val phone_number: String,
        val created_at: Int,
        val updated_at: Int,
        val user_url: String
)