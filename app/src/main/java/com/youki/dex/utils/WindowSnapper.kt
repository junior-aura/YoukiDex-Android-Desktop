package com.youki.dex.utils

import android.graphics.Rect

/**
 * ClauDEX snap-on-drop: where a window the user just dragged should land.
 *
 * Input is the drag's EXTREMES (of the finger, see zoneFor), not where the window ended: measured on a
 * SM-A055M, the system pulls a window back from the edge when it is
 * released (dragged to top 21 -> settled at 82), so the final position does
 * not say which edge the user pushed into. Edges the system itself claims
 * (on One UI, top and bottom turn the window into split screen) never reach
 * here - the caller checks that the task is still freeform. On One UI that
 * is every edge (sides "stash" the window into the edge), so there this only
 * logs; it acts where the system leaves edges free (AOSP-like tablets).
 *
 *   side only          -> half of that side
 *   top only           -> EXPANDED (the whole area)
 *   top + side         -> half of that side ("parallel", the owner's diagonal)
 *   bottom + side      -> quarter at that corner
 *   nothing pushed     -> NONE (stays where it was dropped)
 */
object WindowSnapper {

    enum class Zone { NONE, EXPAND, LEFT, RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    /**
     * Where the FINGER went, not the window: the user holds the caption, which
     * sits centered on the window's top edge, so the finger is about
     * (centerX, top). [fingers] = (min centerX, min top, max centerX, max top)
     * over the drag. Measured on a SM-A055M: holding the caption at mid
     * height, the window body hangs below the screen (bottom 846 on a 720 px
     * screen) - reading the body as "pushed down" picked a bottom quarter for
     * what was a plain drag to the left edge.
     */
    fun zoneFor(
        fingers: Rect, screen: Rect, area: Rect, edgeX: Int, edgeY: Int,
        startX: Int, startY: Int, minTravel: Int
    ): Zone {
        // an edge counts only if the finger also TRAVELLED toward it: measured
        // on a SM-A055M, grabbing the caption of a window already at the top
        // dips the finger a few px, which read as "pushed to the top" and threw
        // a window being dragged sideways back to the center, 3 times in 20 s
        val left = fingers.left < screen.left + edgeX && startX - fingers.left >= minTravel
        val right = fingers.right > area.right - edgeX && fingers.right - startX >= minTravel
        val top = fingers.top < screen.top + edgeY && startY - fingers.top >= minTravel
        val bottom = fingers.bottom > screen.bottom - 2 * edgeY && fingers.bottom - startY >= minTravel
        return when {
            left && right -> Zone.NONE                 // dragged across: ambiguous
            left && bottom && !top -> Zone.BOTTOM_LEFT
            right && bottom && !top -> Zone.BOTTOM_RIGHT
            left -> Zone.LEFT
            right -> Zone.RIGHT
            top -> Zone.EXPAND
            else -> Zone.NONE
        }
    }

    /**
     * Rectangle for [zone] inside [area]; null for NONE. A quarter needs half
     * the area's height to hold [minSide] (the freeform floor, 220 dp): on a
     * 720 px phone it does not - the system grew a 333 px quarter to 413 and it
     * spilled below the screen - so it falls back to that side's half.
     */
    fun boundsFor(zone: Zone, area: Rect, minSide: Int): Rect? {
        val midX = area.left + area.width() / 2
        val midY = area.top + area.height() / 2
        val quarters = area.height() / 2 >= minSide
        return when (zone) {
            Zone.NONE -> null
            Zone.EXPAND -> Rect(area)
            Zone.LEFT -> Rect(area.left, area.top, midX, area.bottom)
            Zone.RIGHT -> Rect(midX, area.top, area.right, area.bottom)
            Zone.BOTTOM_LEFT ->
                if (quarters) Rect(area.left, midY, midX, area.bottom) else boundsFor(Zone.LEFT, area, minSide)
            Zone.BOTTOM_RIGHT ->
                if (quarters) Rect(midX, midY, area.right, area.bottom) else boundsFor(Zone.RIGHT, area, minSide)
        }
    }

    /**
     * Where a neighbour gives way: a window covering most of the half the
     * dragged one took (typically an EXPANDED one) moves to the other half.
     * Null when the zone is not a side half.
     */
    fun complementFor(zone: Zone, area: Rect, minSide: Int): Rect? = when (zone) {
        Zone.LEFT -> boundsFor(Zone.RIGHT, area, minSide)
        Zone.RIGHT -> boundsFor(Zone.LEFT, area, minSide)
        else -> null
    }

    /**
     * "Side by side" for [n] windows inside [area], in order (first = left /
     * top-left). Sized by what fits the freeform floor [minSide]: two rows
     * only if half the height holds it, columns up to width / minSide. On a
     * 1600x720 phone: 2 -> halves, 3 -> three columns; on a tablet 4 -> 2x2.
     * Windows beyond the capacity are left as they are (list is shorter).
     */
    fun tile(area: Rect, n: Int, minSide: Int): List<Rect> {
        if (n <= 0 || area.isEmpty) return emptyList()
        val maxRows = if (area.height() / 2 >= minSide) 2 else 1
        val maxCols = maxOf(1, area.width() / minSide)
        val k = minOf(n, maxRows * maxCols)
        val rows = if (k > maxCols) 2 else 1
        val out = ArrayList<Rect>(k)
        for (r in 0 until rows) {
            // the first row takes the extra window when k is odd
            val inRow = if (rows == 1) k else if (r == 0) (k + 1) / 2 else k / 2
            val top = area.top + area.height() * r / rows
            val bottom = area.top + area.height() * (r + 1) / rows
            for (c in 0 until inRow) {
                val left = area.left + area.width() * c / inRow
                val right = area.left + area.width() * (c + 1) / inRow
                out.add(Rect(left, top, right, bottom))
            }
        }
        return out
    }

    /** [window] covers at least half of [target]. */
    fun occupies(window: Rect, target: Rect): Boolean {
        val i = Rect(window)
        if (!i.intersect(target)) return false
        return i.width().toLong() * i.height() * 2 >= target.width().toLong() * target.height()
    }
}
