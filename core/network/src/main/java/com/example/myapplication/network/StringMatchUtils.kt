package com.example.myapplication.network

/**
 * Levenshtein edit-distance for fuzzy title matching.
 */
internal fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    var prev = IntArray(a.length + 1) { it }
    for (j in 1..b.length) {
        val curr = IntArray(a.length + 1).apply {
            set(0, j)
            for (i in 1..a.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                set(i, minOf(prev[i - 1] + cost, prev[i] + 1, get(i - 1) + 1))
            }
        }
        prev = curr
    }
    return prev[a.length]
}

/**
 * Returns true when two titles are "close enough" to be considered duplicates.
 */
internal fun isTitleSimilar(a: String, b: String): Boolean {
    val q = a.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
    if (q.isEmpty()) return false
    if (b.isBlank()) return false
    val normalized = b.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
    if (normalized.isEmpty()) return false
    if (normalized.contains(q) || q.contains(normalized)) return true
    val dist = levenshtein(q, normalized)
    val maxLen = maxOf(q.length, normalized.length)
    return (1.0 - dist.toDouble() / maxLen) > 0.7
}

/**
 * True if [query] is similar to any non-blank entry in [targets].
 */
internal fun isTitleSimilar(query: String, vararg targets: String?): Boolean {
    for (t in targets) {
        if (t.isNullOrBlank()) continue
        if (isTitleSimilar(query, t)) return true
    }
    return false
}
