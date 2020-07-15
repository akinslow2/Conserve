package CompanyCamAPI.Requests

data class Auth1RequestParameters(
        // client id provided by company cam
        val client_id: String,

        // uri to redirect the user to after they authorize your application
        val redirect_uri: String,

        // must be set to "code"
        val response_type: String = "code"
)