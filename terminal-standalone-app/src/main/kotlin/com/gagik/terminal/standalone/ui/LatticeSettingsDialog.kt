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

import com.gagik.terminal.standalone.config.StandaloneTerminalSettings
import com.gagik.terminal.ui.swing.settings.TerminalTheme
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * A redesigned modern, category-card Settings Dialog for the standalone terminal.
 *
 * Exposes full configuration settings bound to TOML, utilizing custom toggle switches
 * and aligned settings rows with headers, subtitles, and standard Cancel/Apply/OK actions.
 */
internal class LatticeSettingsDialog(
    parent: JFrame,
    private val settings: StandaloneTerminalSettings,
    private val onSave: () -> Unit,
) : JDialog(parent, "Settings", true) {
    private val cardLayout = CardLayout()
    private val cardPanel =
        JPanel(cardLayout).apply {
            isOpaque = false
        }

    private val sidebarPanel =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = LatticeChrome.topBarBackground
            border =
                BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, LatticeChrome.border),
                    EmptyBorder(20, 10, 20, 10),
                )
            preferredSize = Dimension(165, 0)
        }

    private val categories = listOf("Appearance", "Behavior")
    private val categoryLabels = ArrayList<CategoryLabel>()

    // Inputs
    private val columnsSpinner: JSpinner
    private val rowsSpinner: JSpinner
    private val themeCombo: JComboBox<String>
    private val fontFamilyCombo: JComboBox<String>
    private val fontSizeSpinner: JSpinner
    private val treatAmbiguousSwitch: LatticeSwitch
    private val useSystemFallbackSwitch: LatticeSwitch
    private val cursorShapeCombo: JComboBox<String>
    private val cursorBlinkSpinner: JSpinner

    init {
        contentPane.background = LatticeChrome.surface
        layout = BorderLayout()
        minimumSize = Dimension(650, 480)
        isResizable = false

        val currentSwingSettings = settings.current()

        // Initialize Appearance Controls
        columnsSpinner =
            JSpinner(SpinnerNumberModel(currentSwingSettings.columns, 20, 500, 1)).apply {
                putClientProperty("JComponent.roundRect", true)
            }
        rowsSpinner =
            JSpinner(SpinnerNumberModel(currentSwingSettings.rows, 5, 200, 1)).apply {
                putClientProperty("JComponent.roundRect", true)
            }
        themeCombo =
            JComboBox(TerminalTheme.entries.map { themeName(it) }.toTypedArray()).apply {
                selectedItem = themeName(settings.theme)
                putClientProperty("JComponent.roundRect", true)
            }
        val commonFonts =
            arrayOf("Cascadia Mono", "Cascadia Code", "Consolas", "Fira Code", "JetBrains Mono", "Courier New", Font.MONOSPACED)
        fontFamilyCombo =
            JComboBox(commonFonts).apply {
                selectedItem = currentSwingSettings.font.family
                isEditable = true
                putClientProperty("JComponent.roundRect", true)
            }
        fontSizeSpinner =
            JSpinner(SpinnerNumberModel(currentSwingSettings.font.size, 8, 72, 1)).apply {
                putClientProperty("JComponent.roundRect", true)
            }

        // Initialize Behavior Controls
        treatAmbiguousSwitch = LatticeSwitch(settings.treatAmbiguousAsWide) {}
        useSystemFallbackSwitch = LatticeSwitch(currentSwingSettings.useSystemFallbackFonts) {}

        val cursorShapes = arrayOf("Block", "Underline", "Beam")
        cursorShapeCombo =
            JComboBox(cursorShapes).apply {
                selectedItem = settings.cursorShape.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                putClientProperty("JComponent.roundRect", true)
            }

        cursorBlinkSpinner =
            JSpinner(SpinnerNumberModel(currentSwingSettings.cursorBlinkMillis, 100, 5000, 50)).apply {
                putClientProperty("JComponent.roundRect", true)
            }

        // Add Category Cards
        cardPanel.add(buildAppearanceCard(), "Appearance")
        cardPanel.add(buildBehaviorCard(), "Behavior")

        // Setup Sidebar
        buildSidebar()

        // Assembly
        add(sidebarPanel, BorderLayout.WEST)
        add(cardPanel, BorderLayout.CENTER)
        add(buildBottomControls(), BorderLayout.SOUTH)

        setLocationRelativeTo(parent)
    }

    private fun themeName(theme: TerminalTheme): String =
        theme.name.lowercase(Locale.ROOT).split("_").joinToString(" ") {
            it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString() }
        }

    private fun themeByName(name: String): TerminalTheme {
        val normalized = name.uppercase(Locale.ROOT).replace(" ", "_")
        return TerminalTheme.entries.firstOrNull { it.name == normalized } ?: TerminalTheme.ONE_DARK
    }

    private fun buildSidebar() {
        val titleLabel =
            JLabel("Settings").apply {
                foreground = LatticeChrome.textPrimary
                font = font.deriveFont(Font.BOLD, 18f)
                alignmentX = LEFT_ALIGNMENT
                border = EmptyBorder(0, 4, 15, 0)
            }
        sidebarPanel.add(titleLabel)

        categories.forEachIndexed { index, name ->
            val label = CategoryLabel(name, index == 0)
            label.alignmentX = LEFT_ALIGNMENT
            label.addMouseListener(
                object : MouseAdapter() {
                    override fun mousePressed(e: MouseEvent) {
                        selectCategory(name)
                    }
                },
            )
            categoryLabels.add(label)
            sidebarPanel.add(label)
            sidebarPanel.add(Box.createVerticalStrut(4))
        }
    }

    private fun selectCategory(categoryName: String) {
        cardLayout.show(cardPanel, categoryName)
        categoryLabels.forEach { label ->
            label.setSelected(label.categoryName == categoryName)
        }
    }

    private fun buildAppearanceCard(): JPanel {
        val panel =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                border = EmptyBorder(25, 25, 25, 25)
            }

        // Title Header
        val headerLabel =
            JLabel("Appearance Settings").apply {
                foreground = LatticeChrome.textPrimary
                font = font.deriveFont(Font.BOLD, 16f)
                alignmentX = LEFT_ALIGNMENT
            }
        val subheaderLabel =
            JLabel("Configure layout sizes, monospace fonts, and color palettes.").apply {
                foreground = LatticeChrome.textSecondary
                font = font.deriveFont(Font.PLAIN, 11f)
                alignmentX = LEFT_ALIGNMENT
            }
        panel.add(headerLabel)
        panel.add(Box.createVerticalStrut(2))
        panel.add(subheaderLabel)
        panel.add(Box.createVerticalStrut(20))

        // Group 1: Window Geometry
        val geometryCard =
            LatticeSettingsGroupCard("Window Geometry").apply {
                alignmentX = LEFT_ALIGNMENT
                addRow(0, "Columns", "Startup width of the terminal grid in character cells", columnsSpinner)
                addRow(1, "Rows", "Startup height of the terminal grid in character rows", rowsSpinner)
            }
        panel.add(geometryCard)
        panel.add(Box.createVerticalStrut(16))

        // Group 2: Typography
        val fontCard =
            LatticeSettingsGroupCard("Typography").apply {
                alignmentX = LEFT_ALIGNMENT
                addRow(0, "Font Family", "Primary monospace family used for terminal text runs", fontFamilyCombo)
                addRow(1, "Font Size (pt)", "Default size of character glyphs in points", fontSizeSpinner)
            }
        panel.add(fontCard)
        panel.add(Box.createVerticalStrut(16))

        // Group 3: Color Palette
        val themeCard =
            LatticeSettingsGroupCard("Theme Options").apply {
                alignmentX = LEFT_ALIGNMENT
                addRow(0, "Color Theme", "Terminal foreground, background, and ANSI 16-color lookup mapping", themeCombo)
            }
        panel.add(themeCard)

        panel.add(Box.createVerticalGlue())
        return panel
    }

    private fun buildBehaviorCard(): JPanel {
        val panel =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                border = EmptyBorder(25, 25, 25, 25)
            }

        // Title Header
        val headerLabel =
            JLabel("Behavior & Execution").apply {
                foreground = LatticeChrome.textPrimary
                font = font.deriveFont(Font.BOLD, 16f)
                alignmentX = LEFT_ALIGNMENT
            }
        val subheaderLabel =
            JLabel("Customize character width, fallback font resolve policy, and cursor shapes.").apply {
                foreground = LatticeChrome.textSecondary
                font = font.deriveFont(Font.PLAIN, 11f)
                alignmentX = LEFT_ALIGNMENT
            }
        panel.add(headerLabel)
        panel.add(Box.createVerticalStrut(2))
        panel.add(subheaderLabel)
        panel.add(Box.createVerticalStrut(20))

        // Group 1: Text Layout
        val layoutCard =
            LatticeSettingsGroupCard("Text & Layout").apply {
                alignmentX = LEFT_ALIGNMENT
                addRow(0, "Ambiguous as Wide", "Render East Asian ambiguous characters with double cell width", treatAmbiguousSwitch)
                addRow(
                    1,
                    "System Font Fallbacks",
                    "Query system catalog to resolve characters missing in primary font",
                    useSystemFallbackSwitch,
                )
            }
        panel.add(layoutCard)
        panel.add(Box.createVerticalStrut(16))

        // Group 2: Cursor Behavior
        val cursorCard =
            LatticeSettingsGroupCard("Cursor Settings").apply {
                alignmentX = LEFT_ALIGNMENT
                addRow(0, "Cursor Shape", "Visual outline shape representation of the cursor pointer", cursorShapeCombo)
                addRow(1, "Cursor Blink Period (ms)", "Time interval in milliseconds representing cursor blink cycles", cursorBlinkSpinner)
            }
        panel.add(cursorCard)

        panel.add(Box.createVerticalGlue())
        return panel
    }

    private fun buildBottomControls(): JPanel =
        JPanel(FlowLayout(FlowLayout.RIGHT, 12, 12)).apply {
            background = LatticeChrome.surface
            border = BorderFactory.createMatteBorder(1, 0, 0, 0, LatticeChrome.border)

            val cancelButton =
                JButton("Cancel").apply {
                    putClientProperty("JButton.buttonType", "roundRect")
                    background = LatticeChrome.controlBackground
                    foreground = LatticeChrome.textPrimary
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    addActionListener {
                        dispose()
                    }
                }

            val applyButton =
                JButton("Apply").apply {
                    putClientProperty("JButton.buttonType", "roundRect")
                    background = LatticeChrome.controlBackground
                    foreground = LatticeChrome.textPrimary
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    addActionListener {
                        saveSettings()
                    }
                }

            val okButton =
                JButton("OK").apply {
                    putClientProperty("JButton.buttonType", "roundRect")
                    background = LatticeChrome.accent
                    foreground = Color.WHITE
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    addActionListener {
                        saveSettings()
                        dispose()
                    }
                }

            add(cancelButton)
            add(applyButton)
            add(okButton)
        }

    private fun saveSettings() {
        val selectedThemeName = themeCombo.selectedItem as String
        settings.theme = themeByName(selectedThemeName)
        settings.treatAmbiguousAsWide = treatAmbiguousSwitch.isSelected()

        settings.columns = columnsSpinner.value as Int
        settings.rows = rowsSpinner.value as Int
        settings.fontFamily = fontFamilyCombo.selectedItem as String
        settings.fontSize = fontSizeSpinner.value as Int
        settings.useSystemFallbackFonts = useSystemFallbackSwitch.isSelected()
        settings.cursorBlinkMillis = cursorBlinkSpinner.value as Int

        val selectedShape = (cursorShapeCombo.selectedItem as String).lowercase(Locale.ROOT)
        settings.cursorShape = selectedShape

        onSave()
    }

    private inner class CategoryLabel(
        val categoryName: String,
        private var selected: Boolean,
    ) : JPanel() {
        private var hovered = false
        private val nameLabel =
            JLabel(categoryName).apply {
                font = font.deriveFont(if (selected) Font.BOLD else Font.PLAIN, 13f)
                foreground = if (selected) LatticeChrome.textPrimary else LatticeChrome.textSecondary
                border = EmptyBorder(0, 8, 0, 0)
            }

        init {
            layout = BorderLayout()
            isOpaque = false
            border = EmptyBorder(8, 12, 8, 12)
            add(nameLabel, BorderLayout.CENTER)

            addMouseListener(
                object : MouseAdapter() {
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
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }

        fun setSelected(newSelected: Boolean) {
            selected = newSelected
            nameLabel.font = nameLabel.font.deriveFont(if (selected) Font.BOLD else Font.PLAIN, 13f)
            nameLabel.foreground = if (selected) LatticeChrome.textPrimary else LatticeChrome.textSecondary
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            if (selected) {
                // Background subtle highlight
                g2.color = LatticeChrome.controlBackground
                g2.fillRoundRect(0, 0, width, height, 8, 8)

                // Accent vertical line indicator on the left
                g2.color = LatticeChrome.accent
                g2.fillRoundRect(2, 4, 3, height - 8, 2, 2)
            } else if (hovered) {
                g2.color = LatticeChrome.tabHoverBackground
                g2.fillRoundRect(0, 0, width, height, 8, 8)
            }

            g2.dispose()
        }
    }
}
