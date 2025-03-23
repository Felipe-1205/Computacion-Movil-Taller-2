package com.example.supertaller2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.supertaller2.databinding.ActivityImagenBinding
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class Imagen : AppCompatActivity() {

    private lateinit var binding: ActivityImagenBinding
    private lateinit var uriCamera: Uri
    val PERM_CAMERA_CODE = 1000
    val REQUEST_VIDEO_CAPTURE = 1002
    val PERM_GALERY_GROUP_CODE = 2000
    var outputPath: Uri? = null

    private val mGetContentGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uriLocal ->
            if (uriLocal != null) {
                loadimage(uriLocal)
            }
        }
    private val mGetContentGalleryvid =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uriLocal ->
            if (uriLocal != null) {
                loadvideo(uriLocal)
            }
        }


    private val mGetContentCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            if (result) {
                loadimage(uriCamera)
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intfile()

        binding.camara.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    if (binding.switch1.isChecked)
                        dispatchTakeVideoIntent()
                    else
                        mGetContentCamera.launch(uriCamera)
                }

                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    Toast.makeText(this, "El permiso de Camara es necesario para usar esta actividad 😭", Toast.LENGTH_SHORT).show()
                }

                else -> {
                    requestPermissions(arrayOf(Manifest.permission.CAMERA), PERM_CAMERA_CODE)
                }
            }
        }

        binding.galeria.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    if (binding.switch1.isChecked)
                        mGetContentGalleryvid.launch("video/*")
                    else
                        mGetContentGallery.launch("image/*")
                }

                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    Toast.makeText(this, "El permiso de Galeria es necesario para usar esta actividad 😭", Toast.LENGTH_SHORT).show()
                }

                else -> {
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        permissions.plus(Manifest.permission.READ_MEDIA_IMAGES)
                        permissions.plus(Manifest.permission.READ_MEDIA_VIDEO)
                    }
                    requestPermissions(permissions, PERM_GALERY_GROUP_CODE)
                }
            }
        }
    }
    private fun dispatchTakeVideoIntent() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            val file = File(
                filesDir.toString() + File.separator + "${
                    DateFormat.getDateInstance().format(Date())
                }.mp4"
            )

            outputPath =
                FileProvider.getUriForFile(this, "com.example.supertaller2.fileprovider", file)
            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputPath)
            takeVideoIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
            } ?: run {
                if (takeVideoIntent.resolveActivity(packageManager) != null) {
                    // Aquí lanzas la intención
                } else {
                    Log.e("video", "No hay actividad para manejar la captura de video.")
                    val activitiesInfo = packageManager.queryIntentActivities(takeVideoIntent, PackageManager.MATCH_DEFAULT_ONLY)
                    for (info in activitiesInfo) {
                        Log.d("video", "Actividad encontrada: ${info.activityInfo.packageName} - ${info.activityInfo.name}")
                    }
                    Toast.makeText(this, "No hay actividad para manejar la captura de video.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERM_CAMERA_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mGetContentCamera.launch(uriCamera)
                } else {
                    Toast.makeText(this, "Me acaban de negar los permisos de Camara 😭", Toast.LENGTH_SHORT).show()
                }
            }
            PERM_GALERY_GROUP_CODE -> {
                for ((index, permission) in permissions.withIndex()) {
                    Log.d("PermissionResult", "$permission: ${grantResults[index]}")
                }

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mGetContentGallery.launch("image/*")
                } else {
                    Toast.makeText(this, "Me acaban de negar los permisos de Galeria 😭", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun loadimage(uriLocal: Uri) {
        try {
            binding.preview.removeAllViews()
            val imageStream: InputStream? = contentResolver.openInputStream(uriLocal)
            val selectedImage: Bitmap = BitmapFactory.decodeStream(imageStream)
            val imageView = ImageView(this)
            imageView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            imageView.setImageBitmap(selectedImage)
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            imageView.adjustViewBounds = true
            binding.preview.addView(imageView)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }
    private fun loadvideo(uriLocal: Uri) {
        try {
            binding.preview.removeAllViews()
            val videoView: VideoView = VideoView(this)
            videoView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
            )
            videoView.setVideoURI(uriLocal)
            videoView.foregroundGravity = View.TEXT_ALIGNMENT_CENTER
            videoView.setMediaController(MediaController(this))
            videoView.start()
            videoView.setZOrderOnTop(true)
            binding.preview.addView(videoView)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun intfile() {
        val file = File(filesDir, "picFromCamera")
        uriCamera = FileProvider.getUriForFile(this, "com.example.supertaller2.fileprovider", file)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_VIDEO_CAPTURE -> {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Video tomado correctamente", Toast.LENGTH_SHORT).show()
                    binding.preview.removeAllViews()
                    val videoView: VideoView = VideoView(this)
                    videoView.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                    )
                    videoView.setVideoURI(outputPath)
                    videoView.foregroundGravity = View.TEXT_ALIGNMENT_CENTER
                    videoView.setMediaController(MediaController(this))
                    videoView.start()
                    videoView.setZOrderOnTop(true)
                    binding.preview.addView(videoView)
                } else {
                    Toast.makeText(this, "No se pudo tomar el video", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}