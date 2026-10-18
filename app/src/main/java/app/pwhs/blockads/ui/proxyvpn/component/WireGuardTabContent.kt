package app.pwhs.blockads.ui.proxyvpn.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import app.pwhs.blockads.data.entities.WireGuardProfile
import app.pwhs.blockads.ui.wireguard.component.EmptyState
import app.pwhs.blockads.ui.wireguard.component.ProfileRow

@Composable
fun WireGuardTabContent(
    profiles: List<WireGuardProfile>,
    activeId: String?,
    isProxyActive: Boolean,
    isLoading: Boolean,
    onSelectProfile: (String) -> Unit,
    onEditProfile: (String) -> Unit,
    onRenameProfile: (String, String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onImportUri: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var renameTarget by remember { mutableStateOf<WireGuardProfile?>(null) }
    var deleteTarget by remember { mutableStateOf<WireGuardProfile?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onImportUri(it) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator()
        }

        if (profiles.isEmpty() && !isLoading) {
            EmptyState(modifier = Modifier.fillMaxSize())
        } else if (profiles.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileRow(
                        profile = profile,
                        isActive = isProxyActive && profile.id == activeId,
                        onClick = { onSelectProfile(profile.id) },
                        onEdit = { onEditProfile(profile.id) },
                        onRename = { renameTarget = profile },
                        onDelete = { deleteTarget = profile },
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
            icon = { Icon(Icons.Filled.FileOpen, contentDescription = null) },
            text = { Text(stringResource(R.string.wireguard_import_button)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    renameTarget?.let { target ->
        var text by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.wireguard_rename_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameProfile(target.id, text.trim())
                        renameTarget = null
                    },
                    enabled = text.isNotBlank() && text != target.name,
                ) { Text(stringResource(R.string.wireguard_action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.wireguard_action_cancel))
                }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.wireguard_delete_dialog_title, target.name)) },
            text = { Text(stringResource(R.string.wireguard_delete_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteProfile(target.id)
                    deleteTarget = null
                }) {
                    Text(stringResource(R.string.wireguard_action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.wireguard_action_cancel))
                }
            },
        )
    }
}
