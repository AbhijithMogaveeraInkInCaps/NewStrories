package com.inkincaps.abhijith.com.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.flask.colorpicker.BuildConfig
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.inkincaps.abhijith.R
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
                    this.findViewById<PlayerView>(R.id.exoplayerView).visibility = View.INVISIBLE
                    findViewById<ImageView>(R.id.bg_image).visibility = View.VISIBLE
                    options = Options.IMAGE
                    findViewById<View>(R.id.activity_main).setBackgroundColor(Color.BLACK)
                    (findViewById<View>(R.id.bg_image) as ImageView).setImageURI(data.data)
                }
            }
        }

        if(resultCode == RESULT_OK){
            if(requestCode == SELECT_VIDEO){
                this.findViewById<ImageView>(R.id.bg_image).visibility = View.INVISIBLE
                this.findViewById<PlayerView>(R.id.exoplayerView).visibility = View.VISIBLE
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
const val SELECT_VIDEO = 1

sealed class Options {
    object IMAGE : Options()
    object VIDEO : Options()
    object COLOR : Options()
}