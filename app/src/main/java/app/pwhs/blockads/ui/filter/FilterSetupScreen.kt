package app.pwhs.blockads.ui.filter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pwhs.blockads.R
import app.pwhs.blockads.data.entities.FilterList as FilterListEntity
import app.pwhs.blockads.ui.event.UiEventEffect
import app.pwhs.blockads.ui.filter.component.AddFilterBottomSheet
import app.pwhs.blockads.ui.filter.component.CustomTabContent
import app.pwhs.blockads.ui.filter.component.filterListItems
import app.pwhs.blockads.ui.filter.component.FilterSearchBar
import app.pwhs.blockads.ui.filter.component.FilterTabRow
import app.pwhs.blockads.ui.filter.component.FilterTopBar
import app.pwhs.blockads.ui.filter.data.FilterCategoryTab
import app.pwhs.blockads.ui.filter.data.matchesCategory
import app.pwhs.blockads.ui.filter.data.resolvedCategory
import app.pwhs.blockads.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSetupScreen(
    modifier: Modifier = Modifier,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    viewModel: FilterSetupViewModel = koinViewModel(),
    onNavigateToFilterDetail: (filterId: Long) -> Unit = {}
) {
    val allLists by viewModel.filterLists.collectAsStateWithLifecycle()
    val filterLists by viewModel.filteredFilterLists.collectAsStateWithLifecycle()
    val isUpdatingFilter by viewModel.isUpdatingFilter.collectAsStateWithLifecycle()
    val updateProgress by viewModel.updateProgress.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val starredUrls by viewModel.starredFilterUrls.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var filterToEdit by remember { mutableStateOf<FilterListEntity?>(null) }
    var isSearchVisible by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(initialPage = 0) { FilterCategoryTab.entries.size }
    val coroutineScope = rememberCoroutineScope()

    UiEventEffect(viewModel.events)

    LaunchedEffect(Unit) {
        viewModel.filterAddedEvent.collect {
            showAddDialog = false
            filterToEdit = null
        }
    }

    val activeCounts = remember(allLists, starredUrls) {
        val counts = mutableMapOf<FilterCategoryTab, Int>()
        for (tab in FilterCategoryTab.entries) {
            counts[tab] = 0
        }
        for (filter in allLists) {
            if (!filter.isEnabled) continue
            counts[FilterCategoryTab.ALL] = (counts[FilterCategoryTab.ALL] ?: 0) + 1
            if (starredUrls.contains(filter.url)) {
                counts[FilterCategoryTab.STARRED] = (counts[FilterCategoryTab.STARRED] ?: 0) + 1
            }
            if (!filter.isBuiltIn) {
                counts[FilterCategoryTab.CUSTOM] = (counts[FilterCategoryTab.CUSTOM] ?: 0) + 1
            } else {
                val cat = filter.resolvedCategory()
                counts[cat] = (counts[cat] ?: 0) + 1
            }
        }
        counts
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            FilterTopBar(
                isSearchVisible = isSearchVisible,
                onToggleSearch = {
                    isSearchVisible = !isSearchVisible
                    if (!isSearchVisible) viewModel.setSearchQuery("")
                },
                canNavigateBack = canNavigateBack,
                onNavigateBack = onNavigateBack,
                isUpdatingFilter = isUpdatingFilter,
                updateProgress = updateProgress,
                onUpdateAllFilters = { viewModel.updateAllFilters() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            FilterSearchBar(
                visible = isSearchVisible,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) }
            )

            FilterTabRow(
                selectedTab = FilterCategoryTab.entries[pagerState.currentPage],
                onTabSelected = { tab ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(tab.ordinal)
                    }
                },
                activeCounts = activeCounts
            )

            val isSearching = searchQuery.isNotBlank()

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val currentTab = FilterCategoryTab.entries[page]
                val displayFilters = remember(filterLists, currentTab, starredUrls) {
                    val tabFilters = filterLists.filter { it.matchesCategory(currentTab, starredUrls) }
                    if (currentTab == FilterCategoryTab.ALL) {
                        tabFilters.sortedWith(compareByDescending<FilterListEntity> { it.isEnabled }.thenBy { it.name })
                    } else {
                        tabFilters
                    }
                }

                if (currentTab == FilterCategoryTab.CUSTOM) {
                    if (isSearching && displayFilters.isEmpty()) {
                        EmptyCategorySearch(searchQuery)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
                        ) {
                            item {
                                CustomTabContent(
                                    customFilters = displayFilters,
                                    onToggle = { viewModel.toggleFilterList(it) },
                                    onDelete = { viewModel.deleteFilterList(it) },
                                    onEdit = { filterToEdit = it },
                                    onFilterClick = onNavigateToFilterDetail,
                                    onShowAddDialog = { showAddDialog = true },
                                    starredUrls = starredUrls,
                                    onToggleStar = { viewModel.toggleStarredFilter(it) }
                                )
                            }
                        }
                    }
                } else if (currentTab == FilterCategoryTab.STARRED && displayFilters.isEmpty() && !isSearching) {
                    EmptyStarredFilter()
                } else {
                    if (isSearching && displayFilters.isEmpty()) {
                        EmptyCategorySearch(searchQuery)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
                        ) {
                            filterListItems(
                                filters = displayFilters,
                                onToggle = { viewModel.toggleFilterList(it) },
                                onFilterClick = onNavigateToFilterDetail,
                                starredUrls = starredUrls,
                                onToggleStar = { viewModel.toggleStarredFilter(it) }
                            )
                        }
                    }
                }
            }
        }

        val isAddingCustomFilter by viewModel.isAddingCustomFilter.collectAsStateWithLifecycle()
        val addFilterProgress by viewModel.addFilterProgress.collectAsStateWithLifecycle()
        if (showAddDialog || filterToEdit != null) {
            AddFilterBottomSheet(
                onDismiss = {
                    if (!isAddingCustomFilter) {
                        showAddDialog = false
                        filterToEdit = null
                    }
                },
                onAdd = { name, url, buildLocally ->
                    if (!isAddingCustomFilter) {
                        viewModel.addFilterList(name, url, buildLocally)
                    }
                },
                filterToEdit = filterToEdit,
                onEdit = { filter, name, url, buildLocally ->
                    if (!isAddingCustomFilter) {
                        viewModel.editFilterList(filter, name, url, buildLocally)
                    }
                },
                existingUrls = filterLists.map { it.url },
                isValidating = isAddingCustomFilter,
                progress = addFilterProgress
            )
        }
    }
}

@Composable
private fun EmptyCategorySearch(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = TextSecondary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.filter_search_no_results, searchQuery),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyStarredFilter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = TextSecondary.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.filter_starred_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
