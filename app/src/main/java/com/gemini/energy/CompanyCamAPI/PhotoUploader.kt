package CompanyCamAPI

import CompanyCamAPI.Objects.Photo
import CompanyCamAPI.Objects.Project
import CompanyCamAPI.Requests.AddCommentRequest
import CompanyCamAPI.Requests.CreateProjectRequest
import CompanyCamAPI.Requests.UploadPhotoRequest
import CompanyCamAPI.Types.Coordinate
import com.gemini.energy.CompanyCamAPI.Requests.ApplyTagToPhotoRequest
import com.gemini.energy.CompanyCamAPI.Requests.CreateTagRequest
import com.gemini.energy.branch
import com.gemini.energy.service.ParseAPI
import com.gemini.energy.service.responses.UploadImageResponse
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.URI
import java.util.*

class PhotoUploader {

    val ccService: CompanyCamService = CompanyCamServiceFactory.makeService()

    fun UploadPhoto(
            photoFile: File,
            projectName: String,
            photoTags: Array<String>,
            callback: (success: Boolean, exception: Throwable?) -> Unit) {
        //1 Create a Coroutine scope using a job to be able to cancel when needed
        val mainActivityJob = Job()

        //2 Handle exceptions if any
        val errorHandler = CoroutineExceptionHandler { _, exception ->
            callback(false, exception)
        }

        //3 the Coroutine runs using the Main (UI) dispatcher
        val coroutineScope = CoroutineScope(mainActivityJob + Dispatchers.Main)
        coroutineScope.launch(errorHandler) {

            // have to upload photo to parse server before uploading to company cam
            val parsePhoto = uploadImageToParseServer(photoFile, "$projectName.jpg")

            val projectId = getProjectId("$branch - $projectName")
            val photo = uploadPhoto(projectId, parsePhoto.url)

            for (tag in photoTags) {
                addTagToPhoto(tag, photo.id)

            }
            // TODO: delete photo from parse
            // will need real parse master key to delete
//            ParseAPI.create().deleteImage(parsePhoto.name)

            callback(true, null)
        }
    }

    // returns the URI to the image on parse server
    private suspend fun uploadImageToParseServer(photo: File, imageName: String): UploadImageResponse {
        val body = photo.asRequestBody("image/*".toMediaTypeOrNull())
        return ParseAPI.create().uploadImage(imageName, body)
    }

    // returns the uploaded photo object
    private suspend fun uploadPhoto(projectId: String, photoUri: String): Photo {
        val photoRequest = UploadPhotoRequest(
                Coordinate(0f, 0f),
                photoUri,
                (Date().time / 1000).toInt()
        )
        return ccService.uploadPhoto(projectId, photoRequest)
    }

    // returns the project id that matches the project name
    private suspend fun getProjectId(projectName: String): String {

        val projects = ccService.getExistingProjects()
        val existing = projects.find { p -> p.name == projectName }

        if (existing != null)
            return existing.id

        val new = createProject(projectName)
        return new.id
    }

    // creates a new project
    private suspend fun createProject(projectName: String): Project {
        val options = CreateProjectRequest(projectName)
        return ccService.createProject(options)
    }

    // adds a tag to a photo
    private suspend fun addTagToPhoto(tagName: String, photoId: String) {
        ccService.createTag(CreateTagRequest(tagName))
        ccService.addTagToPhoto(photoId, ApplyTagToPhotoRequest(listOf(tagName)))
    }
}
