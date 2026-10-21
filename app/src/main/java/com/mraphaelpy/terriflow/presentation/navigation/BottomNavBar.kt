package com.mraphaelpy.terriflow.presentation.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun TerriflowBottomNavBar(
    selected: BottomTab?,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 6.dp,
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = BarMargin)
            .fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.height(BarHeight).padding(horizontal = 6.dp)) {
            val tabs = BottomTab.entries
            val itemWidth = maxWidth / tabs.size
            val selectedIndex = selected?.let(tabs::indexOf) ?: -1
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex.coerceAtLeast(0) + (itemWidth - IndicatorWidth) / 2,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "indicatorOffset"
            )
            val indicatorAlpha by animateFloatAsState(if (selectedIndex >= 0) 1f else 0f, label = "indicatorAlpha")

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset, y = 8.dp)
                    .size(width = IndicatorWidth, height = 32.dp)
                    .graphicsLayer { alpha = indicatorAlpha }
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            )

            Row(modifier = Modifier.fillMaxSize().selectableGroup()) {
                tabs.forEach { tab ->
                    val isSelected = tab == selected
                    NavTabItem(
                        tab = tab,
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(tab)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTabItem(tab: BottomTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "iconScale"
    )
    val iconColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "iconColor"
    )
    val labelColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "labelColor"
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.padding(top = 8.dp).height(32.dp), contentAlignment = Alignment.Center) {
            Crossfade(targetState = selected, label = "icon") { isSelected ->
                Icon(
                    if (isSelected) tab.selectedIcon else tab.icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                )
            }
        }
        Text(
            tab.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = labelColor,
            maxLines = 1
        )
    }
}

private val IndicatorWidth = 64.dp
