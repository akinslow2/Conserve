package CompanyCamAPI

import CompanyCamAPI.Objects.Comment
import CompanyCamAPI.Objects.Photo
import CompanyCamAPI.Objects.Project
import CompanyCamAPI.Objects.Tag
import CompanyCamAPI.Requests.AddCommentRequest
import CompanyCamAPI.Requests.Auth2RequestParameters
import CompanyCamAPI.Requests.CreateProjectRequest
import CompanyCamAPI.Requests.UploadPhotoRequest
import CompanyCamAPI.Responses.Auth2Response
import com.gemini.energy.CompanyCamAPI.Requests.ApplyTagToPhotoRequest
import com.gemini.energy.CompanyCamAPI.Requests.CreateTagRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CompanyCamService {
    // Authentication: step 1
//    redirect the user to the CompanyCam authorization URI.
    /// https://app.companycam.com/oauth/authorize?client_id={client_id}&redirect_uri={authorized_redirect_uri}&response_type=code&scope=read+write+destroy


    // Authentication: step 2
    /// curl -X POST --data "client_id={client_id}&client_secret={client_secret}&code={code_from_uri}&grant_type=authorization_code&redirect_uri={authorized_redirect_uri}" "https://app.companycam.com/oauth/token"
    @POST("oauth/token")
    fun getAuthToken(@Body request: Auth2RequestParameters): Call<Auth2Response>

    // upload a photo
    @POST("v2/projects/{project_id}/photos/")
    suspend fun uploadPhoto(
            @Path("project_id") projectId: String,
            @Body uploadPhotoRequest: UploadPhotoRequest): Photo

    // creates a new project
    // returns the uploaded project object
    @POST("v2/projects/")
    suspend fun createProject(@Body createProjectRequest: CreateProjectRequest): Project

    // list all projects
    // Returns a list of projects you’ve previously created. The projects are returned in sorted order, with the most recent projects appearing first
    @GET("v2/projects?status=active")
    suspend fun getExistingProjects(): List<Project>

    // creates a new tag
    @POST("v2/tags/")
    suspend fun createTag(@Body content: CreateTagRequest): Tag

    // tags a photo with an existing tag
    @POST("v2/photos/{photo_id}/tags/")
    suspend fun addTagToPhoto(@Path("photo_id") photoId: String, @Body tags: ApplyTagToPhotoRequest): List<Tag>
}