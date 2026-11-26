package com.example.arrowescape.game

enum class Dir(val dx: Int, val dy: Int) {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0)
}

data class Cell(val x: Int, val y: Int) {
    operator fun plus(d: Dir) = Cell(x + d.dx, y + d.dy)
    fun inside(w: Int, h: Int) = x in 0 until w && y in 0 until h
}

/**
 * A snake-shaped arrow. [cells] runs from tail (first) to head (last);
 * the arrowhead points in the direction of the last step.
 */
data class Snake(val id: Int, val cells: List<Cell>) {
    val head: Cell get() = cells.last()

    val dir: Dir
        get() {
            val a = cells[cells.size - 2]
            val b = cells.last()
            return Dir.entries.first { it.dx == b.x - a.x && it.dy == b.y - a.y }
        }

    fun reversed() = copy(cells = cells.reversed())
}

enum class Difficulty(val label: String) {
    EASY("Easy"), MEDIUM("Medium"), HARD("Hard")
}

data class Level(
    val difficulty: Difficulty,
    val number: Int,
    val width: Int,
    val height: Int,
    val snakes: List<Snake>,
)

object Board {
    /** Cells in front of the snake's head, up to the board edge. */
    fun ray(s: Snake, w: Int, h: Int): List<Cell> {
        val out = mutableListOf<Cell>()
        var c = s.head + s.dir
        while (c.inside(w, h)) {
            out += c
            c += s.dir
        }
        return out
    }

    /** True if nothing (including the snake itself) lies between its head and the edge. */
    fun isClear(s: Snake, occupied: Map<Cell, Int>, w: Int, h: Int): Boolean {
        var c = s.head + s.dir
        while (c.inside(w, h)) {
            if (occupied.containsKey(c)) return false
            c += s.dir
        }
        return true
    }

    fun occupancy(snakes: Collection<Snake>): HashMap<Cell, Int> {
        val map = HashMap<Cell, Int>()
        for (s in snakes) for (c in s.cells) map[c] = s.id
        return map
    }
}
