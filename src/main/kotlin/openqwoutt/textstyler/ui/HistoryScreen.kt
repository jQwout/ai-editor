package openqwoutt.miniapp.textstyler.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import openqwoutt.miniapp.textstyler.domain.model.Interaction
import openqwoutt.miniapp.textstyler.domain.model.InteractionStatus
import openqwoutt.miniapp.textstyler.presentation.HistoryViewModel

// Telegram-style dark palette (same as TextStylerScreen)
private val Bg = Color(0xFF0F0F0F)
private val Surface = Color(0xFF1A1A1A)
private val Accent = Color(0xFF8774E1)
private val AccentSoft = Color(0xFF8774E1).copy(alpha = 0.15f)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8E8E93)
private val ErrorColor = Color(0xFFF44336)
private val SuccessColor = Color(0xFF4CAF50)

/**
 * History screen showing past user interactions.
 */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Back",
                        tint = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "History",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (state.items.isNotEmpty()) {
                TextButton(onClick = { viewModel.deleteAll() }) {
                    Text(
                        text = "Clear All",
                        color = ErrorColor,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Empty state
        if (state.items.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No history yet",
                    color = TextSecondary,
                    fontSize = 16.sp
                )
            }
        } else {
            // History list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = state.items,
                    key = { it.id }
                ) { interaction ->
                    InteractionItem(
                        interaction = interaction,
                        onDelete = { viewModel.delete(interaction.id) }
                    )
                }
            }
        }
    }
}

/**
 * Single interaction item with expandable details.
 */
@Composable
private fun InteractionItem(
    interaction: Interaction,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status icon
                Icon(
                    imageVector = if (interaction.status == InteractionStatus.SUCCESS)
                        Icons.Default.Refresh else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (interaction.status == InteractionStatus.SUCCESS)
                        SuccessColor else ErrorColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = interaction.mode.uppercase(),
                    color = if (interaction.status == InteractionStatus.SUCCESS)
                        Accent else ErrorColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = interaction.relativeTime,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Preview
        Text(
            text = "\"${interaction.inputPreview}\"",
            color = TextPrimary,
            fontSize = 12.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 1
        )

        if (interaction.outputText.isNullOrBlank().not()) {
            Text(
                text = "→ ${interaction.outputPreview}",
                color = if (interaction.status == InteractionStatus.SUCCESS)
                    TextPrimary else ErrorColor,
                fontSize = 12.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 1
            )
        }

        // Expanded content
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))

                // Full input
                Text(
                    text = "INPUT:",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = interaction.inputText,
                    color = TextPrimary,
                    fontSize = 12.sp
                )

                if (interaction.outputText.isNullOrBlank().not()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "OUTPUT:",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = interaction.outputText,
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }

                // Error message
                if (interaction.status == InteractionStatus.ERROR &&
                    interaction.errorMessage.isNullOrBlank().not()
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ERROR:",
                        color = ErrorColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = interaction.errorMessage,
                        color = ErrorColor,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ErrorColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}