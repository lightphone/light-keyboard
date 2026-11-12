package com.thelightphone.lp3keyboard

import android.content.ContentResolver
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.res.ResourcesCompat
import com.thelightphone.lp3Keyboard.textentry.TextEntryAction
import com.thelightphone.lp3Keyboard.textentry.TextEntryIcon
import com.thelightphone.lp3Keyboard.textentry.TextEntryMode
import com.thelightphone.lp3Keyboard.textentry.TextEntryPosition
import com.thelightphone.lp3Keyboard.textentry.TextEntryProtocol
import com.thelightphone.lp3Keyboard.textentry.TextEntryRequest
import com.thelightphone.lp3Keyboard.ui.DarkKeyboardColors
import com.thelightphone.lp3Keyboard.ui.EditAction
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardTheme
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardWrapper
import com.thelightphone.lp3Keyboard.ui.SpecialKey
import com.thelightphone.lp3Keyboard.ui.lightFontFamily
import com.thelightphone.lp3Keyboard.ui.viewmodel.EnQwertyLp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3RepeatableKeyboardCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.core.graphics.createBitmap

// MULTI-LINE LAYOUT IS NOT FINAL!

// sizes from the light SDK's (now-legacy) LightTextInputEditor
// in an ideal world, we would import these in the sdk repo.
// grid units: the screen is 27 units wide
private const val GRID_WIDTH = 27
private const val TOP_BAR_HEIGHT_UNITS = 3f
private const val TOP_BAR_PADDING_UNITS = 1f
private const val TITLE_MAX_WIDTH_UNITS = 18f
private const val BOTTOM_BAR_HEIGHT_UNITS = 4f
private const val BOTTOM_BAR_PADDING_UNITS = 2f
private const val ICON_SIZE_UNITS = 2f
private const val INPUT_PADDING_UNITS = 2f
private const val INPUT_UNDERLINE_GAP_UNITS = 0.5f
private const val KEYBOARD_BOTTOM_HEIGHT_UNITS = 5f

// LightOS designs for a 600px tall screen, see designPx()
private const val DESIGN_HEIGHT_PX = 600f
private const val INPUT_UNDERLINE_THICKNESS_PX = 3f

