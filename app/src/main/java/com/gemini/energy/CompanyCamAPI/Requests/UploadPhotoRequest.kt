package CompanyCamAPI.Requests

import CompanyCamAPI.Types.Coordinate

data class UploadPhotoRequest(
        // lat and long of the project
        val coordinates: Coordinate,
        // uri of the photo
        val uri: String,
        // timestamp when the photo was taken
        val captured_at: Int
)