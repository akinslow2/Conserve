package CompanyCamAPI.Requests

data class Auth2RequestParameters(
        // client id provided by company cam
        val client_id: String,

        // secret key provided by company cam
        val client_secret: String,

        // included in the code parameter from the redirect from the previous request
        val code: String,

        // same URI as in Auth step 1
        val redirect_uri: String,

        val grant_type: String = "authorization_code"
)