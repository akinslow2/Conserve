package CompanyCamAPI.Responses

data class Auth2Response(
        // this is the bearer token that will be used to authenticate further requests
        val access_token: String,
        val token_type: String,
        val expires_in: Int,
        val refresh_token: String,
        val scope: String,
        val created_at: Int
)