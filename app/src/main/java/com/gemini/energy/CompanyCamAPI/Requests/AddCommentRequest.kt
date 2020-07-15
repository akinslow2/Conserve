package CompanyCamAPI.Requests

data class AddCommentRequest(
        val comment: Content
) {

    constructor(commentText: String) : this(
            comment = Content(commentText)
    )
}

data class Content(
        val content: String
)
