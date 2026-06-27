package com.mrjis.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mrjis.calendar.ui.theme.CalendarTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalendarTheme {
                CalendarApp()
            }
        }
    }
}

@Composable
fun CalendarApp() {
    val today = LocalDate.now()
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(today) }

    // Drives AnimatedContent slide direction.
    // +1 → next month (new content enters from right)
    // -1 → prev month (new content enters from left)
    var slideDirection by remember { mutableIntStateOf(1) }

    var showYearPicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    fun goToPrevious() {
        slideDirection = -1
        currentYearMonth = currentYearMonth.minusMonths(1)
    }

    fun goToNext() {
        slideDirection = 1
        currentYearMonth = currentYearMonth.plusMonths(1)
    }

    // Accumulated horizontal drag distance for the current gesture.
    // Kept at this level so the entire screen surface — not just the grid —
    // responds to swipes.
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Attach gesture to the full-screen Box so whitespace above/below
                // the grid is also swipeable — not just the grid cells themselves.
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart  = { dragAccumulator = 0f },
                        onDragCancel = { dragAccumulator = 0f },
                        onHorizontalDrag = { _, delta -> dragAccumulator += delta },
                        onDragEnd = {
                            // Positive delta = finger moved right = going back in time
                            // Negative delta = finger moved left  = going forward in time
                            when {
                                dragAccumulator >  60f -> goToPrevious()
                                dragAccumulator < -60f -> goToNext()
                            }
                            dragAccumulator = 0f
                        }
                    )
                }
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                CalendarHeader(
                    yearMonth = currentYearMonth,
                    slideDirection = slideDirection,
                    onMonthClick = { showMonthPicker = true },
                    onYearClick  = { showYearPicker  = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                WeekdayHeader()

                Spacer(modifier = Modifier.height(6.dp))

                // Snapshot slideDirection before entering the AnimatedContent lambda.
                // transitionSpec runs during the animation phase — if we read
                // slideDirection directly inside it, a subsequent recomposition
                // (e.g. rapid swipe) can flip the value before the transition
                // finishes, producing the wrong slide direction.
                val capturedDirection = slideDirection

                AnimatedContent(
                    targetState = currentYearMonth,
                    transitionSpec = {
                        val dir = capturedDirection
                        slideInHorizontally(
                            animationSpec = tween(220),
                            // Next: enter from right (+fullWidth). Prev: enter from left (-fullWidth).
                            initialOffsetX = { if (dir > 0) it else -it }
                        ) togetherWith slideOutHorizontally(
                            animationSpec = tween(220),
                            // Next: exit to left (-fullWidth). Prev: exit to right (+fullWidth).
                            targetOffsetX = { if (dir > 0) -it else it }
                        )
                    },
                    label = "CalendarGridSlide"
                ) { animatedYearMonth ->
                    CalendarGrid(
                        yearMonth = animatedYearMonth,
                        selectedDate = selectedDate,
                        today = today,
                        onDateSelected = { selectedDate = it }
                    )
                }
            }

            // Pinned today card — tapping it resets the calendar to the current month.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                TodayDateCard(
                    today = today,
                    onClick = {
                        val target = YearMonth.now()
                        if (target != currentYearMonth) {
                            slideDirection = if (target > currentYearMonth) 1 else -1
                            currentYearMonth = target
                        }
                    }
                )
            }
        }
    }

    if (showYearPicker) {
        YearPickerDialog(
            currentYear = currentYearMonth.year,
            onYearSelected = { year ->
                val target = YearMonth.of(year, currentYearMonth.month)
                slideDirection = if (target > currentYearMonth) 1 else -1
                currentYearMonth = target
                showYearPicker = false
            },
            onDismiss = { showYearPicker = false }
        )
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            currentMonth = currentYearMonth.monthValue,
            onMonthSelected = { month ->
                val target = YearMonth.of(currentYearMonth.year, month)
                slideDirection = if (target > currentYearMonth) 1 else -1
                currentYearMonth = target
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }
}

