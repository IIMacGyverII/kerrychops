package com.kerrysgame.game

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class SpriteSheetSpec(
    val assetPath: String,
    val frameWidth: Int,
    val frameHeight: Int
)

object SpriteSheetCatalog {
    val kerry = SpriteSheetSpec(assetPath = "sprites/kerry_sheet.png", frameWidth = 32, frameHeight = 32)
    val tree = SpriteSheetSpec(assetPath = "sprites/tree_sheet.png", frameWidth = 64, frameHeight = 64)
    val effects = SpriteSheetSpec(assetPath = "sprites/effects_sheet.png", frameWidth = 16, frameHeight = 16)
    val background = SpriteSheetSpec(assetPath = "sprites/background_sheet.png", frameWidth = 16, frameHeight = 16)

    val all: List<SpriteSheetSpec> = listOf(kerry, tree, effects, background)
}

object SpriteSheetSupport {
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
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
}
