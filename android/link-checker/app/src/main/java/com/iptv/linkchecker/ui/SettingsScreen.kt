package com.iptv.linkchecker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iptv.linkchecker.data.IgnoredDomain
import com.iptv.linkchecker.ui.theme.CardSurface
import com.iptv.linkchecker.ui.theme.GradientEnd
import com.iptv.linkchecker.ui.theme.GradientStart
import com.iptv.linkchecker.ui.theme.OnSurfaceDim
import com.iptv.linkchecker.ui.theme.PurplePrimary
import com.iptv.linkchecker.ui.theme.SurfaceVariant
import com.iptv.linkchecker.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val ignoredDomains by viewModel.ignoredDomains.collectAsState()
    var newDomainText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Gradient Header ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(GradientStart, GradientEnd)
                    ),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Manage your preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }

        // ── Content ─────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── Section Title Card ──────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PurplePrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShieldMoon,
                                contentDescription = null,
                                tint = PurplePrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🛡️ Domain Ignore List",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Channels from ignored domains will auto-pass as LIVE without checking. Use this for premium providers that block bot requests.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceDim
                            )
                        }
                    }
                }
            }

            // ── Add Domain Input ────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newDomainText,
                            onValueChange = { newDomainText = it },
                            placeholder = {
                                Text(
                                    "e.g. cdn.my-provider.com",
                                    color = OnSurfaceDim.copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = OnSurfaceDim.copy(alpha = 0.3f),
                                cursorColor = PurplePrimary,
                                focusedLabelColor = PurplePrimary
                            )
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                val domain = newDomainText.trim()
                                if (domain.isNotBlank()) {
                                    viewModel.addIgnoredDomain(domain)
                                    newDomainText = ""
                                }
                            },
                            enabled = newDomainText.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurplePrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add")
                        }
                    }
                }
            }

            // ── Domain Count Label ──────────────────────────────────────
            item {
                Text(
                    text = if (ignoredDomains.isEmpty()) ""
                    else "${ignoredDomains.size} ignored domain${if (ignoredDomains.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceDim,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            // ── Empty State ─────────────────────────────────────────────
            if (ignoredDomains.isEmpty()) {
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(400)) +
                                slideInVertically(animationSpec = tween(400)) { it / 2 }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShieldMoon,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = OnSurfaceDim.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No ignored domains",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurfaceDim
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Add domains that should skip checking.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceDim.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Domain List ─────────────────────────────────────────────
            itemsIndexed(
                items = ignoredDomains,
                key = { _, domain -> domain.id }
            ) { index, domain ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = index * 50)) +
                            slideInVertically(
                                animationSpec = tween(300, delayMillis = index * 50)
                            ) { it / 3 },
                    exit = fadeOut(animationSpec = tween(200)) +
                            slideOutVertically(animationSpec = tween(200)) { -it / 3 }
                ) {
                    DomainCard(
                        domain = domain,
                        onDelete = { viewModel.removeIgnoredDomain(domain) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DomainCard(
    domain: IgnoredDomain,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PurplePrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = PurplePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = domain.domain,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatTimestamp(domain.addedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDim.copy(alpha = 0.7f)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove domain",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < 60_000L -> "Added just now"
        diff < 3_600_000L -> "Added ${diff / 60_000L}m ago"
        diff < 86_400_000L -> "Added ${diff / 3_600_000L}h ago"
        diff < 604_800_000L -> "Added ${diff / 86_400_000L}d ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
            "Added ${sdf.format(java.util.Date(millis))}"
        }
    }
}
