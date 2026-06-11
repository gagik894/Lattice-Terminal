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
import com.gagik.terminal.workspace.TerminalProfile
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * Creates and wires the standalone terminal window.
 *
 * FlatLaf merges [JMenuBar] into the custom title bar when
 * `useWindowDecorations=true` and `JFrame.setDefaultLookAndFeelDecorated(true)`
 * are active. We place [LatticeTabBar] at the left of the menu bar and the
 * action buttons at the right, with a horizontal glue in between.
 *
 */
internal class LatticeWindowFactory(
    private val settings: StandaloneTerminalSettings,
    private val profiles: List<TerminalProfile>,
) {
    fun createWindow(): LatticeWindow {
        val frame =
            JFrame(LatticeChrome.APP_TITLE).apply {
                defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
                minimumSize = Dimension(720, 420)
            }

        val tabContentPanel =
            JPanel(CardLayout()).apply {
                background = LatticeChrome.terminalBackground
                isOpaque = true
            }

        // Tab bar callbacks reference tabManager; forward via lambda so the
        // lateinit is resolved at call time, not at construction time.
        lateinit var tabManager: LatticeTabManager
        lateinit var tabBar: LatticeTabBar

        tabBar =
            LatticeTabBar(
                onTabSelected = { id -> tabManager.onTabSelected(id) },
                onTabClose = { id -> tabManager.closeTab(id) },
                onNewTab = { tabManager.openTab(profiles.first()) },
                onMenuClick = { x, y -> showDropdownMenu(frame, tabBar, x, y, profiles, tabManager) },
                onTabColorChanged = { id, color -> tabManager.onTabColorChanged(id, color) },
                onTabRenameRequested = { id, newName -> tabManager.onTabRenameRequested(id, newName) },
            )

        tabManager = LatticeTabManager(frame, tabBar, tabContentPanel, settings, profiles.first())

        installMenuBar(frame, tabBar)
        styleTitleBar(frame)
        frame.contentPane = tabContentPanel

        frame.addWindowListener(
            object : WindowAdapter() {
                override fun windowClosed(event: WindowEvent) {
                    tabManager.closeAllTabs()
                }
            },
        )

        return LatticeWindow(frame, tabManager)
    }

    /**
     * Places [tabBar] and the action panel inside a [JMenuBar] that FlatLaf
     * renders in the title bar area.
     */
    private fun installMenuBar(
        frame: JFrame,
        tabBar: LatticeTabBar,
    ) {
        frame.jMenuBar =
            JMenuBar().apply {
                isOpaque = false
                border = EmptyBorder(0, 0, 0, 0)
                add(tabBar)
                add(Box.createHorizontalGlue())
            }
    }

    private fun showDropdownMenu(
        frame: JFrame,
        invoker: java.awt.Component,
        x: Int,
        y: Int,
        profiles: List<TerminalProfile>,
        tabManager: LatticeTabManager,
    ) {
        val popup =
            JPopupMenu().apply {
                background = LatticeChrome.popupBackground
                border = BorderFactory.createLineBorder(LatticeChrome.border)
            }

        profiles.forEach { profile ->
            val item =
                JMenuItem(profile.displayName).apply {
                    background = LatticeChrome.popupBackground
                    foreground = LatticeChrome.textPrimary
                    addActionListener { tabManager.openTab(profile) }
                }
            popup.add(item)
        }

        popup.addSeparator()

        val settingsItem =
            JMenuItem("Settings...").apply {
                background = LatticeChrome.popupBackground
                foreground = LatticeChrome.textPrimary
                addActionListener {
                    LatticeSettingsDialog(frame, settings) {
                        tabManager.reloadAllPanes()
                    }.isVisible = true
                }
            }
        popup.add(settingsItem)

        val commandPaletteItem =
            JMenuItem("Command palette").apply {
                background = LatticeChrome.popupBackground
                foreground = LatticeChrome.textPrimary
            }
        popup.add(commandPaletteItem)

        val aboutItem =
            JMenuItem("About").apply {
                background = LatticeChrome.popupBackground
                foreground = LatticeChrome.textPrimary
            }
        popup.add(aboutItem)

        popup.show(invoker, x, y)
    }

    /**
     * Applies FlatLaf title bar styling: background colour, no icon, no title
     * text (the tab bar provides context instead).
     */
    private fun styleTitleBar(frame: JFrame) {
        frame.rootPane.apply {
            background = LatticeChrome.surface
            putClientProperty("JRootPane.titleBarBackground", LatticeChrome.topBarBackground)
            putClientProperty("JRootPane.titleBarForeground", LatticeChrome.textPrimary)
            putClientProperty("JRootPane.titleBarShowIcon", false)
            putClientProperty("JRootPane.titleBarShowTitle", false)
        }
    }
}

/**
 * Standalone window and its terminal tab controller.
 */
internal data class LatticeWindow(
    val frame: JFrame,
    val tabManager: LatticeTabManager,
)
