/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import moe.rukamori.archivetune.constants.ListStyle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.ui.utils.LocalIsTvDevice

val LocalMenuState = compositionLocalOf { MenuState() }

@Stable
class MenuState(
    isVisible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    var isVisible by mutableStateOf(isVisible)
    var content by mutableStateOf(content)
    /** Set by the player menu while it is open so the sheet follows the list style. */
    var listStyled by mutableStateOf(false)

    internal var dialogContent by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    @OptIn(ExperimentalMaterial3Api::class)
    fun show(content: @Composable ColumnScope.() -> Unit) {
        dialogContent = null
        isVisible = true
        this.content = content
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun dismiss() {
        isVisible = false
    }

    fun showDialog(content: @Composable () -> Unit) {
        isVisible = false
        dialogContent = content
    }

    fun dismissDialog() {
        dialogContent = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetMenu(
    modifier: Modifier = Modifier,
    state: MenuState,
    background: Color = MaterialTheme.colorScheme.surface,
) {
    val focusManager = LocalFocusManager.current
    val isTvDevice = LocalIsTvDevice.current

    state.dialogContent?.invoke()

    if (state.isVisible) {
        ListStyleTheme(enabled = state.listStyled) { backdrop ->
            val blurRadius = LocalListBlurRadius.current
            BackAwareModalBottomSheet(
                scrimColor = if (blurRadius > 0) Color.Transparent else BottomSheetDefaults.ScrimColor,
                blurBehindRadius = blurRadius,
                onDismissRequest = {
                    if (!isTvDevice) focusManager.clearFocus()
                    state.isVisible = false
                },
                containerColor =
                    when {
                        backdrop != null -> Color.Transparent
                        LocalListStyled.current -> MaterialTheme.colorScheme.surface
                        else -> background
                    },
                contentColor = MaterialTheme.colorScheme.onSurface,
                dragHandle = {
                    Box(
                        modifier =
                            Modifier
                                .padding(vertical = 12.dp)
                                .size(width = 40.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                    )
                },
                shape = if (LocalListStyle.current == ListStyle.SINGLE_PIECE) RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp) else BottomSheetDefaults.ExpandedShape,
                modifier =
                    modifier.fillMaxHeight().then(if (LocalListStyle.current == ListStyle.SINGLE_PIECE) Modifier.padding(top = 40.dp) else Modifier).then(
                        if (backdrop != null) {
                            Modifier.background(backdrop, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                ) {
                    state.content(this)
                }
            }
        }
    }
}
