package dev.techda.sahatisukari.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class SahatiDestination(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

internal val sahatiDestinations = listOf(
    SahatiDestination("الرئيسية", Icons.Outlined.Home),
    SahatiDestination("ملفي الصحي", Icons.Outlined.Person),
    SahatiDestination("قياسات السكر", Icons.Outlined.Bloodtype),
    SahatiDestination("أدويتي", Icons.Outlined.Medication),
    SahatiDestination("يومياتي", Icons.Outlined.CalendarMonth),
    SahatiDestination("التقارير", Icons.Outlined.Assessment),
)

@Composable
internal fun SahatiBottomNavigation(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sahatiDestinations.forEachIndexed { index, destination ->
                val selected = selectedIndex == index
                val foreground = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = SahatiUiTokens.navItemHeight)
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(selected = selected, onClick = { onSelect(index) }, role = Role.Tab)
                        .testTag("sahati-nav-item-$index")
                        .padding(horizontal = 1.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        destination.icon,
                        contentDescription = null,
                        tint = foreground,
                        modifier = Modifier.size(SahatiUiTokens.navIconSize).testTag("sahati-nav-icon-$index"),
                    )
                    Text(
                        destination.title,
                        color = foreground,
                        fontSize = SahatiUiTokens.navLabelSize,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().testTag("sahati-nav-label-$index"),
                    )
                }
            }
        }
    }
}
