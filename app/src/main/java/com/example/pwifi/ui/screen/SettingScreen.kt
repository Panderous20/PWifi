package com.example.pwifi.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pwifi.ui.component.PWifiScaffold
import com.example.pwifi.ui.theme.PWifiTheme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.pwifi.R
import com.example.pwifi.utils.changeAppLanguage
import com.example.pwifi.utils.getCurrentLanguageCode
import com.example.pwifi.utils.supportedLanguages

@Composable
fun SettingScreen(
    paddingValues: PaddingValues,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    // State quản lý việc hiện Dialog
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Lấy ngôn ngữ hiện tại (để hiển thị tick xanh trong dialog)
    val currentLanguageCode = getCurrentLanguageCode(context)

    PWifiScaffold(
        title = stringResource(R.string.setting_title)
    ) { innerPadding ->

        // Setup Dialog
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguageCode = currentLanguageCode,
                onDismiss = { showLanguageDialog = false },
                onLanguageSelected = { newCode ->
                    changeAppLanguage(context, newCode) // Gọi hàm đổi ngôn ngữ
                    showLanguageDialog = false // Đóng dialog
                }
            )
        }

        SettingDetail(
            paddingValues = paddingValues,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            onToggleTheme = onToggleTheme,
            onLanguageClick = { showLanguageDialog = true },
            currentLanguageName = supportedLanguages.find { it.code == currentLanguageCode }?.name ?: "English"
        )
    }
}

@Composable
fun SettingDetail(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    onToggleTheme: () -> Unit,
    onLanguageClick: () -> Unit,
    currentLanguageName: String
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.general_setting),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        // Item chọn ngôn ngữ
        SettingItemRow(
            icon = Icons.Default.Language,
            title = stringResource(R.string.language),
            subtitle = currentLanguageName,
            onClick = onLanguageClick
        )
        // Item Dark Mode
        SettingItemRow(
            icon = Icons.Default.DarkMode,
            title = stringResource(R.string.dark_mode),
            subtitle = stringResource(R.string.dark_mode_subtitle),
            onClick = onToggleTheme
        )
        SettingItemRow(
            icon = Icons.Default.Info,
            title = stringResource(R.string.about_app),
            subtitle = stringResource(R.string.version, "1.0.0"),
            onClick = {}
        )
    }
}

@Composable
fun SettingItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguageCode: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.choose_language)) },
        text = {
            Column {
                supportedLanguages.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(language.code) } // Chọn xong thì trigger event
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (language.code == currentLanguageCode),
                            onClick = { onLanguageSelected(language.code) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${language.icon}  ${language.name}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Preview
@Composable
fun SettingScreenPreview() {
    PWifiTheme {
        SettingScreen(
            paddingValues = PaddingValues(0.dp),{}
        )
    }
}