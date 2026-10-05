package com.adityakulkarni.haircare

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Shower
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    // bumped on resume so ticks made elsewhere (or a new day) show up here
    private var tick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        Reminders.scheduleNext(this)
        setContent { HairCareTheme { Screen(tick) { tick++ } } }
    }

    override fun onResume() {
        super.onResume()
        tick++
        updateWidgets(this)
    }
}

private val PHOTO_ANGLES = listOf("crown", "hairline", "top")

/** A new entry in the Pictures/RootCause album for the camera to write into; null if the store refuses. */
private fun newPhotoUri(c: Context, angle: String): Uri? = c.contentResolver.insert(
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "scalp-${LocalDate.now()}-$angle.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RootCause")
    },
)

private fun iconFor(id: String): ImageVector = when (id) {
    "workout" -> Icons.Rounded.FitnessCenter
    "meditate" -> Icons.Rounded.SelfImprovement
    "minoxidil" -> Icons.Rounded.WaterDrop
    "massage" -> Icons.Rounded.Spa
    "keto" -> Icons.Rounded.Shower
    "photos" -> Icons.Rounded.PhotoCamera
    else -> Icons.Rounded.Medication
}

@Composable
private fun Screen(tick: Int, changed: () -> Unit) {
    val c = LocalContext.current
    val p = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    var celebrate by remember { mutableStateOf(false) }

    tick // read so the screen recomposes when it changes
    val today = LocalDate.now()
    val checked = Store.checked(c, today)
    val doneDays = Store.doneDays(c)
    val done = today in doneDays
    val streak = streak(doneDays, today)
    val tasks = tasksFor(today)
    val u = urgency(done, LocalTime.now())

    fun toggle(id: String) {
        val on = id !in checked
        Store.toggle(c, id)
        val nowDone = Store.isDoneToday(c)
        haptics.performHapticFeedback(
            when {
                nowDone && !done -> HapticFeedbackType.Confirm
                on -> HapticFeedbackType.ToggleOn
                else -> HapticFeedbackType.ToggleOff
            },
        )
        if (nowDone && !done) celebrate = true
        changed()
        updateWidgets(c)
    }

    // Progress photos: camera opens once per angle, shots land in Pictures/RootCause.
    // Saveable so a rotation or process death while the camera is open doesn't lose our place.
    var shot by rememberSaveable { mutableIntStateOf(-1) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var startShot: (Int) -> Unit = {}
    fun finishPhotos(taken: Boolean) {
        shot = -1
        pending = null
        if (taken && "photos" !in Store.checked(c)) toggle("photos")
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (!ok) {
            pending?.let { c.contentResolver.delete(Uri.parse(it), null, null) } // drop the empty entry
            finishPhotos(taken = shot > 0)
        } else if (shot < PHOTO_ANGLES.lastIndex) {
            startShot(shot + 1)
        } else {
            finishPhotos(taken = true)
        }
    }
    startShot = { i ->
        val uri = newPhotoUri(c, PHOTO_ANGLES[i])
        if (uri == null) {
            finishPhotos(taken = i > 0)
        } else {
            shot = i
            pending = uri.toString()
            Toast.makeText(c, "Photo ${i + 1} of ${PHOTO_ANGLES.size}: ${PHOTO_ANGLES[i]}", Toast.LENGTH_LONG).show()
            try {
                camera.launch(uri)
            } catch (e: ActivityNotFoundException) {
                c.contentResolver.delete(uri, null, null)
                Toast.makeText(c, "No camera app found", Toast.LENGTH_SHORT).show()
                finishPhotos(taken = i > 0)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(p.bg)) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Header(today)
            Hero(u, streak, doneDays, today)
            TodayProgress(tasks.count { it.id in checked }, tasks.size)
            listOf(MORNING, SCALP).forEach { group ->
                val items = tasks.filter { it.group == group }
                if (items.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel(group)
                        items.forEach { t ->
                            TaskCard(t, t.id in checked) {
                                if (t.id == "photos" && t.id !in checked) startShot(0) else toggle(t.id)
                            }
                        }
                    }
                }
            }
            Stats(streak, bestStreak(doneDays), doneDays.size)
            MilestoneCard(streak)
            Calendar(doneDays, today)
            AddWidgetButton()
            Spacer(Modifier.height(8.dp))
        }
        Celebration(celebrate, streak) { celebrate = false }
    }
}

