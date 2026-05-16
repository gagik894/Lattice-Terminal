package com.gagik.terminal.render.cache

import com.gagik.terminal.render.api.*

/**
 * Caller-owned primitive cache for render frames.
 *
 * This cache consumes [TerminalRenderFrameReader] and stores copied primitive
 * row data after the frame callback returns. It deliberately contains no
 * backend-specific glyph runs, font state, paint objects, selection model, or UI
 * timer logic. Swing, Compose, and other renderers can build their local layout
 * and paint caches from this primitive data.
 *
 * @param columns initial cache width in cells.
 * @param rows initial cache height in rows.
 */
class TerminalRenderCache(
    columns: Int,
    rows: Int,
) : TerminalRenderFrameConsumer {
    /**
     * Cached visible width in cells.
     */
    var columns: Int = columns
        private set

    /**
     * Cached visible height in rows.
     */
    var rows: Int = rows
        private set

    /**
     * Retained history lines reported by the most recent frame.
     */
    var historySize: Int = 0
        private set

    /**
     * Resolved scrollback offset copied by the most recent frame.
     */
    var scrollbackOffset: Int = 0
        private set

    /**
     * Copied row code words. See [TerminalRenderFrame.copyLine].
     */
    var codeWords: Array<IntArray> = EMPTY_INT_ROWS
        private set

    /**
     * Copied primary public render attribute words.
     */
    var attrWords: Array<LongArray> = EMPTY_LONG_ROWS
        private set

    /**
     * Copied public render cell flags.
     */
    var flags: Array<IntArray> = EMPTY_INT_ROWS
        private set

    /**
     * Copied optional public extra-attribute words.
     */
    var extraAttrWords: Array<LongArray> = EMPTY_LONG_ROWS
        private set

    /**
     * Copied optional hyperlink identifiers. Zero means no hyperlink.
     */
    var hyperlinkIds: Array<IntArray> = EMPTY_INT_ROWS
        private set

    /**
     * Copied cluster references by row and column. Zero means no cluster; other
     * values pack a codepoint offset in the high 32 bits and a codepoint length
     * in the low 32 bits.
     */
    var clusterRefs: Array<LongArray> = EMPTY_LONG_ROWS
        private set

    /**
     * Packed codepoint storage referenced by [clusterRefs].
     */
    var clusterCodepoints: IntArray = IntArray(0)
        private set

    /**
     * Cached per-row render generations.
     */
    var lineGenerations: LongArray = LongArray(0)
        private set

    /**
     * Cached per-row soft-wrap flags.
     */
    var lineWrapped: BooleanArray = BooleanArray(0)
        private set

    /**
     * Last copied frame generation.
     */
    var frameGeneration: Long = UNINITIALIZED_GENERATION
        private set

    /**
     * Last copied structure generation.
     */
    var structureGeneration: Long = UNINITIALIZED_GENERATION
        private set

    /**
     * Last copied active buffer kind.
     */
    var activeBuffer: TerminalRenderBufferKind = TerminalRenderBufferKind.PRIMARY
        private set

    /**
     * Last copied cursor state.
     */
    var cursor: TerminalRenderCursor? = null
        private set

    /**
     * Whether the most recent [updateFrom] call resized the primitive storage.
     */
    var resizedOnLastUpdate: Boolean = false
        private set

    /**
     * Whether the most recent [updateFrom] call changed the cursor state.
     */
    var cursorChangedOnLastUpdate: Boolean = false
        private set

    private var clusterCodepointCount: Int = 0
    private var nextClusterCodepoints: IntArray = IntArray(INITIAL_CLUSTER_CODEPOINT_CAPACITY)
    private var nextClusterCodepointCount: Int = 0
    private var clusterSinkRow: Int = NO_CLUSTER_SINK_ROW
    private var clusterSinkRefs: Array<LongArray> = EMPTY_LONG_ROWS

    /**
     * Reused sink to avoid allocating one capturing lambda per copied row.
     *
     * [clusterSinkRow] is set immediately before [TerminalRenderFrame.copyLine]
     * and reset immediately after it. This object must not escape this cache.
     */
    private val reusableClusterDataSink = TerminalRenderClusterDataSink { column, codepoints, offset, length ->
        val row = clusterSinkRow
        val copiedRefs = clusterSinkRefs
        check(row != NO_CLUSTER_SINK_ROW) {
            "TerminalRenderClusterDataSink invoked outside copyLine"
        }
        check(row in copiedRefs.indices) {
            "Cluster sink row out of bounds: row=$row, rows=${copiedRefs.size}"
        }
        val refRow = copiedRefs[row]
        check(column in refRow.indices) {
            "Cluster sink column out of bounds: column=$column, columns=${refRow.size}"
        }

        refRow[column] = appendNextCluster(codepoints, offset, length)
    }

    init {
        require(columns > 0) { "columns must be > 0, was $columns" }
        require(rows > 0) { "rows must be > 0, was $rows" }
        resizeStorage(columns, rows)
    }


    /**
     * Copies changed rows and cursor state from [reader].
     *
     * The read callback is used only to copy primitive frame data into this
     * cache. After this method returns, renderers should build glyph or text
     * runs from the copied rows and paint from their own backend-specific state.
     *
     * @param reader source of the short-lived render frame.
     */
    fun updateFrom(reader: TerminalRenderFrameReader) {
        updateFrom(reader, scrollbackOffset = 0)
    }

    /**
     * Copies changed rows and cursor state for a caller-owned scrollback
     * viewport.
     *
     * [scrollbackOffset] is requested in lines from the live bottom viewport.
     * The reader clamps it to available history; this cache stores the resolved
     * value reported by the frame and treats changes to it as row-mapping
     * changes even when line generations happen to match.
     *
     * @param reader source of the short-lived render frame.
     * @param scrollbackOffset requested lines above the live bottom viewport.
     */
    fun updateFrom(reader: TerminalRenderFrameReader, scrollbackOffset: Int) {
        updateFrom(reader, scrollbackOffset, viewportRows = 0)
    }

    /**
     * Copies changed rows and cursor state for a caller-owned scrollback
     * viewport with optional render-only overscan rows.
     *
     * A [viewportRows] value greater than zero asks the reader for that many
     * rows without resizing terminal state. Readers clamp the resolved count
     * before exposing [TerminalRenderFrame.rows], and this cache resizes only to
     * the resolved frame shape.
     *
     * @param reader source of the short-lived render frame.
     * @param scrollbackOffset requested lines above the live bottom viewport.
     * @param viewportRows requested render rows, or zero for the reader default.
     */
    fun updateFrom(reader: TerminalRenderFrameReader, scrollbackOffset: Int, viewportRows: Int) {
        if (viewportRows > 0) {
            reader.readRenderFrame(scrollbackOffset, viewportRows, this)
        } else {
            reader.readRenderFrame(scrollbackOffset, this)
        }
    }

    override fun accept(frame: TerminalRenderFrame) {
        resizedOnLastUpdate = false

        if (columns != frame.columns || rows != frame.rows) {
                resizeStorage(frame.columns, frame.rows)
                resizedOnLastUpdate = true
                structureGeneration = UNINITIALIZED_GENERATION
            }

            cursorChangedOnLastUpdate = false

            val viewportChanged = this.scrollbackOffset != frame.scrollbackOffset
            val structureChanged = structureGeneration != frame.structureGeneration || viewportChanged
            if (structureChanged) {
                clearAllClusters()
            }
            beginClusterCopy()

            var row = 0
            while (row < frame.rows) {
                val lineGeneration = frame.lineGeneration(row)
                val wrapped = frame.lineWrapped(row)

                if (
                    structureChanged ||
                    lineGenerations[row] != lineGeneration ||
                    lineWrapped[row] != wrapped
                ) {
                    clearClusterRow(row)

                    clusterSinkRow = row
                    clusterSinkRefs = clusterRefs
                    try {
                        frame.copyLine(
                            row = row,
                            codeWords = codeWords[row],
                            attrWords = attrWords[row],
                            flags = flags[row],
                            extraAttrWords = extraAttrWords[row],
                            hyperlinkIds = hyperlinkIds[row],
                            clusterDataSink = reusableClusterDataSink,
                        )
                    } finally {
                        clusterSinkRow = NO_CLUSTER_SINK_ROW
                        clusterSinkRefs = EMPTY_LONG_ROWS
                    }

                    lineGenerations[row] = lineGeneration
                    lineWrapped[row] = wrapped
                } else {
                    preserveClusterRow(row)
                }

                row++
            }
            finishClusterCopy()

            activeBuffer = frame.activeBuffer

            val oldCursor = cursor
            val newCursor = frame.cursor
            if (oldCursor != newCursor) {
                cursorChangedOnLastUpdate = true
            }

        cursor = newCursor
        historySize = frame.historySize
        this.scrollbackOffset = frame.scrollbackOffset
        frameGeneration = frame.frameGeneration
        structureGeneration = frame.structureGeneration
    }

    private fun resizeStorage(newColumns: Int, newRows: Int) {
        require(newColumns > 0) { "columns must be > 0, was $newColumns" }
        require(newRows > 0) { "rows must be > 0, was $newRows" }

        columns = newColumns
        rows = newRows

        codeWords = Array(newRows) { IntArray(newColumns) }
        attrWords = Array(newRows) { LongArray(newColumns) }
        flags = Array(newRows) { IntArray(newColumns) }
        extraAttrWords = Array(newRows) { LongArray(newColumns) }
        hyperlinkIds = Array(newRows) { IntArray(newColumns) }
        clusterRefs = Array(newRows) { LongArray(newColumns) }
        clusterCodepoints = IntArray(0)
        clusterCodepointCount = 0
        nextClusterCodepointCount = 0

        lineGenerations = LongArray(newRows) { UNINITIALIZED_GENERATION }
        lineWrapped = BooleanArray(newRows)

        cursor = null
        historySize = 0
        scrollbackOffset = 0
        frameGeneration = UNINITIALIZED_GENERATION
        structureGeneration = UNINITIALIZED_GENERATION
        activeBuffer = TerminalRenderBufferKind.PRIMARY
        cursorChangedOnLastUpdate = false
        clusterSinkRow = NO_CLUSTER_SINK_ROW
        clusterSinkRefs = EMPTY_LONG_ROWS
    }

    private fun clearAllClusters() {
        var row = 0
        while (row < rows) {
            clearClusterRow(row)
            row++
        }
    }

    private fun clearClusterRow(row: Int) {
        clusterRefs[row].fill(NO_CLUSTER_REF)
    }

    private fun beginClusterCopy() {
        nextClusterCodepointCount = 0
    }

    private fun finishClusterCopy() {
        val previous = clusterCodepoints
        clusterCodepoints = nextClusterCodepoints
        clusterCodepointCount = nextClusterCodepointCount
        nextClusterCodepoints = previous
        nextClusterCodepointCount = 0
    }

    private fun preserveClusterRow(row: Int) {
        val refs = clusterRefs[row]
        var column = 0
        while (column < refs.size) {
            val ref = refs[column]
            if (ref != NO_CLUSTER_REF) {
                val offset = clusterOffset(ref)
                val length = clusterLength(ref)
                refs[column] = appendNextCluster(clusterCodepoints, offset, length)
            }
            column++
        }
    }

    private fun appendNextCluster(codepoints: IntArray, offset: Int, length: Int): Long {
        require(length > 0) { "cluster length must be > 0, was $length" }
        require(offset >= 0 && codepoints.size - offset >= length) {
            "cluster source has insufficient capacity: size=${codepoints.size}, offset=$offset, length=$length"
        }
        ensureNextClusterCapacity(nextClusterCodepointCount + length)
        val nextOffset = nextClusterCodepointCount
        System.arraycopy(codepoints, offset, nextClusterCodepoints, nextOffset, length)
        nextClusterCodepointCount += length
        return packClusterRef(nextOffset, length)
    }

    private fun ensureNextClusterCapacity(required: Int) {
        if (required <= nextClusterCodepoints.size) return

        var newCapacity = maxOf(INITIAL_CLUSTER_CODEPOINT_CAPACITY, nextClusterCodepoints.size * 2)
        while (newCapacity < required) {
            newCapacity *= 2
        }
        nextClusterCodepoints = nextClusterCodepoints.copyOf(newCapacity)
    }

    /**
     * Returns cluster text for diagnostics or compatibility callers.
     *
     * Rendering should consume [clusterRefs] and [clusterCodepoints] directly so
     * repeated paint passes do not allocate strings for cache hits.
     */
    fun clusterText(row: Int, column: Int): String? {
        val ref = clusterRefs[row][column]
        if (ref == NO_CLUSTER_REF) return null
        return String(clusterCodepoints, clusterOffset(ref), clusterLength(ref))
    }

    /**
     * Returns the codepoint offset encoded in [ref].
     */
    fun clusterOffset(ref: Long): Int = (ref ushr 32).toInt()

    /**
     * Returns the codepoint length encoded in [ref].
     */
    fun clusterLength(ref: Long): Int = (ref and 0xFFFF_FFFFL).toInt()

    private companion object {
        private const val UNINITIALIZED_GENERATION = -1L
        private const val NO_CLUSTER_SINK_ROW = -1
        private const val NO_CLUSTER_REF = 0L
        private const val INITIAL_CLUSTER_CODEPOINT_CAPACITY = 256

        private val EMPTY_INT_ROWS: Array<IntArray> = emptyArray()

        private val EMPTY_LONG_ROWS: Array<LongArray> = emptyArray()

        private fun packClusterRef(offset: Int, length: Int): Long {
            return (offset.toLong() shl 32) or (length.toLong() and 0xFFFF_FFFFL)
        }

    }
}
