package com.gemini.energy.CompanyCamAPI.Requests

import com.amazonaws.services.s3.AmazonS3Client

data class DeleteImageS3Params(
        val client: AmazonS3Client,
        val name: String
)