@Composable
private fun Header(today: LocalDate) {
    val p = LocalPalette.current
    Column(Modifier.padding(top = 4.dp)) {
        Text("RootCause", style = MaterialTheme.typography.headlineMedium, color = p.text)
        Text(
            today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
            style = MaterialTheme.typography.bodyMedium, color = p.textMuted,
        )
    }
}

@Composable
private fun Hero(u: Urgency, streak: Int, doneDays: Set<LocalDate>, today: LocalDate) {
    val (top, bottom) = heroColors(u)
    val doneToday = today in doneDays
    // Flame pulses when the streak is in danger, like Duolingo's nagging widget
    val pulse = rememberInfiniteTransition(label = "pulse")
    val beat by pulse.animateFloat(
        1f, if (u == Urgency.WARN || u == Urgency.PANIC) 1.12f else 1f,
        infiniteRepeatable(tween(if (u == Urgency.PANIC) 420 else 700), RepeatMode.Reverse), label = "beat",
    )
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(top, bottom))).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("$streak", style = MaterialTheme.typography.displayLarge, color = Color.White)
                Text(
                    "day streak",
                    style = MaterialTheme.typography.titleMedium, color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(headline(u, streak), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.92f))
            }
            Icon(
                Icons.Rounded.LocalFireDepartment,
                contentDescription = if (doneToday) "Flame lit, today is done" else "Flame not lit yet",
                tint = if (doneToday) Color(0xFFFDE68A) else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(96.dp).scale(beat),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // days before the first completed day aren't "missed", the app just wasn't in use yet
            val start = doneDays.minOrNull() ?: today
            lastWeek(today).forEach { d -> WeekDot(d, d in doneDays, d == today, d.isBefore(start), bottom) }
        }
    }
}

