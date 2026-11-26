package com.example.arrowescape.ui

import android.content.SharedPreferences
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arrowescape.game.Board
import com.example.arrowescape.game.Cell
import com.example.arrowescape.game.Level
import com.example.arrowescape.game.LevelGenerator
import com.example.arrowescape.game.Snake
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.min

object Palette {
    val background = Color(0xFFF5EBDD)
    val ink = Color(0xFF2B2118)
    val dot = Color(0xFFD9CBB6)
    val red = Color(0xFFD64B3C)
    val hint = Color(0xFFF2A83B)
    val water = Color(0xFF4A9FE0)
    val waterEmpty = Color(0xFFBDB3A6)
    val title = Color(0xFFC8893A)
    val subtitle = Color(0xFF8A4FD0)
}

private const val MAX_LIVES = 3
private const val HINTS_PER_LEVEL = 3

private enum class Status { PLAYING, WON, LOST }

/** A snake in motion: it occupies [length] consecutive points of [path], starting at [progress]. */
private class MoveAnim(val path: List<Cell>, val length: Int) {
    val progress = Animatable(0f)
}

private class Geo(val cell: Float, val origin: Offset) {
    fun px(x: Float, y: Float) = Offset(origin.x + (x + 0.5f) * cell, origin.y + (y + 0.5f) * cell)
}

private fun geometry(w: Float, h: Float, level: Level): Geo {
    val c = min(w / level.width, h / level.height)
    return Geo(c, Offset((w - c * level.width) / 2f, (h - c * level.height) / 2f))
}

@Composable
fun GameScreen(prefs: SharedPreferences) {
    var levelNumber by remember { mutableIntStateOf(prefs.getInt("level", 1)) }
    var attempt by remember { mutableIntStateOf(0) }
    val level = remember(levelNumber) { LevelGenerator.generate(levelNumber) }

    key(levelNumber, attempt) {
        LevelPlay(
            level = level,
            onRestart = { attempt++ },
            onNext = {
                levelNumber++
                prefs.edit().putInt("level", levelNumber).apply()
            },
        )
    }
}

@Composable
private fun LevelPlay(level: Level, onRestart: () -> Unit, onNext: () -> Unit) {
    val scope = rememberCoroutineScope()
    val board = remember { mutableStateListOf<Snake>().apply { addAll(level.snakes) } }
    val anims = remember { mutableStateMapOf<Int, MoveAnim>() }
    val red = remember { mutableStateListOf<Int>() }
    var lives by remember { mutableIntStateOf(MAX_LIVES) }
    var hints by remember { mutableIntStateOf(HINTS_PER_LEVEL) }
    var hintId by remember { mutableStateOf<Int?>(null) }
    var status by remember { mutableStateOf(Status.PLAYING) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    fun tap(s: Snake) {
        if (status != Status.PLAYING || lives <= 0 || anims.containsKey(s.id)) return
        val occ = Board.occupancy(board)
        val ray = Board.ray(s, level.width, level.height)
        val blockAt = ray.indexOfFirst { occ.containsKey(it) }

        if (blockAt == -1) {
            // Clear path: slide out head-first until fully off the board.
            board.remove(s)
            if (hintId == s.id) hintId = null
            val run = ray.size + s.cells.size
            val tail = generateSequence(s.head + s.dir) { it + s.dir }.take(run).toList()
            val anim = MoveAnim(s.cells + tail, s.cells.size)
            anims[s.id] = anim
            scope.launch {
                anim.progress.animateTo(
                    run.toFloat(),
                    tween((run * 30).coerceIn(250, 900), easing = FastOutLinearInEasing),
                )
                anims.remove(s.id)
                if (board.isEmpty() && anims.isEmpty() && status == Status.PLAYING) {
                    status = Status.WON
                }
            }
        } else {
            // Blocked: bump into the obstacle, turn red, slide back, lose a drop.
            lives -= 1
            red.add(s.id)
            val anim = MoveAnim(s.cells + ray.take(blockAt + 1), s.cells.size)
            anims[s.id] = anim
            scope.launch {
                anim.progress.animateTo(blockAt + 0.25f, tween(80 + blockAt * 35))
                anim.progress.animateTo(0f, tween(180 + blockAt * 30, easing = FastOutSlowInEasing))
                anims.remove(s.id)
                delay(600)
                red.remove(s.id)
                if (lives <= 0 && status == Status.PLAYING) status = Status.LOST
            }
        }
    }

    fun useHint() {
        if (status != Status.PLAYING || hints <= 0) return
        if (hintId != null && board.any { it.id == hintId }) return
        val occ = Board.occupancy(board)
        val s = board.firstOrNull {
            !anims.containsKey(it.id) && Board.isClear(it, occ, level.width, level.height)
        } ?: return
        hintId = s.id
        hints -= 1
    }

    Column(Modifier.fillMaxSize()) {
        // Title bar
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Level ${level.number}",
                    color = Palette.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    level.difficulty,
                    color = Palette.subtitle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                )
            }
            IconButton(onClick = onRestart, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Refresh, contentDescription = "Restart", tint = Palette.ink)
            }
        }

        // Lives + hint
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(MAX_LIVES) { i ->
                Drop(filled = i < lives)
                Spacer(Modifier.width(6.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                "💡 $hints",
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { useHint() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                fontSize = 16.sp,
                color = Palette.ink,
            )
        }

        // Board: pinch to zoom / drag to pan, tap an arrow to move it.
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp)
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 4f)
                        offset = if (newScale == 1f) {
                            Offset.Zero
                        } else {
                            centroid - (centroid - offset) * (newScale / scale) + pan
                        }
                        scale = newScale
                    }
                }
                .pointerInput(level) {
                    detectTapGestures { p ->
                        val geo = geometry(size.width.toFloat(), size.height.toFloat(), level)
                        val local = (p - offset) / scale
                        val bx = (local.x - geo.origin.x) / geo.cell
                        val by = (local.y - geo.origin.y) / geo.cell
                        val occ = Board.occupancy(board)
                        // Pick the occupied cell nearest the finger (forgiving on small cells).
                        var bestId: Int? = null
                        var bestD = 0.8f * 0.8f
                        for (dy in -1..1) for (dx in -1..1) {
                            val c = Cell(floor(bx).toInt() + dx, floor(by).toInt() + dy)
                            val id = occ[c] ?: continue
                            val ddx = c.x + 0.5f - bx
                            val ddy = c.y + 0.5f - by
                            val d = ddx * ddx + ddy * ddy
                            if (d < bestD) {
                                bestD = d
                                bestId = id
                            }
                        }
                        bestId?.let { id -> board.firstOrNull { it.id == id }?.let { tap(it) } }
                    }
                }
        ) {
            val geo = geometry(size.width, size.height, level)
            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                val occ = Board.occupancy(board)
                val dotR = geo.cell * 0.06f
                for (y in 0 until level.height) for (x in 0 until level.width) {
                    if (!occ.containsKey(Cell(x, y))) {
                        drawCircle(Palette.dot, dotR, geo.px(x.toFloat(), y.toFloat()))
                    }
                }

                fun colorFor(id: Int) = when {
                    id in red -> Palette.red
                    id == hintId -> Palette.hint
                    else -> Palette.ink
                }

                for (s in board) {
                    if (!anims.containsKey(s.id)) drawSnake(s.cells, 0f, s.cells.size, colorFor(s.id), geo)
                }
                for ((id, a) in anims) {
                    drawSnake(a.path, a.progress.value, a.length, colorFor(id), geo)
                }
            }
        }
    }

    if (status == Status.WON) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = { TextButton(onClick = onNext) { Text("Next level") } },
            title = { Text("Level complete!") },
            text = { Text("Drops left: $lives of $MAX_LIVES") },
        )
    }
    if (status == Status.LOST) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = { TextButton(onClick = onRestart) { Text("Try again") } },
            title = { Text("Out of drops") },
            text = { Text("You tapped a blocked arrow $MAX_LIVES times.") },
        )
    }
}

