package com.mraphaelpy.terriflow.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val BarHeight = 64.dp
private val BarMargin = 12.dp

val LocalBottomBarClearance = compositionLocalOf { 0.dp }

@Composable
fun bottomBarClearance(): Dp {
    val systemInset = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    return systemInset + BarHeight + BarMargin * 2
}

val tabScreenInsets: WindowInsets
    @Composable get() = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars)

@Composable
fun floatingControlsBottomPadding(): Dp {
    val clearance = LocalBottomBarClearance.current
    if (clearance > 0.dp) return clearance
    return with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() } + 16.dp
}

enum class BottomTab(val label: String, val selectedIcon: ImageVector, val icon: ImageVector) {
    HOME("Início", Icons.Filled.Home, Icons.Outlined.Home),
    TERRITORIES("Territórios", Icons.Filled.GridView, Icons.Outlined.GridView),
    MAP("Mapa", Icons.Filled.Map, Icons.Outlined.Map)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TerriflowBottomNavBar(
    selected: BottomTab?,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 6.dp,
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = BarMargin)
            .fillMaxWidth()
    ) {
        ShortNavigationBar(
            containerColor = Color.Transparent,
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            BottomTab.entries.forEach { tab ->
                val isSelected = tab == selected
                ShortNavigationBarItem(
                    selected = isSelected,
                    onClick = { if (!isSelected) onSelect(tab) },
                    icon = { Icon(if (isSelected) tab.selectedIcon else tab.icon, contentDescription = null) },
                    label = { Text(tab.label) }
                )
            }
        }
    }
}
