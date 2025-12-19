// ui/components/ChapterItem.kt
package com.yume.reader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ChapterItem(
    chapterNumber: Int,
    title: String,
    durationMinutes: Int,
    isRead: Boolean,
    isCurrent: Boolean,
    onChapterClick: () -> Unit,
    onToggleRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onChapterClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Номер главы и статус
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "#$chapterNumber",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                // Индикатор прочтения
                IconButton(
                    onClick = onToggleRead,
                    modifier = Modifier.size(24.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = if (isRead) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = if (isRead) "Прочитано" else "Не прочитано",
                        tint = if (isRead) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Заголовок главы
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Время чтения
                Text(
                    text = formatReadingTime(durationMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Индикатор текущей главы
            if (isCurrent) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Читаю сейчас",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun formatReadingTime(minutes: Int): String {
    return when {
        minutes >= 60 -> {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            if (remainingMinutes > 0) "$hours ч $remainingMinutes мин" else "$hours ч"
        }
        else -> "$minutes мин"
    }
}