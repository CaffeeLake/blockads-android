package app.pwhs.blockads.ui.proxyvpn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pwhs.blockads.R
import app.pwhs.blockads.data.datastore.AppPreferences
import app.pwhs.blockads.ui.proxyvpn.component.AddProxyNodeDialog
import app.pwhs.blockads.ui.proxyvpn.component.ProxyNodesTabContent
import app.pwhs.blockads.ui.proxyvpn.component.WireGuardTabContent
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyVpnScreen(
    onNavigateBack: () -> Unit,
    onEditProfile: (String) -> Unit,
    onNavigateToVpnSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProxyVpnViewModel = koinViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isProxyActive by viewModel.isProxyActive.collectAsStateWithLifecycle()
    val activeProxyType by viewModel.activeProxyType.collectAsStateWithLifecycle()

    val wgProfiles by viewModel.wgProfiles.collectAsStateWithLifecycle()
    val activeWgId by viewModel.activeWgProfileId.collectAsStateWithLifecycle()
    val isLoadingWg by viewModel.isLoadingWg.collectAsStateWithLifecycle()

    val proxyProfiles by viewModel.proxyProfiles.collectAsStateWithLifecycle()
    val activeProxyId by viewModel.activeProxyProfileId.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    var showAddProxyDialog by remember { mutableStateOf(false) }

    val renamedMsg = stringResource(R.string.wireguard_renamed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProxyVpnUiEvent.ProfileImported ->
                    snackbarHostState.showSnackbar(resources.getString(R.string.wireguard_imported, event.name))
                is ProxyVpnUiEvent.ProfileDeleted ->
                    snackbarHostState.showSnackbar(resources.getString(R.string.wireguard_deleted, event.name))
                is ProxyVpnUiEvent.ProfileActivated ->
                    snackbarHostState.showSnackbar(resources.getString(R.string.wireguard_active, event.name))
                is ProxyVpnUiEvent.ProfileRenamed ->
                    snackbarHostState.showSnackbar(renamedMsg)
                is ProxyVpnUiEvent.ProxyVpnToggled ->
                    snackbarHostState.showSnackbar(
                        if (event.enabled) resources.getString(R.string.wireguard_enabled_restarting)
                        else resources.getString(R.string.wireguard_disabled_restarting)
                    )
                is ProxyVpnUiEvent.HttpsFilteringDisabled ->
                    snackbarHostState.showSnackbar(resources.getString(R.string.wireguard_https_filtering_disabled))
            }
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.proxy_vpn_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.accessibility_navigate_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToVpnSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.vpn_settings_title)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Master Toggle Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isProxyActive) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    }
                )
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.proxy_vpn_master_toggle),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.proxy_vpn_master_toggle_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isProxyActive,
                        onCheckedChange = { viewModel.toggleProxyVpn() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Protocol Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.proxy_vpn_tab_wireguard),
                                maxLines = 1
                            )
                            if (wgProfiles.isNotEmpty()) {
                                Badge(
                                    containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Text("${wgProfiles.size}")
                                }
                            }
                        }
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.proxy_vpn_tab_proxies),
                                maxLines = 1
                            )
                            if (proxyProfiles.isNotEmpty()) {
                                Badge(
                                    containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Text("${proxyProfiles.size}")
                                }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> WireGuardTabContent(
                        profiles = wgProfiles,
                        activeId = activeWgId,
                        isProxyActive = isProxyActive && activeProxyType == AppPreferences.PROXY_TYPE_WIREGUARD,
                        isLoading = isLoadingWg,
                        onSelectProfile = { viewModel.setActiveWireGuardProfile(it) },
                        onEditProfile = onEditProfile,
                        onRenameProfile = { id, name -> viewModel.renameWireGuardProfile(id, name) },
                        onDeleteProfile = { viewModel.deleteWireGuardProfile(it) },
                        onImportUri = { viewModel.importWireGuardFromUri(it) }
                    )

                    1 -> ProxyNodesTabContent(
                        profiles = proxyProfiles,
                        activeId = activeProxyId,
                        isProxyActive = isProxyActive && (activeProxyType == AppPreferences.PROXY_TYPE_SHADOWSOCKS || activeProxyType == AppPreferences.PROXY_TYPE_NODE),
                        onSelectProfile = { viewModel.setActiveProxyProfile(it.id) },
                        onAddNodeClick = { showAddProxyDialog = true },
                        onRenameProfile = { id, name -> viewModel.renameProxyProfile(id, name) },
                        onDeleteProfile = { viewModel.deleteProxyProfile(it) }
                    )
                }
            }
        }
    }

    if (showAddProxyDialog) {
        AddProxyNodeDialog(
            onDismiss = { showAddProxyDialog = false },
            onSave = { uri ->
                val result = viewModel.saveProxyNodeLink(uri)
                result.isSuccess
            }
        )
    }
}
