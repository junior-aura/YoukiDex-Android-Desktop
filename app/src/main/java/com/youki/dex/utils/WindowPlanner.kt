package com.youki.dex.utils

import android.graphics.Rect

/**
 * ClauDEX "smart" launch mode: decides where a NEW window goes from what is
 * already on screen.
 *
 *   - nothing open            -> fill the available area (maximized);
 *   - something ~maximized    -> the regular "standard" floating window,
 *                                and the user arranges things;
 *   - other floating windows  -> the largest free rectangle, if it is big
 *                                enough to be useful (e.g. one window on the
 *                                left half -> the new one takes the right half).
 *
 * Pure geometry on purpose: the caller supplies the available area and the
 * bounds of the app windows on screen (PerfectServer reads them through the
 * accessibility service), so this is easy to reason about in isolation.
 */
object WindowPlanner {

    /** [bounds] null means "use the regular standard window". */
    data class Plan(val kind: Kind, val bounds: Rect?)

    enum class Kind { MAXIMIZED, STANDARD, FREE_SPACE }

    /** A window covering at least this share of the area counts as maximized. */
    private const val MAXIMIZED_SHARE_PCT = 90
    /** A free rectangle is only worth using above these fractions of the area. */
    private const val MIN_FREE_WIDTH_PCT = 35
    private const val MIN_FREE_HEIGHT_PCT = 50

    fun plan(area: Rect, windows: List<Rect>): Plan {
        if (area.isEmpty) return Plan(Kind.STANDARD, null)

        // only the part of each window that falls inside the area matters
        val inside = windows.mapNotNull { w -> Rect(w).takeIf { it.intersect(area) } }
        if (inside.isEmpty()) return Plan(Kind.MAXIMIZED, Rect(area))

        val areaSize = size(area)
        if (inside.any { size(it) * 100 >= areaSize * MAXIMIZED_SHARE_PCT })
            return Plan(Kind.STANDARD, null)

        val free = largestFreeRect(area, inside)
        if (free != null &&
            free.width() * 100 >= area.width() * MIN_FREE_WIDTH_PCT &&
            free.height() * 100 >= area.height() * MIN_FREE_HEIGHT_PCT
        ) return Plan(Kind.FREE_SPACE, free)

        return Plan(Kind.STANDARD, null)
    }

    /**
     * Largest axis-aligned rectangle inside [area] that overlaps none of the
     * [obstacles]. Candidate edges are the area's and the obstacles' edges;
     * the optimum always lies on them. O(n^4 * n) for n windows, which is
     * nothing for the handful of windows a phone or tablet shows.
     */
    fun largestFreeRect(area: Rect, obstacles: List<Rect>): Rect? {
        val xs = (listOf(area.left, area.right) + obstacles.flatMap { listOf(it.left, it.right) })
            .filter { it in area.left..area.right }.distinct().sorted()
        val ys = (listOf(area.top, area.bottom) + obstacles.flatMap { listOf(it.top, it.bottom) })
            .filter { it in area.top..area.bottom }.distinct().sorted()

        var best: Rect? = null
        var bestSize = 0L
        for (i in xs.indices) for (j in i + 1 until xs.size) {
            for (k in ys.indices) for (l in k + 1 until ys.size) {
                val r = Rect(xs[i], ys[k], xs[j], ys[l])
                val s = size(r)
                if (s <= bestSize) continue
                // Rect.intersects is strict: windows that only touch an edge are fine
                if (obstacles.none { Rect.intersects(it, r) }) {
                    best = r
                    bestSize = s
                }
            }
        }
        return best
    }

    private fun size(r: Rect): Long = r.width().toLong() * r.height().toLong()
}
