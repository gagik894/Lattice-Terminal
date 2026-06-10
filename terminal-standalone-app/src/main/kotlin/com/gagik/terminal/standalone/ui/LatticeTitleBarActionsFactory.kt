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
import com.gagik.terminal.workspace.TerminalProfile
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Insets
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenu
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JRadioButtonMenuItem
import javax.swing.JSeparator
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder

/**
 * Builds the trailing action panel placed in the window title bar.
 *
 * The panel contains a profiles/commands button and a settings button.
 * It is intended to be passed to FlatLaf's
 * `JRootPane.titleBarTrailingComponent` client property.
 */
internal class LatticeTitleBarActionsFactory(
    private val settings: StandaloneTerminalSettings,
    private val tabManager: LatticeTabManager,
    private val profiles: List<TerminalProfile>,
) {
    /**
     * Pre-built trailing panel ready to be wired into the title bar.
     * Contains the profiles/commands dropdown button and the settings button.
     */
    val trailingPanel: JPanel =
        JPanel(FlowLayout(FlowLayout.LEFT, 4, 6)).apply {
            isOpaque = false
            add(buildProfilesButton())
            add(buildSettingsButton())
        }

    // -------------------------------------------------------------------------
    // Button constructors
    // -------------------------------------------------------------------------

    private fun buildProfilesButton(): JButton =
        commandButton(PROFILE_MENU_GLYPH, "Profiles and commands").apply {
            preferredSize = Dimension(34, 28)
            addActionListener {
                buildProfilesPopup().show(this, 0, height)
            }
        }

    private fun buildSettingsButton(): JButton =
        commandButton(SETTINGS_MENU_GLYPH, "Settings").apply {
            preferredSize = Dimension(42, 28)
            addActionListener {
                buildSettingsPopup().show(this, -width / 2, height)
            }
        }

    // -------------------------------------------------------------------------
    // Popup menus
    // -------------------------------------------------------------------------

    private fun buildProfilesPopup(): JPopupMenu =
        JPopupMenu().apply {
            profiles.forEachIndexed { index, profile ->
                add(profile.displayName).addActionListener {
                    tabManager.openTab(profile)
                }
                getComponent(index).name = profile.id
            }
            add(JSeparator(SwingConstants.HORIZONTAL))
            add("Close tab").addActionListener {
                tabManager.selectedPane?.let { pane ->
                    tabManager.closeTab(pane.tab.id)
                }
            }
        }

    private fun buildSettingsPopup(): JPopupMenu =
        JPopupMenu().apply {
            add(buildThemeMenu())
            add(buildWidthItem())
        }

    private fun buildThemeMenu(): JMenu {
        val themeMenu = JMenu("Theme")
        val themeGroup = ButtonGroup()
        TerminalTheme.entries.forEach { theme ->
            val item = JRadioButtonMenuItem(theme.displayName(), theme == settings.theme)
            themeGroup.add(item)
            item.addActionListener {
                settings.theme = theme
                tabManager.reloadAllPanes()
            }
            themeMenu.add(item)
        }
        return themeMenu
    }

    private fun buildWidthItem(): JCheckBoxMenuItem =
        JCheckBoxMenuItem("Ambiguous as wide", settings.treatAmbiguousAsWide).apply {
            addActionListener {
                settings.treatAmbiguousAsWide = isSelected
                tabManager.reloadAllPanes()
            }
        }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private fun commandButton(
        text: String,
        tooltip: String,
    ): JButton =
        JButton(text).apply {
            toolTipText = tooltip
            preferredSize = Dimension(34, 28)
            margin = Insets(0, 0, 0, 0)
            isFocusable = false
            isOpaque = false
            border = EmptyBorder(0, 0, 1, 0)
            putClientProperty("JButton.buttonType", "toolBarButton")
        }

    private fun TerminalTheme.displayName(): String =
        name.lowercase().split("_").joinToString(" ") {
            it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
        }

    private companion object {
        private const val PROFILE_MENU_GLYPH = "\u25BE"
        private const val SETTINGS_MENU_GLYPH = "\u22EF"
    }
}
