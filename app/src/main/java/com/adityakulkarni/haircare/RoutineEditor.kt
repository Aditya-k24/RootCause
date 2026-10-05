package com.adityakulkarni.haircare

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

/** Full-screen list of the routine. Every change saves immediately; Back returns to today. */
@Composable
fun RoutineEditor(onClose: () -> Unit) {
    val c = LocalContext.current
    val p = LocalPalette.current
    var routine by remember { mutableStateOf(Store.routine(c)) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var deleting by remember { mutableStateOf<Task?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    fun save(r: List<Task>) {
        routine = r
        Store.saveRoutine(c, r)
        updateWidgets(c)
    }

    /** Swap with the nearest task in the same section, in direction [dir] (-1 up, +1 down). */
    fun move(t: Task, dir: Int) {
        val i = routine.indexOf(t)
        var j = i + dir
        while (j in routine.indices && routine[j].group != t.group) j += dir
        if (j !in routine.indices) return
        save(routine.toMutableList().also { it[i] = it[j]; it[j] = t })
    }

    BackHandler(onBack = onClose)
    Column(
        Modifier.fillMaxSize().background(p.bg).safeDrawingPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = p.text) }
            Spacer(Modifier.width(4.dp))
            Text("Your routine", style = MaterialTheme.typography.headlineMedium, color = p.text)
        }
        Text(
            "Tap a task to change it. Changes save as you go.",
            style = MaterialTheme.typography.bodyMedium, color = p.textMuted,
        )
        GROUPS.forEach { g ->
            val items = routine.filter { it.group == g }
            if (items.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(g)
                    items.forEachIndexed { i, t ->
                        RoutineRow(
                            t, first = i == 0, last = i == items.lastIndex, canDelete = routine.size > 1,
                            onEdit = { editing = t }, onMove = { move(t, it) }, onDelete = { deleting = t },
                        )
                    }
                }
            }
        }
        val add = remember { MutableInteractionSource() }
        Chunky(
            p.green, p.greenLip,
            Modifier.fillMaxWidth().clickable(add, null, role = Role.Button) {
                editing = Task("t${System.currentTimeMillis()}", "", "", ANYTIME, "check")
            },
            shape = RoundedCornerShape(16.dp), interaction = add,
        ) {
            Row(Modifier.align(Alignment.Center).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Add, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("ADD TASK", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
        TextButton({ confirmReset = true }, Modifier.align(Alignment.CenterHorizontally)) {
            Text("Reset to suggested routine", style = MaterialTheme.typography.titleSmall, color = p.textMuted)
        }
        Spacer(Modifier.height(8.dp))
    }

    editing?.let { t ->
        val isNew = routine.none { it.id == t.id }
        TaskDialog(t, isNew, onDismiss = { editing = null }) { saved ->
            save(if (isNew) routine + saved else routine.map { if (it.id == saved.id) saved else it })
            editing = null
        }
    }
    deleting?.let { t ->
        ThemedDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete “${t.label}”?") },
            text = { Text("Past streak days stay as they are.") },
            confirmButton = {
                TextButton({ save(routine - t); deleting = null }) { Text("Delete", color = Color(0xFFDC2626)) }
            },
            dismissButton = { TextButton({ deleting = null }) { Text("Cancel") } },
        )
    }
    if (confirmReset) {
        ThemedDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset routine?") },
            text = { Text("Your tasks are replaced with the suggested hair-care routine. Your streak is kept.") },
            confirmButton = { TextButton({ save(DEFAULT_ROUTINE); confirmReset = false }) { Text("Reset") } },
            dismissButton = { TextButton({ confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RoutineRow(
    t: Task,
    first: Boolean,
    last: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    val p = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    Chunky(
        p.surface, p.surfaceLip,
        Modifier.fillMaxWidth().clickable(interaction, null, role = Role.Button, onClick = onEdit),
        interaction = interaction,
    ) {
        Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (t.group == SCALP) p.scalpTint else p.morningTint),
                contentAlignment = Alignment.Center,
            ) { Icon(iconFor(t.icon), null, tint = if (t.group == SCALP) p.scalp else p.amberLip, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.label, style = MaterialTheme.typography.titleMedium, color = p.text)
                Text(
                    describe(t.schedule) + if (t.camera) " · opens camera" else "",
                    style = MaterialTheme.typography.bodySmall, color = p.textMuted,
                )
            }
            IconButton({ onMove(-1) }, enabled = !first) {
                Icon(Icons.Rounded.KeyboardArrowUp, "Move ${t.label} up", tint = p.textMuted.copy(alpha = if (first) 0.3f else 1f))
            }
            IconButton({ onMove(1) }, enabled = !last) {
                Icon(Icons.Rounded.KeyboardArrowDown, "Move ${t.label} down", tint = p.textMuted.copy(alpha = if (last) 0.3f else 1f))
            }
            IconButton(onDelete, enabled = canDelete) {
                Icon(Icons.Rounded.Delete, "Delete ${t.label}", tint = p.textMuted.copy(alpha = if (canDelete) 1f else 0.3f))
            }
        }
    }
}

private enum class Repeat { DAILY, WEEKDAYS, MONTHLY }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskDialog(task: Task, isNew: Boolean, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    val p = LocalPalette.current
    var label by remember { mutableStateOf(task.label) }
    var hint by remember { mutableStateOf(task.hint) }
    var group by remember { mutableStateOf(task.group) }
    var icon by remember { mutableStateOf(task.icon) }
    var camera by remember { mutableStateOf(task.camera) }
    var repeat by remember {
        mutableStateOf(
            when {
                task.schedule.monthDay != null -> Repeat.MONTHLY
                task.schedule.weekdays.size == 7 -> Repeat.DAILY
                else -> Repeat.WEEKDAYS
            },
        )
    }
    var weekdays by remember { mutableStateOf(task.schedule.weekdays.takeIf { it.size < 7 } ?: setOf(DayOfWeek.MONDAY)) }
    var monthDay by remember { mutableIntStateOf(task.schedule.monthDay ?: 1) }
    val valid = label.isNotBlank() && (repeat != Repeat.WEEKDAYS || weekdays.isNotEmpty())

    ThemedDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New task" else "Edit task") },
        confirmButton = {
            TextButton(
                {
                    val schedule = when (repeat) {
                        Repeat.DAILY -> Schedule()
                        Repeat.WEEKDAYS -> Schedule(weekdays)
                        Repeat.MONTHLY -> Schedule(monthDay = monthDay)
                    }
                    onSave(task.copy(label = label.trim(), hint = hint.trim(), group = group, icon = icon, schedule = schedule, camera = camera))
                },
                enabled = valid,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(label, { label = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
                OutlinedTextField(hint, { hint = it }, Modifier.fillMaxWidth(), label = { Text("Note (optional)") })

                FieldLabel("Section")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GROUPS.forEach { g -> Chip(g, group == g) { group = g } }
                }

                FieldLabel("Repeats")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Every day", repeat == Repeat.DAILY) { repeat = Repeat.DAILY }
                    Chip("Some days", repeat == Repeat.WEEKDAYS) { repeat = Repeat.WEEKDAYS }
                    Chip("Monthly", repeat == Repeat.MONTHLY) { repeat = Repeat.MONTHLY }
                }
                when (repeat) {
                    Repeat.WEEKDAYS -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.entries.forEach { d ->
                            val on = d in weekdays
                            Chip(d.getDisplayName(JTextStyle.SHORT, Locale.getDefault()), on) {
                                weekdays = if (on) weekdays - d else weekdays + d
                            }
                        }
                    }
                    Repeat.MONTHLY -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("On the", color = p.text)
                        // ponytail: capped at 28 so it never skips a short month
                        IconButton({ monthDay = (monthDay - 1).coerceAtLeast(1) }) { Icon(Icons.Rounded.Remove, "Earlier day") }
                        Text(ordinal(monthDay), style = MaterialTheme.typography.titleMedium, color = p.text)
                        IconButton({ monthDay = (monthDay + 1).coerceAtMost(28) }) { Icon(Icons.Rounded.Add, "Later day") }
                        Text("of each month", color = p.text)
                    }
                    Repeat.DAILY -> {}
                }

                FieldLabel("Icon")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ICONS.forEach { key ->
                        val on = key == icon
                        Box(
                            Modifier.size(44.dp).clip(CircleShape)
                                .background(if (on) p.amber.copy(alpha = 0.25f) else p.muted)
                                .then(if (on) Modifier.border(2.dp, p.amberLip, CircleShape) else Modifier)
                                .clickable(role = Role.RadioButton) { icon = key },
                            contentAlignment = Alignment.Center,
                        ) { Icon(iconFor(key), "Icon $key", tint = if (on) p.amberLip else p.textMuted) }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Opens camera", style = MaterialTheme.typography.titleSmall, color = p.text)
                        Text(
                            "Ticking it takes crown, hairline and top photos",
                            style = MaterialTheme.typography.bodySmall, color = p.textMuted,
                        )
                    }
                    Switch(camera, { camera = it })
                }
            }
        },
    )
}

@Composable
private fun FieldLabel(text: String) =
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = LocalPalette.current.textMuted)

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    FilterChip(
        selected, onClick, { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = p.amber.copy(alpha = 0.25f), selectedLabelColor = p.text,
        ),
    )
}

/** AlertDialog in the app's palette instead of Material's default lavender surface. */
@Composable
private fun ThemedDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
) {
    val p = LocalPalette.current
    AlertDialog(
        onDismissRequest = onDismissRequest, confirmButton = confirmButton, dismissButton = dismissButton,
        title = title, text = text,
        containerColor = p.surface, titleContentColor = p.text, textContentColor = p.text,
    )
}
