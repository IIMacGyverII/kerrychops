package com.kerrysgame.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class SpriteSheetSpec(
    val assetPath: String,
    val frameWidth: Int,
    val frameHeight: Int
)

object SpriteSheetCatalog {
    val tree = SpriteSheetSpec(assetPath = "sprites/tree_sheet2.png", frameWidth = 473, frameHeight = 542)
    val treeStump = SpriteSheetSpec(assetPath = "sprites/tree_stump_sheet.png", frameWidth = 32, frameHeight = 32)
    val effects = SpriteSheetSpec(assetPath = "sprites/effects_sheet.png", frameWidth = 205, frameHeight = 185)
    val background = SpriteSheetSpec(assetPath = "sprites/background_sheet.png", frameWidth = 1280, frameHeight = 720)

    val all: List<SpriteSheetSpec> = listOf(tree, treeStump, effects, background)
}

object SpriteSheetSupport {
    fun listAssets(context: Context, folder: String): List<String> {
        return context.assets.list(folder)?.sorted() ?: emptyList()
    }

    fun loadFrame(
        context: Context,
        assetPath: String,
        targetHeightPx: Int,
        removeWhite: Boolean
    ): ImageBitmap? {
        return runCatching {
            context.assets.open(assetPath).use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                val srcHeight = options.outHeight.coerceAtLeast(1)
                val sample = (srcHeight / targetHeightPx).coerceAtLeast(1)

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
                context.assets.open(assetPath).use { decodeStream ->
                    val decoded = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
                    val cleaned = if (decoded != null && removeWhite) removeNearWhiteBackground(decoded) else decoded
                    cleaned?.asImageBitmap()
                }
            }
        }.getOrNull()
    }
    fun hasAsset(context: Context, assetPath: String): Boolean {
        return runCatching {
            context.assets.open(assetPath).close()
            true
        }.getOrDefault(false)
    }

    fun missingAssets(context: Context): List<String> {
        return SpriteSheetCatalog.all
            .map { it.assetPath }
            .filterNot { hasAsset(context, it) }
    }

    fun loadImageBitmap(context: Context, spec: SpriteSheetSpec): ImageBitmap? {
        return runCatching {
            context.assets.open(spec.assetPath).use { stream ->
                val decoded = BitmapFactory.decodeStream(stream)
                val cleaned = decoded
                cleaned?.asImageBitmap()
            }
        }.getOrNull()
    }

    private fun removeNearBlackBackground(source: Bitmap): Bitmap {
        val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]
            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)
            if (red <= 25 && green <= 25 && blue <= 25) {
                pixels[i] = 0
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun removeBackgroundByCornerColor(source: Bitmap): Bitmap {
        val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        fun colorAt(x: Int, y: Int): Int = pixels[(y * width + x).coerceIn(0, pixels.lastIndex)]
        val c1 = colorAt(0, 0)
        val c2 = colorAt(width - 1, 0)
        val c3 = colorAt(0, height - 1)
        val c4 = colorAt(width - 1, height - 1)
        val avgR = (Color.red(c1) + Color.red(c2) + Color.red(c3) + Color.red(c4)) / 4
        val avgG = (Color.green(c1) + Color.green(c2) + Color.green(c3) + Color.green(c4)) / 4
        val avgB = (Color.blue(c1) + Color.blue(c2) + Color.blue(c3) + Color.blue(c4)) / 4

        val tolerance = 140
        for (i in pixels.indices) {
            val color = pixels[i]
            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)
            val dr = kotlin.math.abs(red - avgR)
            val dg = kotlin.math.abs(green - avgG)
            val db = kotlin.math.abs(blue - avgB)
            val brightness = (red + green + blue) / 3
            if ((dr <= tolerance && dg <= tolerance && db <= tolerance) || brightness <= 80) {
                pixels[i] = 0
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun removeNearWhiteBackground(source: Bitmap): Bitmap {
        val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        fun isNearWhite(pixel: Int): Boolean {
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            return red >= 220 && green >= 220 && blue >= 220
        }

        // Remove only white pixels connected to the image border (background).
        val queue = IntArray(width * height)
        var head = 0
        var tail = 0

        fun enqueueIfWhite(index: Int) {
            if (index < 0 || index >= pixels.size) return
            if (pixels[index] == 0) return
            if (!isNearWhite(pixels[index])) return
            pixels[index] = 0
            queue[tail++] = index
        }

        // Seed queue from border pixels.
        for (x in 0 until width) {
            enqueueIfWhite(x)
            enqueueIfWhite((height - 1) * width + x)
        }
        for (y in 0 until height) {
            enqueueIfWhite(y * width)
            enqueueIfWhite(y * width + (width - 1))
        }

        while (head < tail) {
            val idx = queue[head++]
            val x = idx % width
            val y = idx / width

            if (x > 0) enqueueIfWhite(idx - 1)
            if (x < width - 1) enqueueIfWhite(idx + 1)
            if (y > 0) enqueueIfWhite(idx - width)
            if (y < height - 1) enqueueIfWhite(idx + width)
        }

        // Edge spill suppression: reduce bright fringe near transparent pixels.
        val processed = pixels.copyOf()
        for (i in processed.indices) {
            val pixel = processed[i]
            if (pixel == 0) continue

            val alpha = Color.alpha(pixel)
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            val bright = red >= 190 && green >= 190 && blue >= 190
            if (!bright) continue

            val x = i % width
            val y = i / width
            var touchingTransparent = false

            if (x > 0 && processed[i - 1] == 0) touchingTransparent = true
            if (x < width - 1 && processed[i + 1] == 0) touchingTransparent = true
            if (y > 0 && processed[i - width] == 0) touchingTransparent = true
            if (y < height - 1 && processed[i + width] == 0) touchingTransparent = true

            if (touchingTransparent) {
                val newAlpha = (alpha * 0.15f).toInt().coerceIn(0, 255)
                val newRed = (red * 0.75f).toInt().coerceIn(0, 255)
                val newGreen = (green * 0.75f).toInt().coerceIn(0, 255)
                val newBlue = (blue * 0.75f).toInt().coerceIn(0, 255)
                pixels[i] = Color.argb(newAlpha, newRed, newGreen, newBlue)
            }
        }

        // Final matte trim: kill stubborn bright halo directly touching transparency.
        val trimmed = pixels.copyOf()
        for (i in trimmed.indices) {
            val pixel = trimmed[i]
            if (pixel == 0) continue

            val alpha = Color.alpha(pixel)
            if (alpha == 0) continue

            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            val x = i % width
            val y = i / width

            var touchingTransparent = false
            if (x > 0 && trimmed[i - 1] == 0) touchingTransparent = true
            if (x < width - 1 && trimmed[i + 1] == 0) touchingTransparent = true
            if (y > 0 && trimmed[i - width] == 0) touchingTransparent = true
            if (y < height - 1 && trimmed[i + width] == 0) touchingTransparent = true

            if (touchingTransparent) {
                val maxChannel = maxOf(red, green, blue)
                val minChannel = minOf(red, green, blue)
                val lowSaturation = (maxChannel - minChannel) <= 28
                if (maxChannel >= 170 && lowSaturation) {
                    // Remove light gray/white fringe outright.
                    pixels[i] = 0
                } else if (maxChannel >= 140 && lowSaturation) {
                    // Strongly fade borderline fringe.
                    val fadedAlpha = (alpha * 0.08f).toInt().coerceIn(0, 255)
                    pixels[i] = Color.argb(fadedAlpha, red, green, blue)
                }
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
