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
 * here - the caller checks that the task is still freeform.
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
    fun zoneFor(fingers: Rect, screen: Rect, area: Rect, edgeX: Int, edgeY: Int): Zone {
        val left = fingers.left < screen.left + edgeX
        val right = fingers.right > area.right - edgeX
        val top = fingers.top < screen.top + edgeY
        val bottom = fingers.bottom > screen.bottom - 2 * edgeY
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

    /** [window] covers at least half of [target]. */
    fun occupies(window: Rect, target: Rect): Boolean {
        val i = Rect(window)
        if (!i.intersect(target)) return false
        return i.width().toLong() * i.height() * 2 >= target.width().toLong() * target.height()
    }
}
