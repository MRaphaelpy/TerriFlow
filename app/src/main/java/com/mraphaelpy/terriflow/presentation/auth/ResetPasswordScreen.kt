package com.mraphaelpy.terriflow.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.presentation.components.FormErrorText
import com.mraphaelpy.terriflow.presentation.components.LoadingButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar senha") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Digite seu e-mail para receber um link de recuperação de senha.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            uiState.error?.let {
                Spacer(Modifier.height(8.dp))
                FormErrorText(message = it)
            }

            if (uiState.resetEmailSent) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "E-mail de recuperação enviado. Verifique sua caixa de entrada.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(24.dp))

            LoadingButton(
                text = "Enviar link de recuperação",
                isLoading = uiState.isLoading,
                enabled = !uiState.resetEmailSent,
                onClick = { viewModel.sendPasswordReset(email) }
            )
        }
    }
}