@Composable
fun TextEntryScreen(
    state: TextFieldState,
    request: TextEntryRequest,
    keyboardViewModel: Lp3KeyboardViewModel<*>,
    onCursorMove: (offset: Int) -> Unit,
    onAction: (actionId: String) -> Unit,
) {
    val colors = DarkKeyboardColors //TODO
    val ctx = LocalContext.current
    val fontFamily = remember { lightFontFamily(ctx) }
    val styles = TextEntryStyles(fontFamily, colors.foreground)

    @Composable
    fun ActionAt(position: TextEntryPosition, heightUnits: Float, textStyle: TextStyle) {
        ActionButton(
            request.actionAt(position),
            heightUnits,
            textStyle,
            colors.foreground,
            onAction
        )
    }

    Lp3KeyboardTheme(colors) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            TopBar(
                left = { ActionAt(TextEntryPosition.TopLeft, TOP_BAR_HEIGHT_UNITS, styles.fine) },
                title = request.title.orEmpty(),
                titleStyle = styles.fine,
                right = {
                    ActionAt(
                        TextEntryPosition.TopRight,
                        TOP_BAR_HEIGHT_UNITS,
                        styles.fine
                    )
                },
                modifier = Modifier.padding(bottom = 1f.gridUnits()),
            )

            InputText(
                state = state,
                singleLine = request.mode == TextEntryMode.Simple,
                style = styles.heading,
                color = colors.foreground,
                onCursorMove = onCursorMove,
                onLongPress = keyboardViewModel::showEditMenu,
                modifier = Modifier.weight(1f),
            )

            val bottom = listOf(
                TextEntryPosition.BottomLeft,
                TextEntryPosition.BottomCenter,
                TextEntryPosition.BottomRight,
            ).map { request.actionAt(it) }
            val bottomBar: (@Composable () -> Unit)? = if (bottom.any { it != null }) {
                {
                    BottomBar(bottom) { action ->
                        ActionButton(
                            action,
                            BOTTOM_BAR_HEIGHT_UNITS,
                            styles.button,
                            colors.foreground,
                            onAction
                        )
                    }
                }
            } else {
                null
            }
            val layout by keyboardViewModel.layoutFlow.collectAsState()
            val keyboardOptions by keyboardViewModel.keyboardOptionsFlow.collectAsState()
            val layoutOptions by keyboardViewModel.layoutOptionsFlow.collectAsState()
            val editMenuOpen by keyboardViewModel.editMenuFlow.collectAsState()
            // back closes whatever the close button would before it leaves the screen
            BackHandler(
                enabled = editMenuOpen || !layout.isRootLayout,
                onBack = keyboardViewModel::onBack
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(top = 10.dp),
            ) {
                Lp3KeyboardWrapper(
                    layout = layout,
                    keyboardOptions = keyboardOptions,
                    layoutOptions = layoutOptions,
                    callback = keyboardViewModel,
                    swipeCallback = null,
                    additionalBottomHeight = KEYBOARD_BOTTOM_HEIGHT_UNITS.gridUnits(),
                    bottomBar = bottomBar,
                    // only while open: without an overlay, the close button closes sub-layouts like emoji
                    onOverlayDismissed = if (editMenuOpen) keyboardViewModel::onBack else null,
                    overlay = if (editMenuOpen) {
                        { EditMenu(keyboardViewModel.locale, styles.button, keyboardViewModel::onEditAction) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/** the LightOS type scale entries this screen uses, scaled to the screen height */
private class TextEntryStyles(val heading: TextStyle, val fine: TextStyle, val button: TextStyle) {
    companion object {
        @Composable
        operator fun invoke(fontFamily: FontFamily, color: Color) = TextEntryStyles(
            heading = TextStyle(
                color = color,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 38f.designPxSp(),
                lineHeight = (38f * 1.35f).designPxSp(),
            ),
            fine = TextStyle(
                color = color,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 25f.designPxSp(),
                letterSpacing = (25f * 0.03f).designPxSp(),
                lineHeight = (25f * 1.15f).designPxSp(),
            ),
            button = TextStyle(
                color = color,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 30f.designPxSp(),
                letterSpacing = (30f * 0.15f).designPxSp(),
                lineHeight = (30f * 1.10f).designPxSp(),
            ),
        )
    }
}

@Composable
private fun TopBar(
    left: @Composable () -> Unit,
    title: String,
    titleStyle: TextStyle,
    right: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barHeight = TOP_BAR_HEIGHT_UNITS.gridUnits()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .padding(horizontal = TOP_BAR_PADDING_UNITS.gridUnits()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(barHeight).zIndex(2f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            left()
            Spacer(Modifier.weight(1f))
            right()
        }
        // centered on the screen rather than between the buttons
        Box(Modifier.fillMaxWidth().height(barHeight), contentAlignment = Alignment.Center) {
            BasicText(
                text = title,
                style = titleStyle.copy(textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = TITLE_MAX_WIDTH_UNITS.gridUnits()),
            )
        }
    }
}

@Composable
private fun InputText(
    state: TextFieldState,
    singleLine: Boolean,
    style: TextStyle,
    color: Color,
    onCursorMove: (offset: Int) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier,
) {
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val currentOnCursorMove by rememberUpdatedState(onCursorMove)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = INPUT_PADDING_UNITS.gridUnits())
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    textLayout?.let { currentOnCursorMove(it.getOffsetForPosition(down.position)) }
                    if (awaitLongPressOrCancellation(down.id) != null) {
                        currentOnLongPress()
                        // the rest of the press is the long press's, not a cursor drag
                        // TODO need to fix dragging
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                        return@awaitEachGesture
                    }
                    // moved past the touch slop while still down: drag the cursor
                    if (currentEvent.changes.none { it.id == down.id && it.pressed }) return@awaitEachGesture
                    drag(down.id) { change ->
                        textLayout?.let { currentOnCursorMove(it.getOffsetForPosition(change.position)) }
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.TopStart,
    ) {
        Column(Modifier.fillMaxWidth()) {
            BasicText(
                text = state.text.toString(),
                style = style,
                onTextLayout = { textLayout = it },
                maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                softWrap = !singleLine,
                overflow = if (singleLine) TextOverflow.StartEllipsis else TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(INPUT_UNDERLINE_GAP_UNITS.gridUnits()))
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(INPUT_UNDERLINE_THICKNESS_PX.designPxDp())
                    .background(color)
            )
        }
        textLayout?.let { layout ->
            val cursor = layout.getCursorRect(
                state.selection.min.coerceIn(
                    0,
                    layout.layoutInput.text.length
                )
            )
            Box(
                Modifier
                    .offset { IntOffset(cursor.left.toInt(), cursor.top.toInt()) }
                    .width(2.dp)
                    .height(with(LocalDensity.current) { cursor.height.toDp() })
                    .background(color)
            )
        }
    }
}

@Composable
private fun EditMenu(
    locale: Locale,
    textStyle: TextStyle,
    onEdit: (EditAction) -> Unit,
) {
    val resources = localizedResources(locale)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EditAction.entries.forEach { action ->
            Box(
                modifier = Modifier
                    .clickable(interactionSource = null, indication = null) { onEdit(action) }
                    .height(BOTTOM_BAR_HEIGHT_UNITS.gridUnits())
                    .padding(horizontal = BOTTOM_BAR_PADDING_UNITS.gridUnits()),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(resources.getString(action.label), style = textStyle, maxLines = 1)
            }
        }
    }
}

private val EditAction.label: Int
    @StringRes get() = when (this) {
        EditAction.Copy -> R.string.edit_copy
        EditAction.Paste -> R.string.edit_paste
        EditAction.Clear -> R.string.edit_clear
    }

/** this app's resources in [locale], whatever the phone's language is */
@Composable
private fun localizedResources(locale: Locale): Resources {
    val context = LocalContext.current
    return remember(context, locale) {
        val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
        context.createConfigurationContext(config).resources
    }
}

@Composable
private fun BottomBar(
    items: List<TextEntryAction?>,
    item: @Composable (TextEntryAction?) -> Unit,
) {
    val (left, center, right) = items
    val alone = left == null && right == null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BOTTOM_BAR_HEIGHT_UNITS.gridUnits())
            .padding(horizontal = if (alone) 0.dp else BOTTOM_BAR_PADDING_UNITS.gridUnits()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            alone -> Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) { item(center) }
            // icons on the sides leave the center text all the room between them
            left?.content !is TextEntryAction.Content.Text &&
                    center?.content is TextEntryAction.Content.Text &&
                    right?.content !is TextEntryAction.Content.Text -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item(it) }
            }

            else -> {
                Slot(Alignment.CenterStart) { item(left) }
                Slot(Alignment.Center) { item(center) }
                Slot(Alignment.CenterEnd) { item(right) }
            }
        }
    }
}

