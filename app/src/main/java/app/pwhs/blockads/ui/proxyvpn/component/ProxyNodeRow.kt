package app.pwhs.blockads.ui.proxyvpn.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pwhs.blockads.R
import app.pwhs.blockads.data.entities.ProxyProfile
import app.pwhs.blockads.data.geoip.GeoIpLookup
import app.pwhs.blockads.utils.NodePingResult
import app.pwhs.blockads.utils.WireGuardGeoIpHelper

@Composable
fun ProxyNodeRow(
    profile: ProxyProfile,
    isActive: Boolean,
    pingResult: NodePingResult? = null,
    onPing: () -> Unit,
    onClick: () -> Unit,
    onRename: (ProxyProfile) -> Unit,
    onDelete: (ProxyProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    var countryCode by remember(profile.server, profile.tag) {
        mutableStateOf(WireGuardGeoIpHelper.getCountryCodeFast(profile.server, profile.tag))
    }

    LaunchedEffect(profile.server, profile.tag) {
        if (countryCode == null) {
            countryCode = WireGuardGeoIpHelper.resolveCountryCode(profile.server, profile.tag)
        }
    }

    val flagEmoji = countryCode?.let { GeoIpLookup.countryCodeToEmoji(it) }

    val cleanDisplayName = remember(profile.displayName, flagEmoji) {
        var name = profile.displayName.trim()
        if (flagEmoji != null && name.startsWith(flagEmoji)) {
            name = name.removePrefix(flagEmoji).trim()
        }
        val stripped = name.replace(Regex("^[\\uD83C\\uDDE6-\\uD83C\\uDDFF]{2}\\s*"), "").trim()
        stripped.ifEmpty { profile.displayName }
    }

    val subtitle = remember(profile) {
        "${profile.protocol.displayName} • ${profile.server}:${profile.port}"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        border = if (isActive) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag Image or Server Squircle Icon
            CountryFlag(countryCode = countryCode)

            Spacer(modifier = Modifier.width(12.dp))

            // Main Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cleanDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Latency Indicator on Right
            NodeLatencyBadge(
                result = pingResult,
                onClick = onPing
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Radio Button for Active Selection
            RadioButton(
                selected = isActive,
                onClick = onClick,
                modifier = Modifier.size(32.dp)
            )

            // More Options Dropdown Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.wireguard_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.proxy_ping_single)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onPing()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.wireguard_action_rename)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DriveFileRenameOutline,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRename(profile)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.wireguard_action_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete(profile)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeLatencyBadge(
    result: NodePingResult?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        when (result) {
            null -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = stringResource(R.string.proxy_ping_single),
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Ping",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            is NodePingResult.Testing -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is NodePingResult.Success -> {
                val color = when {
                    result.latencyMs < 150 -> Color(0xFF2E7D32)
                    result.latencyMs < 350 -> Color(0xFFE65100)
                    else -> MaterialTheme.colorScheme.error
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = stringResource(R.string.proxy_ping_ms, result.latencyMs),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
            is NodePingResult.Timeout -> {
                Text(
                    text = stringResource(R.string.proxy_ping_timeout),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
            is NodePingResult.Error -> {
                Text(
                    text = stringResource(R.string.proxy_ping_error),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
