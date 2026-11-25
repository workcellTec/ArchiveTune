/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.component

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.findViewTreeNavigationEventDispatcherOwner
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.ui.back.BackOwnershipPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackAwareModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.modalWindowInsets },
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    blurBehindRadius: Int = 0,
    content: @Composable ColumnScope.() -> Unit,
) {
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets,
        scrimColor = scrimColor,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
    ) {
        val scope = rememberCoroutineScope()

        val inheritedScreenOwner = checkNotNull(LocalNavigationEventDispatcherOwner.current) {
            "No NavigationEventDispatcherOwner in the composition hosting this modal bottom sheet"
        }
        val view = LocalView.current
        SheetWindowBlur(view, blurBehindRadius)
        val sheetWindowOwner =
            remember(view, inheritedScreenOwner) {
                val resolved = view.findViewTreeNavigationEventDispatcherOwner()
                val picked = BackOwnershipPolicy.pickSheetOwner(resolved, inheritedScreenOwner)
                if (picked != null) {
                    picked
                } else {
                    Log.w(
                        SheetBackTag,
                        "No window-scoped NavigationEventDispatcherOwner for this sheet: back " +
                            "will land on the inherited screen dispatcher and never reach the sheet.",
                    )
                    inheritedScreenOwner
                }
            }

        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides sheetWindowOwner) {
            BackHandler(enabled = sheetState.isVisible) {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) currentOnDismissRequest()
                }
            }
            content()
        }
    }
}

private const val SheetBackTag = "BackAwareModalBottomSheet"

/**
 * Blurs the screen behind the sheet (Android 12+). The sheet lives in its own window, so a
 * RenderEffect on the activity's content view blurs only what is behind it, on every device
 * (window blur-behind is switched off by some vendors).
 */
@Composable
private fun SheetWindowBlur(
    view: android.view.View,
    radius: Int,
) {
    androidx.compose.runtime.DisposableEffect(view, radius) {
        val active = radius > 0 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
        var entry: SheetBlurStack.Entry? = null
        if (active) {
            var ctx = view.context
            while (ctx is android.content.ContextWrapper && ctx !is android.app.Activity) ctx = ctx.baseContext
            val screen = (ctx as? android.app.Activity)?.findViewById<android.view.View>(android.R.id.content)
            entry = SheetBlurStack.Entry(radius, view.rootView)
            SheetBlurStack.add(entry, screen)
        }
        onDispose { entry?.let(SheetBlurStack::remove) }
    }
}

/**
 * Sheets can stack (queue, then a song menu). Each open sheet blurs everything beneath it: the
 * screen gets the strongest radius, and each sheet gets the radius of the sheets opened above it,
 * so a menu over the queue blurs the queue with the same level the user chose.
 */
private object SheetBlurStack {
    class Entry(val radius: Int, val root: android.view.View)

    private val entries = mutableListOf<Entry>()
    private var screen: android.view.View? = null

    fun add(entry: Entry, screenView: android.view.View?) {
        entries.add(entry)
        if (screenView != null) screen = screenView
        apply()
    }

    fun remove(entry: Entry) {
        entries.remove(entry)
        val old = screen
        apply()
        if (entries.isEmpty()) {
            if (old != null) setBlur(old, 0)
            screen = null
        }
        setBlur(entry.root, 0)
    }

    private fun apply() {
        screen?.let { setBlur(it, entries.maxOfOrNull { e -> e.radius } ?: 0) }
        entries.forEachIndexed { i, e ->
            setBlur(e.root, entries.drop(i + 1).maxOfOrNull { it.radius } ?: 0)
        }
    }

    @android.annotation.SuppressLint("NewApi")
    private fun setBlur(
        view: android.view.View,
        radius: Int,
    ) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
        view.setRenderEffect(
            if (radius <= 0) {
                null
            } else {
                val r = radius.toFloat()
                android.graphics.RenderEffect.createBlurEffect(r, r, android.graphics.Shader.TileMode.CLAMP)
            },
        )
    }
}
