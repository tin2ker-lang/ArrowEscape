package com.example.arrowescape.game

import kotlin.random.Random

/**
 * Builds a level by packing the grid with random winding snakes, then making it
 * solvable: snakes are removed greedily (removal only ever frees space, so greedy
 * order doesn't matter). Any snake left stuck is flipped to point the other way,
 * or else split in two (tiny ones are deleted). The result always has a solution.
 *
 * The same level number always produces the same puzzle.
 */
object LevelGenerator {

    fun sizeFor(number: Int): Pair<Int, Int> {
        val w = (5 + number / 3).coerceAtMost(20)
        val h = (w * 1.45f).toInt()
        return w to h
    }

    fun generate(number: Int): Level {
        val rnd = Random(number * 7919L + 17L)
        val (w, h) = sizeFor(number)
        val maxLen = (4 + number / 4).coerceAtMost(16)

        // 1. Pack the grid with random walks.
        val owner = Array(h) { IntArray(w) { -1 } }
        val paths = mutableListOf<List<Cell>>()
        for (idx in (0 until w * h).shuffled(rnd)) {
            val start = Cell(idx % w, idx / w)
            if (owner[start.y][start.x] != -1) continue
            val target = rnd.nextInt(2, maxLen + 1)
            val path = mutableListOf(start)
            var dir = Dir.entries.random(rnd)
            while (path.size < target) {
                val last = path.last()
                val options = Dir.entries.filter { d ->
                    val c = last + d
                    c.inside(w, h) && owner[c.y][c.x] == -1 && c !in path
                }
                if (options.isEmpty()) break
                // Keep going straight sometimes, otherwise turn: gives the zig-zag look.
                dir = if (dir in options && rnd.nextFloat() < 0.45f) dir else options.random(rnd)
                path += last + dir
            }
            if (path.size < 2) continue
            val id = paths.size
            path.forEach { owner[it.y][it.x] = id }
            paths += path
        }

        // 2. Make it solvable.
        val current = HashMap<Int, Snake>()
        paths.forEachIndexed { i, p -> current[i] = Snake(i, p) }
        val occ = Board.occupancy(current.values)
        val remaining = current.keys.toMutableSet()

        fun removeFromBoard(id: Int) {
            current[id]!!.cells.forEach { occ.remove(it) }
            remaining.remove(id)
        }

        while (true) {
            var progress = true
            while (progress) {
                progress = false
                for (id in remaining.toList()) {
                    if (Board.isClear(current[id]!!, occ, w, h)) {
                        removeFromBoard(id)
                        progress = true
                    }
                }
            }
            if (remaining.isEmpty()) break

            val stuck = remaining.shuffled(rnd)
            // Flipping a snake keeps the same cells, so earlier removals stay valid.
            val flippable = stuck.firstOrNull { Board.isClear(current[it]!!.reversed(), occ, w, h) }
            if (flippable != null) {
                current[flippable] = current[flippable]!!.reversed()
            } else {
                // Split a stuck snake in two (each half gets a fresh chance), or drop a tiny one.
                val victim = stuck.first()
                val cells = current[victim]!!.cells
                if (cells.size >= 4) {
                    val mid = cells.size / 2
                    val newId = current.keys.max() + 1
                    current[victim] = Snake(victim, cells.subList(0, mid).toList())
                    current[newId] = Snake(newId, cells.subList(mid, cells.size).toList())
                    cells.subList(mid, cells.size).forEach { occ[it] = newId }
                    remaining.add(newId)
                } else {
                    removeFromBoard(victim)
                    current.remove(victim)
                }
            }
        }

        val snakes = current.values
            .sortedBy { it.id }
            .mapIndexed { i, s -> s.copy(id = i) }
        return Level(number, w, h, snakes)
    }
}
