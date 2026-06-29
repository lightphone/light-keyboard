package com.thelightphone.lp3Keyboard.ui

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import com.thelightphone.lp3Keyboard.ui.layout.SwipeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

/**
 * Lets external code observe where letter keys land on screen,
 * so it can build a normalized [letters, cx, cy] layout for a swipe decoder
 * without hardcoding key sizes.
 */
abstract class Lp3KeyboardLayoutCapture(val letters: String) : SwipeConfig {
    protected val letterBounds = mutableStateMapOf<Int, Rect>()

    override val boundsFlow: Flow<Int>
        get() = snapshotFlow { letterBounds.size }.filter { it >= letters.length }

    /**
     * Build [letters, cx, cy] normalized to the bounding box of all letter
     * key rectangles. Returns null until every char in [letters] has reported
     * a position. The same bounding box should normalize live swipe touch coordinates
     * before they reach SwipeDecoder.recognize().
     */
    override fun deriveLayout(): Triple<String, FloatArray, FloatArray>? {
        val n = letters.length
        val rectangles = Array(n) { letterBounds[letters[it].code] ?: return null }
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (r in rectangles) {
            if (r.left < minX) minX = r.left
            if (r.right > maxX) maxX = r.right
            if (r.top < minY) minY = r.top
            if (r.bottom > maxY) maxY = r.bottom
        }
        val w = (maxX - minX).coerceAtLeast(1f)
        val h = (maxY - minY).coerceAtLeast(1f)
        val cx = FloatArray(n)
        val cy = FloatArray(n)
        for (i in 0 until n) {
            val r = rectangles[i]
            cx[i] = ((r.left + r.right) / 2f - minX) / w
            cy[i] = ((r.top + r.bottom) / 2f - minY) / h
        }
        return Triple(letters, cx, cy)
    }

    /**
     * Root-relative rectangle enclosing every letter key. Same coordinate space as
     * Compose's onGloballyPositioned/boundsInRoot.
     * Null until all letters have reported.
     */
    override fun letterBoundsRect(): Rect? {
        val keyRectangles = letters.map { letterBounds[it.code] ?: return null }
        return Rect(
            left = keyRectangles.minOf { it.left },
            top = keyRectangles.minOf { it.top },
            right = keyRectangles.maxOf { it.right },
            bottom = keyRectangles.maxOf { it.bottom }
        )
    }
}