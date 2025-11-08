package com.example.notesmakingapp

import android.provider.Settings
import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.notesmakingapp.ui.theme.NotesMakingAppTheme
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class Note(val fileName: String, val content: String)

class MainActivity : ComponentActivity() {

    // This is for the notification permission
    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)
        checkAndRequestPermission() // Ask for permission on start
        setContent {
            NotesMakingAppTheme {
                NoteAppScreen()
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Note Reminders"
            val descriptionText = "Channel for note reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("NOTE_REMINDER_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteAppScreen() {
    val context = LocalContext.current
    val newNoteContent = remember { mutableStateOf("") }
    val savedNotes = remember { mutableStateOf<List<Note>>(emptyList()) }

    val calendar = Calendar.getInstance()
    val selectedDateTime = remember { mutableStateOf<Calendar?>(null) }

    val formattedDateTime = selectedDateTime.value?.let {
        SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(it.time)
    } ?: ""

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            selectedDateTime.value = (selectedDateTime.value ?: Calendar.getInstance()).apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDateTime.value = (selectedDateTime.value ?: Calendar.getInstance()).apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            timePickerDialog.show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // THIS IS THE NEW PERMISSION CHECK
    fun checkAndRequestAlarmPermission(onGranted: () -> Unit) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                // Permission is already granted, run the function
                onGranted()
            } else {
                // Permission is not granted, send user to settings
                Toast.makeText(
                    context,
                    "Please grant permission to set alarms",
                    Toast.LENGTH_LONG
                ).show()
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                    context.startActivity(it)
                }
            }
        } else {
            // On older versions, the manifest permission is enough
            onGranted()
        }
    }


    fun scheduleReminder(noteContent: String, timestamp: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("NOTE_CONTENT", noteContent)
            putExtra("NOTE_ID", timestamp.toInt())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            timestamp.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timestamp,
                pendingIntent
            )
            Toast.makeText(context, "Reminder set!", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            // This catch is still a good fallback
            Toast.makeText(context, "Permission denied to set alarm", Toast.LENGTH_SHORT).show()
        }
    }


    fun loadNotes() {
        val filesDir = context.filesDir
        val noteFiles = filesDir.listFiles { file ->
            file.name.startsWith("note_") && file.name.endsWith(".txt")
        }
        savedNotes.value = noteFiles
            ?.map { Note(fileName = it.name, content = it.readText()) }
            ?.sortedByDescending { it.fileName }
            ?: emptyList()
    }

    fun saveNote(): String? {
        if (newNoteContent.value.isBlank()) {
            Toast.makeText(context, "Note cannot be empty", Toast.LENGTH_SHORT).show()
            return null
        }
        val fileName = "note_${System.currentTimeMillis()}.txt"
        val file = File(context.filesDir, fileName)
        try {
            file.writeText(newNoteContent.value)
            Toast.makeText(context, "Note saved!", Toast.LENGTH_SHORT).show()
            return fileName
        } catch (e: IOException) {
            Toast.makeText(context, "Error saving note", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    fun deleteNote(fileName: String) {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.delete()
            Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
            loadNotes()
        }
    }

    LaunchedEffect(Unit) {
        loadNotes()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Saved Notes",
                style = MaterialTheme. typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(savedNotes.value) { note ->
                    SavedNoteCard(note = note, onDelete = { deleteNote(note.fileName) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "New Note",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = newNoteContent.value,
                onValueChange = { newNoteContent.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                placeholder = { Text("Start writing...") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (formattedDateTime.isNotEmpty()) {
                Text(
                    text = "Reminder: $formattedDateTime",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // THIS BUTTON'S ONCLICK HAS BEEN UPDATED
                OutlinedButton(
                    onClick = {
                        // We now check for permission first
                        checkAndRequestAlarmPermission {
                            // This code only runs if permission is granted
                            datePickerDialog.show()
                        }
                    },
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Set Reminder")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reminder")
                }
                Button(onClick = {
                    val savedFileName = saveNote()
                    if (savedFileName != null) {
                        val reminderTime = selectedDateTime.value
                        if (reminderTime != null && reminderTime.timeInMillis > System.currentTimeMillis()) {
                            scheduleReminder(newNoteContent.value, reminderTime.timeInMillis)
                        }
                        loadNotes()
                        newNoteContent.value = ""
                        selectedDateTime.value = null
                    }
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Done, contentDescription = "Save Note")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SavedNoteCard(note: Note, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = note.content,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Note",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NoteAppScreenPreview() {
    NotesMakingAppTheme {
        NoteAppScreen()
    }
}