package CompanyCamAPI

//import kotlinx.coroutines.*
import CompanyCamAPI.Objects.Photo
import CompanyCamAPI.Objects.Project
import CompanyCamAPI.Requests.AddCommentRequest
import CompanyCamAPI.Requests.CreateProjectRequest
import CompanyCamAPI.Requests.UploadPhotoRequest
import CompanyCamAPI.Types.Coordinate
import java.util.*

class PhotoUploader {

    val ccService: CompanyCamService = CompanyCamServiceFactory.makeService()

    fun UploadPhoto(
            photoUri: String,
            projectName: String,
            photoTags: Array<String>,
            callback: (success: Boolean, exception: Throwable?) -> Unit) {

        //1 Create a Coroutine scope using a job to be able to cancel when needed
//        val mainActivityJob = Job()
//
//        //2 Handle exceptions if any
//        val errorHandler = CoroutineExceptionHandler { _, exception ->
//            callback(false, exception)
//        }
//
//        //3 the Coroutine runs using the Main (UI) dispatcher
//        val coroutineScope = CoroutineScope(mainActivityJob + Dispatchers.Main)
//        coroutineScope.launch(errorHandler) {
//
//            val projectId = getProjectId(projectName)
//            val photo = uploadPhoto(projectId, photoUri)
//
//            for (tag in photoTags) {
//                addCommentToPhoto(tag, photo.id)
//            }
//
//            callback(true, null)
//        }
        callback(false, Throwable("need to get libs imported"))
    }

    private suspend fun addCommentToPhoto(commentText: String, photoId: String) {
        ccService.addComment(
                AddCommentRequest(commentText),
                photoId)
    }

    private suspend fun uploadPhoto(projectId: String, photoUri: String): Photo {
        val photoRequest = UploadPhotoRequest(
                Coordinate(0f, 0f),
                photoUri,
                (Date().time / 1000).toInt()
        )
        return ccService.uploadPhoto(projectId, photoRequest)
    }

    private suspend fun getProjectId(projectName: String): String {

        val projects = ccService.getExistingProjects()
        val existing = projects.find { p -> p.name == projectName }

        if (existing != null)
            return existing.id

        val new = createProject(projectName)
        return new.id
    }

    private suspend fun createProject(projectName: String): Project {
        val options = CreateProjectRequest(projectName)
        return ccService.createProject(options)
    }
}
