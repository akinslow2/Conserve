package CompanyCamAPI

import CompanyCamAPI.Objects.Comment
import CompanyCamAPI.Objects.Photo
import CompanyCamAPI.Objects.Project
import CompanyCamAPI.Requests.AddCommentRequest
import CompanyCamAPI.Requests.Auth2RequestParameters
import CompanyCamAPI.Requests.CreateProjectRequest
import CompanyCamAPI.Requests.UploadPhotoRequest
import CompanyCamAPI.Responses.Auth2Response
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
    /// POST https://api.companycam.com/v2/projects/:project_id/photos/
    @POST("v2/projects/{project_id}/photos/")
    suspend fun uploadPhoto(
            @Path("project_id") projectId: String,
            @Body uploadPhotoRequest: UploadPhotoRequest): Photo

    // create project
    // should return a project object, but not sure
    /// POST https://api.companycam.com/v2/projects
    /// curl -X POST -H "Content-type: application/json" -H "Authorization: Bearer <ACCESS_TOKEN>"
    /// https://api.companycam.com/v2/projects/ -d '{"project":{"name":"Psych Office","address":{"street_address_1":"2756 O Hara Lane","city":"Santa Barbara","state":"CA","postal_code":"93101","country":"US"},"coordinates":{"lat":34.398307,"lon":-119.712367}}'}
    @POST("v2/projects/")
    suspend fun createProject(@Body createProjectRequest: CreateProjectRequest): Project

    // list all projects
    // Returns a list of projects you’ve previously created. The projects are returned in sorted order, with the most recent projects appearing first
    /// GET https://api.companycam.com/v2/projects
    @GET("v2/projects?status=active")
    suspend fun getExistingProjects(): List<Project>

    // get specific project
    // Retrieves the details of an existing project. You need only supply the unique project identifier that was returned upon project creation
    /// GET https://api.companycam.com/v2/projects/{project_id}
    @GET("v2/projects/{project_id}")
    fun getProject(@Path("project_id") projectId: String): Call<Project>

    @POST("v2/photos/{photo_id}/comments/")
    suspend fun addComment(@Body content: AddCommentRequest, @Path("photo_id") photoId: String): Comment

//    fun addTagToPhoto():
}