@Composable
private fun WeekDot(d: LocalDate, done: Boolean, isToday: Boolean, beforeStart: Boolean, heroColor: Color) {
    val name = d.dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.getDefault())
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$name: " + when {
                done -> "done"
                isToday -> "not done yet"
                beforeStart -> "before you started"
                else -> "missed"
            }
        },
    ) {
        Text(
            d.dayOfWeek.getDisplayName(JTextStyle.NARROW, Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = if (isToday) 1f else 0.8f),
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.size(34.dp).clip(CircleShape)
                .background(if (done) Color.White else Color.White.copy(alpha = 0.18f))
                .then(if (isToday && !done) Modifier.border(2.5.dp, Color.White, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            when {
                done -> Icon(Icons.Rounded.Check, null, tint = heroColor, modifier = Modifier.size(22.dp))
                !isToday && !beforeStart -> Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TodayProgress(done: Int, total: Int) {
    val p = LocalPalette.current
    val complete = done == total
    val fraction by animateFloatAsState(done / total.toFloat(), spring(stiffness = Spring.StiffnessLow), label = "progress")
    val bar by animateColorAsState(if (complete) p.green else p.amber, label = "bar")
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Today", style = MaterialTheme.typography.headlineMedium, color = p.text, modifier = Modifier.weight(1f))
            Text("$done / $total", style = MaterialTheme.typography.titleMedium, color = if (complete) p.green else p.textMuted)
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).background(p.muted)) {
            Box(
                Modifier.fillMaxWidth(fraction.coerceAtLeast(0.001f)).height(14.dp).clip(CircleShape).background(bar),
            ) {
                // glossy highlight, Duolingo-style
                Box(
                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp).fillMaxWidth().height(3.dp)
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.35f)),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                complete -> "All done. Your hair thanks you."
                done == 0 -> "Tap a task when you've done it."
                total - done == 1 -> "One more to keep your streak!"
                else -> "${total - done} to go."
            },
            style = MaterialTheme.typography.bodyMedium, color = p.textMuted,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(), style = MaterialTheme.typography.labelSmall, color = LocalPalette.current.textMuted,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

/** Card with a darker bottom "lip" that squashes when pressed, the tactile Duolingo look. */
@Composable
private fun Chunky(
    face: Color,
    lip: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    border: BorderStroke? = null,
    interaction: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
) {
    val pressed by interaction.collectIsPressedAsState()
    val lipH = 4.dp
    val press by animateDpAsState(if (pressed) lipH else 0.dp, tween(90), label = "press")
    Box(modifier.clip(shape).background(lip).padding(bottom = lipH - press)) {
        Box(
            Modifier.fillMaxWidth().clip(shape).background(face)
                .then(if (border != null) Modifier.border(border, shape) else Modifier),
            content = content,
        )
    }
}

@Composable
private fun TaskCard(t: Task, on: Boolean, onToggle: () -> Unit) {
    val p = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val face by animateColorAsState(if (on) p.greenTint else p.surface, label = "face")
    val lip by animateColorAsState(if (on) p.green.copy(alpha = 0.55f) else p.surfaceLip, label = "lip")
    val checkScale by animateFloatAsState(
        if (on) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "check",
    )
    val (tint, ink) = if (t.group == SCALP) p.scalpTint to p.scalp else p.morningTint to p.amberLip
    Chunky(
        face, lip,
        modifier = Modifier.fillMaxWidth().clickable(interaction, indication = null, role = Role.Checkbox, onClick = onToggle)
            .semantics(mergeDescendants = true) { stateDescription = if (on) "Done" else "Not done" },
        interaction = interaction,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(if (on) p.green.copy(alpha = 0.18f) else tint),
                contentAlignment = Alignment.Center,
            ) { Icon(iconFor(t.id), null, tint = if (on) p.greenLip else ink, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(t.label, style = MaterialTheme.typography.titleMedium, color = if (on) p.textMuted else p.text)
                Text(t.hint, style = MaterialTheme.typography.bodySmall, color = p.textMuted)
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(34.dp).clip(CircleShape)
                    .border(2.5.dp, if (on) p.green else p.textMuted.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(34.dp).scale(checkScale).clip(CircleShape).background(p.green),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
            }
        }
    }
}

@Composable
private fun Stats(current: Int, best: Int, total: Int) {
    val p = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(Icons.Rounded.LocalFireDepartment, p.amber, "$current", "Current", Modifier.weight(1f))
        StatCard(Icons.Rounded.EmojiEvents, Color(0xFFEAB308), "$best", "Best", Modifier.weight(1f))
        StatCard(Icons.Rounded.CalendarMonth, p.scalp, "$total", "Total days", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(icon: ImageVector, tint: Color, value: String, label: String, modifier: Modifier) {
    val p = LocalPalette.current
    Chunky(p.surface, p.surfaceLip, modifier) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = p.text)
            Text(label, style = MaterialTheme.typography.bodySmall, color = p.textMuted)
        }
    }
}

@Composable
private fun MilestoneCard(streak: Int) {
    val p = LocalPalette.current
    val next = nextMilestone(streak)
    val prev = MILESTONES.lastOrNull { it.day <= streak }?.day ?: 0
    Chunky(p.surface, p.surfaceLip) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Flag, null, tint = p.amberLip, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("NEXT MILESTONE", style = MaterialTheme.typography.labelSmall, color = p.textMuted)
            }
            Spacer(Modifier.height(8.dp))
            if (next == null) {
                Text("Every milestone reached!", style = MaterialTheme.typography.titleMedium, color = p.text)
                return@Column
            }
            Text("Day ${next.day} · ${next.title}", style = MaterialTheme.typography.titleMedium, color = p.text)
            Text(next.why, style = MaterialTheme.typography.bodyMedium, color = p.textMuted)
            Spacer(Modifier.height(12.dp))
            val f = (streak - prev) / (next.day - prev).toFloat()
            val anim by animateFloatAsState(f, label = "milestone")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(10.dp).clip(CircleShape).background(p.muted)) {
                    Box(Modifier.fillMaxWidth(anim.coerceAtLeast(0.001f)).height(10.dp).clip(CircleShape).background(p.amber))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    (next.day - streak).let { if (it == 1) "1 day to go" else "$it days to go" }, style = MaterialTheme.typography.bodySmall, color = p.textMuted,
                )
            }
        }
    }
}

