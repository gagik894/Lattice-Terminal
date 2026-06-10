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
import javax.swing.Box
import javax.swing.JFrame
import javax.swing.JMenuBar
import javax.swing.JPanel
import javax.swing.WindowConstants
import javax.swing.border.EmptyBorder

/**
 * Creates and wires the standalone terminal window.
 *
 * FlatLaf merges [JMenuBar] into the custom title bar when
 * `useWindowDecorations=true` and `JFrame.setDefaultLookAndFeelDecorated(true)`
 * are active. We place [LatticeTabBar] at the left of the menu bar and the
 * action buttons at the right, with a horizontal glue in between.
 *
 * ```
 * ┌──────────────────────────────────────────────┐
 * │  [tab1] [tab2] [+]        [≡] [⋯]  [ − □ × ]│  ← JMenuBar (title bar)
 * ├──────────────────────────────────────────────┤
 * │                                              │
 * │            terminal content                  │  ← content pane
 * │                                              │
 * └──────────────────────────────────────────────┘
 * ```
 */
internal class LatticeWindowFactory(
    private val settings: StandaloneTerminalSettings,
    private val profiles: List<TerminalProfile>,
) {
    fun createWindow(): LatticeWindow {
        val frame =
            JFrame(LatticeChrome.APP_TITLE).apply {
                defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
                background = LatticeChrome.SURFACE
                minimumSize = Dimension(720, 420)
            }

        val tabContentPanel =
            JPanel(CardLayout()).apply {
                background = LatticeChrome.TERMINAL_BACKGROUND
                isOpaque = true
            }

        // Tab bar callbacks reference tabManager; forward via lambda so the
        // lateinit is resolved at call time, not at construction time.
        lateinit var tabManager: LatticeTabManager
        val tabBar =
            LatticeTabBar(
                onTabSelected = { id -> tabManager.onTabSelected(id) },
                onTabClose = { id -> tabManager.closeTab(id) },
                onNewTab = { tabManager.openTab(profiles.first()) },
            )

        tabManager = LatticeTabManager(frame, tabBar, tabContentPanel, settings)
        val actionsFactory = LatticeTitleBarActionsFactory(settings, tabManager, profiles)

        installMenuBar(frame, tabBar, actionsFactory)
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
        actionsFactory: LatticeTitleBarActionsFactory,
    ) {
        frame.jMenuBar =
            JMenuBar().apply {
                isOpaque = false
                border = EmptyBorder(0, 0, 0, 0)
                add(tabBar)
                add(Box.createHorizontalGlue())
                add(actionsFactory.trailingPanel)
            }
    }

    /**
     * Applies FlatLaf title bar styling: background colour, no icon, no title
     * text (the tab bar provides context instead).
     */
    private fun styleTitleBar(frame: JFrame) {
        frame.rootPane.apply {
            putClientProperty("JRootPane.titleBarBackground", LatticeChrome.TOP_BAR_BACKGROUND)
            putClientProperty("JRootPane.titleBarForeground", LatticeChrome.TEXT_PRIMARY)
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
