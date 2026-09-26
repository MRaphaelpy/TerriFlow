package com.mraphaelpy.terriflow.presentation.congregation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.presentation.components.FormErrorText
import com.mraphaelpy.terriflow.presentation.components.LoadingButton
import com.mraphaelpy.terriflow.presentation.components.LoadingDialog
import kotlinx.coroutines.delay

@Composable
fun CongregationSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: CongregationSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var congregationName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(4) }
    val clipboardManager = LocalClipboardManager.current

    // Ao entrar com código: navega imediatamente
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete && uiState.congregation == null) {
            onSetupComplete()
        }
    }

    // Ao criar congregação: conta 4 segundos mostrando o código, depois navega
    LaunchedEffect(uiState.congregation) {
        if (uiState.congregation != null) {
            countdown = 4
            repeat(4) {
                delay(1000)
                countdown--
            }
            onSetupComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                text = "Congregação",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Configure sua congregação para continuar",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // Abas só aparecem enquanto a congregação não foi criada
            if (uiState.congregation == null) {
                PrimaryTabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Criar") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Entrar") })
                }

                Spacer(Modifier.height(24.dp))

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (tab == 0) {
                            Text(
                                "Nova Congregação",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = congregationName,
                                onValueChange = { congregationName = it },
                                label = { Text("Nome da congregação") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Um código único será gerado para que outros irmãos possam entrar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            uiState.error?.let {
                                Spacer(Modifier.height(8.dp))
                                FormErrorText(message = it)
                            }
                            Spacer(Modifier.height(24.dp))
                            LoadingButton(
                                text = "Criar Congregação",
                                isLoading = uiState.isLoading,
                                onClick = { viewModel.create(congregationName) }
                            )
                        } else {
                            Text(
                                "Entrar em Congregação",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { joinCode = it.uppercase() },
                                label = { Text("Código da congregação") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Peça o código ao administrador da sua congregação.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            uiState.error?.let {
                                Spacer(Modifier.height(8.dp))
                                FormErrorText(message = it)
                            }
                            Spacer(Modifier.height(24.dp))
                            LoadingButton(
                                text = "Entrar",
                                isLoading = uiState.isLoading,
                                onClick = { viewModel.joinByCode(joinCode) }
                            )
                        }
                    }
                }
            }

            // Card do código — aparece após criar a congregação
            uiState.congregation?.let { congregation ->
                Spacer(Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "✅ Congregação criada!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            congregation.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))

                        // Código com botão de copiar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Código de acesso",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    congregation.code,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            FilledTonalIconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(congregation.code))
                                }
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copiar código"
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Compartilhe este código com os irmãos para que eles possam entrar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Barra de progresso + contador regressivo
                LinearProgressIndicator(
                    progress = { (4 - countdown) / 4f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Entrando no app em $countdown segundo${if (countdown != 1) "s" else ""}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                // Botão para entrar imediatamente sem esperar
                OutlinedButton(
                    onClick = onSetupComplete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Entrar agora")
                }
            }
        }

        if (uiState.isLoading) LoadingDialog("Configurando...")
    }
}
