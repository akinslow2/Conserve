package CompanyCamAPI.Objects

import CompanyCamAPI.Types.ImageUri
import android.location.Address

data class Company(
        val id: String,
        val name: String,
        val status: String,
        val address: Address,
        val logo: Array<ImageUri>
)