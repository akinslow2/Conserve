package CompanyCamAPI.Objects

import CompanyCamAPI.Types.Address
import CompanyCamAPI.Types.Coordinate
import CompanyCamAPI.Types.ImageUri
import CompanyCamAPI.Types.ProjectIntegration

// the project object
// companies can have multiple projects
// a project belongs to one company
data class Project(
        val id: String,
        val company_id: String,
        val creator_id: String,
        val creator_type: String,
        val creator_name: String,
        val status: String,
        val name: String,
        val address: Address,
        val coordinates: Coordinate,
        val feature_image: Array<ImageUri>,
        val project_url: String,
        val embedded_project_url: String,
        val integrations: Array<ProjectIntegration>,
        val slug: String,
        val public: Boolean,
        val geofence: Array<Coordinate>,
        val created_at: Int,
        val updated_at: Int
)