package com.thelightphone.lp3keyboard

import android.content.SharedPreferences
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardSwipeCallback
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardView
import com.thelightphone.lp3Keyboard.ui.SpecialKey
import com.thelightphone.lp3Keyboard.ui.layout.LayoutRegistryItem
import com.thelightphone.lp3Keyboard.ui.layout.buildRootViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3RepeatableKeyboardCallback

class IMEService : LifecycleInputMethodService(),
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    Lp3RepeatableKeyboardCallback {

    private var renderedLayout: LayoutRegistryItem? = null
    private var viewModel: Lp3KeyboardViewModel<*>? = null

    private var layoutPrefs: SharedPreferences? = null
    private val layoutChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == LayoutPreferences.KEY_ACTIVE_LAYOUT) {
                refreshLayoutIfNeeded()
            }
        }

    private fun refreshLayoutIfNeeded() {
        if (LayoutPreferences.getActiveLayout(this) != renderedLayout) {
            setInputView(onCreateInputView())
        }
    }

    private fun buildViewModel(layout: LayoutRegistryItem): Lp3KeyboardViewModel<*> {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val dummySwipeCallback = object : Lp3KeyboardSwipeCallback<Unit> {}
                return layout.buildRootViewModel(
                    this@IMEService,
                    dummySwipeCallback,
                    haptic = ::tick
                ) as T
            }
        }
        // Key by the layout's uniqueId so each layout gets its own retained ViewModel instance.
        return ViewModelProvider(store, factory)[layout.uniqueId, ViewModel::class.java]
                as Lp3KeyboardViewModel<*>
    }

    override fun onCreateInputView(): View {
        val layout = LayoutPreferences.getActiveLayout(this)
        val vm = buildViewModel(layout)
        renderedLayout = layout
        viewModel = vm
        val view = Lp3KeyboardView(this, vm).apply {
            // don't need the keyboard view itself ot handle external keys, Android inputs will do it
            handleHardwareKeyboardInput = false
        }
        setCandidatesViewShown(false)
        window?.window?.let {
            it.decorView.apply {
                setViewTreeLifecycleOwner(this@IMEService)
                setViewTreeViewModelStoreOwner(this@IMEService)
                setViewTreeSavedStateRegistryOwner(this@IMEService)
            }
        }
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        refreshLayoutIfNeeded()
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        layoutPrefs = LayoutPreferences.registerOnChange(this, layoutChangeListener)
    }

    override fun onDestroy() {
        layoutPrefs?.unregisterOnSharedPreferenceChangeListener(layoutChangeListener)
        store.clear()
        super.onDestroy()
    }

    override val viewModelStore: ViewModelStore
        get() = store
    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    private val store = ViewModelStore()
    private val vibrator by lazy { getSystemService(Vibrator::class.java) }

    private fun tick() {
        // 50ms feels good on LP3, other device motors may allow faster buzz
        vibrator.vibrate(50)
    }

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onWindowHidden() {
        super.onWindowHidden()
        viewModel?.cancelHeldKeys()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        updateCapsMode()
    }

    private fun updateCapsMode() {
        val ic = currentInputConnection ?: return
        val ei = currentInputEditorInfo ?: return
        // might be set if the TextField is set to capitalize sentence starts, for example
        val caps = ic.getCursorCapsMode(ei.inputType)
        viewModel?.setCapsMode(caps != 0)
    }

    override fun onKeyPressed(code: Int) {
    }

    override fun onSubmitWord(word: CharSequence) {
        currentInputConnection?.commitText("$word ", 1)
    }

    override fun onSpecialKeyPressed(key: SpecialKey) {
        when (key) {
            SpecialKey.Space -> {
                currentInputConnection?.commitText(" ", 1)
                updateCapsMode()
            }

            else -> {}
        }
    }

    override fun onKeyReleased(code: Int) {
        val text = buildString { appendCodePoint(code) }
        currentInputConnection?.commitText(text, 1)
        updateCapsMode()
    }

    override fun onSpecialKeyReleased(key: SpecialKey) {
        when (key) {
            SpecialKey.Backspace -> {
                val ic = currentInputConnection ?: return
                val before = ic.getTextBeforeCursor(1, 0)
                val charsToDelete =
                    if (!before.isNullOrEmpty() && Character.isLowSurrogate(before[0])) 2 else 1
                ic.deleteSurroundingText(charsToDelete, 0)
                updateCapsMode()
            }

            SpecialKey.Return -> {
                currentInputConnection?.commitText("\n", 1)
            }

            SpecialKey.Close -> {
                requestHideSelf(0)
            }

            else -> {}
        }
    }

    override fun onKeyLongPressed(code: Int) {
    }

    private fun deletePrecedingWord() {
        val ic = currentInputConnection ?: return
        // Get text before cursor to find the word boundary (max 100 chars long)
        val before = ic.getTextBeforeCursor(100, 0) ?: return
        val trimmed = before.trimEnd()
        val lastSpace = trimmed.indexOfLast { it.isWhitespace() }
        // Delete from cursor back to start of word (including trailing spaces)
        val charsToDelete = before.length - (if (lastSpace >= 0) lastSpace + 1 else 0)
        ic.deleteSurroundingText(charsToDelete, 0)
        updateCapsMode()
    }

    override fun onSpecialKeyLongPressed(key: SpecialKey) {
        when (key) {
            SpecialKey.Backspace -> {
                deletePrecedingWord()
            }

            else -> {}
        }
    }

    override fun onKeyRepeated(code: Int) {
        onKeyReleased(code)
    }

    override fun onSpecialKeyRepeated(specialKey: SpecialKey) {
        when (specialKey) {
            SpecialKey.Space -> {
                currentInputConnection?.commitText(" ", 1)
                updateCapsMode()
            }

            SpecialKey.Backspace -> {
                deletePrecedingWord()
            }

            else -> {}
        }
    }
}
