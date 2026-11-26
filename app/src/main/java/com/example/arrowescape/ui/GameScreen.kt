package com.example.arrowescape.ui

import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arrowescape.GameAudio
import com.example.arrowescape.game.Board
import com.example.arrowescape.game.Cell
import com.example.arrowescape.game.Difficulty
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

private fun levelKey(d: Difficulty) = "level_${d.name}"

/** Top-level navigation: difficulty menu, or a level in progress. */
@Composable
fun App(prefs: SharedPreferences, audio: GameAudio) {
    var selected by rememberSaveable { mutableStateOf<Difficulty?>(null) }
    val current = selected
    if (current == null) {
        MenuScreen(prefs, audio, onPick = { selected = it })
    } else {
        BackHandler { selected = null }
        GameScreen(current, prefs, audio, onBack = { selected = null })
    }
}

@Composable
private fun MenuScreen(prefs: SharedPreferences, audio: GameAudio, onPick: (Difficulty) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AudioToggles(audio)
        }
        Spacer(Modifier.weight(1f))
        Logo(Modifier.size(120.dp))
        Spacer(Modifier.height(16.dp))
        Text("Arrow Escape", color = Palette.ink, fontWeight = FontWeight.Bold, fontSize = 32.sp)
        Text(
            "Tap an arrow to slide it out.\nDon't let it hit another one.",
            color = Palette.ink.copy(alpha = 0.6f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(40.dp))
        Difficulty.entries.forEach { d ->
            val level = prefs.getInt(levelKey(d), 1)
            Button(
                onClick = { onPick(d) },
                colors = ButtonDefaults.buttonColors(containerColor = d.color()),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(vertical = 6.dp),
            ) {
                Text(d.label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.weight(1f))
                Text("Level $level", fontSize = 15.sp, color = Color.White.copy(alpha = 0.85f))
            }
        }
        Spacer(Modifier.weight(1.3f))
    }
}

private fun Difficulty.color() = when (this) {
    Difficulty.EASY -> Color(0xFF6BAF7A)
    Difficulty.MEDIUM -> Color(0xFFD9963A)
    Difficulty.HARD -> Color(0xFF8A4FD0)
}

/** Same arrow as the app icon. */
@Composable
private fun Logo(modifier: Modifier) {
    Canvas(modifier) {
        val k = size.minDimension / 108f
        fun p(x: Float, y: Float) = Offset(x * k, y * k)
        val stroke = Stroke(width = 7 * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(Path().apply {
            moveTo(30 * k, 78 * k); lineTo(30 * k, 54 * k); lineTo(52 * k, 54 * k)
            lineTo(52 * k, 36 * k); lineTo(68 * k, 36 * k)
        }, Palette.ink, style = stroke)
        drawPath(Path().apply {
            moveTo(p(66f, 26f).x, p(66f, 26f).y); lineTo(82 * k, 36 * k); lineTo(66 * k, 46 * k); close()
        }, Palette.ink)
        drawLine(Palette.hint, p(46f, 74f), p(64f, 74f), strokeWidth = 6 * k, cap = StrokeCap.Round)
        drawPath(Path().apply {
            moveTo(62 * k, 67 * k); lineTo(75 * k, 74 * k); lineTo(62 * k, 81 * k); close()
        }, Palette.hint)
    }
}

@Composable
private fun AudioToggles(audio: GameAudio) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ToggleEmoji(if (audio.soundOn) "🔊" else "🔇", true) { audio.toggleSound() }
        ToggleEmoji("🎵", audio.musicOn) { audio.toggleMusic() }
    }
}

@Composable
private fun ToggleEmoji(emoji: String, on: Boolean, onClick: () -> Unit) {
    Text(
        emoji,
        fontSize = 20.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .alpha(if (on) 1f else 0.3f)
            .padding(8.dp),
    )
}

@Composable
private fun GameScreen(difficulty: Difficulty, prefs: SharedPreferences, audio: GameAudio, onBack: () -> Unit) {
    var levelNumber by remember(difficulty) { mutableIntStateOf(prefs.getInt(levelKey(difficulty), 1)) }
    var attempt by remember { mutableIntStateOf(0) }
    val level = remember(difficulty, levelNumber) { LevelGenerator.generate(difficulty, levelNumber) }

    key(difficulty, levelNumber, attempt) {
        LevelPlay(
            level = level,
            audio = audio,
            onBack = onBack,
            onRestart = { attempt++ },
            onNext = {
                levelNumber++
                prefs.edit().putInt(levelKey(difficulty), levelNumber).apply()
            },
        )
    }
}

@Composable
private fun LevelPlay(
    level: Level,
    audio: GameAudio,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onNext: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
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
            audio.playEscape()
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
                    audio.playWin()
                }
            }
        } else {
            // Blocked: bump into the obstacle, turn red, slide back, lose a drop.
            lives -= 1
            red.add(s.id)
            audio.playBlocked()
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Palette.ink)
            }
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
                    level.difficulty.label,
                    color = level.difficulty.color(),
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
            AudioToggles(audio)
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
