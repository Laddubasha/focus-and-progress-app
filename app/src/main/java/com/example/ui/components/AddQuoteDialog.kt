package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun AddQuoteDialog(
    onDismiss: () -> Unit,
    onAddQuote: (quoteText: String, author: String) -> Unit
) {
    var quoteText by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Custom Motivational Quote",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("add_quote_dialog_title")
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = quoteText,
                    onValueChange = {
                        quoteText = it
                        error = false
                    },
                    label = { Text("Motivational Quote") },
                    placeholder = { Text("e.g., Discipline equals freedom.") },
                    isError = error,
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quote_text_input")
                )
                if (error) {
                    Text(
                        text = "Quote text cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author / Source (Optional)") },
                    placeholder = { Text("e.g., Jocko Willink") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quote_author_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (quoteText.isBlank()) {
                        error = true
                    } else {
                        onAddQuote(quoteText, author)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_quote_button")
            ) {
                Text("Add Quote")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_quote_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
