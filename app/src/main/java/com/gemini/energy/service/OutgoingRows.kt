package com.gemini.energy.service

import android.content.Context
import android.os.Environment
import android.util.Log
import com.dropbox.core.v2.files.FileMetadata
import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.presentation.audit.DropBox
import com.gemini.energy.presentation.audit.UploadFileTask
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

class DataHolder {

    /**
     * Set from EBase
     * */
    var computable: Computable<*>? = null
    set(value) {
        this.path = StringBuilder()

                .append("${value?.auditName?.toLowerCase()?.replace("\\s+".toRegex(), "_")}/")
                .append("${value?.zoneName?.toLowerCase()?.replace("\\s+".toRegex(), "_")}/")
                .append("${value?.auditScopeType?.value?.toLowerCase()}_")
                .append("${value?.auditScopeSubType?.toString()?.toLowerCase()}_")
                .append("${value?.auditScopeName?.toLowerCase()?.replace("[^a-zA-Z0-9]".toRegex(), "_")}/")

                .toString()

        field = value
    }

    /**
     * These variables are exposed and are supposed to be set by Each Computables - @EBase
     * */
    var header: MutableList<String>? = mutableListOf()
    var rows: MutableList<Map<String, String>>? = mutableListOf()
    var fileName: String = ""

    /**
     * Set during runtime via the computable set method
     * */
    var path: String = ""

    override fun toString(): String {
        return "fileName: [$fileName]\n" +
                "path: [$path]\n" +
                "header: $header\n" +
                "rows count: [${rows?.count()}]"
    }

}

class OutgoingRows(private val context: Context) {
    lateinit var computable: Computable<*>
    lateinit var dataHolder: MutableList<DataHolder>

    /**
     * Saves the Data to the Internal File System
     * */
    fun save() {

        // Step 1: Loop over through the Data Holder
        // Step 2: Extract out the individual components
        // Step 3: Feed those components to the data method
        // Step 4: Collect those data string in a list
        // Step 5: Concatenate the list and writeToLocal that to the file

        Log.d(TAG, "Data Holder Count - [${dataHolder.count()}] - (${thread()})")

        synchronized(dataHolder) {
            val iterator = dataHolder.iterator()
            while (iterator.hasNext()) {
                val eachData = iterator.next()
                val outgoing = StringBuilder()

                val header = eachData.header
                val rows = eachData.rows

                Log.d(TAG, "## Debug :: Data Holder - (${thread()}) ##")
                Log.d(TAG, eachData.path)
                Log.d(TAG, eachData.fileName)

                Log.d(TAG, header.toString())
                Log.d(TAG, rows.toString())

                outgoing.append(data(header, rows))
                val file = getFile(eachData.path, eachData.fileName)
                val data = outgoing.toString()
                val path = "/Gemini/Energy/${eachData.path}${eachData.fileName}"

                writeToLocal(data, file)
                writeToDropBox(data, path)
            }

        }
    }

    @Synchronized
    private fun writeToDropBox(data: String, path: String) {

        try {
            if (DropBox.hasToken()) {
                val uploadTask = UploadFileTask(DropBox.getClient(), object: UploadFileTask.Callback {
                    override fun onUploadComplete(result: FileMetadata) {
                        Timber.d("DropBox - File Upload Complete")
                    }
                    override fun onError(e: Exception?) {
                        Timber.d("Error - DropBox")
                        e?.printStackTrace()
                    }
                })

                uploadTask.execute(data, path)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @Synchronized
    private fun writeToLocal(data: String, file: File) {
        try {
            val inputStream = ByteArrayInputStream(data.toByteArray())
            val outputStream = BufferedOutputStream(FileOutputStream(file))

            val buffer = ByteArray(1024)
            var bytesRead = inputStream.read(buffer, 0, buffer.size)

            while (bytesRead > 0) {
                outputStream.write(buffer, 0, bytesRead)
                bytesRead = inputStream.read(buffer, 0, buffer.size)
            }

            inputStream.close()
            outputStream.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     * UtilityRate Methods
     * */
    private fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    private fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
    }

    private fun parseFilenameFromPath(filePath: String): String {
        val index = filePath.lastIndexOf('/') + 1
        return filePath.substring(index)
    }


    /**
     * Makes sure the Directory is created and all the folder structures are in place
     * */
    fun getDocumentFolderPath(subFolderPath: String? = null): File? {

        val folderDir: File?

        if (isExternalStorageWritable()) {
            Log.d(TAG, "External - Storage :: Writable - (${thread()})")
            if (subFolderPath == null) {
                folderDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).path)
            } else {
                folderDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).path + "/$subFolderPath")
            }
        } else {
            Log.d(TAG, "External - Storage :: Not Writable")
            if (subFolderPath == null) {
                folderDir = File(context.filesDir.path + "/Documents")
            } else {
                folderDir = File(context.filesDir.path + "/Documents/$subFolderPath")
            }
        }

        if (!folderDir.isDirectory) {
            Log.d(TAG, "****** Creating Directory *******")
            folderDir.mkdirs()
        }

        Log.d(TAG, folderDir.toString())
        return folderDir
    }


    /**
     * File Object where the Data gets written to
     * */
    private fun getFile(path: String, fileName: String): File {
        Log.d(TAG, "GET FILE -- $GEMINI$path")
        val directory = getDocumentFolderPath("$GEMINI$path")!!
        return File(directory.toString(), fileName)
    }


    companion object {

        private const val TAG = "OutgoingRows"
        private const val GEMINI = "gemini/"
        const val DEBUG = "${GEMINI}debug/"

        /**
         * Creating a Comma Separated CSV Data String
         * */
        @Synchronized
        private fun data(header: List<String>?, rows: MutableList<Map<String, String>>?): String {
            val buffer = StringBuilder()
            buffer.append(header?.joinToString())
            buffer.append("\r\n")

            for (row in rows!!) {
                val tmp: MutableList<String> = mutableListOf()
                header?.let { hdr ->
                    for (item in hdr) {
                        tmp.add(row[item] ?: "")
                    }
                }
                buffer.append(tmp.joinToString())
                buffer.append("\r\n")
            }

            return buffer.toString()
        }
    }

    private fun thread() = Thread.currentThread().name

}