package com.mraphaelpy.terriflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Botão primário com estado de carregamento.
 *
 * Quando [isLoading] é true, desabilita o botão e exibe um [CircularProgressIndicator].
 * Quando false, exibe o [text] com um [leadingIcon] opcional.
 *
 * Substitui o padrão repetido em LoginScreen, RegisterScreen, ResetPasswordScreen,
 * CreateTerritoryScreen e EditTerritoryScreen.
 *
 * @param text Label exibido quando não está carregando.
 * @param isLoading Controla a exibição do spinner.
 * @param onClick Ação ao clicar — não é chamado enquanto isLoading = true.
 * @param leadingIcon Ícone opcional antes do texto.
 * @param enabled Desabilita o botão independente do estado de loading.
 * @param height Altura total do botão.
 */
@Composable
fun LoadingButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.extraLarge,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    leadingIcon: ImageVector? = null,
    height: Dp = 52.dp
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = shape,
        colors = colors,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