private fun pointAt(path: List<Cell>, f: Float): Offset {
    val i = floor(f).toInt().coerceIn(0, path.size - 1)
    if (i >= path.size - 1) return Offset(path.last().x.toFloat(), path.last().y.toFloat())
    val t = f - i
    val a = path[i]
    val b = path[i + 1]
    return Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
}

private fun DrawScope.drawSnake(path: List<Cell>, start: Float, length: Int, color: Color, geo: Geo) {
    val end = start + length - 1
    val pts = ArrayList<Offset>()
    pts += pointAt(path, start)
    var k = floor(start).toInt() + 1
    while (k < end) {
        pts += Offset(path[k].x.toFloat(), path[k].y.toFloat())
        k++
    }
    pts += pointAt(path, end)

    val line = Path()
    pts.forEachIndexed { idx, p ->
        val q = geo.px(p.x, p.y)
        if (idx == 0) line.moveTo(q.x, q.y) else line.lineTo(q.x, q.y)
    }
    drawPath(
        line,
        color,
        style = Stroke(width = geo.cell * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Arrowhead at the front, pointing along the current segment.
    val i = floor(end).toInt().coerceIn(0, path.size - 2)
    val dx = (path[i + 1].x - path[i].x).toFloat()
    val dy = (path[i + 1].y - path[i].y).toFloat()
    val tipBase = pts.last()
    val c = geo.px(tipBase.x, tipBase.y)
    val tip = c + Offset(dx, dy) * (geo.cell * 0.24f)
    val back = c - Offset(dx, dy) * (geo.cell * 0.10f)
    val perp = Offset(-dy, dx) * (geo.cell * 0.2f)
    val head = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(back.x + perp.x, back.y + perp.y)
        lineTo(back.x - perp.x, back.y - perp.y)
        close()
    }
    drawPath(head, color)
}

@Composable
private fun Drop(filled: Boolean) {
    Canvas(Modifier.size(width = 16.dp, height = 22.dp)) {
        val w = size.width
        val h = size.height
        val r = w / 2f
        val cy = h - r
        val c = if (filled) Palette.water else Palette.waterEmpty
        val top = Path().apply {
            moveTo(w / 2f, 0f)
            lineTo(w, cy)
            lineTo(0f, cy)
            close()
        }
        drawPath(top, c)
        drawCircle(c, r, Offset(w / 2f, cy))
    }
}
