package com.mraphaelpy.terriflow.presentation.auth

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.presentation.components.FormErrorText
import com.mraphaelpy.terriflow.presentation.components.LoadingButton

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    onNeedsCongregationSetup: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.isLoggedIn) { if (uiState.isLoggedIn) onRegisterSuccess() }
    LaunchedEffect(uiState.needsCongregationSetup) { if (uiState.needsCongregationSetup) onNeedsCongregationSetup() }

    AuthLayout(
        icon = Icons.Default.PersonAdd,
        title = "Criar conta",
        subtitle = "Depois você entra na sua congregação com o código de convite",
        onNavigateBack = onNavigateBack,
        footer = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Já tem uma conta?", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onNavigateBack) { Text("Entrar", fontWeight = FontWeight.Bold) }
            }
        }
    ) {
        NameField(name, { name = it })
        EmailField(email, { email = it })
        PasswordField(
            password,
            { password = it },
            onDone = { viewModel.register(name, email, password) },
            supporting = "Mínimo de 6 caracteres"
        )
        uiState.error?.let { FormErrorText(message = it) }
        LoadingButton(text = "Criar conta", isLoading = uiState.isLoading, onClick = { viewModel.register(name, email, password) })
    }
}
