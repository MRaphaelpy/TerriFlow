package com.mraphaelpy.terriflow.presentation.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.luminance
import com.mraphaelpy.terriflow.ui.theme.SeedColorSchemes
import com.mraphaelpy.terriflow.ui.theme.label
import com.mraphaelpy.terriflow.ui.theme.seed
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mraphaelpy.terriflow.core.updater.UpdateDialog
import com.mraphaelpy.terriflow.domain.model.Congregation
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.domain.repository.AppColor
import com.mraphaelpy.terriflow.domain.repository.ThemeMode
import com.mraphaelpy.terriflow.presentation.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showEditNameDialog by remember { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.updateProfilePicture(uri)
    }
    val pickPhoto = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.currentUser?.let { user ->
                ProfileCard(user = user, onChangePhoto = pickPhoto, onEditName = { showEditNameDialog = true })
            }

            SectionTitle("Congregação")
            CongregationCard(uiState.congregation, uiState.congregationLoading)

            SectionTitle("Aparência")
            SettingsCard {
                Text("Tema", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ThemeSelector(selected = uiState.themeMode, onSelect = viewModel::updateThemeMode)
                Spacer(Modifier.size(4.dp))
                Text("Cor de destaque", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ColorSelector(selected = uiState.appColor, onSelect = viewModel::updateAppColor)
            }

            SectionTitle("Aplicativo")
            UpdateCard(
                version = viewModel.updateManager.currentVersion,
                message = uiState.updateMessage,
                isChecking = uiState.isCheckingUpdate,
                onCheck = viewModel::checkForUpdates
            )
            AuthorCard()
        }
    }

    uiState.availableUpdate?.let { updateInfo ->
        UpdateDialog(
            updateInfo = updateInfo,
            onDismiss = viewModel::dismissUpdateDialog,
            updateManager = viewModel.updateManager
        )
    }

    val user = uiState.currentUser
    if (showEditNameDialog && user != null) {
        EditNameDialog(
            currentName = user.name,
            onSave = {
                viewModel.updateMyName(it)
                showEditNameDialog = false
            },
            onDismiss = { showEditNameDialog = false }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileCard(user: User, onChangePhoto: () -> Unit, onEditName: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.clickable(onClick = onChangePhoto)) {
                UserAvatar(user = user, size = 96.dp)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .offset(x = 4.dp, y = 4.dp)
                        .background(MaterialTheme.colorScheme.primary, MaterialShapes.Cookie6Sided.toShape()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Trocar foto", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                user.name.ifBlank { "Sem nome" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            if (user.email.isNotBlank()) {
                Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                Text(
                    user.role.label(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            FilledTonalButton(onClick = onEditName, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Editar nome")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CongregationCard(congregation: Congregation?, isLoading: Boolean) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    SettingsCard {
        when {
            isLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            congregation == null -> Text("Nenhuma congregação vinculada", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(congregation.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Código de acesso", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(congregation.code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        FilledTonalIconButton(onClick = { clipboard.setText(AnnotatedString(congregation.code)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar código")
                        }
                        FilledTonalIconButton(onClick = {
                            val text = "Entre na congregação ${congregation.name} no TerriFlow usando o código: ${congregation.code}"
                            context.startActivity(
                                Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Compartilhar código")
                            )
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartilhar código")
                        }
                    }
                }
                Text(
                    "Compartilhe o código com os irmãos para que eles entrem na congregação pelo app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class ThemeOption(val mode: ThemeMode, val label: String, val icon: ImageVector)

private val ThemeOptions = listOf(
    ThemeOption(ThemeMode.SYSTEM, "Sistema", Icons.Default.BrightnessAuto),
    ThemeOption(ThemeMode.LIGHT, "Claro", Icons.Default.LightMode),
    ThemeOption(ThemeMode.DARK, "Escuro", Icons.Default.DarkMode)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween), modifier = Modifier.fillMaxWidth()) {
        ThemeOptions.forEachIndexed { index, option ->
            ToggleButton(
                checked = selected == option.mode,
                onCheckedChange = { onSelect(option.mode) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ThemeOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton }
            ) {
                Icon(option.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(option.label, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSelector(selected: AppColor, onSelect: (AppColor) -> Unit) {
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val options = AppColor.entries.filter { it != AppColor.DYNAMIC || supportsDynamic }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val context = LocalContext.current
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        maxItemsInEachRow = 4,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEach { option ->
            val scheme = remember(option, isDark) {
                when {
                    option != AppColor.DYNAMIC -> SeedColorSchemes.of(option.seed, isDark)
                    isDark -> dynamicDarkColorScheme(context)
                    else -> dynamicLightColorScheme(context)
                }
            }
            ColorOption(
                scheme = scheme,
                label = option.label,
                selected = selected == option,
                icon = if (option == AppColor.DYNAMIC) Icons.Default.AutoAwesome else null,
                modifier = Modifier.weight(1f)
            ) { onSelect(option) }
        }
    }
}

@Composable
private fun ColorOption(
    scheme: ColorScheme,
    label: String,
    selected: Boolean,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val ringWidth by animateDpAsState(if (selected) 3.dp else 0.dp, label = "ring")
    val cornerPercent by animateIntAsState(if (selected) 30 else 50, label = "corner")
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val shape = RoundedCornerShape(cornerPercent)
        Box(
            modifier = Modifier
                .size(56.dp)
                .border(ringWidth, MaterialTheme.colorScheme.onSurface, shape)
                .padding(if (selected) 5.dp else 0.dp)
                .clip(shape),
            contentAlignment = Alignment.Center
        ) {
            Column(Modifier.matchParentSize()) {
                Box(Modifier.fillMaxWidth().weight(1f).background(scheme.primary))
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    Box(Modifier.fillMaxHeight().weight(1f).background(scheme.secondaryContainer))
                    Box(Modifier.fillMaxHeight().weight(1f).background(scheme.tertiary))
                }
            }
            when {
                selected -> SwatchBadge(Icons.Default.Check, scheme)
                icon != null -> SwatchBadge(icon, scheme)
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun SwatchBadge(icon: ImageVector, scheme: ColorScheme) {
    Box(
        modifier = Modifier.size(24.dp).background(scheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateCard(version: String, message: String?, isChecking: Boolean, onCheck: () -> Unit) {
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text("TerriFlow", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Versão $version", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        message?.let {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
            }
        }
        Button(onClick = onCheck, enabled = !isChecking, modifier = Modifier.fillMaxWidth()) {
            if (isChecking) {
                LoadingIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Verificando…")
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Verificar atualizações")
            }
        }
    }
}

private const val AUTHOR_NAME = "Marcos Rafael"
private const val AUTHOR_GITHUB_USER = "MRaphaelpy"
private const val AUTHOR_GITHUB_URL = "https://github.com/$AUTHOR_GITHUB_USER"

@Composable
private fun AuthorCard() {
    val context = LocalContext.current
    Surface(
        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AUTHOR_GITHUB_URL))) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text("Desenvolvido por $AUTHOR_NAME", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("github.com/$AUTHOR_GITHUB_USER", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Abrir no GitHub", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EditNameDialog(currentName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text("Editar meu nome") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome completo") },
                singleLine = true,
                isError = name.isBlank(),
                supportingText = { if (name.isBlank()) Text("O nome não pode ficar vazio") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank() && name.trim() != currentName) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
