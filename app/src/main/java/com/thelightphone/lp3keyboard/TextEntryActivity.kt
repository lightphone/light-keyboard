package com.thelightphone.lp3keyboard

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.LayoutOptions
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardSwipeCallback
import com.thelightphone.lp3Keyboard.ui.keyboardOptions
import com.thelightphone.lp3Keyboard.ui.layout.buildRootViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.TextFieldKeyboardCallback
import com.thelightphone.lp3Keyboard.textentry.TextEntryMode
import com.thelightphone.lp3Keyboard.textentry.TextEntryProtocol
import com.thelightphone.lp3Keyboard.textentry.TextEntryRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Full-screen text entry using the embedded LP3 keyboard.
 *
 * Callers share text through content URIs, see [TextEntryProtocol]:
 * - [TextEntryProtocol.EXTRA_SOURCE_URI]: readable URI of the starting text (optional)
 * - [TextEntryProtocol.EXTRA_RESULT_URI]: writable URI the entered text is written to on submit (optional)
 * - [TextEntryProtocol.EXTRA_REPLY]: mutable PendingIntent sent with [TextEntryProtocol.EXTRA_ACTION_ID]
 *   when the user hits an action, or without it when they cancel
 * - the title, mode and actions to show, see [TextEntryRequest]
 *
 * Grant the URIs with FLAG_GRANT_READ_URI_PERMISSION / FLAG_GRANT_WRITE_URI_PERMISSION (e.g. via
 * ClipData). [Intent.EXTRA_TEXT] still works as starting text for callers without a source URI.
 *
 * Only one caller's session is open at a time: a new launch cancels the previous session, and once
 * a session ends its text is wiped as soon as the screen is hidden.
 *
 * The activity is a single instance that hides itself instead of finishing, so repeat launches
 * resume the existing screen (via [onNewIntent]) rather than building a new one from scratch.
 *
 * NOTE: THIS APPLICATION WILL EVENTUALLY BE INSTALLED ON EVERY LIGHT PHONE
 * AND THIS ACTIVITY WILL BE THE PRIMARY SOURCE OF TEXT ENTRY FOR LIGHTOS AND EXTERNAL TOOLS!
 */
class TextEntryActivity : AppCompatActivity() {

    private val textState: TextEntryViewModel by viewModels()
    private var layoutId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // the system's task snapshot would otherwise show the last caller's text to the next one
        setRecentsScreenshotEnabled(false)
        hideSystemBars()
        if (savedInstanceState == null) textState.startSession(intent)
        onBackPressedDispatcher.addCallback(this) { back() }

