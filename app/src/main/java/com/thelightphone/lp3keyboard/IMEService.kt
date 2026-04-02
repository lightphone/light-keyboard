package com.thelightphone.lp3keyboard

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
import com.thelightphone.lp3Keyboard.ui.DefaultLp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardCallback
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardView
import com.thelightphone.lp3Keyboard.ui.SpecialKey

class IMEService : LifecycleInputMethodService(),
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    Lp3KeyboardCallback {

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
    }

    override fun onSpecialKeyPressed(key: SpecialKey) {
    }

    override fun onKeyReleased(code: Int) {
        currentInputConnection?.commitText(Char(code).toString(), 1)
        updateCapsMode()
    }

    override fun onSpecialKeyReleased(key: SpecialKey) {
    }

    override fun onKeyLongPressed(code: Int) {
    }

    override fun onSpecialKeyLongPressed(key: SpecialKey) {
    }

}