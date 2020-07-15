package CompanyCamAPI.Enums

enum class ErrorCodes(val code: Int) {
    InvalidRequest(400),

    // user needs to authenticate or authentication failed
    Unauthorized(401),

    // payment is required
    SubscriptionExpired(402),

    // user doesn't have privilege to access the resource
    Forbidden(403),

    // resource could not be found
    NotFound(404),

    // entry is not unique
    Conflict(409),

    // invalid data that could not be processed
    Unprocessable(422),

    // internal company cam server error
    ServerError(500)
}