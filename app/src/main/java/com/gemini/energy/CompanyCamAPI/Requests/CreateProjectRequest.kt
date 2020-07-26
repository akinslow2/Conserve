package CompanyCamAPI.Requests

import CompanyCamAPI.Types.Address
import CompanyCamAPI.Types.Coordinate

data class CreateProjectRequest(
        val name: String,
        val address: Address,
        val coordinates: Coordinate,
        val geofence: Array<Coordinate>
) {

    constructor(name: String, address: Address) : this(
            name,
            address,
            Coordinate(0f, 0f),
            arrayOf()
    )
}
