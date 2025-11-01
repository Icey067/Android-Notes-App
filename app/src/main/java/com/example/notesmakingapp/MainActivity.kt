package com.example.notesmakingapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
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

data class Note(val fileName: String, val content: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotesMakingAppTheme {
                NoteAppScreen()
            }
        }
    }
}

@Composable
fun NoteAppScreen() {
    val context = LocalContext.current
    val newNoteContent = remember { mutableStateOf("") }
    val savedNotes = remember { mutableStateOf<List<Note>>(emptyList()) }

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

    fun saveNote() {
        if (newNoteContent.value.isBlank()) {
            Toast.makeText(context, "Note cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        val fileName = "note_${System.currentTimeMillis()}.txt"
        val file = File(context.filesDir, fileName)
        try {
            file.writeText(newNoteContent.value)
            Toast.makeText(context, "Note saved!", Toast.LENGTH_SHORT).show()
            newNoteContent.value = ""
            loadNotes()
        } catch (e: IOException) {
            Toast.makeText(context, "Error saving note", Toast.LENGTH_SHORT).show()
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
                style = MaterialTheme.typography.headlineSmall,
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

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = { saveNote() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Done, contentDescription = "Save Note")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save")
                }
                OutlinedButton(onClick = { newNoteContent.value = "" }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear Input")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
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