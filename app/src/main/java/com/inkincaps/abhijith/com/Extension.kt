package com.inkincaps.abhijith.com

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

fun View.getScreenShot(fillWithBlack:Boolean=false):Bitmap{
    return Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888).apply {
        if(fillWithBlack)
            eraseColor(Color.BLACK)
        draw(Canvas(this))
    }
}


fun Bitmap.toNineIstoSixteenAspectFile(context: Context,callback:(File)->Unit) {
    val file = File(getNewFileName(context))
    val fileOutputStream = FileOutputStream(file)
    compress(Bitmap.CompressFormat.PNG,90, fileOutputStream)
    fileOutputStream.close()
    GlobalScope.launch {
        callback(Compressor.compress(context, file) {
            resolution(540, 960)
            quality(80)
            destination(File(Environment.getExternalStorageDirectory()
                .toString() + "/Android/data/"
                    + context.packageName
                    + "/Files/abhi.png"))
            format(Bitmap.CompressFormat.JPEG)
            size(2_097_152) // 2 MB
        })
    }
}

@SuppressLint("SimpleDateFormat")
private fun getNewFileName(context: Context):String{
    return Environment.getExternalStorageDirectory().toString()+"/AndroidTest/${context.applicationContext.packageName}/Files".apply {
           if(!File(this).exists()){
               if(!File(this).mkdir()){
                   throw Exception("Unable to create folder")
               }
           }
    }+"/${SimpleDateFormat("ddMMyyyy_HHmm").format(Date())}/.png"
}

@SuppressLint("SimpleDateFormat")
private fun getNewPngFile(context: Context):File{
    return File(Environment.getExternalStorageDirectory().toString()+"/AndroidTest/${context.applicationContext.packageName}/Files".apply {
           if(!File(this).exists()){
               if(!File(this).mkdir()){
                   throw Exception("Unable to create folder")
               }
           }
    }+"/${SimpleDateFormat("ddMMyyyy_HHmm").format(Date())}/.png")
}