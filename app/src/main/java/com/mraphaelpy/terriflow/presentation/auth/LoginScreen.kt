package com.mraphaelpy.terriflow.presentation.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.mraphaelpy.terriflow.R
import com.mraphaelpy.terriflow.presentation.components.FormErrorText
import com.mraphaelpy.terriflow.presentation.components.LoadingButton

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToReset: () -> Unit,
    onNeedsCongregationSetup: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.isLoggedIn) { if (uiState.isLoggedIn) onLoginSuccess() }
    LaunchedEffect(uiState.needsCongregationSetup) { if (uiState.needsCongregationSetup) onNeedsCongregationSetup() }

    val webClientId = stringResource(id = R.string.default_web_client_id)
    val googleSignInClient = remember(context, webClientId) {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(webClientId).requestEmail().build()
        )
    }
    val googleAuthLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            val token = account?.idToken
            if (token != null) viewModel.signInWithGoogle(token) else viewModel.onGoogleSignInFailed(null)
        } catch (e: ApiException) {
            viewModel.onGoogleSignInFailed(e.statusCode)
        }
    }

    AuthLayout(
        icon = Icons.Default.Map,
        title = "TerriFlow",
        subtitle = "Gerenciador de territórios",
        footer = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = onNavigateToReset) { Text("Esqueci minha senha") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Não tem conta?", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onNavigateToRegister) { Text("Cadastre-se", fontWeight = FontWeight.Bold) }
                }
            }
        }
    ) {
        EmailField(email, { email = it })
        PasswordField(password, { password = it }, onDone = { viewModel.login(email, password) })
        uiState.error?.let { FormErrorText(message = it) }
        LoadingButton(text = "Entrar", isLoading = uiState.isLoading, onClick = { viewModel.login(email, password) })
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text("ou", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp))
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
        OutlinedButton(
            onClick = { googleSignInClient.signOut().addOnCompleteListener { googleAuthLauncher.launch(googleSignInClient.signInIntent) } },
            enabled = !uiState.isLoading,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(painterResource(R.drawable.ic_google), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text("Entrar com Google", fontWeight = FontWeight.SemiBold)
        }
    }
}
