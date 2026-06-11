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
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * A styled card panel that groups related settings in a clean visual container.
 *
 * Each settings row displays a title and a description on the left side,
 * with the aligned interactive control on the right side.
 */
internal class LatticeSettingsGroupCard(
    title: String,
) : JPanel() {
    private val contentPanel =
        JPanel().apply {
            layout = GridBagLayout()
            isOpaque = false
        }

    init {
        layout = BorderLayout()
        isOpaque = false
        background = LatticeChrome.popupBackground
        border =
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LatticeChrome.border, 1, true),
                EmptyBorder(12, 16, 12, 16),
            )

        val titleLabel =
            JLabel(title).apply {
                foreground = LatticeChrome.accent
                font = font.deriveFont(Font.BOLD, 13f)
                border = EmptyBorder(0, 0, 8, 0)
            }
        add(titleLabel, BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = background
        g2.fillRoundRect(0, 0, width, height, 8, 8)
        g2.dispose()
    }

    /**
     * Appends a new form row containing configuration descriptors on the left
     * and a right-aligned control input.
     */
    fun addRow(
        row: Int,
        name: String,
        description: String,
        component: JComponent,
    ) {
        val gbc =
            GridBagConstraints().apply {
                gridy = row
                insets = Insets(6, 0, 6, 0)
                fill = GridBagConstraints.HORIZONTAL
            }

        // 1. Text descriptors (Title & Subtitle description)
        gbc.gridx = 0
        gbc.weightx = 1.0
        val textPanel =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                val nameLabel =
                    JLabel(name).apply {
                        foreground = LatticeChrome.textPrimary
                        font = font.deriveFont(Font.BOLD, 12f)
                    }
                val descLabel =
                    JLabel(description).apply {
                        foreground = LatticeChrome.textSecondary
                        font = font.deriveFont(Font.PLAIN, 10f)
                    }
                add(nameLabel)
                add(Box.createVerticalStrut(2))
                add(descLabel)
            }
        contentPanel.add(textPanel, gbc)

        // 2. Aligned input component
        gbc.gridx = 1
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.EAST

        if (component !is LatticeSwitch && component !is JCheckBox) {
            component.preferredSize = Dimension(150, 26)
            component.minimumSize = Dimension(150, 26)
            component.maximumSize = Dimension(150, 26)
        }
        contentPanel.add(component, gbc)
    }
}
