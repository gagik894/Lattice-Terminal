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
import com.gagik.terminal.workspace.TerminalWorkspace
import com.gagik.terminal.workspace.TerminalWorkspaceListener
import com.gagik.terminal.workspace.TerminalWorkspaceOpenOptions
import com.gagik.terminal.workspace.TerminalWorkspaceTab
import java.awt.CardLayout
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities

/**
 * Owns standalone terminal tabs and tab-scoped session lifecycle.
 *
 * The manager is intentionally host-side. It creates and closes PTY-backed
 * panes, updates Swing tab metadata, and coordinates settings reloads across
 * live panes without leaking this policy into reusable UI components.
 */
internal class LatticeTabManager(
    private val frame: JFrame,
    private val tabPane: JTabbedPane,
    private val tabContentPanel: JPanel,
    private val settings: StandaloneTerminalSettings,
) {
    private val tabs = ArrayList<LatticeTerminalPane>(INITIAL_TAB_CAPACITY)
    private val workspace = TerminalWorkspace(StandaloneWorkspaceListener())

    init {
        tabPane.addChangeListener {
            selectedPane?.let { pane ->
                if (workspace.selectedTab()?.id != pane.tab.id) {
                    workspace.selectTab(pane.tab.id)
                }
                (tabContentPanel.layout as CardLayout).show(tabContentPanel, pane.tab.id)
            }
            updateFrameTitleFromSelection()
            selectedPane?.requestFocus()
        }
    }

    val selectedPane: LatticeTerminalPane?
        get() {
            val selectedIndex = tabPane.selectedIndex
            return if (selectedIndex in tabs.indices) tabs[selectedIndex] else null
        }

    fun openTab(profile: TerminalProfile): Boolean {
        val workspaceTab =
            try {
                workspace.openTab(
                    profile = profile,
                    options =
                        settings.current().let { snapshot ->
                            TerminalWorkspaceOpenOptions(
                                columns = snapshot.columns,
                                rows = snapshot.rows,
                                treatAmbiguousAsWide = snapshot.treatAmbiguousAsWide,
                            )
                        },
                )
            } catch (exception: Exception) {
                showStartError(profile, exception)
                return false
            }

        val pane = LatticeTerminalPane.create(workspaceTab, settings)
        tabs += pane
        tabPane.addTab(profile.displayName, null)
        tabContentPanel.add(pane.component, pane.tab.id)
        tabPane.setTabComponentAt(
            tabs.lastIndex,
            LatticeTabComponent(profile.displayName) {
                closePane(pane)
            },
        )
        tabPane.selectedIndex = tabs.lastIndex
        frame.rootPane.revalidate()
        frame.rootPane.repaint()
        updateFrameTitleFromSelection()
        pane.requestFocus()
        return true
    }

    fun closeSelectedTab() {
        val selectedIndex = tabPane.selectedIndex
        if (selectedIndex !in tabs.indices) return
        closeTabAt(selectedIndex)
    }

    fun closeAllTabs() {
        while (tabs.isNotEmpty()) {
            closeTabAt(tabs.lastIndex)
        }
        workspace.close()
    }

    fun reloadAllPanes() {
        val snapshot = settings.current()
        var index = 0
        while (index < tabs.size) {
            tabs[index].reloadSettings()
            index++
        }
        workspace.applySettings(
            palette = snapshot.palette,
            treatAmbiguousAsWide = snapshot.treatAmbiguousAsWide,
        )
    }

    private fun closeTabAt(index: Int) {
        val pane = tabs.removeAt(index)
        tabPane.removeTabAt(index)
        tabContentPanel.remove(pane.component)
        pane.close()
        workspace.closeTab(pane.tab.id)
        frame.rootPane.revalidate()
        frame.rootPane.repaint()
        updateFrameTitleFromSelection()
        selectedPane?.requestFocus()
    }

    private fun closePane(pane: LatticeTerminalPane) {
        val index = tabs.indexOf(pane)
        if (index >= 0) closeTabAt(index)
    }

    private fun updateTabTitle(
        tabId: String,
        title: String,
    ) {
        var index = 0
        while (index < tabs.size) {
            if (tabs[index].tab.id == tabId) {
                tabPane.setTitleAt(index, title)
                (tabPane.getTabComponentAt(index) as? LatticeTabComponent)?.title = title
                frame.rootPane.revalidate()
                frame.rootPane.repaint()
                if (index == tabPane.selectedIndex) {
                    frame.title = title
                }
                return
            }
            index++
        }
    }

    private fun updateFrameTitleFromSelection() {
        val index = tabPane.selectedIndex
        frame.title =
            if (index in 0 until tabPane.tabCount) {
                tabPane.getTitleAt(index)
            } else {
                LatticeChrome.APP_TITLE
            }
    }

    private fun showStartError(
        profile: TerminalProfile,
        exception: Exception,
    ) {
        JOptionPane.showMessageDialog(
            frame,
            exception.message ?: exception.javaClass.name,
            "Unable to start ${profile.displayName}",
            JOptionPane.ERROR_MESSAGE,
        )
    }

    private inner class StandaloneWorkspaceListener : TerminalWorkspaceListener {
        override fun bell(tab: TerminalWorkspaceTab) {
            SwingUtilities.invokeLater {
                frame.toolkit.beep()
            }
        }

        override fun titleChanged(
            tab: TerminalWorkspaceTab,
            title: String,
        ) {
            SwingUtilities.invokeLater {
                updateTabTitle(tab.id, title)
            }
        }
    }

    private companion object {
        private const val INITIAL_TAB_CAPACITY = 4
    }
}
