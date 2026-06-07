package com.ares.ewe.presentation.ui.main.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ares.ewe.core.theme.DobbyColors
import com.ares.ewe.presentation.components.MainTabContentBottomInset
import com.ares.ewe.presentation.ui.components.MarketplaceSkeletonLottieContent

@Composable
fun CouponsLottiePreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = MainTabContentBottomInset),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = DobbyColors.Dark,
            )
        }
        Text(
            text = "Cupones",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = DobbyColors.Dark,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = "Vista previa skeleton loader",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        MarketplaceSkeletonLottieContent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            backgroundColor = Color.White,
        )
    }
}
