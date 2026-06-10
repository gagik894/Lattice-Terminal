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
import java.awt.Insets
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.Box
import javax.swing.JFrame
import javax.swing.JMenuBar
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.WindowConstants
import javax.swing.border.EmptyBorder

/**
 * Creates and wires the standalone terminal window chrome.
 */
internal class LatticeWindowFactory(
    private val settings: StandaloneTerminalSettings,
    private val profiles: List<TerminalProfile>,
) {
    private val tabPane = JTabbedPane()

    fun createWindow(): LatticeWindow {
        val tabContentPanel =
            JPanel(CardLayout()).apply {
                background = LatticeChrome.TERMINAL_BACKGROUND
            }

        val frame =
            JFrame(LatticeChrome.APP_TITLE).apply {
                defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
                background = LatticeChrome.SURFACE
                minimumSize = Dimension(720, 420)
            }

        val tabManager = LatticeTabManager(frame, tabPane, tabContentPanel, settings)

        // Build JMenuBar containing the tabs and actions
        val actionsFactory = LatticeTitleBarActionsFactory(settings, tabManager, profiles)
        val menuBar =
            JMenuBar().apply {
                isOpaque = false
                border = EmptyBorder(0, 8, 0, 8)
                add(tabPane)
                add(actionsFactory.newTabButton)
                add(actionsFactory.profilesButton)
                add(Box.createHorizontalGlue())
                add(actionsFactory.settingsButton)
            }

        // Configure frame for custom window decorations with embedded menu bar
        frame.rootPane.apply {
            putClientProperty("JRootPane.titleBarBackground", LatticeChrome.TOP_BAR_BACKGROUND)
            putClientProperty("JRootPane.titleBarForeground", LatticeChrome.TITLE_FOREGROUND)
            putClientProperty("JRootPane.titleBarShowIcon", false)
            putClientProperty("JRootPane.titleBarShowTitle", false)
            putClientProperty("JRootPane.titleBarHeight", 40)
        }

        frame.jMenuBar = menuBar
        configureTabPane()
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

    private fun configureTabPane() {
        tabPane.background = LatticeChrome.TAB_BAR_BACKGROUND
        tabPane.foreground = LatticeChrome.TEXT_MUTED
        tabPane.isFocusable = false
        tabPane.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
        tabPane.putClientProperty("JTabbedPane.tabType", "card")
        tabPane.putClientProperty("JTabbedPane.hasFullBorder", false)
        tabPane.putClientProperty("JTabbedPane.showTabSeparators", false)
        tabPane.putClientProperty("JTabbedPane.tabAreaInsets", Insets(5, 0, 0, 0))
        tabPane.putClientProperty("JTabbedPane.tabInsets", Insets(4, 12, 4, 10))
        tabPane.putClientProperty("JTabbedPane.minimumTabWidth", 132)
        tabPane.putClientProperty("JTabbedPane.maximumTabWidth", 220)
        tabPane.putClientProperty("JTabbedPane.scrollButtonsPolicy", "asNeeded")
    }
}

/**
 * Standalone window and its terminal tab controller.
 */
internal data class LatticeWindow(
    val frame: JFrame,
    val tabManager: LatticeTabManager,
)
