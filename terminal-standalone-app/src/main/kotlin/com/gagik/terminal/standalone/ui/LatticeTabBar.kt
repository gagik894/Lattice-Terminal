/*
 * Copyright 2026 Gagik Sargsyan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gagik.terminal.standalone.ui

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.geom.RoundRectangle2D
import javax.swing.JPanel

/**
 * A tab entry displayed in [LatticeTabBar].
 *
 * @property id Stable identifier for this tab.
 * @property title Display label shown in the tab header.
 */
internal data class TabEntry(
    val id: String,
    var title: String,
)

/**
 * Custom-painted horizontal tab bar for the standalone terminal window.
 *
 * Renders a row of closable tab pills followed by an "add tab" button, all
 * custom-painted to match the window title bar background. Selection, hover,
 * and close states are tracked internally; callers receive change events via
 * the supplied callbacks.
 *
 * This panel is intended to be placed as [javax.swing.JRootPane]'s
 * `titleBarLeadingComponent` via FlatLaf's custom window decoration API.
 *
 * @param onTabSelected Invoked with the [TabEntry.id] whenever the user clicks
 *   a tab that is not already selected.
 * @param onTabClose Invoked with the [TabEntry.id] when the user clicks a
 *   tab's close button.
 * @param onNewTab Invoked when the user clicks the "+" button.
 */
