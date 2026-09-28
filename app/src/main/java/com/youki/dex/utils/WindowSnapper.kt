package com.youki.dex.utils

import android.graphics.Rect

/**
 * ClauDEX snap-on-drop: where a window the user just dragged should land.
 *
 * Input is the drag's EXTREMES, not where the window ended: measured on a
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
     * [extremes] = (min left, min top, max right, max bottom) the dragged
     * window reached; [screen] = display bounds; an edge counts as pushed when
     * the window went [edgePx] past it (top: past the area's top).
     */
    fun zoneFor(extremes: Rect, screen: Rect, area: Rect, edgePx: Int): Zone {
        val left = extremes.left < screen.left - edgePx
        val right = extremes.right > screen.right + edgePx
        val top = extremes.top < area.top - edgePx
        val bottom = extremes.bottom > screen.bottom + edgePx
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

    /** Rectangle for [zone] inside [area]; null for NONE. */
    fun boundsFor(zone: Zone, area: Rect): Rect? {
        val midX = area.left + area.width() / 2
        val midY = area.top + area.height() / 2
        return when (zone) {
            Zone.NONE -> null
            Zone.EXPAND -> Rect(area)
            Zone.LEFT -> Rect(area.left, area.top, midX, area.bottom)
            Zone.RIGHT -> Rect(midX, area.top, area.right, area.bottom)
            Zone.BOTTOM_LEFT -> Rect(area.left, midY, midX, area.bottom)
            Zone.BOTTOM_RIGHT -> Rect(midX, midY, area.right, area.bottom)
        }
    }

    /**
     * Where a neighbour gives way: a window covering most of the half the
     * dragged one took (typically an EXPANDED one) moves to the other half.
     * Null when the zone is not a side half.
     */
    fun complementFor(zone: Zone, area: Rect): Rect? = when (zone) {
        Zone.LEFT -> boundsFor(Zone.RIGHT, area)
        Zone.RIGHT -> boundsFor(Zone.LEFT, area)
        else -> null
    }

    /** [window] covers at least half of [target]. */
    fun occupies(window: Rect, target: Rect): Boolean {
        val i = Rect(window)
        if (!i.intersect(target)) return false
        return i.width().toLong() * i.height() * 2 >= target.width().toLong() * target.height()
    }
}
