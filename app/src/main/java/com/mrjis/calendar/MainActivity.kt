package com.mrjis.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

// ─── Top-level view model (plain state) ──────────────────────────────────────

@Composable
fun CalendarApp() {
    val today = LocalDate.now()
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(today) }

    // Dialog visibility
    var showYearPicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Calendar content above the fixed footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(bottom = 120.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                CalendarHeader(
                    yearMonth = currentYearMonth,
                    onPreviousMonth = { currentYearMonth = currentYearMonth.minusMonths(1) },
                    onNextMonth = { currentYearMonth = currentYearMonth.plusMonths(1) },
                    onMonthClick = { showMonthPicker = true },
                    onYearClick = { showYearPicker = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                WeekdayHeader()

                Spacer(modifier = Modifier.height(4.dp))

                CalendarGrid(
                    yearMonth = currentYearMonth,
                    selectedDate = selectedDate,
                    today = today,
                    onDateSelected = { selectedDate = it }
                )
            }

            // ── Selected date info panel — fixed at bottom, tappable to go to today ──
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                SelectedDateCard(
                    today = today,
                    onClick = { currentYearMonth = YearMonth.now() }
                )
            }
        }
    }

    if (showYearPicker) {
        YearPickerDialog(
            currentYear = currentYearMonth.year,
            onYearSelected = { year ->
                currentYearMonth = YearMonth.of(year, currentYearMonth.month)
                showYearPicker = false
            },
            onDismiss = { showYearPicker = false }
        )
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            currentMonth = currentYearMonth.monthValue,
            onMonthSelected = { month ->
                currentYearMonth = YearMonth.of(currentYearMonth.year, month)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }
}

// ─── Header row ──────────────────────────────────────────────────────────────

@Composable
fun CalendarHeader(
    yearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthClick: () -> Unit,
    onYearClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = yearMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onMonthClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = yearMonth.year.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onYearClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
        }
    }
}

// ─── Weekday row — Sat start, Fri end ────────────────────────────────────────

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

// ─── Day grid ────────────────────────────────────────────────────────────────

@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val isoDay = yearMonth.atDay(1).dayOfWeek.value
    val firstDayOffset = when (isoDay) {
        6 -> 0 // Sat
        7 -> 1 // Sun
        else -> isoDay + 1 // Mon=2..Fri=6
    }
    val daysInMonth = yearMonth.lengthOfMonth()

    val cells = buildList<LocalDate?> {
        repeat(firstDayOffset) { add(null) }
        for (day in 1..daysInMonth) add(yearMonth.atDay(day))
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
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
    ) {
        if (date != null) {
            val bgColor = if (isSelected) primaryColor else Color.Transparent
            val textColor = when {
                isSelected -> onPrimary
                isToday -> primaryColor
                else -> MaterialTheme.colorScheme.onBackground
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(bgColor)
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
}

// ─── Selected date info card ──────────────────────────────────────────────────

@Composable
fun SelectedDateCard(today: LocalDate, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Today",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = today.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "${today.dayOfMonth} " +
                        "${today.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())} " +
                        "${today.year}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ─── Year Picker Dialog — scrollable LazyColumn of 3-col rows ────────────────

@Composable
fun YearPickerDialog(
    currentYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val allYears = (1948..2100).toList()
    val allRows = allYears.chunked(3)

    // Index of the row that contains currentYear
    val currentRowIndex = allRows.indexOfFirst { row -> row.contains(currentYear) }
    // Scroll so the current-year row sits in the visible center (offset by ~2 rows)
    val initialIndex = (currentRowIndex - 2).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
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
                                            if (isSelected)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { onYearSelected(year) }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = year.toString(),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            // Pad incomplete last row
                            repeat(3 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ─── Month Picker Dialog ──────────────────────────────────────────────────────

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
                    Pair(m, java.time.Month.of(m)
                        .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()))
                }
                val rows = months.chunked(3)
                rows.forEach { row ->
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
                                        if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { onMonthSelected(monthNum) }
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = monthName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
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