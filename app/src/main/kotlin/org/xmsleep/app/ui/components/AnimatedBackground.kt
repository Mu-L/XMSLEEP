package org.xmsleep.app.ui.components

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.load
import org.xmsleep.app.ui.BackgroundSelection

@Composable
fun AnimatedBackground(
    backgroundSelection: BackgroundSelection,
    customBackgroundUri: String? = null,
    customBackgroundColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (backgroundSelection == BackgroundSelection.Custom && customBackgroundUri != null) {
        val uri = Uri.parse(customBackgroundUri)
        val path = uri.path ?: ""
        val isVideo = path.endsWith(".mp4", ignoreCase = true)

        val bgColorInt = customBackgroundColor?.let {
            if (it != Color.Unspecified) it.toArgb() else null
        }

        if (isVideo) {
            AndroidView(
                factory = { ctx ->
                    android.widget.VideoView(ctx).apply {
                        bgColorInt?.let { color -> setBackgroundColor(color) }
                        setVideoURI(uri)
                        setOnPreparedListener { mp ->
                            mp.setLooping(true)
                            start()
                        }
                        scaleX = 1f
                        scaleY = 1f
                    }
                },
                update = { view ->
                    if (!view.isPlaying) {
                        view.setVideoURI(uri)
                        view.start()
                    }
                },
                modifier = modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.2f }
            )
        } else {
            val imageUri = uri
            key(imageUri) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.ImageView(ctx).apply {
                            bgColorInt?.let { color -> setBackgroundColor(color) }
                            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        }.also { view ->
                            val file = imageUri.path?.let { java.io.File(it) }
                            if (file?.exists() == true) {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        val source = android.graphics.ImageDecoder.createSource(file)
                                        val drawable = android.graphics.ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                                            val maxDimension = 1080
                                            val max = maxOf(info.size.width, info.size.height)
                                            if (max > maxDimension) {
                                                val scale = maxDimension.toFloat() / max
                                                decoder.setTargetSize(
                                                    (info.size.width * scale).toInt(),
                                                    (info.size.height * scale).toInt()
                                                )
                                            }
                                        }
                                        view.setImageDrawable(drawable)
                                        if (drawable is android.graphics.drawable.AnimatedImageDrawable) {
                                            drawable.repeatCount = android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
                                            if (!drawable.isRunning) drawable.start()
                                        }
                                    } else {
                                        val opts = android.graphics.BitmapFactory.Options().apply {
                                            inJustDecodeBounds = true
                                        }
                                        android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
                                        val maxDimension = 1080
                                        val max = maxOf(opts.outWidth, opts.outHeight)
                                        opts.inSampleSize = if (max > maxDimension) max / maxDimension else 1
                                        opts.inJustDecodeBounds = false
                                        val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
                                        if (bitmap != null) view.setImageBitmap(bitmap)
                                    }
                                } catch (_: Exception) {
                                    view.load(imageUri)
                                }
                            } else {
                                view.load(imageUri)
                            }
                        }
                    },
                    modifier = modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.2f }
                )
            }
        }
    } else if (backgroundSelection != BackgroundSelection.None && backgroundSelection.resourceId != null) {
        if (backgroundSelection.isResourceValid(context)) {
            AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    }
                },
                update = { view ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = android.graphics.ImageDecoder.createSource(
                            context.resources,
                            backgroundSelection.resourceId
                        )
                        val drawable = android.graphics.ImageDecoder.decodeDrawable(source)
                        if (drawable is android.graphics.drawable.AnimatedImageDrawable) {
                            drawable.repeatCount = android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
                            drawable.start()
                        }
                        view.setImageDrawable(drawable)
                    } else {
                        view.setImageResource(backgroundSelection.resourceId)
                    }
                },
                modifier = modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.2f }
            )
        }
    }
}