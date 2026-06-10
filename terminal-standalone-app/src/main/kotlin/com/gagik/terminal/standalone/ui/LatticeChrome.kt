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

import java.awt.Color
import java.awt.Dimension

/**
 * Visual constants for the standalone Swing host.
 *
 * All colours follow an 80% dark / 20% accent palette, matching the feel
 * of modern terminal applications such as Windows Terminal and VS Code.
 */
internal object LatticeChrome {
    const val APP_TITLE = "Lattice"

    // ── Surfaces ──────────────────────────────────────────────────────────────

    /** Root window background and deep surface. */
    val SURFACE: Color = Color(0x0D0D0D)

    /** Title bar and tab strip background. */
    val TOP_BAR_BACKGROUND: Color = Color(0x252526)

    /** Active terminal content background. Intentionally darker than the bar
     *  so that the selected tab creates a seamless visual connection. */
    val TERMINAL_BACKGROUND: Color = Color(0x0D0D0D)

    // ── Tab states ────────────────────────────────────────────────────────────

    /** Background of the selected tab — matches [TERMINAL_BACKGROUND] so the
     *  tab blends into the content pane below it. */
    val TAB_SELECTED_BG: Color = Color(0x0D0D0D)

    /** Background shown when hovering over an unselected tab. */
    val TAB_HOVER_BG: Color = Color(0x2F2F2F)

    // ── Controls ──────────────────────────────────────────────────────────────

    val POPUP_BACKGROUND: Color = Color(0x2B2B2B)
    val CONTROL_BACKGROUND: Color = Color(0x3C3C3C)
    val CONTROL_HOVER: Color = Color(0x4A4A4A)
    val CONTROL_PRESSED: Color = Color(0x555555)

    // ── Text ──────────────────────────────────────────────────────────────────

    /** Primary text — used for selected tab labels and active UI elements. */
    val TEXT_PRIMARY: Color = Color(0xE8E8E8)

    /** Secondary text — used for unselected tab labels. */
    val TEXT_SECONDARY: Color = Color(0x8A8A8A)

    // ── Accent ────────────────────────────────────────────────────────────────

    /** Brand accent — used as the selected-tab indicator line. */
    val ACCENT: Color = Color(0x4DA3FF)

    // ── Borders ───────────────────────────────────────────────────────────────

    val BORDER: Color = Color(0x3A3A3A)

    // ── Scrollbar ─────────────────────────────────────────────────────────────

    val SCROLLBAR_TRACK: Color = Color(0x0D0D0D)
    val SCROLLBAR_THUMB: Color = Color(0x555555)
    val SCROLLBAR_THUMB_HOVER: Color = Color(0x707070)
    val SCROLLBAR_THUMB_PRESSED: Color = Color(0x909090)
    val SCROLLBAR_SIZE: Dimension = Dimension(10, 1)
}
