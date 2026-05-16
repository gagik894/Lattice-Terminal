package com.gagik.terminal.ui.swing.render.font

import java.awt.Font
import java.awt.GraphicsEnvironment
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Resolves installed system font family names away from the Swing event-dispatch thread.
 *
 * This intentionally avoids [GraphicsEnvironment.getAllFonts], which forces the
 * JVM font manager to instantiate and retain every installed physical font.
 * Rendering code turns these family names into concrete [Font] instances only
 * after primary and configured fallbacks fail for a glyph.
 */
internal interface TerminalSystemFontFamilies {
    /**
     * Returns loaded font family names, or starts background loading and returns empty.
     */
    fun familiesOrStartLoading(): List<String>
}

internal object TerminalSystemFallbackFonts : TerminalSystemFontFamilies {
    private val started = AtomicBoolean(false)
    private val loadedFamilies = AtomicReference<List<String>?>(null)
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "terminal-ui-system-font-loader").apply {
            isDaemon = true
        }
    }

    /**
     * Returns loaded font family names, or starts a background load and returns empty.
     */
    override fun familiesOrStartLoading(): List<String> {
        val loaded = loadedFamilies.get()
        if (loaded != null) return loaded

        if (started.compareAndSet(false, true)) {
            executor.execute {
                loadedFamilies.compareAndSet(null, loadSystemFontFamilies())
            }
        }
        return emptyList()
    }

    private fun loadSystemFontFamilies(): List<String> {
        return try {
            GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .availableFontFamilyNames
                .asList()
                .distinct()
        } catch (_: RuntimeException) {
            emptyList()
        }
    }
}
