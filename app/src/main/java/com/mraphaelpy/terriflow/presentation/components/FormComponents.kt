package com.mraphaelpy.terriflow.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Texto de erro para campos de formulário.
 *
 * Substitui o padrão repetido em LoginScreen, RegisterScreen, ResetPasswordScreen,
 * CreateTerritoryScreen e EditTerritoryScreen.
 *
 * O espaçamento acima fica a cargo do chamador para evitar coupling de layout.
 */
@Composable
fun FormErrorText(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier
    )
}
