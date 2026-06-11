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
 * A highly polished, IDE-style settings dialog featuring a clean sidebar and flat form layouts.
 */
internal class LatticeSettingsDialog(
    parent: JFrame,
    private val settings: StandaloneTerminalSettings,
    private val onApply: () -> Unit,
) : JDialog(parent, "Terminal Settings", true) {
    private val cardLayout = CardLayout()

    // Opaque panel is critical for CardLayout to clear previous artifacts correctly
    private val cardPanel =
        JPanel(cardLayout).apply {
            isOpaque = true
            background = LatticeChrome.surface
        }
    private val sidebarPanel =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = true
            background = LatticeChrome.surface
            border = EmptyBorder(8, 0, 8, 0)
            preferredSize = Dimension(180, -1)
        }

    private val categories = mutableListOf<CategoryLabel>()

    // Factory Helpers
    private fun createTextField(
        initialValue: String,
        width: Int,
    ) = JTextField(initialValue).apply { applySizing(this, width) }

    private fun createSpinner(
        initialValue: Int,
        min: Int,
        max: Int,
        step: Int,
        width: Int,
    ) = JSpinner(SpinnerNumberModel(initialValue, min, max, step)).apply {
        applySizing(this, width)
    }

    private fun createFloatSpinner(
        initialValue: Float,
        min: Double,
        max: Double,
        step: Double,
        width: Int,
    ) = JSpinner(SpinnerNumberModel(initialValue.toDouble(), min, max, step)).apply {
        applySizing(this, width)
    }

    private fun <T> createComboBox(
        items: Array<T>,
        initialValue: T,
        width: Int,
    ) = JComboBox(items).apply {
        selectedItem = initialValue
        applySizing(this, width)
    }

    // Form Controls - Application
    private val shellPathField = createTextField(settings.shellPath, 220)
    private val startDirectoryField = createTextField(settings.startDirectory, 220)
    private val audibleBellCheckbox = JCheckBox("Audible bell", settings.audibleBell)

    // Form Controls - Appearance
    private val fontFamilyCombo =
        createComboBox(GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames, settings.fontFamily, 220)
    private val fontSizeSpinner = createSpinner(settings.fontSize, 8, 72, 1, 70)
    private val lineHeightSpinner = createFloatSpinner(settings.lineHeight, 0.5, 3.0, 0.1, 70)
    private val columnsSpinner = createSpinner(settings.columns, 40, 300, 1, 70)
    private val rowsSpinner = createSpinner(settings.rows, 10, 100, 1, 70)
    private val scrollbackSpinner = createSpinner(settings.scrollbackLines, 0, 100000, 100, 80)
    private val windowOpacitySpinner = createFloatSpinner(settings.windowOpacity, 0.1, 1.0, 0.05, 70)
    private val themeCombo = createComboBox(TerminalTheme.entries.map { it.name }.toTypedArray(), settings.theme.name, 220)

    // Form Controls - Behavior
    private val treatAmbiguousCheckbox = JCheckBox("Treat East Asian ambiguous characters as wide", settings.treatAmbiguousAsWide)
    private val useSystemFallbackCheckbox = JCheckBox("Use system font fallback for missing glyphs", settings.useSystemFallbackFonts)
    private val pasteOnMiddleClickCheckbox = JCheckBox("Paste on middle mouse button click", settings.pasteOnMiddleClick)
    private val cursorBlinkSpinner = createSpinner(settings.cursorBlinkMillis, 0, 5000, 50, 70)
    private val cursorShapeCombo = createComboBox(arrayOf("block", "underline", "beam"), settings.cursorShape.lowercase(Locale.ROOT), 150)

    init {
        size = Dimension(750, 550)
        setLocationRelativeTo(parent)
        layout = BorderLayout()
        isUndecorated = false

        // Setup Main Container
        val splitPane =
            JPanel(BorderLayout()).apply {
                isOpaque = true
                background = LatticeChrome.surface
                add(sidebarPanel, BorderLayout.WEST)
                add(
                    JScrollPane(cardPanel).apply {
                        isOpaque = true
                        viewport.isOpaque = true
                        viewport.background = LatticeChrome.surface
                        border = BorderFactory.createMatteBorder(0, 1, 0, 0, LatticeChrome.border)
                    },
                    BorderLayout.CENTER,
                )
            }

        add(splitPane, BorderLayout.CENTER)
        add(buildFooterPanel(), BorderLayout.SOUTH)

        // Add Pages
        addPage("Application", buildApplicationPanel())
        addPage("Appearance", buildAppearancePanel())
        addPage("Behavior", buildBehaviorPanel())

        selectCategory(categories.first().categoryName)
    }

    private fun applySizing(
        component: JComponent,
        width: Int,
    ) {
        component.preferredSize = Dimension(width, 26)
    }

    private fun addPage(
        title: String,
        panel: JPanel,
    ) {
        val categoryLabel = CategoryLabel(title)
        categories.add(categoryLabel)
        sidebarPanel.add(categoryLabel)

        // Container with padding to hold the content panel
        val contentContainer =
            JPanel(BorderLayout()).apply {
                isOpaque = false
                border = EmptyBorder(0, 16, 16, 16)
                add(panel, BorderLayout.NORTH)
            }

        cardPanel.add(contentContainer, title)

        categoryLabel.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    selectCategory(title)
                }
            },
        )
    }

    private fun selectCategory(title: String) {
        categories.forEach { it.updateState(it.categoryName == title) }
        cardLayout.show(cardPanel, title)
    }

    private fun buildApplicationPanel(): JPanel {
        val panel =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
            }

        panel.add(LatticeSectionHeader("Project Settings"))
        val projectSection = createSectionPanel()
        addFormRow(projectSection, 0, "Shell path:", shellPathField)
        addFormRow(projectSection, 1, "Start directory:", startDirectoryField)
        panel.add(projectSection)

        panel.add(LatticeSectionHeader("Application Settings"))
        val appSection = createSectionPanel()
        addCheckboxRow(appSection, 0, audibleBellCheckbox)
        panel.add(appSection)

        return panel
    }

    private fun buildAppearancePanel(): JPanel {
        val panel =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
            }

        panel.add(LatticeSectionHeader("Typography & Theme"))
        val typoSection = createSectionPanel()
        addFormRow(typoSection, 0, "Font family:", fontFamilyCombo)
        val lhLabel =
            JLabel("Line height:").apply {
                foreground = LatticeChrome.textPrimary
                font = font.deriveFont(Font.PLAIN, 13f)
            }
        addFormRow(typoSection, 1, "Font size:", fontSizeSpinner, Box.createHorizontalStrut(10), lhLabel, lineHeightSpinner)
        addFormRow(typoSection, 2, "Color theme:", themeCombo)
        panel.add(typoSection)

        panel.add(LatticeSectionHeader("Window Layout"))
        val windowSection = createSectionPanel()
        val rowsLabel =
            JLabel("Rows:").apply {
                foreground = LatticeChrome.textPrimary
                font = font.deriveFont(Font.PLAIN, 13f)
            }
        addFormRow(windowSection, 0, "Columns:", columnsSpinner, Box.createHorizontalStrut(10), rowsLabel, rowsSpinner)
        addFormRow(windowSection, 1, "Scrollback lines:", scrollbackSpinner)
        addFormRow(windowSection, 2, "Window opacity:", windowOpacitySpinner)
        panel.add(windowSection)

        return panel
    }

    private fun buildBehaviorPanel(): JPanel {
        val panel =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
            }

        panel.add(LatticeSectionHeader("Terminal Behavior"))
        val behaviorSection = createSectionPanel()
        addCheckboxRow(
            behaviorSection,
            0,
            treatAmbiguousCheckbox,
            "Render East Asian ambiguous characters (e.g. smart quotes, emojis) with double cell width.",
        )
        addCheckboxRow(
            behaviorSection,
            2,
            useSystemFallbackCheckbox,
            "Query the system catalog to resolve characters and symbols missing in the primary typeface.",
        )
        addCheckboxRow(
            behaviorSection,
            4,
            pasteOnMiddleClickCheckbox,
        )
        panel.add(behaviorSection)

        panel.add(LatticeSectionHeader("Cursor Settings"))
        val cursorSection = createSectionPanel()
        addFormRow(cursorSection, 0, "Cursor shape:", cursorShapeCombo)
        addFormRow(cursorSection, 1, "Blink period (ms):", cursorBlinkSpinner)
        panel.add(cursorSection)

        return panel
    }

    private fun createSectionPanel(): JPanel =
        JPanel(GridBagLayout()).apply {
            isOpaque = false
            alignmentX = LEFT_ALIGNMENT
            border = EmptyBorder(0, 16, 0, 0)
        }

    private fun addFormRow(
        panel: JPanel,
        row: Int,
        labelText: String,
        vararg components: Component,
    ) {
        val label =
            JLabel(labelText).apply {
                foreground = LatticeChrome.textPrimary
                font = font.deriveFont(Font.PLAIN, 13f)
            }

        val gbc =
            GridBagConstraints().apply {
                gridy = row
                fill = GridBagConstraints.NONE
                anchor = GridBagConstraints.WEST
            }

        gbc.gridx = 0
        gbc.weightx = 0.0
        gbc.insets = Insets(6, 0, 6, 12)
        panel.add(label, gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        gbc.insets = Insets(6, 0, 6, 0)

        if (components.size == 1) {
            panel.add(components[0], gbc)
        } else {
            val wrapper =
                JPanel(FlowLayout(FlowLayout.LEFT, 10, 0)).apply {
                    isOpaque = false
                }
            components.forEach { wrapper.add(it) }
            panel.add(wrapper, gbc)
        }
    }

    private fun addCheckboxRow(
        panel: JPanel,
        row: Int,
        checkbox: JCheckBox,
        description: String? = null,
    ) {
        val gbc =
            GridBagConstraints().apply {
                gridy = row
                gridx = 0
                gridwidth = 2
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.WEST
                insets = Insets(4, 0, 0, 0)
            }
        panel.add(checkbox, gbc)

        if (description != null) {
            val descGbc =
                GridBagConstraints().apply {
                    gridy = row + 1
                    gridx = 0
                    gridwidth = 2
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    anchor = GridBagConstraints.WEST
                    insets = Insets(2, 22, 12, 0) // Indent to match checkbox text
                }
            val descLabel =
                JLabel("<html>$description</html>").apply {
                    foreground = LatticeChrome.textSecondary
                    font = font.deriveFont(Font.PLAIN, 12f)
                }
            panel.add(descLabel, descGbc)
        }
    }

    private fun buildFooterPanel(): JPanel =
        JPanel(FlowLayout(FlowLayout.RIGHT, 12, 12)).apply {
            isOpaque = true
            background = LatticeChrome.surface
            border = BorderFactory.createMatteBorder(1, 0, 0, 0, LatticeChrome.border)

            val okButton =
                JButton("OK").apply {
                    addActionListener {
                        applyChanges()
                        dispose()
                    }
                }

            val cancelButton =
                JButton("Cancel").apply {
                    addActionListener { dispose() }
                }

            val applyButton =
                JButton("Apply").apply {
                    addActionListener { applyChanges() }
                }

            add(okButton)
            add(cancelButton)
            add(applyButton)
            this@LatticeSettingsDialog.rootPane.defaultButton = okButton
        }

    private fun applyChanges() {
        settings.shellPath = shellPathField.text
        settings.startDirectory = startDirectoryField.text
        settings.audibleBell = audibleBellCheckbox.isSelected
        settings.pasteOnMiddleClick = pasteOnMiddleClickCheckbox.isSelected
        settings.scrollbackLines = scrollbackSpinner.value as Int
        settings.lineHeight = (lineHeightSpinner.value as Double).toFloat()
        settings.windowOpacity = (windowOpacitySpinner.value as Double).toFloat()

        settings.fontFamily = fontFamilyCombo.selectedItem as String
        settings.fontSize = fontSizeSpinner.value as Int
        settings.columns = columnsSpinner.value as Int
        settings.rows = rowsSpinner.value as Int
        settings.treatAmbiguousAsWide = treatAmbiguousCheckbox.isSelected
        settings.useSystemFallbackFonts = useSystemFallbackCheckbox.isSelected
        settings.cursorBlinkMillis = cursorBlinkSpinner.value as Int
        settings.cursorShape = cursorShapeCombo.selectedItem as String

        val themeName = themeCombo.selectedItem as String
        settings.theme = TerminalTheme.entries.firstOrNull { it.name == themeName } ?: TerminalTheme.TOKYO_NIGHT

        onApply()
    }

    private inner class CategoryLabel(
        val categoryName: String,
    ) : JPanel() {
        private var selected = false
        private var hovered = false

        private val nameLabel =
            JLabel(categoryName).apply {
                font = font.deriveFont(Font.PLAIN, 13f)
                foreground = LatticeChrome.textPrimary
                border = EmptyBorder(0, 12, 0, 0)
            }

        init {
            layout = BorderLayout()
            isOpaque = false
            maximumSize = Dimension(Int.MAX_VALUE, 32)
            preferredSize = Dimension(Int.MAX_VALUE, 32)

            add(nameLabel, BorderLayout.CENTER)

            addMouseListener(
                object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent?) {
                        hovered = true
                        repaint()
                    }

                    override fun mouseExited(e: MouseEvent?) {
                        hovered = false
                        repaint()
                    }
                },
            )
        }

        fun updateState(isSelected: Boolean) {
            this.selected = isSelected
            nameLabel.font = nameLabel.font.deriveFont(if (isSelected) Font.BOLD else Font.PLAIN)
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            if (selected) {
                g.color = LatticeChrome.controlHover
                g.fillRect(8, 0, width - 16, height)
            } else if (hovered) {
                g.color = LatticeChrome.tabHoverBackground
                g.fillRect(8, 0, width - 16, height)
            }
            super.paintComponent(g)
        }
    }
}
