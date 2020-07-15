package CompanyCamAPI.Types

data class Address(
        val street_address_1: String,
        val street_address_2: String,
        val city: String,
        val state: String,
        val postal_code: String,
        // short country identifier
        val country: String
)