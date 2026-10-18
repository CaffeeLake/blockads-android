package app.pwhs.blockads.ui.proxyvpn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Surface
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
import app.pwhs.blockads.ui.proxyvpn.component.CountryFlagRegistry
import app.pwhs.blockads.ui.proxyvpn.component.ProxyNodesTabContent
import app.pwhs.blockads.ui.proxyvpn.component.WindscribeHeroCard
import app.pwhs.blockads.ui.proxyvpn.component.WireGuardTabContent
import app.pwhs.blockads.utils.NodePingResult
import app.pwhs.blockads.utils.WireGuardGeoIpHelper
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
    val pingResults by viewModel.pingResults.collectAsStateWithLifecycle()
    val isPingingAll by viewModel.isPingingAll.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val networkName by viewModel.networkName.collectAsStateWithLifecycle()
    val networkType by viewModel.networkType.collectAsStateWithLifecycle()
    val currentIp by viewModel.currentIp.collectAsStateWithLifecycle()
    val isRefreshingIp by viewModel.isRefreshingIp.collectAsStateWithLifecycle()
    val lastSubUrl by viewModel.lastSubscriptionUrl.collectAsStateWithLifecycle()
    val isUpdatingSub by viewModel.isUpdatingSubscription.collectAsStateWithLifecycle()
    val proxySubscriptions by viewModel.proxySubscriptions.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val resources = LocalResources.current
    var showAddProxyDialog by remember { mutableStateOf(false) }
    var showSubscriptionsSheet by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

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
                is ProxyVpnUiEvent.DuplicatesRemoved ->
                    snackbarHostState.showSnackbar(
                        if (event.count > 0) "Removed ${event.count} duplicate node(s)"
                        else "No duplicate nodes found"
                    )
                is ProxyVpnUiEvent.SubscriptionUpdated ->
                    snackbarHostState.showSnackbar("Subscription updated ($event.count node(s))")
            }
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val activeWg = remember(wgProfiles, activeWgId) {
        wgProfiles.firstOrNull { it.id == activeWgId } ?: wgProfiles.firstOrNull()
    }
    val activeProxy = remember(proxyProfiles, activeProxyId) {
        proxyProfiles.firstOrNull { it.id == activeProxyId } ?: proxyProfiles.firstOrNull()
    }

    val isWgActive = activeProxyType == AppPreferences.PROXY_TYPE_WIREGUARD
    val showWg = if (isProxyActive) isWgActive else (selectedTab == 0)

    val isCardConnected = isProxyActive && (if (isWgActive) activeWg != null else activeProxy != null)

    val heroServerName = if (showWg) {
        activeWg?.name.orEmpty()
    } else {
        activeProxy?.displayName.orEmpty()
    }

    val heroCountryCode: String? = remember(showWg, activeWg, activeProxy) {
        if (showWg) {
            activeWg?.name?.let { WireGuardGeoIpHelper.getCountryCodeFast(it, it) }
        } else {
            activeProxy?.let { WireGuardGeoIpHelper.getCountryCodeFast(it.server, it.tag) }
        }
    }

    val heroProtocolText = if (showWg) {
        "WireGuard"
    } else {
        activeProxy?.protocolBadgeText ?: "Proxy"
    }

    val heroPort = if (showWg) {
        activeWg?.config?.peers?.firstOrNull()?.endpoint?.substringAfterLast(":", "")?.toIntOrNull() ?: 51820
    } else {
        activeProxy?.port ?: 443
    }

    val heroLatency = if (showWg) {
        null
    } else {
        (activeProxy?.let { pingResults[it.id] } as? NodePingResult.Success)?.latencyMs
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (isSearchActive) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    stringResource(R.string.proxy_search_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.accessibility_navigate_back)
                            )
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            } else {
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
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.proxy_search_hint)
                            )
                        }
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
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val heroHost = if (showWg) {
                activeWg?.config?.peers?.firstOrNull()?.endpoint?.substringBeforeLast(":") ?: "wireguard.vpn"
            } else {
                activeProxy?.server ?: "vpn.pwhs.app"
            }

            // Windscribe-style Cyber Dark Cockpit Hero Card
            WindscribeHeroCard(
                isConnected = isCardConnected,
                onToggleConnection = { viewModel.toggleProxyVpn() },
                serverName = heroServerName,
                countryCode = heroCountryCode,
                protocol = heroProtocolText,
                port = heroPort,
                serverHost = heroHost,
                latencyMs = heroLatency,
                networkName = networkName,
                networkType = networkType,
                currentIp = currentIp,
                isRefreshingIp = isRefreshingIp,
                onRefreshIp = { viewModel.refreshNetworkAndIp() }
            )

            // Modern Segmented Pill Tabs
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 1: WireGuard
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.selectTab(0) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (selectedTab == 0) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val countText = if (wgProfiles.isNotEmpty()) " (${wgProfiles.size})" else ""
                            Text(
                                text = "${stringResource(R.string.proxy_vpn_tab_wireguard)}$countText",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Tab 2: Proxy Nodes
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.selectTab(1) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (selectedTab == 1) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val countText = if (proxyProfiles.isNotEmpty()) " (${proxyProfiles.size})" else ""
                            Text(
                                text = "${stringResource(R.string.proxy_vpn_tab_proxies)}$countText",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> WireGuardTabContent(
                        profiles = wgProfiles,
                        activeId = activeWgId,
                        isProxyActive = isProxyActive && activeProxyType == AppPreferences.PROXY_TYPE_WIREGUARD,
                        isLoading = isLoadingWg,
                        searchQuery = searchQuery,
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
                        pingResults = pingResults,
                        isPingingAll = isPingingAll,
                        searchQuery = searchQuery,
                        onPingNode = { viewModel.pingProxyNode(it) },
                        onPingAll = { viewModel.pingAllProxyNodes() },
                        onSelectProfile = { viewModel.setActiveProxyProfile(it.id) },
                        onAddNodeClick = { showAddProxyDialog = true },
                        onRenameProfile = { id, name -> viewModel.renameProxyProfile(id, name) },
                        onDeleteProfile = { viewModel.deleteProxyProfile(it) },
                        onClearSearch = { searchQuery = "" },
                        hasSubscription = proxySubscriptions.isNotEmpty() || !lastSubUrl.isNullOrBlank(),
                        isUpdatingSubscription = isUpdatingSub,
                        onUpdateSubscription = { viewModel.updateSubscription() },
                        onOpenSubscriptions = { showSubscriptionsSheet = true },
                        onRemoveDuplicates = { viewModel.removeDuplicates() }
                    )
                }
            }
        }
    }

    if (showAddProxyDialog) {
        AddProxyNodeDialog(
            onDismiss = { showAddProxyDialog = false },
            onSave = { uri ->
                viewModel.saveProxyNodeLink(uri)
            }
        )
    }

    if (showSubscriptionsSheet) {
        app.pwhs.blockads.ui.proxyvpn.component.ManageSubscriptionsBottomSheet(
            subscriptions = proxySubscriptions,
            isUpdating = isUpdatingSub,
            onDismiss = { showSubscriptionsSheet = false },
            onUpdateSubscription = { viewModel.updateSubscription(it) },
            onUpdateAll = { viewModel.updateAllSubscriptions() },
            onDeleteSubscription = { id, deleteNodes -> viewModel.deleteSubscription(id, deleteNodes) },
            onAddSubscription = { url ->
                coroutineScope.launch {
                    viewModel.saveProxyNodeLink(url)
                }
            },
            onRemoveDuplicates = { viewModel.removeDuplicates() }
        )
    }
}