        val layout = LayoutPreferences.getActiveLayout(this)
        layoutId = layout.uniqueId
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return layout.buildRootViewModel(
                    textState.editor,
                    object : Lp3KeyboardSwipeCallback<Unit> {},
                    haptic = textState::tick,
                    // only show the close button on sub-layouts (e.g. emoji), where it returns to letters
                    optionsForLayout = { LayoutOptions(displayCloseButton = !it.isRootLayout) },
                    keyboardOptionsFlow = textState.keyboardOptions,
                ) as T
            }
        }
        val keyboardViewModel = ViewModelProvider(this, factory)[layout.uniqueId, ViewModel::class.java]
                as Lp3KeyboardViewModel<*>
        textState.keyboardViewModel = keyboardViewModel
        textState.onFinished = ::hide

        setContent {
            TextEntryScreen(
                state = textState.state,
                onCursorMove = textState.editor::moveCursor,
                request = textState.request,
                keyboardViewModel = keyboardViewModel,
                onAction = { actionId ->
                    textState.tick()
                    textState.act(actionId)
                },
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textState.onFinished = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        textState.startSession(intent)
        // the keyboard view model is built for one layout, so rebuild if the user switched layouts
        if (LayoutPreferences.getActiveLayout(this).uniqueId != layoutId) recreate()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun back() {
        textState.tick()
        textState.cancel()
        hide()
    }

    private fun hide() {
        moveTaskToBack(true)
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onPause() {
        super.onPause()
        textState.keyboardViewModel?.cancelHeldKeys()
    }

    // the user went home or to recents. Not really a thing on standard Light setup
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        textState.cancel()
    }

    override fun onStop() {
        super.onStop()
        textState.clearEndedSession()
    }
}

class TextEntryViewModel(app: Application) : AndroidViewModel(app) {

    private class Session(val source: Uri?, val result: Uri?, val reply: PendingIntent?)

    private companion object {
        const val SOURCE_WAIT_MS = 200L
    }

    val state = TextFieldState()

    val editor = TextFieldKeyboardCallback(
        state,
        // a single line has nowhere for return to go, so it finishes with the caller's action
        onReturn = { request.returnActionId?.let(::act) },
        onCapsModeChange = { keyboardViewModel?.setCapsMode(it) },
    )

    val text: String get() = state.text.toString()

    /** the user's keyboard settings, read again for every session */
    private val _keyboardOptions = MutableStateFlow(readKeyboardOptions())
    val keyboardOptions: StateFlow<KeyboardOptions> = _keyboardOptions

    // no dictation or swipe decoding available yet
    private fun readKeyboardOptions() = getApplication<Application>().contentResolver.keyboardOptions()
        .copy(displayVoice = false, swipeEnabled = false)

    var request by mutableStateOf(TextEntryRequest())
        private set

    /** called when a session ends from inside the view model, e.g. by the return key */
    var onFinished: (() -> Unit)? = null

    // kept here rather than in the activity so it survives a recreate()
    private var session: Session? = null
    private var loadJob: Job? = null

    var keyboardViewModel: Lp3KeyboardViewModel<*>? = null
        set(value) {
            field = value
            editor.updateCapsMode()
        }

    private val vibrator by lazy { app.getSystemService(Vibrator::class.java) }

    fun tick() {
        // 50ms feels good on LP3, other device motors may allow faster buzz
        vibrator.vibrate(50)
    }

    fun reset(initialText: String) = editor.setText(initialText)

    fun startSession(intent: Intent) {
        cancel()
        val newSession = Session(
            intent.getParcelableExtra(TextEntryProtocol.EXTRA_SOURCE_URI, Uri::class.java),
            intent.getParcelableExtra(TextEntryProtocol.EXTRA_RESULT_URI, Uri::class.java),
            intent.getParcelableExtra(TextEntryProtocol.EXTRA_REPLY, PendingIntent::class.java),
        )
        session = newSession
        request = TextEntryRequest.readFrom(intent)
        _keyboardOptions.value = readKeyboardOptions()
        editor.singleLine = request.mode == TextEntryMode.Simple
        reset(intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty())

        val source = newSession.source ?: return
        val load = viewModelScope.async(Dispatchers.IO) {
            runCatching {
                getApplication<Application>().contentResolver.openInputStream(source)
                    ?.bufferedReader()?.use { it.readText() }
            }.getOrNull().orEmpty()
        }
        // runs before the screen draws, so waiting a moment for the text means it's there
        // from the first frame instead of popping in. a slow caller only holds the screen up for
        // SOURCE_WAIT_MS, then its text shows whenever it arrives
        // looks better to hold the screen black rather than draw and have the text pop in
        val loaded = runBlocking { withTimeoutOrNull(SOURCE_WAIT_MS) { load.await() } }
        if (loaded != null) {
            reset(loaded)
            return
        }
        loadJob = viewModelScope.launch {
            val text = load.await()
            if (session === newSession) reset(text)
        }
    }

    fun act(actionId: String) {
        if (actionId == TextEntryProtocol.ACTION_ID_CANCEL) cancel() else save(actionId)
        onFinished?.invoke()
    }

    /** writes the text to the caller's result URI, then tells the caller which action finished it */
    private fun save(actionId: String) {
        val saving = session ?: return
        val savedText = text
        endSession()
        viewModelScope.launch(Dispatchers.IO) {
            val result = saving.result
            val written = result != null && runCatching {
                getApplication<Application>().contentResolver.openOutputStream(result, "wt")!!
                    .bufferedWriter().use { it.write(savedText) }
            }.isSuccess
            reply(saving, actionId.takeIf { written })
        }
    }

    /** drops the open session, if any, and tells its caller nothing was saved */
    fun cancel() {
        val cancelled = session
        endSession()
        cancelled?.let { reply(it, null) }
    }

    private fun endSession() {
        loadJob?.cancel()
        loadJob = null
        session = null
    }

    fun clearEndedSession() {
        if (session == null) reset("")
    }

    // a null actionId tells the caller the user cancelled
    private fun reply(session: Session, actionId: String?) {
        try {
            session.reply?.send(
                getApplication(),
                0,
                Intent().apply { actionId?.let { putExtra(TextEntryProtocol.EXTRA_ACTION_ID, it) } },
            )
        } catch (_: PendingIntent.CanceledException) {
            // caller is gone
        }
    }

    override fun onCleared() {
        cancel()
    }
}
