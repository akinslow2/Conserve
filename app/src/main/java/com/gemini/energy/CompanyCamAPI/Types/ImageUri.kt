package CompanyCamAPI.Types

data class ImageUri(
        // image type: original or thumbnail
        val type: String,
        // full uri of the photo
        val uri: String
)