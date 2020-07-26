package com.gemini.energy.presentation.base

import CompanyCamAPI.CompanyCamServiceFactory
import CompanyCamAPI.Enums.ErrorCodes
import CompanyCamAPI.PhotoUploader
import CompanyCamAPI.Types.Address
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.util.Log
import android.view.View
import android.widget.Toast
import com.gemini.energy.ImageFilePath
import com.gemini.energy.R
import com.gemini.energy.data.local.system.AuditDatabase
import com.gemini.energy.databinding.ActivityHomeDetailBinding
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_home_detail.*
import kotlinx.android.synthetic.main.activity_home_mini_bar.*
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

open class BaseActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var crossfader: Crossfader<GmailStyleCrossFadeSlidingPaneLayout>
    var binder: ActivityHomeDetailBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        binder = DataBindingUtil
                .setContentView(this, R.layout.activity_home_detail)

        setupToolbar()
        setupCrossfader()
    }

    open fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    private fun setupCrossfader() {
        val first = layoutInflater.inflate(R.layout.activity_home_side_bar, null)
        val second = layoutInflater.inflate(R.layout.activity_home_mini_bar, null)

        crossfader
                .withContent(findViewById(R.id.root_home_container))
                .withFirst(first, WIDTH_FIRST)
                .withSecond(second, WIDTH_SECOND)
                .withResizeContentPanel(true)
                .withGmailStyleSwiping()
                .build()

        crossfader.crossFadeSlidingPaneLayout.openPane()
        crossfader.crossFadeSlidingPaneLayout.setOffset(1f)
        crossfader.resize(1f)

        side_panel_button.setOnClickListener {
            crossfader.crossFade()
        }
    }

    companion object {
        private const val WIDTH_SECOND = 150
        private const val WIDTH_FIRST = 350
    }

    /// needed for image upload

    private val takeImageRequestCode = 1
    private val selectImageRequestCode = 2
    private val selectMultipleImageRequestCode = 3
    private val takeImagePermissionRequestCode = 101
    private val galleryImagePermissionRequestCode = 102
    private val galleryMultipleImagePermissionRequestCode = 103

    // file of the last photo taken
    private lateinit var photoFile: File

    // service for uploading to company cam
    private val photoUploader = PhotoUploader()

    private val fileProviderAuthority = "com.gemini.energy.android.fileprovider"

    private lateinit var projectName: String
    private lateinit var projectAddress: Address
    private lateinit var tags: List<String>


    // upload photo if get photo was successful
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            takeImageRequestCode -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Toast.makeText(applicationContext, "PHOTO SUCCESS! uploading image", Toast.LENGTH_SHORT).show()

                    linlaHeaderProgress.visibility = View.VISIBLE
                    photoUploader.UploadPhoto(photoFile, projectName, projectAddress, tags.toTypedArray())
                    { success, error ->
                        linlaHeaderProgress.visibility = View.GONE

                        if (success)
                            Toast.makeText(applicationContext, "Upload success", Toast.LENGTH_LONG).show()
                        else {
                            Log.e(
                                    "BaseActivity upload taken image to compnay cam",
                                    "ERROR: ${error?.message} ${error?.stackTrace}")
                            handleCompanyCamException(error)
                        }
                    }
                }
                else Toast.makeText(applicationContext, "image capture failed", Toast.LENGTH_SHORT).show()

            }
            selectImageRequestCode -> {
                if (resultCode == Activity.RESULT_OK && data?.data != null) {
                    Toast.makeText(applicationContext, "SELECT PHOTO SUCCESS! uploading image", Toast.LENGTH_SHORT).show()

                    val file = File(ImageFilePath.getPath(this, data.data))
                    linlaHeaderProgress.visibility = View.VISIBLE
                    photoUploader.UploadPhoto(file, projectName, projectAddress, tags.toTypedArray())
                    { success, error ->
                        linlaHeaderProgress.visibility = View.GONE

                        if (success)
                            Toast.makeText(applicationContext, "Upload success", Toast.LENGTH_LONG).show()
                        else {
                            Log.e(
                                    "BaseActivity upload selected image to compnay cam",
                                    "ERROR: ${error?.message} ${error?.stackTrace}")

                            handleCompanyCamException(error)
                        }
                    }
                }
                else Toast.makeText(applicationContext, "select single image failed", Toast.LENGTH_SHORT).show()
            }
            selectMultipleImageRequestCode -> {
                if (resultCode == Activity.RESULT_OK && data?.clipData != null) {
                    linlaHeaderProgress.visibility = View.VISIBLE

                    val clips = data.clipData!!
                    val itemCount = clips.itemCount

                    for (i in 0 until itemCount) {
                        val file = File(ImageFilePath.getPath(this, clips.getItemAt(i).uri))
                        photoUploader.UploadPhoto(file, projectName, projectAddress, tags.toTypedArray()) { success, error ->

                            if (i == itemCount - 1)
                                linlaHeaderProgress.visibility = View.GONE

                            if (success) {
                                Toast.makeText(applicationContext, "${i + 1} in $itemCount Upload success", Toast.LENGTH_SHORT).show()
                            } else if (!success)
                                Toast.makeText(
                                        applicationContext,
                                        "${i + 1} in $itemCount error uploading image: ${error?.message}",
                                        Toast.LENGTH_SHORT
                                ).show()
                        }
                    }

                } else Toast.makeText(applicationContext, "select multiple image failed", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Log.d("-----", "unknown activity result $requestCode")
            }
        }
    }

    // callback for permissions request
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        if (requestCode == takeImagePermissionRequestCode
                && (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            takePictureAndStore()
        }
        else if (requestCode == galleryImagePermissionRequestCode
                && (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            selectImageFromGallery()
        }
        else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }


    fun startPhotoUploadToCompanyCam(projectName: String?, address: Address, tags: List<String>) {
        if (!checkForCompanyCamAuth()) return

        if (projectName == null || projectName.isBlank()) {
            AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Please select a project to upload to.")
                    .setPositiveButton("Ok") { dialog, _ -> dialog.cancel() }
                    .create()
                    .show()
            return
        }

        this.projectName = projectName
        this.tags = tags
        this.projectAddress = address

        AlertDialog.Builder(this)
                .setTitle("Uploading new image(s) to $projectName.")
                .setCancelable(true)
                .setItems(arrayOf(
                        "Take New Image",
                        "Select Single Image From Gallery",
                        "Upload Multiple From Gallery")) { _, selected ->
                    when (selected) {
                        0 -> takePictureAndUploadToCompanyCam(projectName, tags)
                        1 -> uploadImageFromGallery(projectName, tags)
                        2 -> uploadMultipleFromGallery(projectName, tags)
                        else -> Log.d("------", "unexpected select response $selected")
                    }
                }
                .create()
                .show()
    }

    fun getAddressFromAuditId(auditId: Long?): Address {
        if (auditId == null) return Address("", "", "", "", "", "")
        // create connection to DB to retreive project address
        val db = AuditDatabase.newInstance(this)
        // get feature info for the selected audit
        val features = db.featureDao().getAllByAudit(auditId).blockingGet()

        // get the address from the list of features
        val addressFeature = features.find { f -> f.key == "General Client Info Address" }

        return Address(
                addressFeature?.valueString ?: "",
                "",
                "",
                "",
                ""
        )
    }

    // returns true if an auth token is saved
    private fun checkForCompanyCamAuth(): Boolean {
        if (CompanyCamServiceFactory.bearerToken() != null) return true

        AlertDialog.Builder(this)
                .setTitle("Login Needed")
                .setMessage("Please log into company cam to upload images.")
                .setNeutralButton("Cancel") { dialog, _ -> dialog.cancel() }
                .setPositiveButton("Ok") {
                    dialog, _ -> dialog.cancel()
                    // do auth
                    val authIntent = CompanyCamServiceFactory.authorizeIntent()
                    if (authIntent?.resolveActivity(packageManager) != null)
                        startActivity(authIntent)
                }
                .create()
                .show()

        return false

    }

    // displays appropriate error message to the user
    private fun handleCompanyCamException(exception: Throwable?) {
        if (exception is HttpException) {
            when (exception.code()) {
                // TODO: add login button
                ErrorCodes.Unauthorized.code -> AlertDialog.Builder(this)
                        .setTitle("Your Company Cam Authorization Expired")
                        .setMessage("Please log into company cam again.")
                        .setPositiveButton("Ok") { dialog, _ -> dialog.cancel() }
                        .create()
                        .show()

                ErrorCodes.SubscriptionExpired.code ->
                    AlertDialog.Builder(this)
                            .setTitle("Company Cam Subscription Expired")
                            .setMessage("The company cam subscription has expired. Please renew the subscription and try again.")
                            .setPositiveButton("Ok") { dialog, _ -> dialog.cancel() }
                            .create()
                            .show()

                ErrorCodes.ServerError.code -> AlertDialog.Builder(this)
                        .setTitle("Company Cam Server Error")
                        .setMessage("Company Cam is having issues with their server. Please try again later.")
                        .setPositiveButton("Ok") { dialog, _ -> dialog.cancel() }
                        .create()
                        .show()
                else ->
                    AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("error uploading image: ${exception.message}")
                        .setPositiveButton("Ok") { dialog, _ -> dialog.cancel() }
                        .create()
                        .show()
            }

        }
        else
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("error uploading image: ${exception?.message}")
                .setPositiveButton("Ok") { dialog, _ -> dialog.cancel() }
                .create()
                .show()
    }

    // takes a picture and uploads it to company cam
    private fun takePictureAndUploadToCompanyCam(projectName: String, tags: List<String>) {
        this.projectName = projectName
        this.tags = tags
        Log.i("-----", "take picture for upload")

        // make sure we have a camera
        if (!baseContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(applicationContext, "camera is not available", Toast.LENGTH_LONG).show()
            return
        }

        if (checkTakeImagePermissions())
            takePictureAndStore()
        else requestTakeImagePermission()
    }

    // select 1 image from the gallery to upload to compnay cam
    private fun uploadImageFromGallery(projectName: String, tags: List<String>) {
        this.projectName = projectName
        this.tags = tags
        Log.d("-----", "select single for upload")

        if (checkGalleryPermissions())
            selectImageFromGallery()
        else requestGalleryPermission(false)
    }

    // select multiple images to upload to company cam
    private fun uploadMultipleFromGallery(projectName: String, tags: List<String>) {
        this.projectName = projectName
        this.tags = tags
        Log.d("-----", "select multiple for upload")
        if (checkGalleryPermissions())
            selectMultipleFromGallery()
        else requestGalleryPermission(true)
    }

    // takes picture and saves to custom location
    // need to use currentPhotoPath to get image in onActivityResult
    private fun takePictureAndStore() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(this, "Error taking photo", Toast.LENGTH_SHORT).show()
                null
            }

            photoFile?.also {
                val photoURI = FileProvider.getUriForFile(
                        this,
                        fileProviderAuthority,
                        it)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, takeImageRequestCode)
            }
        }
    }

    // create image file with path when saving image to custom location
    // sets currentPhotoPath for later use in onActivityResult
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                "JPEG_${timestamp}_",
                ".jpg",
                storageDirectory
        ).apply {
            photoFile = this
        }
    }

    // select image from gallery
    // photo data is returned in onActivityResult
    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, selectImageRequestCode)
        }
    }

    // select multiple images from gallery
    // photo data is returned in onActivityResult
    private fun selectMultipleFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, selectMultipleImageRequestCode)
        }
    }

    // check permissions necessary for image capture
    private fun checkTakeImagePermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)

    }

    // check permissions needed for selecting image from gallery
    private fun checkGalleryPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
    }

    // request permissions for image capture
    private fun requestTakeImagePermission() {
        val shouldShowCamera = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
        val shouldShowStorage = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        if (shouldShowCamera || shouldShowStorage) {
            Toast.makeText(this, "No Permission to use the Camera services", Toast.LENGTH_SHORT).show()
        }

        ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE),
                takeImagePermissionRequestCode)
    }

    // request permissions for selecting image from gallery
    private fun requestGalleryPermission(isMultiple: Boolean) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Permission has not been granted for gallery", Toast.LENGTH_SHORT).show()
        }

        ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                if (isMultiple) galleryMultipleImagePermissionRequestCode else galleryImagePermissionRequestCode)
    }
}