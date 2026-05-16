package com.chiko.musicplayer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.chiko.musicplayer.data.Folder

/**
 * Pick (or create) the Music subfolder a whole playlist will be saved into.
 * Defaults to a "New folder" pre-filled with the playlist title.
 */
@Composable
fun PlaylistDownloadDialog(
    existingFolders: List<Folder>,
    trackCount: Int,
    suggestedName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var newFolderMode by remember { mutableStateOf(true) }
    var newFolderName by remember { mutableStateOf(suggestedName.take(64)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Download $trackCount track${if (trackCount == 1) "" else "s"}",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                if (newFolderMode) {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it.take(64) },
                        label = { Text("Folder name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newFolderName.isNotBlank()) onConfirm(newFolderName.trim())
                        }),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (existingFolders.isNotEmpty()) {
                        Spacer(Modifier.size(8.dp))
                        TextButton(onClick = { newFolderMode = false }) {
                            Text("Choose existing folder")
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.heightIn(max = 360.dp),
                    ) {
                        item {
                            FolderPickerRow(
                                icon = Icons.Rounded.Add,
                                label = "New folder…",
                                onClick = { newFolderMode = true },
                            )
                        }
                        items(existingFolders, key = { it.id }) { folder ->
                            FolderPickerRow(
                                icon = Icons.Rounded.Folder,
                                label = folder.name,
                                subtitle = "${folder.songCount} ${if (folder.songCount == 1) "track" else "tracks"}",
                                onClick = { onConfirm(folder.name) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (newFolderMode) {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) onConfirm(newFolderName.trim())
                    },
                    enabled = newFolderName.isNotBlank(),
                ) {
                    Text("Create & download")
                }
            } else {
                Spacer(Modifier.size(0.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    )
}