// ─── Calendar header ──────────────────────────────────────────────────────────

@Composable
fun CalendarHeader(
    yearMonth: YearMonth,
    slideDirection: Int,
    onMonthClick: () -> Unit,
    onYearClick: () -> Unit
) {
    // Same direction-capture reasoning as CalendarApp — snapshot before the lambda.
    val capturedDirection = slideDirection

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = yearMonth,
            transitionSpec = {
                val dir = capturedDirection
                (slideInHorizontally(
                    animationSpec = tween(220),
                    initialOffsetX = { if (dir > 0) it / 2 else -it / 2 }
                ) + fadeIn(tween(180))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(220),
                            targetOffsetX = { if (dir > 0) -it / 2 else it / 2 }
                        ) + fadeOut(tween(120)))
            },
            label = "HeaderSlide"
        ) { ym ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ym.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onMonthClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = ym.year.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onYearClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ─── Weekday label row ────────────────────────────────────────────────────────

// Week starts on Saturday to match the locale convention used in this app.
private val WEEKDAYS = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")

@Composable
fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        WEEKDAYS.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Day grid ─────────────────────────────────────────────────────────────────

@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    // ISO day-of-week: Mon=1 … Sat=6, Sun=7.
    // Convert to our Sat-first column offset (Sat→0, Sun→1, Mon→2, …).
    val isoDay = yearMonth.atDay(1).dayOfWeek.value
    val firstDayOffset = when (isoDay) {
        6    -> 0           // Saturday
        7    -> 1           // Sunday
        else -> isoDay + 1  // Mon–Fri
    }

    val cells = buildList<LocalDate?> {
        repeat(firstDayOffset) { add(null) } // leading empty cells
        for (day in 1..yearMonth.lengthOfMonth()) add(yearMonth.atDay(day))
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(cells) { date ->
            DayCell(
                date = date,
                isSelected = date == selectedDate,
                isToday = date == today,
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
fun DayCell(
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimary    = MaterialTheme.colorScheme.onPrimary

    // Outer Box maintains the 1:1 aspect ratio for every grid cell,
    // including the null leading-offset cells.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
    ) {
        if (date == null) return@Box // empty leading cell — render nothing

        val bgColor = if (isSelected) primaryColor else Color.Transparent
        val textColor = when {
            isSelected -> onPrimary
            isToday    -> primaryColor
            else       -> MaterialTheme.colorScheme.onBackground
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(bgColor)
                // Today indicator: outline ring when not selected
                .then(
                    if (isToday && !isSelected)
                        Modifier.border(1.5.dp, primaryColor, CircleShape)
                    else Modifier
                )
                .clickable { onDateSelected(date) }
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 14.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Today card ───────────────────────────────────────────────────────────────

// Displays the full date string for today and acts as a "jump to today" button.
@Composable
fun TodayDateCard(today: LocalDate, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${today.dayOfMonth} " +
                        "${today.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())} " +
                        "${today.year}, " +
                        today.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ─── Year picker dialog ───────────────────────────────────────────────────────

@Composable
fun YearPickerDialog(
    currentYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val allYears = (1991..2050).toList()
    val allRows  = allYears.chunked(3)

    // Scroll so the current year is roughly centred rather than at the top.
    val currentRowIndex = allRows.indexOfFirst { it.contains(currentYear) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentRowIndex - 2).coerceAtLeast(0)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Year",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allRows.size) { rowIndex ->
                        val row = allRows[rowIndex]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { year ->
                                val isSelected = year == currentYear
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { onYearSelected(year) }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = year.toString(),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            // Fill trailing slots when the last row has fewer than 3 years.
                            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ─── Month picker dialog ──────────────────────────────────────────────────────

@Composable
fun MonthPickerDialog(
    currentMonth: Int,
    onMonthSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Month",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val months = (1..12).map { m ->
                    m to java.time.Month.of(m).getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                }

                months.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { (monthNum, monthName) ->
                            val isSelected = monthNum == currentMonth
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { onMonthSelected(monthNum) }
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = monthName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}