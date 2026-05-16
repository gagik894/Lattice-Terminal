package com.gagik.terminal.ui.swing.render.cache

import java.awt.Font
import java.awt.GraphicsEnvironment

internal object TerminalCacheTestFonts {
    const val FALLBACK_ONLY_TEXT: String = "\u03A9"
    const val MISSING_CODE_POINT: Int = 0x10FFFF

    fun primary(size: Float): Font {
        return load("fonts/Inconsolata.ttf", size)
    }

    fun fallback(size: Float): Font {
        return load("fonts/DroidSans.ttf", size)
    }

    fun registerFallbackFamily(size: Float = 14f): String {
        val font = fallback(size)
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font)
        return font.family
    }

    private fun load(path: String, size: Float): Font {
        val stream = requireNotNull(TerminalCacheTestFonts::class.java.classLoader.getResourceAsStream(path)) {
            "Missing test font resource: $path"
        }
        return stream.use { input ->
            Font.createFont(Font.TRUETYPE_FONT, input).deriveFont(Font.PLAIN, size)
        }
    }
}
