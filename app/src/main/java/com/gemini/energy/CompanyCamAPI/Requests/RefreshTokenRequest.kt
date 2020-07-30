package com.gemini.energy.CompanyCamAPI.Requests

data class RefreshTokenRequest (
        // client id provided by company cam
        val client_id: String,

        // secret key provided by company cam
        val client_secret: String,

        val refresh_token: String,

        // same URI as in Auth step 1
        val redirect_uri: String,

        val grant_type: String = "refresh_token"
)