@Composable
private fun Calendar(doneDays: Set<LocalDate>, today: LocalDate) {
    val p = LocalPalette.current
    // 5 full Mon–Sun weeks ending with this week
    val end = today.with(DayOfWeek.SUNDAY).let { if (it.isBefore(today)) it.plusWeeks(1) else it }
    val days = (34 downTo 0).map { end.minusDays(it.toLong()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Last 5 weeks")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.entries.forEach {
                Text(
                    it.getDisplayName(JTextStyle.NARROW, Locale.getDefault()), Modifier.weight(1f),
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = p.textMuted,
                )
            }
        }
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { d ->
                    val future = d.isAfter(today)
                    val done = d in doneDays
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (done) p.amber else p.muted.copy(alpha = if (future) 0.4f else 1f))
                            .then(if (d == today) Modifier.border(2.dp, p.amberLip, RoundedCornerShape(10.dp)) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${d.dayOfMonth}", fontSize = 12.sp, fontFamily = Nunito, fontWeight = FontWeight.Bold,
                            color = if (done) Color.White else p.textMuted.copy(alpha = if (future) 0.4f else 1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddWidgetButton() {
    val c = LocalContext.current
    val p = LocalPalette.current
    val awm = AppWidgetManager.getInstance(c)
    val receiver = ComponentName(c, HairCareWidgetReceiver::class.java)
    if (!awm.isRequestPinAppWidgetSupported || awm.getAppWidgetIds(receiver).isNotEmpty()) return
    val interaction = remember { MutableInteractionSource() }
    Chunky(
        p.surface, p.surfaceLip,
        Modifier.fillMaxWidth().clickable(interaction, null) { awm.requestPinAppWidget(receiver, null, null) },
        interaction = interaction,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Widgets, null, tint = p.amberLip)
            Spacer(Modifier.width(12.dp))
            Text("Add the streak widget to your home screen", style = MaterialTheme.typography.titleSmall, color = p.text)
        }
    }
}

@Composable
private fun Celebration(visible: Boolean, streak: Int, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            val bounce = rememberInfiniteTransition(label = "bounce")
            val y by bounce.animateFloat(0f, -10f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "y")
            val reached = MILESTONES.firstOrNull { it.day == streak }
            Column(
                Modifier.padding(24.dp).animateEnterExit(enter = scaleIn(spring(dampingRatio = 0.55f)))
                    .clip(RoundedCornerShape(32.dp)).background(p.surface).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Rounded.LocalFireDepartment, null, tint = p.amber,
                    modifier = Modifier.size(120.dp).offset(y = y.dp),
                )
                Text("$streak", style = MaterialTheme.typography.displayLarge, color = p.amber)
                Text(
                    "day streak!",
                    style = MaterialTheme.typography.headlineMedium, color = p.text,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    reached?.let { "Milestone: ${it.title}. ${it.why}" }
                        ?: nextMilestone(streak)?.let {
                            val n = it.day - streak
                            "$n more ${if (n == 1) "day" else "days"} to reach Day ${it.day}: ${it.title}."
                        }
                        ?: "Legendary consistency.",
                    style = MaterialTheme.typography.bodyMedium, color = p.textMuted, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                val interaction = remember { MutableInteractionSource() }
                Chunky(
                    p.green, p.greenLip,
                    Modifier.fillMaxWidth().clickable(interaction, null, onClick = onDismiss),
                    shape = RoundedCornerShape(16.dp), interaction = interaction,
                ) {
                    Text(
                        "CONTINUE", style = MaterialTheme.typography.labelLarge, color = Color.White,
                        modifier = Modifier.align(Alignment.Center).padding(vertical = 14.dp),
                    )
                }
            }
        }
    }
}
