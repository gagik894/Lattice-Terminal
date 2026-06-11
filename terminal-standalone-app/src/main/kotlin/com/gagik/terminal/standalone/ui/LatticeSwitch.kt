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

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent

/**
 * A custom modern toggle switch (slider) component.
 *
 * It replaces standard Swing JCheckBox with a premium pill-shaped slider switch,
 * supporting hover transitions and palette-aware outline drawing.
 */
internal class LatticeSwitch(
    private var selected: Boolean = false,
    private val onChange: (Boolean) -> Unit,
) : JComponent() {
    private var hovered = false

    init {
        preferredSize = Dimension(40, 20)
        minimumSize = Dimension(40, 20)
        maximumSize = Dimension(40, 20)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        addMouseListener(
            object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    if (isEnabled) {
                        selected = !selected
                        onChange(selected)
                        repaint()
                    }
                }

                override fun mouseEntered(e: MouseEvent) {
                    hovered = true
                    repaint()
                }

                override fun mouseExited(e: MouseEvent) {
                    hovered = false
                    repaint()
                }
            },
        )
    }

    fun isSelected(): Boolean = selected

    fun setSelected(value: Boolean) {
        if (selected != value) {
            selected = value
            repaint()
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val w = width
        val h = height

        // 1. Draw track background
        if (selected) {
            g2.color =
                if (hovered) {
                    // Slightly lighter active color on hover
                    blendColors(LatticeChrome.accent, Color.WHITE, 0.15f)
                } else {
                    LatticeChrome.accent
                }
        } else {
            g2.color =
                if (hovered) {
                    LatticeChrome.controlHover
                } else {
                    LatticeChrome.controlBackground
                }
        }
        g2.fillRoundRect(0, 0, w, h, h, h)

        // 2. Draw border
        g2.color = if (hovered) LatticeChrome.accent else LatticeChrome.border
        g2.drawRoundRect(0, 0, w - 1, h - 1, h, h)

        // 3. Draw sliding thumb
        g2.color = Color.WHITE
        val thumbSize = h - 6
        val thumbX = if (selected) w - thumbSize - 3 else 3
        val thumbY = 3
        g2.fillOval(thumbX, thumbY, thumbSize, thumbSize)

        g2.dispose()
    }

    private fun blendColors(
        c1: Color,
        c2: Color,
        ratio: Float,
    ): Color {
        val r = (c1.red * (1 - ratio) + c2.red * ratio).toInt().coerceIn(0, 255)
        val g = (c1.green * (1 - ratio) + c2.green * ratio).toInt().coerceIn(0, 255)
        val b = (c1.blue * (1 - ratio) + c2.blue * ratio).toInt().coerceIn(0, 255)
        return Color(r, g, b)
    }
}
