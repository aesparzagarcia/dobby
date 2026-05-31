package com.ares.ewe.presentation.ui.auth.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe.core.theme.DobbyColors
import com.ares.ewe.presentation.viewmodel.auth.register.AddUserInfoViewModel
import kotlinx.coroutines.launch

/** Mismos tonos que [com.ares.ewe.presentation.ui.auth.phone.PhoneScreen]. */
private val BrandGreen = Color(0xFF2ECC71)
private val SubtitleBlack = Color(0xFF111111)

private const val PAGE_COUNT = 3

@Composable
private fun RegistrationBasicField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    enabled: Boolean,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        textStyle = TextStyle(
            color = Color.Black,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        decorationBox = { inner ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            color = Color.Black.copy(alpha = 0.35f),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                inner()
            }
        },
    )
}

@Composable
fun AddUserInfoScreen(
    onComplete: () -> Unit,
    viewModel: AddUserInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(PAGE_COUNT) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) DobbyColors.Primary
                            else Color(0xFFE0E0E0)
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            // Solo "Siguiente" avanza paso; evita saltar sin validar nombre/apellidos.
            userScrollEnabled = false,
        ) { page ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (page) {
                    0 -> RegistrationStepTexts(
                        title = "Ingresa tu nombre",
                        subtitle = "LO USAREMOS PARA PERSONALIZAR TU EXPERIENCIA",
                    )
                    1 -> RegistrationStepTexts(
                        title = "Ingresa tus apellidos",
                        subtitle = "COMO APARECEN EN TU IDENTIFICACIÓN",
                    )
                    else -> RegistrationStepTexts(
                        title = "Ingresa tu correo electrónico",
                        subtitle = "TE ENVIAREMOS NOTIFICACIONES Y CONFIRMACIONES",
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                when (page) {
                    0 -> RegistrationBasicField(
                        value = uiState.name,
                        onValueChange = viewModel::onNameChange,
                        placeholder = "Nombre",
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        enabled = !uiState.isLoading,
                    )
                    1 -> RegistrationBasicField(
                        value = uiState.lastName,
                        onValueChange = viewModel::onLastNameChange,
                        placeholder = "Apellidos",
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        enabled = !uiState.isLoading,
                    )
                    else -> RegistrationBasicField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        placeholder = "Correo electrónico",
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                        enabled = !uiState.isLoading,
                    )
                }

                if (uiState.phone.isNotBlank() && page == 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Teléfono verificado: ${uiState.phone}",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall.copy(color = SubtitleBlack),
                    )
                }

                uiState.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.isLoading && page == PAGE_COUNT - 1) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = BrandGreen)
                    }
                } else {
                    Button(
                        onClick = {
                            when (page) {
                                PAGE_COUNT - 1 -> viewModel.submit(onComplete)
                                else -> {
                                    if (viewModel.tryAdvanceFromStep(page)) {
                                        scope.launch {
                                            pagerState.animateScrollToPage(page + 1)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandGreen,
                            contentColor = Color.White,
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        enabled = !uiState.isLoading,
                    ) {
                        Text(
                            text = if (page == PAGE_COUNT - 1) "Crear cuenta" else "Siguiente",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RegistrationStepTexts(title: String, subtitle: String) {
    Text(
        text = title,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontSize = 24.sp,
            lineHeight = 30.sp,
        ),
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = subtitle,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall.copy(
            color = SubtitleBlack,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
            fontSize = 11.sp,
        ),
    )
}
