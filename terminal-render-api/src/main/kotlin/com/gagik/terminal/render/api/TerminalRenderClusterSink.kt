package com.gagik.terminal.render.api

/**
 * Receives grapheme cluster text while a row is copied.
 */
fun interface TerminalRenderClusterSink {
    /**
     * Called during `copyLine()` for a [TerminalRenderCellFlags.CLUSTER] cell.
     *
     * [column] is the visual column of the cluster-leading cell. [text] is the
     * full Unicode grapheme cluster. The text is only guaranteed to be valid for
     * the duration of the surrounding render frame callback unless an
     * implementation documents a longer lifetime.
     *
     * @param column zero-based visual column of the cluster-leading cell.
     * @param text full Unicode grapheme cluster text.
     */
    fun onCluster(column: Int, text: String)
}

/**
 * Receives grapheme cluster code points while a row is copied.
 *
 * This is the allocation-conscious cluster handoff for render caches. The
 * supplied [codepoints] range is valid only for the duration of the callback;
 * receivers that retain it must copy the primitive range into their own
 * storage before returning.
 */
fun interface TerminalRenderClusterDataSink {
    /**
     * Called during `copyLine()` for a [TerminalRenderCellFlags.CLUSTER] cell.
     *
     * @param column zero-based visual column of the cluster-leading cell.
     * @param codepoints source code point buffer.
     * @param offset first code point in [codepoints].
     * @param length number of code points in the cluster.
     */
    fun onCluster(column: Int, codepoints: IntArray, offset: Int, length: Int)
}
