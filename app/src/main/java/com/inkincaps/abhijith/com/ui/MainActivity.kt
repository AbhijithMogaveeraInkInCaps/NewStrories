package com.inkincaps.abhijith.com.ui

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.fragment.app.FragmentActivity
import com.flask.colorpicker.BuildConfig
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.inkincaps.abhijith.R
import com.inkincaps.abhijith.com.FileUtil
import com.inkincaps.abhijith.com.getScreenShot
import com.inkincaps.abhijith.com.permission.ShortenMultiplePermissionListener
import com.inkincaps.abhijith.com.toNineIstoSixteenAspectFile
import com.inkincaps.abhijith.com.ui.TextEditorDialogFragment.OnTextLayerCallback
import com.inkincaps.abhijith.com.ui.adapter.FontsAdapter
import com.inkincaps.abhijith.com.utils.FontProvider
import com.inkincaps.abhijith.com.viewmodel.Font
import com.inkincaps.abhijith.com.viewmodel.Layer
import com.inkincaps.abhijith.com.viewmodel.TextLayer
import com.inkincaps.abhijith.com.widget.MotionView
import com.inkincaps.abhijith.com.widget.MotionView.MotionViewCallback
import com.inkincaps.abhijith.com.widget.entity.ImageEntity
import com.inkincaps.abhijith.com.widget.entity.MotionEntity
import com.inkincaps.abhijith.com.widget.entity.TextEntity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.*
import id.zelory.compressor.saveBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random


class MainActivity : AppCompatActivity(), OnTextLayerCallback {
    private var motionView: MotionView? = null
    private var textEntityEditPanel: View? = null
    private var options: Options = Options.COLOR
    private val motionViewCallback: MotionViewCallback = object : MotionViewCallback {
        override fun onEntitySelected(entity: MotionEntity?) {
            if (entity is TextEntity) {
                textEntityEditPanel!!.visibility = View.VISIBLE
            } else {
                textEntityEditPanel!!.visibility = View.GONE
            }
        }

        override fun onEntityDoubleTap(entity: MotionEntity) {
            startTextEntityEditing()
        }
    }

