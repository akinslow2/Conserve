package CompanyCamAPI.Objects

import CompanyCamAPI.Types.Coordinate
import CompanyCamAPI.Types.ImageUri

data class Photo(
        val id: String,
        val company_id: String,
        val creator_id: String,
        val creator_type: String,
        val creator_name: String,
        val project_id: String,
        val processing_status: String,
        val coordinates: Coordinate,
        val uris: Array<ImageUri>,
        val hash: String,
        val internal: Boolean,
        val captured_at: Int,
        val created_at: Int,
        val updated_at: Int,
        val photo_url: String
)