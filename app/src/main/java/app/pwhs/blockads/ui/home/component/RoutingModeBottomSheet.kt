package app.pwhs.blockads.ui.home.component

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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pwhs.blockads.R
import app.pwhs.blockads.data.datastore.AppPreferences
import app.pwhs.blockads.data.entities.ProxyProfile
import app.pwhs.blockads.data.entities.ShadowsocksProfile
import app.pwhs.blockads.data.entities.WireGuardProfile
import app.pwhs.blockads.data.geoip.GeoIpLookup
import app.pwhs.blockads.ui.theme.AccentBlue
import app.pwhs.blockads.ui.theme.DangerRed
import app.pwhs.blockads.ui.theme.TextSecondary
import app.pwhs.blockads.utils.WireGuardGeoIpHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutingModeBottomSheet(
    currentMode: String,
    isRootAvailable: Boolean,
    activeWgProfile: WireGuardProfile?,
    activeProxyProfile: ProxyProfile?,
    activeSsProfile: ShadowsocksProfile?,
    activeProxyType: String,
    onSelectMode: (String) -> Unit,
    onConfigureProxyVpn: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.routing_mode_sheet_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.routing_mode_sheet_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Local VPN Mode
            RoutingModeCard(
                icon = Icons.Default.VpnLock,
                iconTint = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.routing_mode_local_vpn_title),
                description = stringResource(R.string.routing_mode_local_vpn_desc),
                badge = stringResource(R.string.routing_mode_recommended),
                isSelected = currentMode == AppPreferences.ROUTING_MODE_DIRECT,
                onClick = {
                    onSelectMode(AppPreferences.ROUTING_MODE_DIRECT)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Proxy & VPN Mode (Unified WireGuard + Outline / Shadowsocks)
            val isProxyMode = currentMode == AppPreferences.ROUTING_MODE_PROXY_VPN ||
                currentMode == AppPreferences.ROUTING_MODE_WIREGUARD ||
                currentMode == AppPreferences.ROUTING_MODE_SHADOWSOCKS

            val isWg = activeProxyType == AppPreferences.PROXY_TYPE_WIREGUARD || currentMode == AppPreferences.ROUTING_MODE_WIREGUARD
            val hasActiveProfile = if (isWg) activeWgProfile != null else (activeProxyProfile != null || activeSsProfile != null)

            val proxyStatusText = if (isWg && activeWgProfile != null) {
                val endpoint = activeWgProfile.config.peers.firstOrNull()?.endpoint.orEmpty()
                val cc = WireGuardGeoIpHelper.getCountryCodeFast(endpoint, activeWgProfile.name)
                val flag = cc?.let { GeoIpLookup.countryCodeToEmoji(it) }.orEmpty()
                if (flag.isNotEmpty()) "$flag ${activeWgProfile.name} (WireGuard)" else "${activeWgProfile.name} (WireGuard)"
            } else if (!isWg && activeProxyProfile != null) {
                val cc = WireGuardGeoIpHelper.getCountryCodeFast(activeProxyProfile.server, activeProxyProfile.displayName)
                val flag = cc?.let { GeoIpLookup.countryCodeToEmoji(it) }.orEmpty()
                val protoLabel = activeProxyProfile.protocol.name.lowercase().replaceFirstChar { it.uppercase() }
                if (flag.isNotEmpty()) "$flag ${activeProxyProfile.displayName} ($protoLabel)" else "${activeProxyProfile.displayName} ($protoLabel)"
            } else if (!isWg && activeSsProfile != null) {
                val cc = WireGuardGeoIpHelper.getCountryCodeFast(activeSsProfile.server, activeSsProfile.tag)
                val flag = cc?.let { GeoIpLookup.countryCodeToEmoji(it) }.orEmpty()
                if (flag.isNotEmpty()) "$flag ${activeSsProfile.displayName} (Outline)" else "${activeSsProfile.displayName} (Outline)"
            } else {
                stringResource(R.string.routing_mode_no_proxy_profile)
            }

            RoutingModeCard(
                icon = if (isWg) Icons.Default.Security else Icons.Default.VpnKey,
                iconTint = if (isWg) AccentBlue else Color(0xFF0D9488),
                title = stringResource(R.string.routing_mode_proxy_vpn_title),
                description = stringResource(R.string.routing_mode_proxy_vpn_desc),
                statusText = proxyStatusText,
                statusColor = if (hasActiveProfile) MaterialTheme.colorScheme.primary else DangerRed,
                actionLabel = stringResource(R.string.routing_mode_configure_proxy),
                onAction = onConfigureProxyVpn,
                isSelected = isProxyMode,
                onClick = {
                    if (hasActiveProfile) {
                        onSelectMode(AppPreferences.ROUTING_MODE_PROXY_VPN)
                    } else {
                        onConfigureProxyVpn()
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Root Proxy Mode
            RoutingModeCard(
                icon = Icons.Default.Bolt,
                iconTint = Color(0xFF7C3AED),
                title = stringResource(R.string.routing_mode_root_proxy_title),
                description = stringResource(R.string.routing_mode_root_proxy_desc),
                statusText = if (isRootAvailable) {
                    stringResource(R.string.routing_mode_root_available)
                } else {
                    stringResource(R.string.routing_mode_root_unavailable)
                },
                statusColor = if (isRootAvailable) MaterialTheme.colorScheme.primary else DangerRed,
                isSelected = currentMode == AppPreferences.ROUTING_MODE_ROOT,
                onClick = {
                    onSelectMode(AppPreferences.ROUTING_MODE_ROOT)
                }
            )
        }
    }
}

@Composable
private fun RoutingModeCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    statusText: String? = null,
    statusColor: Color? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (badge != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (statusText != null && statusColor != null) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onClick
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onAction) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
