package com.crypto.calculator.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.crypto.calculator.R
import com.crypto.calculator.extension.toHexString
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import java.io.*


object ShareFileUtil {

    fun saveImage(context: Context, bitmap: Bitmap, fileName: String): ShareFileData? {
        val folderName = context.getString(R.string.app_name)
        val uniqueFileName = "${fileName}_${(System.currentTimeMillis() / 1000).toInt().toHexString()}"
        val shareFileData = ShareFileData()

        //check api version >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, uniqueFileName)
                put(MediaStore.MediaColumns.TITLE, uniqueFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/$folderName")
                // Add the date meta data to ensure the image is added at the front of the gallery
                put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis().toString())
                put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis().toString())
            }

            val resolver = context.contentResolver
            var uri: Uri? = null
            try {
                uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                shareFileData.uri = uri
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        return if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                            Log.d("saveImage", "Failed to save bitmap.")
                            null
                        } else {
                            shareFileData.file = File(
                                "${
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                                }/$folderName", "$uniqueFileName.png"
                            )
                            shareFileData
                        }
                    } ?: run {
                        Log.d("saveImage", "Failed to create new MediaStore record.")
                        null
                    }
                } ?: run {
                    Log.d("saveImage", "Failed to create new MediaStore record.")
                    return null
                }
            } catch (e: IOException) {
                uri?.let { orphanUri ->
                    // Don't leave an orphan entry in the MediaStore
                    resolver.delete(orphanUri, null, null)
                }
                return null
            }
        } else {
            val imagePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}/$folderName"
            val imageDir = File(imagePath)
            if (!imageDir.exists()) imageDir.mkdirs()
            val imageName = "$uniqueFileName.png"
            val imageFile = File(imagePath, imageName)

            try {
                FileOutputStream(imageFile).use {
                    return if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) {
                        Log.d("saveImage", "Failed to save bitmap.")
                        null
                    } else {
                        shareFileData.file = File(imagePath, "$uniqueFileName.png")
                        shareFileData
                    }
                }
            } catch (e: IOException) {
                Log.d("saveImage", e.toString())
                return null
            }
        }

    }

    fun sendFileOut(context: Context, shareFileData: ShareFileData, dataType: String) {
        Log.d("sendFileOut", "${shareFileData.file?.absolutePath}")
        shareFileData.file?.let { file ->
            Intent(Intent.ACTION_SEND).also {
                var fileUri: Uri? = null
                shareFileData.uri?.let { uri ->
                    fileUri = uri
                } ?: run {
                    fileUri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
                }
                it.putExtra(Intent.EXTRA_STREAM, fileUri)
                it.setDataAndType(fileUri, dataType)
                it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                it.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ContextCompat.startActivity(context, it, null)
            }
        }
    }

    fun sendLinkOut(context: Context, msg: String) {
        Intent(Intent.ACTION_SEND).also {
            it.putExtra(Intent.EXTRA_TEXT, msg)
            it.type = "text/plain"
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ContextCompat.startActivity(context, it, null)
        }
    }

    fun saveConfigToJsonFile(context: Context, obj: Any, suffix: String? = null) {
        val fileName = "config-${obj::class.java.simpleName}-$suffix.json"
        val rootDir = Environment.DIRECTORY_DOCUMENTS
        val relativePath = "${context.getString(R.string.app_name)}/Config/${obj::class.java.simpleName}"
        Log.d("saveConfig", "relativePath: $relativePath")
        try {
            val shareFileData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$rootDir/$relativePath")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        val file = File("$rootDir/$relativePath/$fileName")
                        val configInJson = GsonBuilder().setPrettyPrinting().create().toJson(obj)
                        outputStream.write(configInJson.toByteArray())
                        ShareFileData(
                            file = file,
                            uri = file.toUri()
                        )
                    }
                }
            } else {
                val dirPath = "${Environment.getExternalStorageDirectory().absolutePath}/$rootDir/$relativePath"
                Log.d("saveConfig", "dirPath: $dirPath")
                val dir = File(dirPath)
                if (!dir.exists()) {
                    Log.d("saveConfig", "mkdirs")
                    dir.mkdirs()
                }
                val file = File(dir, fileName)
                val writer = FileWriter(file)
                val configInJson = GsonBuilder().setPrettyPrinting().create().toJson(obj)
                writer.write(configInJson)
                writer.close()
                ShareFileData(
                    file = file,
                    uri = file.toUri()
                )
            }
            Log.d("saveConfig", "shareFileData: $shareFileData")
            Toast.makeText(context, "${shareFileData?.file?.name} saved", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.label_save_file_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun loadConfigFromJsonFile(context: Context, obj: Any, onSelect: (selectFile: String) -> Unit) {
        val rootDirPath = "${Environment.getExternalStorageDirectory().absolutePath}/${Environment.DIRECTORY_DOCUMENTS}"
        val relativePath = "${context.getString(R.string.app_name)}/Config/${obj::class.java.simpleName}"
        val path = "$rootDirPath/$relativePath"
        Log.d("loadConfig", "path: $path")
        val dir = File(path)
        if (dir.exists()) {
            val fileList = arrayListOf<String>()
            dir.listFiles()?.let { dirList ->
                for (file in dirList) {
                    if (file.name.endsWith(".json")) {
                        Log.d("loadConfig", "file: ${file.name}")
                        fileList.add(file.name)
                    }
                }
            }

            if (fileList.isNotEmpty()) {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Select File")
                    .setItems(
                        fileList.toTypedArray()
                    ) { dialog, item ->
                        dialog.dismiss()
                        val selectedFile = File("$path/${fileList[item]}")
                        val fileReader = FileReader(selectedFile)
                        val bufferedReader = BufferedReader(fileReader)
                        val stringBuilder = StringBuilder()
                        var line = bufferedReader.readLine()
                        while (line != null) {
                            stringBuilder.append(line).append("\n")
                            line = bufferedReader.readLine()
                        }
                        bufferedReader.close()
                        val jsonStr = stringBuilder.toString()
                        onSelect.invoke(jsonStr)
                    }
                    .setCancelable(false)
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                return
            }
        }

        Toast.makeText(context, "No config files found", Toast.LENGTH_SHORT).show()
    }
}

data class ShareFileData(
    var file: File? = null,
    var uri: Uri? = null
)