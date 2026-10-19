package com.camara.app.stickers

/** A sticker placed on the canvas. Position is normalised (0..1) so it survives
 *  preview-to-bitmap scaling when the capture is baked. */
data class PlacedSticker(
    val id: Long,
    val emoji: String,
    val x: Float,
    val y: Float,
    val scale: Float = 1f,
    val rotation: Float = 0f
)

/** Curated sticker packs. Emoji render everywhere with no asset downloads. */
object StickerPacks {

    val PACKS: List<StickerPack> = listOf(
        StickerPack(
            "Faces",
            listOf("😀", "😎", "🥳", "😍", "🤩", "😜", "🤪", "😇", "🥰", "😏", "🤔", "😴")
        ),
        StickerPack(
            "Love",
            listOf("❤️", "💖", "💕", "💘", "💝", "😘", "🌹", "💐", "💞", "🥰", "💌", "💋")
        ),
        StickerPack(
            "Party",
            listOf("🎉", "🎊", "✨", "🎈", "🎁", "🍾", "🥂", "🎂", "🕺", "💃", "🪩", "🎆")
        ),
        StickerPack(
            "Nature",
            listOf("🌸", "🌺", "🌻", "🍀", "🌈", "☀️", "🌙", "⭐", "🔥", "❄️", "🌊", "🍁")
        ),
        StickerPack(
            "Fun",
            listOf("😺", "🐶", "🐼", "🦄", "🐸", "🍕", "🍩", "🍓", "👑", "💎", "🚀", "⚡")
        ),
        StickerPack(
            "Words",
            listOf("OMG", "WOW", "LOL", "HI!", "YAY", "COOL", "WOW!", "❤", "★", "✌", "✓", "!")
        )
    )
}

data class StickerPack(val name: String, val stickers: List<String>)
