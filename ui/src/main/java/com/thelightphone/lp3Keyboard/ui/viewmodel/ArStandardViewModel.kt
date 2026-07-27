package com.thelightphone.lp3Keyboard.ui.viewmodel

import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.LayoutOptions
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardSwipeCallback
import com.thelightphone.lp3Keyboard.ui.layout.ArShared
import com.thelightphone.lp3Keyboard.ui.layout.ArStandard
import com.thelightphone.lp3Keyboard.ui.layout.EnShared
import com.thelightphone.lp3Keyboard.ui.layout.Layout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Arabic is caseless, so all three caps modes render the same layout — the shift key is never
 * drawn and [Lp3BaseViewModel.capsMode] stays inert. The emoji layer is language-neutral, so it
 * comes from [EnShared].
 */
class ArStandardLp3KeyboardViewModel<SwipeResult>(
    passedCallback: Lp3RepeatableKeyboardCallback,
    swipeCallback: Lp3KeyboardSwipeCallback<SwipeResult>? = null,
    haptic: () -> Unit = {},
    optionsForLayout: (Layout) -> LayoutOptions = {
        LayoutOptions(
            displayCloseButton = true
        )
    },
    keyboardOptionsFlow: StateFlow<KeyboardOptions> = MutableStateFlow(
        KeyboardOptions(
            defaultEmojis,
            displayReturn = true,
            displayVoice = true,
            enableKeyAnimation = true,
            swipeEnabled = false
        )
    )
) : Lp3BaseViewModel<SwipeResult>(
    passedCallback = passedCallback,
    swipeCallback = swipeCallback,
    haptic = haptic,
    optionsForLayout = optionsForLayout,
    keyboardOptionsFlow = keyboardOptionsFlow,
    initialLayout = ArStandard.LettersLayout,
    lowerCaseLayout = ArStandard.LettersLayout,
    upperCaseLayout = ArStandard.LettersLayout,
    capsLockedLayout = ArStandard.LettersLayout,
    numberLayout = ArShared.NumberLayout,
    symbolsLayout = ArShared.SymbolsLayout,
    emojiLayout = EnShared.EmojiLayout,
    extendedCharMapping = ArShared.extendedCharMapping,
)
