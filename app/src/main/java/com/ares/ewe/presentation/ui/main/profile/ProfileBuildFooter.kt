package com.ares.ewe.presentation.ui.main.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ares.ewe.BuildConfig

private val DeleteRed = Color(0xFFEF4444)
private val DeleteRedBg = Color(0xFFFFF1F2)
private val DevPurple = Color(0xFF8B5CF6)
private val DevPurpleBg = Color(0xFFF3E8FF)
private val IconGrayBg = Color(0xFFF2F2F7)

@Composable
fun ProfileAccountSection(
    isDeletingAccount: Boolean,
    deleteAccountError: String?,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ProfileSectionHeader(title = "Cuenta")
        Spacer(modifier = Modifier.height(8.dp))
        ProfileMenuCard {
            ProfileMenuRow(
                title = "Cerrar sesión",
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = onLogout,
                enabled = !isDeletingAccount,
                iconTint = Color(0xFF1F2937),
                iconBackground = IconGrayBg,
            )
            ProfileMenuDivider()
            ProfileMenuRow(
                title = "Eliminar cuenta",
                icon = Icons.Default.DeleteOutline,
                onClick = onDeleteAccount,
                enabled = !isDeletingAccount,
                titleColor = DeleteRed,
                iconTint = DeleteRed,
                iconBackground = DeleteRedBg,
                chevronTint = DeleteRed,
                trailingContent = if (isDeletingAccount) {
                    {
                        CircularProgressIndicator(
                            color = DeleteRed,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                } else {
                    null
                },
            )
        }

        Text(
            text = "Versión ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9CA3AF),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp),
        )

        if (BuildConfig.DEBUG) {
            Spacer(modifier = Modifier.height(20.dp))
            ProfileDevSectionHeader(title = "Opciones de desarrollo")
            Spacer(modifier = Modifier.height(8.dp))
            ProfileMenuCard {
                ProfileMenuRow(
                    title = "Forzar crash (debug)",
                    icon = Icons.Default.BugReport,
                    onClick = { throw RuntimeException("Test Crash") },
                    titleColor = DevPurple,
                    iconTint = DevPurple,
                    iconBackground = DevPurpleBg,
                    chevronTint = DevPurple,
                )
            }
        }

        deleteAccountError?.takeIf { it.isNotBlank() }?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
