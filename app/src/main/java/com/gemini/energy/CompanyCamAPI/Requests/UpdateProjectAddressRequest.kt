package com.gemini.energy.CompanyCamAPI.Requests

import CompanyCamAPI.Types.Address

data class UpdateProjectAddressRequest(
        val project: ProjectAddress
) {
    constructor(address: Address) : this(
            project = ProjectAddress(address)
    )
}

data class ProjectAddress(
        val address: Address
)