internal class LatticeTabBar(
    private val onTabSelected: (id: String) -> Unit,
    private val onTabClose: (id: String) -> Unit,
    private val onNewTab: () -> Unit,
) : JPanel() {
    private val entries = mutableListOf<TabEntry>()
    private var selectedId: String? = null

    /** Index of the tab whose close button is currently hovered, or -1. */
    private var closeHoverIndex = -1

    /** Index of the tab body currently hovered (excluding close button), or -1. */
    private var tabHoverIndex = -1

    /** Whether the "+" new-tab button is currently hovered. */
    private var newTabHovered = false

    init {
        isOpaque = false
        cursor = Cursor.getDefaultCursor()
        installMouseListeners()
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Appends a new tab entry and repaints. */
    fun addTab(entry: TabEntry) {
        entries += entry
        selectedId = entry.id
        revalidate()
        repaint()
    }

    /** Removes the tab with the given [id] and repaints. */
    fun removeTab(id: String) {
        entries.removeIf { it.id == id }
        if (selectedId == id) {
            selectedId = entries.lastOrNull()?.id
        }
        revalidate()
        repaint()
    }

    /** Updates the displayed title for the tab identified by [id]. */
    fun updateTitle(
        id: String,
        title: String,
    ) {
        entries.find { it.id == id }?.title = title
        repaint()
    }

    /** Programmatically selects the tab with the given [id] and repaints. */
    fun selectTab(id: String) {
        selectedId = id
        repaint()
    }

    /** Returns the currently-selected tab id, or null if there are no tabs. */
    fun selectedId(): String? = selectedId

    /** Returns the title of the currently-selected tab, or null if there are no tabs. */
    fun selectedTitle(): String? = entries.find { it.id == selectedId }?.title

    // -------------------------------------------------------------------------
    // Layout / preferred size
    // -------------------------------------------------------------------------

    override fun getPreferredSize(): Dimension {
        val fm = getFontMetrics(font)
        val totalTabWidth = entries.sumOf { tabWidth(it, fm) } + NEW_TAB_BUTTON_WIDTH + TRAILING_SPACE
        return Dimension(totalTabWidth, TAB_BAR_HEIGHT)
    }

    override fun getMinimumSize(): Dimension = Dimension(NEW_TAB_BUTTON_WIDTH + TRAILING_SPACE, TAB_BAR_HEIGHT)

    // -------------------------------------------------------------------------
    // Painting
    // -------------------------------------------------------------------------

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
            paintTabs(g2)
            paintNewTabButton(g2)
        } finally {
            g2.dispose()
        }
    }

    private fun paintTabs(g2: Graphics2D) {
        val fm = g2.fontMetrics
        var x = TAB_START_X
        entries.forEachIndexed { index, entry ->
            val w = tabWidth(entry, fm)
            val selected = entry.id == selectedId
            paintTab(g2, entry, index, x, w, selected)
            x += w
        }
    }

    private fun paintTab(
        g2: Graphics2D,
        entry: TabEntry,
        index: Int,
        x: Int,
        w: Int,
        selected: Boolean,
    ) {
        val y = TAB_TOP_PADDING
        val h = height - TAB_TOP_PADDING
        val fm = g2.fontMetrics

        // Background fill (rounded top corners, flat bottom)
        val bg =
            when {
                selected -> LatticeChrome.TAB_SELECTED_BG
                index == tabHoverIndex -> LatticeChrome.TAB_HOVER_BG
                else -> Color(0, 0, 0, 0)
            }
        if (bg.alpha > 0) {
            g2.color = bg
            val path =
                java.awt.geom.Path2D
                    .Float()
            path.moveTo(x.toFloat(), (y + h).toFloat()) // bottom left
            path.lineTo(x.toFloat(), y + CORNER_RADIUS) // top left straight
            path.quadTo(x.toFloat(), y.toFloat(), x + CORNER_RADIUS, y.toFloat()) // top left curve
            path.lineTo(x + w - CORNER_RADIUS, y.toFloat()) // top straight
            path.quadTo((x + w).toFloat(), y.toFloat(), (x + w).toFloat(), y + CORNER_RADIUS) // top right curve
            path.lineTo((x + w).toFloat(), (y + h).toFloat()) // bottom right
            path.closePath()
            g2.fill(path)
        }

        // Selection accent line at top
        if (selected) {
            g2.color = LatticeChrome.ACCENT
            g2.stroke = BasicStroke(SELECTION_BAR_HEIGHT.toFloat())
            val lineY = y + (SELECTION_BAR_HEIGHT / 2.0f)
            g2.drawLine(x + CORNER_RADIUS.toInt(), lineY.toInt(), x + w - CORNER_RADIUS.toInt(), lineY.toInt())
        }

        // Title label
        val titleFg = if (selected) LatticeChrome.TEXT_PRIMARY else LatticeChrome.TEXT_SECONDARY
        g2.color = titleFg
        val labelX = x + TAB_LABEL_PADDING_LEFT
        val labelMaxWidth = w - TAB_LABEL_PADDING_LEFT - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_MARGIN_RIGHT - 4
        val clipped = clipText(entry.title, labelMaxWidth, fm)
        val textY = y + (h + fm.ascent - fm.descent) / 2
        g2.drawString(clipped, labelX, textY)

        // Close button
        val closeBtnX = x + w - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_MARGIN_RIGHT
        val closeBtnY = y + (h - CLOSE_BUTTON_SIZE) / 2
        paintCloseButton(g2, index, closeBtnX, closeBtnY, selected)
    }

    private fun paintCloseButton(
        g2: Graphics2D,
        index: Int,
        x: Int,
        y: Int,
        tabSelected: Boolean,
    ) {
        val hovered = index == closeHoverIndex
        val visible = tabSelected || index == tabHoverIndex || hovered

        if (visible) {
            if (hovered) {
                g2.color = LatticeChrome.CONTROL_HOVER
                g2.fillRoundRect(x, y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, 4, 4)
            }
            g2.color = if (hovered) LatticeChrome.TEXT_PRIMARY else LatticeChrome.TEXT_SECONDARY
            g2.stroke = BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            val pad = CLOSE_BUTTON_SIZE / 4
            g2.drawLine(x + pad, y + pad, x + CLOSE_BUTTON_SIZE - pad, y + CLOSE_BUTTON_SIZE - pad)
            g2.drawLine(x + CLOSE_BUTTON_SIZE - pad, y + pad, x + pad, y + CLOSE_BUTTON_SIZE - pad)
        }
    }

    private fun paintNewTabButton(g2: Graphics2D) {
        val fm = g2.fontMetrics
        val x = tabsEndX(fm)
        val y = TAB_TOP_PADDING
        val w = NEW_TAB_BUTTON_WIDTH
        val h = height - TAB_TOP_PADDING

        if (newTabHovered) {
            g2.color = LatticeChrome.TAB_HOVER_BG
            g2.fill(RoundRectangle2D.Float(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), CORNER_RADIUS, CORNER_RADIUS))
        }

        g2.color = if (newTabHovered) LatticeChrome.TEXT_PRIMARY else LatticeChrome.TEXT_SECONDARY
        val plusFont = font.deriveFont(14f)
        val pfm = g2.getFontMetrics(plusFont)
        g2.font = plusFont
        val tx = x + (w - pfm.stringWidth("+")) / 2
        val ty = y + (h + pfm.ascent - pfm.descent) / 2
        g2.drawString("+", tx, ty)
        g2.font = font
    }

    // -------------------------------------------------------------------------
    // Hit-testing helpers
    // -------------------------------------------------------------------------

    private fun tabsEndX(fm: FontMetrics): Int = TAB_START_X + entries.sumOf { tabWidth(it, fm) }

    private fun tabWidth(
        entry: TabEntry,
        fm: FontMetrics,
    ): Int {
        val textWidth = fm.stringWidth(entry.title).coerceIn(MIN_LABEL_TEXT_WIDTH, MAX_LABEL_TEXT_WIDTH)
        return TAB_LABEL_PADDING_LEFT + textWidth + CLOSE_BUTTON_SIZE + CLOSE_BUTTON_MARGIN_RIGHT + TAB_PADDING_RIGHT
    }

    /**
     * Returns the tab index at pixel [px] by iterating layout geometry, or -1.
     */
    private fun tabIndexAt(
        px: Int,
        fm: FontMetrics,
    ): Int {
        var x = TAB_START_X
        entries.forEachIndexed { index, entry ->
            val w = tabWidth(entry, fm)
            if (px in x until x + w) return index
            x += w
        }
        return -1
    }

    /**
     * Returns true if [px] falls on the close button of tab at [index].
     */
    private fun isOnCloseButton(
        index: Int,
        px: Int,
        py: Int,
        fm: FontMetrics,
    ): Boolean {
        var x = TAB_START_X
        for (i in 0 until index) x += tabWidth(entries[i], fm)
        val w = tabWidth(entries[index], fm)
        val closeBtnX = x + w - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_MARGIN_RIGHT
        val closeBtnY = (height - CLOSE_BUTTON_SIZE) / 2
        return px in closeBtnX until closeBtnX + CLOSE_BUTTON_SIZE &&
            py in closeBtnY until closeBtnY + CLOSE_BUTTON_SIZE
    }

    private fun isOnNewTabButton(
        px: Int,
        fm: FontMetrics,
    ): Boolean {
        val x = tabsEndX(fm)
        return px in x until x + NEW_TAB_BUTTON_WIDTH
    }

    // -------------------------------------------------------------------------
    // Mouse interaction
    // -------------------------------------------------------------------------

    private fun installMouseListeners() {
        addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val fm = getFontMetrics(font)
                    val index = tabIndexAt(e.x, fm)
                    when {
                        index >= 0 && isOnCloseButton(index, e.x, e.y, fm) -> {
                            onTabClose(entries[index].id)
                        }
                        index >= 0 -> {
                            val id = entries[index].id
                            if (id != selectedId) {
                                selectedId = id
                                repaint()
                                onTabSelected(id)
                            }
                        }
                        isOnNewTabButton(e.x, fm) -> onNewTab()
                    }
                }

                override fun mouseExited(e: MouseEvent) {
                    closeHoverIndex = -1
                    tabHoverIndex = -1
                    newTabHovered = false
                    repaint()
                }
            },
        )
        addMouseMotionListener(
            object : MouseMotionAdapter() {
                override fun mouseMoved(e: MouseEvent) {
                    val fm = getFontMetrics(font)
                    val index = tabIndexAt(e.x, fm)
                    val prevClose = closeHoverIndex
                    val prevTab = tabHoverIndex
                    val prevNew = newTabHovered

                    closeHoverIndex =
                        if (index >= 0 && isOnCloseButton(index, e.x, e.y, fm)) index else -1
                    tabHoverIndex = if (closeHoverIndex < 0) index else -1
                    newTabHovered = index < 0 && isOnNewTabButton(e.x, fm)

                    if (prevClose != closeHoverIndex || prevTab != tabHoverIndex || prevNew != newTabHovered) {
                        repaint()
                    }
                }
            },
        )
    }

    // -------------------------------------------------------------------------
    // Text helpers
    // -------------------------------------------------------------------------

    private fun clipText(
        text: String,
        maxWidth: Int,
        fm: FontMetrics,
    ): String {
        if (fm.stringWidth(text) <= maxWidth) return text
        val ellipsis = "\u2026"
        val ellipsisWidth = fm.stringWidth(ellipsis)
        var clipped = text
        while (clipped.isNotEmpty() && fm.stringWidth(clipped) + ellipsisWidth > maxWidth) {
            clipped = clipped.dropLast(1)
        }
        return clipped + ellipsis
    }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private companion object {
        private const val TAB_BAR_HEIGHT = 40
        private const val TAB_TOP_PADDING = 4
        private const val TAB_START_X = 4
        private const val TAB_LABEL_PADDING_LEFT = 14
        private const val TAB_PADDING_RIGHT = 8
        private const val CLOSE_BUTTON_SIZE = 16
        private const val CLOSE_BUTTON_MARGIN_RIGHT = 8
        private const val NEW_TAB_BUTTON_WIDTH = 34
        private const val TRAILING_SPACE = 8
        private const val CORNER_RADIUS = 6f
        private const val SELECTION_BAR_HEIGHT = 2
        private const val MIN_LABEL_TEXT_WIDTH = 50
        private const val MAX_LABEL_TEXT_WIDTH = 168
    }
}