@Composable
private fun RowScope.Slot(alignment: Alignment, content: @Composable () -> Unit) {
    Box(Modifier.weight(1f), contentAlignment = alignment) { content() }
}

@Composable
private fun ActionButton(
    action: TextEntryAction?,
    heightUnits: Float,
    textStyle: TextStyle,
    color: Color,
    onAction: (actionId: String) -> Unit,
) {
    val iconModifier = Modifier.size(ICON_SIZE_UNITS.gridUnits())
    if (action == null) {
        Spacer(iconModifier)
        return
    }
    val clickable =
        Modifier.clickable(interactionSource = null, indication = null) { onAction(action.id) }
    when (val content = action.content) {
        is TextEntryAction.Content.Icon -> Icon(
            painterResource(content.icon.drawable),
            contentDescription = action.label,
            tint = color,
            modifier = clickable.then(iconModifier),
        )

        is TextEntryAction.Content.CustomIcon -> {
            val ctx = LocalContext.current
            val bitmap by produceState<ImageBitmap?>(null, content.uri) {
                value = withContext(Dispatchers.IO) { loadIcon(ctx, content.uri) }
            }
            bitmap?.let {
                Icon(
                    it,
                    contentDescription = action.label,
                    tint = color,
                    modifier = clickable.then(iconModifier)
                )
            } ?: Spacer(iconModifier)
        }

        is TextEntryAction.Content.Text -> Box(
            modifier = clickable.height(heightUnits.gridUnits()),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                content.text,
                style = textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun windowWidthDp(): Float =
    with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp().value }

@Composable
private fun windowHeightDp(): Float =
    with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp().value }

@Composable
private fun Float.gridUnits(): Dp = (windowWidthDp() / GRID_WIDTH * this).dp

@Composable
private fun Float.designPxSp(): TextUnit = (this * windowHeightDp() / DESIGN_HEIGHT_PX).sp

@Composable
private fun Float.designPxDp(): Dp = (this * windowHeightDp() / DESIGN_HEIGHT_PX).dp

private object PreviewKeyboardCallback : Lp3RepeatableKeyboardCallback {
    override fun onKeyPressed(code: Int) = Unit
    override fun onSpecialKeyPressed(key: SpecialKey) = Unit
    override fun onKeyReleased(code: Int) = Unit
    override fun onSpecialKeyReleased(key: SpecialKey) = Unit
    override fun onKeyLongPressed(code: Int) = Unit
    override fun onSpecialKeyLongPressed(key: SpecialKey) = Unit
    override fun onSubmitWord(word: CharSequence) = Unit
    override fun onKeyRepeated(code: Int) = Unit
    override fun onSpecialKeyRepeated(specialKey: SpecialKey) = Unit
}

@Composable
private fun TextEntryScreenPreview(text: String, cursor: Int, request: TextEntryRequest) {
    val keyboardViewModel = remember { EnQwertyLp3KeyboardViewModel<Unit>(PreviewKeyboardCallback) }
    TextEntryScreen(
        state = remember { TextFieldState(text, TextRange(cursor)) },
        request = request,
        keyboardViewModel = keyboardViewModel,
        onCursorMove = {},
        onAction = {},
    )
}

// the default actions: back and a check in the top bar
@Preview(name = "Multi line", widthDp = 1080 / 3, heightDp = 1240 / 3)
@Composable
private fun PreviewTextEntryMulti() {
    val text = "Picking up bread on the way home.\nAnything else?"
    TextEntryScreenPreview(
        text,
        text.length,
        TextEntryRequest(mode = TextEntryMode.Multi, title = "Note")
    )
}

// like the SDK editor: back on top, submit below the keyboard
@Preview(name = "Single line", widthDp = 1080 / 3, heightDp = 1240 / 3)
@Composable
private fun PreviewTextEntrySimple() {
    TextEntryScreenPreview(
        text = "Grocery list",
        cursor = 7,
        request = TextEntryRequest(
            title = "Name",
            mode = TextEntryMode.Simple,
            actions = listOf(
                TextEntryAction(
                    TextEntryProtocol.ACTION_ID_CANCEL,
                    TextEntryPosition.TopLeft,
                    TextEntryAction.Content.Icon(TextEntryIcon.Back),
                    "Back"
                ),
                TextEntryAction(
                    "submit",
                    TextEntryPosition.BottomCenter,
                    TextEntryAction.Content.Text("SUBMIT")
                ),
            ),
            returnActionId = "submit",
        ),
    )
}

/**
 * loads a caller's icon: an android.resource:// drawable (vectors included) from the caller's
 * package, or an image behind a content:// URI it granted us. null if it can't be loaded.
 */
private fun loadIcon(context: Context, uri: Uri): ImageBitmap? = runCatching {
    val drawable: Drawable? = if (uri.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE) {
        val packageName = uri.authority ?: return null
        val resources = context.packageManager.getResourcesForApplication(packageName)
        val segments = uri.pathSegments
        val id = when (segments.size) {
            1 -> segments[0].toInt() // android.resource://package/id
            2 -> resources.getIdentifier(
                segments[1],
                segments[0],
                packageName
            ) // android.resource://package/type/name
            else -> 0
        }
        ResourcesCompat.getDrawable(resources, id, null)
    } else {
        context.contentResolver.openInputStream(uri)?.use { Drawable.createFromStream(it, null) }
    }
    drawable?.toImageBitmap()
}.getOrNull()

private fun Drawable.toImageBitmap(): ImageBitmap {
    val width = intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = intrinsicHeight.takeIf { it > 0 } ?: 96
    val bitmap = createBitmap(width, height)
    setBounds(0, 0, width, height)
    draw(Canvas(bitmap))
    return bitmap.asImageBitmap()
}
