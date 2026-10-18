package app.pwhs.blockads.ui.proxyvpn.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pwhs.blockads.R
import app.pwhs.blockads.data.entities.ProxyProfile
import app.pwhs.blockads.data.entities.ProxyProtocol
import app.pwhs.blockads.data.geoip.GeoIpLookup
import app.pwhs.blockads.utils.WireGuardGeoIpHelper

@Composable
fun ProxyNodeRow(
    profile: ProxyProfile,
    isActive: Boolean,
    onClick: () -> Unit,
    onRename: (ProxyProfile) -> Unit,
    onDelete: (ProxyProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val countryCode = remember(profile.server, profile.tag) {
        WireGuardGeoIpHelper.getCountryCodeFast(profile.server, profile.tag)
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
        val details = when (profile.protocol) {
            ProxyProtocol.SHADOWSOCKS -> profile.method.ifEmpty { "AEAD" }
            ProxyProtocol.TROJAN -> if (profile.network == "ws") "WS • TLS" else "TLS"
            ProxyProtocol.VLESS -> {
                val net = if (profile.network == "ws") "WS" else "TCP"
                val sec = if (profile.security.isNotEmpty()) profile.security.uppercase() else "TLS"
                "$net • $sec"
            }
        }
        "${profile.server}:${profile.port} • $details"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        border = if (isActive) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag or Key Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (flagEmoji != null) {
                    Text(text = flagEmoji, fontSize = 22.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = cleanDisplayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Protocol badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (profile.protocol) {
                            ProxyProtocol.VLESS -> MaterialTheme.colorScheme.tertiaryContainer
                            ProxyProtocol.TROJAN -> MaterialTheme.colorScheme.secondaryContainer
                            ProxyProtocol.SHADOWSOCKS -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = profile.protocol.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (profile.protocol) {
                                ProxyProtocol.VLESS -> MaterialTheme.colorScheme.onTertiaryContainer
                                ProxyProtocol.TROJAN -> MaterialTheme.colorScheme.onSecondaryContainer
                                ProxyProtocol.SHADOWSOCKS -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    if (isActive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = stringResource(R.string.routing_mode_active),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            RadioButton(
                selected = isActive,
                onClick = onClick,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
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
