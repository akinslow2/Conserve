package com.gemini.energy.CompanyCamAPI.Requests

import com.amazonaws.services.s3.AmazonS3Client
import java.io.File

data class UploadImageS3Params(
        val client: AmazonS3Client,
        val photoFile: File,
        val name: String
)