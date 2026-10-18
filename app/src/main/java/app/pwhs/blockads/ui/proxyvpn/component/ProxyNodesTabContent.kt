package app.pwhs.blockads.ui.proxyvpn.component

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pwhs.blockads.R
import app.pwhs.blockads.data.entities.ProxyProfile
import app.pwhs.blockads.utils.NodePingResult

@Composable
fun ProxyNodesTabContent(
    profiles: List<ProxyProfile>,
    activeId: String?,
    isProxyActive: Boolean,
    pingResults: Map<String, NodePingResult> = emptyMap(),
    isPingingAll: Boolean = false,
    onPingNode: (String) -> Unit = {},
    onPingAll: () -> Unit = {},
    onSelectProfile: (ProxyProfile) -> Unit,
    onAddNodeClick: () -> Unit,
    onRenameProfile: (String, String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    searchQuery: String = "",
    onClearSearch: () -> Unit = {},
    hasSubscription: Boolean = false,
    isUpdatingSubscription: Boolean = false,
    onUpdateSubscription: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var renameTarget by remember { mutableStateOf<ProxyProfile?>(null) }
    var deleteTarget by remember { mutableStateOf<ProxyProfile?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (profiles.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Dns,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.proxy_node_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.proxy_node_empty_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            val availableProtocols = remember(profiles) {
                profiles.map { it.protocol }.distinct()
            }
            var selectedProtocol by remember { mutableStateOf<app.pwhs.blockads.data.entities.ProxyProtocol?>(null) }
            var filterObfuscatedOnly by remember { mutableStateOf(false) }

            val obfuscatedCount = remember(profiles) { profiles.count { it.isObfuscated } }

            val filteredProfiles = remember(profiles, searchQuery, selectedProtocol, filterObfuscatedOnly) {
                profiles.filter { profile ->
                    val matchesObfs = !filterObfuscatedOnly || profile.isObfuscated
                    val matchesProtocol = selectedProtocol == null || profile.protocol == selectedProtocol
                    val matchesQuery = searchQuery.isBlank() ||
                        profile.displayName.contains(searchQuery, ignoreCase = true) ||
                        profile.server.contains(searchQuery, ignoreCase = true) ||
                        profile.tag.contains(searchQuery, ignoreCase = true)
                    matchesObfs && matchesProtocol && matchesQuery
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Compact Filter Chips + Ping All Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            androidx.compose.material3.FilterChip(
                                selected = selectedProtocol == null && !filterObfuscatedOnly,
                                onClick = {
                                    selectedProtocol = null
                                    filterObfuscatedOnly = false
                                },
                                label = {
                                    Text(
                                        "${stringResource(R.string.proxy_filter_all)} (${profiles.size})",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        if (obfuscatedCount > 0) {
                            item {
                                androidx.compose.material3.FilterChip(
                                    selected = filterObfuscatedOnly,
                                    onClick = {
                                        filterObfuscatedOnly = !filterObfuscatedOnly
                                        if (filterObfuscatedOnly) selectedProtocol = null
                                    },
                                    label = {
                                        Text(
                                            "${stringResource(R.string.proxy_filter_obfuscated)} ($obfuscatedCount)",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                        if (availableProtocols.size > 1) {
                            items(availableProtocols) { protocol ->
                                val count = profiles.count { it.protocol == protocol }
                                androidx.compose.material3.FilterChip(
                                    selected = selectedProtocol == protocol && !filterObfuscatedOnly,
                                    onClick = {
                                        filterObfuscatedOnly = false
                                        selectedProtocol = if (selectedProtocol == protocol) null else protocol
                                    },
                                    label = {
                                        Text(
                                            "${protocol.displayName} ($count)",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onPingAll,
                        enabled = !isPingingAll,
                        modifier = Modifier.size(34.dp)
                    ) {
                        if (isPingingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.proxy_ping_test_all),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (hasSubscription) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onUpdateSubscription,
                            enabled = !isUpdatingSubscription,
                            modifier = Modifier.size(34.dp)
                        ) {
                            if (isUpdatingSubscription) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Update Subscription",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // List or Empty Search
                if (filteredProfiles.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.proxy_search_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            onClearSearch()
                            selectedProtocol = null
                            filterObfuscatedOnly = false
                        }) {
                            Text(stringResource(R.string.firewall_clear_search))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = filteredProfiles,
                            key = { it.id }
                        ) { profile ->
                            ProxyNodeRow(
                                profile = profile,
                                isActive = isProxyActive && profile.id == activeId,
                                pingResult = pingResults[profile.id],
                                onPing = { onPingNode(profile.id) },
                                onClick = { onSelectProfile(profile) },
                                onRename = { renameTarget = it },
                                onDelete = { deleteTarget = it }
                            )
                        }
                    }
                }
            }
        }

        // Add Node FAB
        ExtendedFloatingActionButton(
            text = { Text(stringResource(R.string.proxy_node_add_node)) },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            onClick = onAddNodeClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

    // Rename Dialog
    renameTarget?.let { profile ->
        var newName by remember(profile.id) { mutableStateOf(profile.name.ifEmpty { profile.tag }) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.wireguard_rename_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.wireguard_rename_dialog_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newName.trim()
                        if (trimmed.isNotEmpty()) {
                            onRenameProfile(profile.id, trimmed)
                        }
                        renameTarget = null
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text(stringResource(R.string.wireguard_action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.wireguard_action_cancel))
                }
            }
        )
    }

    // Delete Confirmation Dialog
    deleteTarget?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.shadowsocks_delete_confirm_title)) },
            text = {
                Text(stringResource(R.string.shadowsocks_delete_confirm_desc, profile.displayName))
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProfile(profile.id)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.wireguard_action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.wireguard_action_cancel))
                }
            }
        )
    }
}
