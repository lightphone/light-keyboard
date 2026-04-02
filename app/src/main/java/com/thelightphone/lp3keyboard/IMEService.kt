package com.thelightphone.lp3keyboard

import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardView
import com.thelightphone.lp3Keyboard.ui.SpecialKey

class IMEService : LifecycleInputMethodService(),
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    Lp3RepeatableKeyboardCallback {

    private val viewModel: DefaultLp3KeyboardViewModel by lazy {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return DefaultLp3KeyboardViewModel(this@IMEService) as T
            }
        }
        ViewModelProvider(store, factory)[DefaultLp3KeyboardViewModel::class.java]
    }

    override fun onCreateInputView(): View {
        val view = Lp3KeyboardView(this, viewModel)
        setCandidatesViewShown(false);
        window?.window?.apply {
            decorView.let { decorView ->
                decorView.setViewTreeLifecycleOwner(this@IMEService)
                decorView.setViewTreeViewModelStoreOwner(this@IMEService)
                decorView.setViewTreeSavedStateRegistryOwner(this@IMEService)
            }
        }
        return view
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
    }

    override val viewModelStore: ViewModelStore
        get() = store
    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    private val store = ViewModelStore()
    private val vibrator by lazy { getSystemService(Vibrator::class.java) }

    private fun tick() {
        vibrator.vibrate(50)
    }

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        updateCapsMode()
    }

    private fun updateCapsMode() {
        val ic = currentInputConnection ?: return
        val ei = currentInputEditorInfo ?: return
        // might be set if the TextField is set to capitalize sentence starts, for example
        val caps = ic.getCursorCapsMode(ei.inputType)
        viewModel.setCapsMode(caps != 0)
    }

    override fun onKeyPressed(code: Int) {
        tick()
    }

    override fun onSpecialKeyPressed(key: SpecialKey) {
        when(key) {
            SpecialKey.Space -> {
                currentInputConnection?.commitText(" ", 1)
                updateCapsMode()
            }
            else -> {}
        }
        tick()
    }

    override fun onKeyReleased(code: Int) {
        currentInputConnection?.commitText(Char(code).toString(), 1)
        updateCapsMode()
    }

    override fun onSpecialKeyReleased(key: SpecialKey) {
        when (key) {
            SpecialKey.Backspace -> {
                currentInputConnection?.deleteSurroundingText(1, 0)
                updateCapsMode()
            }
            else -> {}
        }
    }

    override fun onKeyLongPressed(code: Int) {
        tick()
    }

    override fun onSpecialKeyLongPressed(key: SpecialKey) {
        when (key) {
            SpecialKey.Backspace -> {
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
            else -> {}
        }
        tick()
    }

    override fun onKeyRepeated(code: Int) {
        println("LP3 REPATED $code")
    }

    override fun onSpecialKeyRepeated(specialKey: SpecialKey) {
        when(specialKey) {
            SpecialKey.Space -> {
                currentInputConnection?.commitText(" ", 1)
                updateCapsMode()
            }
            else -> {}
        }
    }
}