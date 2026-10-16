package app.pwhs.blockads.ui.filter.component

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.pwhs.blockads.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFilterBottomSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, url: String, buildLocally: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    existingUrls: List<String> = emptyList(),
    isValidating: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var name by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var urlList by remember { mutableStateOf(emptyList<String>()) }
    var buildLocally by remember { mutableStateOf(false) }
    var showFormatsHelp by remember { mutableStateOf(false) }

    val nameTrimmed = name.trim()
    val inputTrimmed = urlInput.trim()

    val urlInputError = when {
        inputTrimmed.isEmpty() -> null
        !inputTrimmed.startsWith("http://") && !inputTrimmed.startsWith("https://") ->
            stringResource(R.string.filter_error_invalid_url)
        !Patterns.WEB_URL.matcher(inputTrimmed).matches() ->
            stringResource(R.string.filter_error_invalid_url)
        urlList.any { it.equals(inputTrimmed, ignoreCase = true) } ->
            stringResource(R.string.filter_error_duplicate_url)
        existingUrls.any { it.equals(inputTrimmed, ignoreCase = true) } ->
            stringResource(R.string.filter_error_duplicate_url)
        else -> null
    }

    val isInputValid = inputTrimmed.isNotEmpty() && urlInputError == null

    val addUrlFromInput: (String) -> Unit = { raw ->
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        val validNew = lines.filter { line ->
            (line.startsWith("http://") || line.startsWith("https://")) &&
            Patterns.WEB_URL.matcher(line).matches() &&
            !urlList.any { it.equals(line, ignoreCase = true) }
        }
        if (validNew.isNotEmpty()) {
            urlList = urlList + validNew
            urlInput = ""
        }
    }

    val effectiveUrls = remember(urlList, inputTrimmed, isInputValid) {
        if (isInputValid && !urlList.any { it.equals(inputTrimmed, ignoreCase = true) }) {
            urlList + inputTrimmed
        } else {
            urlList
        }
    }

    val isValid = nameTrimmed.isNotBlank() && effectiveUrls.isNotEmpty() && (inputTrimmed.isEmpty() || isInputValid)

    val handleDismiss: () -> Unit = {
        if (!isValidating) {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = handleDismiss,
        sheetState = sheetState,
        modifier = modifier
            .imePadding()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Icon + Title + Help '?' toggle button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.settings_add_filter_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.filter_custom),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Help Button '?'
                FilledTonalIconButton(
                    onClick = { showFormatsHelp = !showFormatsHelp },
                    colors = if (showFormatsHelp) {
                        IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        IconButtonDefaults.filledTonalIconButtonColors()
                    }
                ) {
                    Icon(
                        imageVector = if (showFormatsHelp) Icons.AutoMirrored.Filled.Help else Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = stringResource(R.string.filter_supported_formats_title)
                    )
                }
            }

            // Expandable Help Card detailing supported formats
            AnimatedVisibility(
                visible = showFormatsHelp,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SupportedFormatsCard()
            }

            // Filter Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.settings_add_filter_name)) },
                placeholder = { Text("e.g. HaGeZi Multi Pro") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (name.isNotEmpty() && !isValidating) {
                        IconButton(onClick = { name = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = !isValidating,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            // URL Input Row + Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text(stringResource(R.string.settings_add_filter_url)) },
                    placeholder = { Text("https://example.com/filter.txt") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (urlInput.isNotEmpty() && !isValidating) {
                            IconButton(onClick = { urlInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        } else if (urlInput.isEmpty() && !isValidating) {
                            IconButton(
                                onClick = {
                                    clipboardManager.getText()?.text?.let { text ->
                                        if (text.contains("\n")) {
                                            addUrlFromInput(text)
                                        } else {
                                            urlInput = text.trim()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = stringResource(R.string.filter_paste_clipboard),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    isError = urlInputError != null,
                    supportingText = urlInputError?.let { err ->
                        { Text(err, color = MaterialTheme.colorScheme.error) }
                    },
                    enabled = !isValidating,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isInputValid && !isValidating) {
                                addUrlFromInput(urlInput)
                            }
                        }
                    )
                )

                FilledTonalIconButton(
                    onClick = { addUrlFromInput(urlInput) },
                    enabled = isInputValid && !isValidating,
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.filter_add_url_button)
                    )
                }
            }

            // List of added URL cards
            FilterUrlChipList(
                urls = urlList,
                onRemoveUrl = { urlList = urlList - it },
                onClearAll = { urlList = emptyList() },
                enabled = !isValidating
            )

            // Build Mode Selector Card (Local Go engine vs Cloud compiler)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = !isValidating) {
                        buildLocally = !buildLocally
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (buildLocally) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (buildLocally) Icons.Outlined.PhoneAndroid else Icons.Outlined.Cloud,
                            contentDescription = null,
                            tint = if (buildLocally) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.filter_build_locally),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (buildLocally) {
                                stringResource(R.string.filter_build_mode_local_desc)
                            } else {
                                stringResource(R.string.filter_build_mode_server_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = buildLocally,
                        onCheckedChange = { buildLocally = it },
                        enabled = !isValidating
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions: Cancel & Add
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = handleDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isValidating
                ) {
                    Text(stringResource(R.string.settings_cancel))
                }

                Button(
                    onClick = {
                        if (isValid && !isValidating) {
                            onAdd(nameTrimmed, effectiveUrls.joinToString("\n"), buildLocally)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    enabled = isValid && !isValidating
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.settings_add),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