    private var fontProvider: FontProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestRuntimePermission()
        fontProvider = FontProvider(resources)
        motionView = findViewById<View>(R.id.main_motion_view) as MotionView
        textEntityEditPanel = findViewById(R.id.main_motion_text_entity_edit_panel)
        motionView!!.setMotionViewCallback(motionViewCallback)
        initTextEntitiesListeners()

    }

    private fun addSticker(stickerResId: Int) {
        motionView!!.post {
            val layer = Layer()
            val pica = BitmapFactory.decodeResource(resources, stickerResId)
            val entity = ImageEntity(layer, pica, motionView!!.width, motionView!!.height)
            motionView!!.addEntityAndPosition(entity)
        }
    }

    private fun initTextEntitiesListeners() {
        findViewById<View>(R.id.text_entity_font_size_increase).setOnClickListener { increaseTextEntitySize() }
        findViewById<View>(R.id.text_entity_font_size_decrease).setOnClickListener { decreaseTextEntitySize() }
        findViewById<View>(R.id.text_entity_color_change).setOnClickListener { changeTextEntityColor() }
        findViewById<View>(R.id.text_entity_font_change).setOnClickListener { changeTextEntityFont() }
        findViewById<View>(R.id.text_entity_edit).setOnClickListener { startTextEntityEditing() }
    }

    private fun increaseTextEntitySize() {
        val textEntity = currentTextEntity()
        if (textEntity != null) {
            textEntity.layer.font.increaseSize(TextLayer.Limits.FONT_SIZE_STEP)
            textEntity.updateEntity()
            motionView!!.invalidate()
        }
    }

    private fun decreaseTextEntitySize() {
        val textEntity = currentTextEntity()
        if (textEntity != null) {
            textEntity.layer.font.decreaseSize(TextLayer.Limits.FONT_SIZE_STEP)
            textEntity.updateEntity()
            motionView!!.invalidate()
        }
    }

    private fun changeTextEntityColor() {
        val textEntity = currentTextEntity() ?: return
        val initialColor = textEntity.layer.font.color
        ColorPickerDialogBuilder
                .with(this@MainActivity)
                .setTitle(R.string.select_color)
                .initialColor(initialColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                .density(8) // magic number
                .setPositiveButton(R.string.ok) { dialog, selectedColor, allColors ->
                    val textEntity = currentTextEntity()
                    if (textEntity != null) {
                        textEntity.layer.font.color = selectedColor
                        textEntity.updateEntity()
                        motionView!!.invalidate()
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, which -> }
                .build()
                .show()
    }

    private fun changeTextEntityFont() {
        val fonts = fontProvider!!.fontNames
        val fontsAdapter = FontsAdapter(this, fonts, fontProvider)
        AlertDialog.Builder(this)
                .setTitle(R.string.select_font)
                .setAdapter(fontsAdapter) { dialogInterface, which ->
                    val textEntity = currentTextEntity()
                    if (textEntity != null) {
                        textEntity.layer.font.typeface = fonts[which]
                        textEntity.updateEntity()
                        motionView!!.invalidate()
                    }
                }
                .show()
    }

    private fun startTextEntityEditing() {
        val textEntity = currentTextEntity()
        if (textEntity != null) {
            val fragment = TextEditorDialogFragment.getInstance(textEntity.layer.text)
            fragment.show(fragmentManager, TextEditorDialogFragment::class.java.name)
        }
    }

    private fun currentTextEntity(): TextEntity? {
        return if (motionView != null && motionView!!.selectedEntity is TextEntity) {
            motionView!!.selectedEntity as TextEntity
        } else {
            null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.main_add_sticker) {
            val intent = Intent(this, StickerSelectActivity::class.java)
            startActivityForResult(intent, SELECT_STICKER_REQUEST_CODE)
            return true
        } else if (item.itemId == R.id.main_add_text) {
            addTextSticker()
        } else if (item.itemId == R.id.image) {
            startImagePickingActivity()
        } else if (item.itemId == R.id.video) {

            val intent = Intent()
            intent.type = "video/*"
            intent.action = Intent.ACTION_GET_CONTENT

            startActivityForResult(
                Intent.createChooser(intent, "Select Video"),
                SELECT_VIDEO
            )

        } else if (item.itemId == R.id.color) {

            options = Options.COLOR

            findViewById<View>(R.id.activity_main).setBackgroundColor(
                Color.rgb(
                    Random.nextInt(
                        0,
                        255
                    ), Random.nextInt(0, 255), Random.nextInt(0, 255)
                )
            )

            findViewById<ImageView>(R.id.bg_image).visibility = View.GONE

            this.findViewById<PlayerView>(R.id.exoplayerView).apply {
                this.player?.stop()
                visibility = View.GONE
            }

        }else if(item.itemId == R.id.save){
            motionView?.thumbnailImage?.let { storeImage(it) }
        }
        return super.onOptionsItemSelected(item)
    }

    protected fun addTextSticker() {
        val textLayer = createTextLayer()
        val textEntity = TextEntity(
            textLayer, motionView!!.width,
            motionView!!.height, fontProvider!!
        )
        motionView!!.addEntityAndPosition(textEntity)

        // move text sticker up so that its not hidden under keyboard
        val center = textEntity.absoluteCenter()
        center.y = center.y * 0.5f
        textEntity.moveCenterTo(center)

        // redraw
        motionView!!.invalidate()
        startTextEntityEditing()
    }

    private fun createTextLayer(): TextLayer {
        val textLayer = TextLayer()
        val font = Font()
        font.color = TextLayer.Limits.INITIAL_FONT_COLOR
        font.size = TextLayer.Limits.INITIAL_FONT_SIZE
        font.typeface = fontProvider!!.defaultFontName
        textLayer.font = font
        if (BuildConfig.DEBUG) {
            textLayer.text = "Hello, world :))"
        }
        return textLayer
    }

    lateinit var imageUri: Uri
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_STICKER_REQUEST_CODE) {
                if (data != null) {
                    val stickerId = data.getIntExtra(StickerSelectActivity.EXTRA_STICKER_ID, 0)
                    if (stickerId != 0) {
                        addSticker(stickerId)
                    }
                }
            }
        }

        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                if (data != null) {
                    this.findViewById<PlayerView>(R.id.exoplayerView).apply {
                        player?.stop()
                        this.visibility = View.GONE
                    }
                    findViewById<ImageView>(R.id.bg_image).visibility = View.VISIBLE
                    options = Options.IMAGE
                    findViewById<View>(R.id.activity_main).setBackgroundColor(Color.BLACK)
                    (findViewById<View>(R.id.bg_image) as ImageView).setImageURI(data.data)
                    imageUri = data.data!!
                }
            }
        }

        if(resultCode == RESULT_OK){
            if(requestCode == SELECT_VIDEO){
                options = Options.VIDEO
                this.findViewById<ImageView>(R.id.bg_image).visibility = View.GONE
                findViewById<View>(R.id.activity_main).setBackgroundColor(Color.BLACK)
                this.findViewById<PlayerView>(R.id.exoplayerView).apply {
                    visibility = View.VISIBLE
                }
                val x = SimpleExoPlayer.Builder(this).build().apply {
                    val uri =
                        setMediaItem(MediaItem.fromUri(data!!.data!!))
                    prepare()
                    playWhenReady = true
                }
                this.findViewById<PlayerView>(R.id.exoplayerView).player = x
            }
        }
    }

    override fun textChanged(text: String) {
        val textEntity = currentTextEntity()
        if (textEntity != null) {
            val textLayer = textEntity.layer
            if (text != textLayer.text) {
                textLayer.text = text
                textEntity.updateEntity()
                motionView!!.invalidate()
            }
        }
    }

    companion object {
        const val SELECT_STICKER_REQUEST_CODE = 123
    }


    private fun requestRuntimePermission() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object : ShortenMultiplePermissionListener() {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (!report.areAllPermissionsGranted()) {
                        finish()
                    }
                }
            })
            .check()
    }

    private fun storeImage(image: Bitmap) {
        image.toNineIstoSixteenAspectFile(this){
            Toast.makeText(this, it.absolutePath, Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.bg_image).getScreenShot(fillWithBlack = true).toNineIstoSixteenAspectFile(this){

        }

        /*   val pictureFile: File = getOutputMediaFile()!!
           try {
               val fos = FileOutputStream(pictureFile)
               findViewById<ImageView>(R.id.bg_image).getScreenShot(fillWithBlack = true).compress(Bitmap.CompressFormat.PNG, 90, fos)
               fos.close()
               Log.e("ABHIIII","compressedImageFile.absolutePath")

               GlobalScope.launch(Dispatchers.IO) {
                   val compressedImageFile = Compressor.compress(this@MainActivity, pictureFile) {
                       resolution(540, 960)
                       quality(80)
                       destination(File(Environment.getExternalStorageDirectory()
                           .toString() + "/Android/data/"
                               + applicationContext.packageName
                               + "/Files/abhi.jpeg"))
                       format(Bitmap.CompressFormat.JPEG)
                       size(2_097_152) // 2 MB
                   }
                   Toast.makeText(
                       this@MainActivity,
                       compressedImageFile.absolutePath,
                       Toast.LENGTH_SHORT
                   ).show()
                   Log.e("ABHIIII",compressedImageFile.absolutePath)
               }

   //            findViewById<ImageView>(R.id.bg_image).visibility = View.VISIBLE
   //            findViewById<ImageView>(R.id.bg_image).setImageBitmap(image)
           } catch (e: FileNotFoundException) {
   //            Log.d(TAG, "File not found: " + e.getMessage())
           } catch (e: IOException) {
   //            Log.d(TAG, "Error accessing file: " + e.getMessage())
           }*/
    }

    private fun getOutputMediaFile(): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        val mediaStorageDir: File = File(
            Environment.getExternalStorageDirectory()
                .toString() + "/Android/data/"
                    + applicationContext.packageName
                    + "/Files"
        )

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        // Create a media file name
        val timeStamp: String = SimpleDateFormat("ddMMyyyy_HHmm").format(Date())
        val mediaFile: File
        val mImageName = "MI_$timeStamp.png"
        mediaFile = File(mediaStorageDir.path + File.separator + mImageName)
        return mediaFile
    }
}

object IntentCollection {

    fun getImagePickingIntent(): Intent {
        val i = Intent()
        i.type = "image/*"
        i.action = Intent.ACTION_GET_CONTENT
        return Intent.createChooser(i, "Select Picture")
    }

}

fun FragmentActivity.startImagePickingActivity(): Int {
    startActivityForResult(
        Intent.createChooser(
            IntentCollection.getImagePickingIntent(),
            "Select Picture"
        ), SELECT_PICTURE
    )
    return SELECT_PICTURE
}

const val SELECT_PICTURE = 1
const val SELECT_VIDEO = 2

sealed class Options {
    object IMAGE : Options()
    object VIDEO : Options()
    object COLOR : Options()
}