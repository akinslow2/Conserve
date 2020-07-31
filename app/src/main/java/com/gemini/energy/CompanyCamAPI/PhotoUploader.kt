package CompanyCamAPI
import CompanyCamAPI.Objects.Photo
import CompanyCamAPI.Objects.Project
import CompanyCamAPI.Requests.CreateProjectRequest
import CompanyCamAPI.Requests.PhotoRequest
import CompanyCamAPI.Requests.UploadPhotoRequest
import CompanyCamAPI.Types.Address
import CompanyCamAPI.Types.Coordinate
import android.os.AsyncTask
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.PutObjectResult
import com.gemini.energy.CompanyCamAPI.Requests.*
import com.gemini.energy.branch
import kotlinx.coroutines.*
import java.io.File
import java.util.*

const val s3BucketName = "gemini-image-storage"

class PhotoUploader(awsSecretKey: String, awsAccessKey: String) {

    private val ccService: CompanyCamService = CompanyCamServiceFactory.makeService()
    private val awsCreds: BasicAWSCredentials = BasicAWSCredentials(awsAccessKey, awsSecretKey)
    private val s3Client: AmazonS3Client

    init {
        s3Client = AmazonS3Client(awsCreds)
    }

    fun UploadPhoto(
            photoFile: File,
            projectName: String,
            projectAddress: Address,
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

            // check if should refresh token
            if (!CompanyCamServiceFactory.hasNonExpiredToken()) {
                val newToken = CompanyCamServiceFactory.makeService().refreshAccessToken(RefreshTokenRequest(
                        CompanyCamServiceFactory.clientId,
                        CompanyCamServiceFactory.secretKey,
                        CompanyCamServiceFactory.refreshToken(),
                        CompanyCamServiceFactory.redirectUri))
                CompanyCamServiceFactory.setToken(newToken)
            }

            // have to upload photo to aws s3 before uploading to company cam
            val photoName = photoFile.name

            UploadImageToS3()
                    .execute(UploadImageS3Params(s3Client, photoFile, photoName))
                    .get()

            val project = getProject("$branch - $projectName", projectAddress)
            if (project.address != projectAddress)
                ccService.updateProjectAddress(project.id, UpdateProjectAddressRequest(projectAddress))

            val photoUrl = "https://gemini-image-storage.s3.us-east-2.amazonaws.com/$photoName"

            val photo = uploadPhoto(project.id, photoUrl)

            for (tag in photoTags) {
                addTagToPhoto(tag, photo.id)
            }

            DeleteImageFromS3()
                    .execute(DeleteImageS3Params(s3Client, photoName))
                    .get()

            callback(true, null)
        }
    }


    // uploads the image to aws s3 && gives image public access
    internal class UploadImageToS3 : AsyncTask<UploadImageS3Params, Void, PutObjectResult?>() {

        override fun doInBackground(vararg p0: UploadImageS3Params?): PutObjectResult? {
            try {
                val obj = p0[0] as UploadImageS3Params
                val result = obj.client.putObject(s3BucketName, obj.name, obj.photoFile)

                obj.client.setObjectAcl(s3BucketName, obj.name, CannedAccessControlList.PublicRead)

                return result
            } catch (e: Exception) {
                //handle exception
                return null
            }
        }
    }


    // delets the image from aws s3
    internal class DeleteImageFromS3 : AsyncTask<DeleteImageS3Params, Void, Void>() {
        override fun doInBackground(vararg p0: DeleteImageS3Params?): Void? {
            val obj = p0[0] as DeleteImageS3Params
            obj.client.deleteObject(s3BucketName, obj.name)
            return null
        }
    }


    // returns the uploaded photo object
    private suspend fun uploadPhoto(projectId: String, photoUri: String): Photo {
        val photoRequest = UploadPhotoRequest(
                PhotoRequest(
                        Coordinate(0f, 0f),
                        photoUri,
                        (Date().time / 1000).toInt())
        )
        return ccService.uploadPhoto(projectId, photoRequest)
    }


    // returns the project id that matches the project name
    private suspend fun getProject(projectName: String, projectAddress: Address): Project {
        val projects = ccService.getExistingProjects()
        val existing = projects.find { p -> p.name == projectName }

        if (existing != null)
            return existing

        return createProject(projectName, projectAddress)
    }


    // creates a new project
    private suspend fun createProject(projectName: String, projectAddress: Address): Project {
        val options = CreateProjectRequest(projectName, projectAddress)
        return ccService.createProject(options)
    }


    // adds a tag to a photo
    private suspend fun addTagToPhoto(tagName: String, photoId: String) {
        ccService.createTag(CreateTagRequest(tagName))
        ccService.addTagToPhoto(photoId, ApplyTagToPhotoRequest(listOf(tagName)))
    }
}
