package com.thelightphone.lp3Keyboard.textentry

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Manages a text entry session, which launches this IME app's TextEntryActivity from any other.
 * Apps running with system uid cannot grant URI permissions,
 * so they should use [TextSessionServer] instead.
 *
 * Text is shared through files behind [TextEntryClientProvider], so large text never goes through
 * an intent. The keyboard is only granted the two files of the current entry, and the grants are
 * revoked as soon as it ends.
 *
 * One entry is open at a time per app: starting another cancels the previous one, which then
 * returns null.
 *
 * [findKeyboard] picks the keyboard app, or returns null if there isn't one. Its answer is cached:
 * call [refreshKeyboardPackage] to update it (e.g. whenever the app comes to the foreground).
 *
 * You must declare this in consuming client tools
 * ```
 * <provider
 *     android:name="com.thelightphone.lp3Keyboard.textentry.TextEntryClientProvider"
 *     android:authorities="${applicationId}.textentry"
 *     android:exported="false"
 *     android:grantUriPermissions="true" />
 * <receiver
 *     android:name="com.thelightphone.lp3Keyboard.textentry.TextEntryClientReplyReceiver"
 *     android:exported="false" />
 * ```
 */
class TextEntryClient(context: Context, private val findKeyboard: suspend () -> String?) {

    private val context = context.applicationContext

    private class KeyboardQuery(val packageName: String?)

    // the last answer from findKeyboard, null until it's been asked
    @Volatile
    private var lastKeyboardQuery: KeyboardQuery? = null

    val isKeyboardAvailable: Boolean?
        get() = lastKeyboardQuery?.let { it.packageName != null }

    suspend fun refreshKeyboardPackage(): String? {
        val packageName = findKeyboard()
        lastKeyboardQuery = KeyboardQuery(packageName)
        return packageName
    }

    // Simple result with short text that can be shared directly in result
    class Result(val actionId: String, val text: String)

    /** the action the user finished with, and the file holding what they entered */
    class FileResult(val actionId: String, val file: File)

    /**
     * the action the user finished with, and a stream of what they entered (UTF-8). The stream's
     * file is already deleted, so nothing is left behind however it ends; its space is freed once
     * the stream is closed (or garbage collected, or the process dies). Read it off the main
     * thread, once, and close it when done, e.g. with use { }.
     */
    class StreamResult(val actionId: String, val text: InputStream) : Closeable by text {
        /** [text] decoded */
        fun reader(): BufferedReader = text.bufferedReader(Charsets.UTF_8)
    }

    /**
     * Opens the keyboard on [initialText], showing [request], and returns what the user entered,
     * or null if they cancelled. Throws [ActivityNotFoundException] if there's no keyboard to
     * enter text with.
     */
    suspend fun enterText(initialText: String = "", request: TextEntryRequest = TextEntryRequest()): Result? {
        val result = editText(initialText, request) ?: return null
        return withContext(Dispatchers.IO) {
            try {
                Result(result.actionId, result.file.readText())
            } finally {
                result.file.delete()
            }
        }
    }

    /**
     * [enterText], but what the user entered comes back as a stream instead of being read into
     * memory first, see [StreamResult].
     */
    suspend fun enterTextStream(
        initialText: String = "",
        request: TextEntryRequest = TextEntryRequest(),
    ): StreamResult? {
        val result = editText(initialText, request) ?: return null
        return withContext(Dispatchers.IO) {
            // deleted while open, so the stream still reads it but nothing can outlive the stream
            try {
                StreamResult(result.actionId, FileInputStream(result.file))
            } finally {
                result.file.delete()
            }
        }
    }

    // editFile() on [initialText], through a source file that's gone by the time this returns
    private suspend fun editText(initialText: String, request: TextEntryRequest): FileResult? {
        val source = withContext(Dispatchers.IO) {
            File.createTempFile("text-entry-source-", ".txt", context.cacheDir).apply { writeText(initialText) }
        }
        try {
            return editFile(source, request)
        } finally {
            source.delete()
        }
    }

    /**
     * Opens the keyboard on the contents of [source] (empty if null), showing [request], and
     * returns a file holding what the user entered, or null if they cancelled. [source] is only
     * read. The caller owns the returned file and should delete it when done. Throws
     * [ActivityNotFoundException] if there's no keyboard to enter text with.
     *
     * content:// icons in [request] are granted to the keyboard for the length of the entry.
     */
    suspend fun editFile(source: File? = null, request: TextEntryRequest = TextEntryRequest()): FileResult? {
        // the cached answer saves asking again. if that keyboard has since gone, startActivity
        // throws ActivityNotFoundException like it would for any missing keyboard
        val keyboardPackage = lastKeyboardQuery?.packageName
            ?: refreshKeyboardPackage()
            ?: throw ActivityNotFoundException("no keyboard for text entry")
        val result = withContext(Dispatchers.IO) {
            File.createTempFile("text-entry-result-", ".txt", context.cacheDir)
        }
        val reply = CompletableDeferred<String?>()
        val opened = sessions.open { Session(source, result, reply) }
        opened.replaced?.reply?.complete(null)

        val iconUris = request.actions.mapNotNull { action ->
            (action.content as? TextEntryAction.Content.CustomIcon)?.uri?.takeIf { it.scheme == "content" }
        }
        var actionId: String? = null
        try {
            try {
                context.startActivity(launchIntent(keyboardPackage, opened.id, request, iconUris))
            } catch (e: ActivityNotFoundException) {
                lastKeyboardQuery = null
                throw e
            }
            actionId = reply.await()
            return actionId?.let { FileResult(it, result) }
        } finally {
            sessions.close(opened.id)
            revokeGrants(opened.id, iconUris)
            if (actionId == null) result.delete()
        }
    }

    private fun launchIntent(
        keyboardPackage: String,
        sessionId: String,
        request: TextEntryRequest,
        iconUris: List<Uri>,
    ): Intent {
        val sourceUri = TextEntryFile.Source.uri(authority(context), sessionId)
        val resultUri = TextEntryFile.Result.uri(authority(context), sessionId)
        val reply = TextEntryIntents.reply(context, TextEntryClientReplyReceiver::class.java, REPLY_SCHEME, sessionId)
        return TextEntryIntents.launch(keyboardPackage, sourceUri, resultUri, reply, request)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or GRANT_FLAGS)
            // grants travel on the clip data; the provider limits source to reads and result to
            // writes, and icons are only ever read
            .apply {
                clipData = ClipData.newRawUri(null, sourceUri).apply {
                    (listOf(resultUri) + iconUris).forEach { addItem(ClipData.Item(it)) }
                }
            }
    }

    private fun revokeGrants(sessionId: String, iconUris: List<Uri>) {
        TextEntryFile.entries.forEach {
            context.revokeUriPermission(it.uri(authority(context), sessionId), GRANT_FLAGS)
        }
        iconUris.forEach { context.revokeUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    class Session internal constructor(
        override val source: File?,
        override val result: File,
        // completes with the action id, or null if the user cancelled
        internal val reply: CompletableDeferred<String?>,
    ) : TextEntrySessionFiles

    internal companion object {
        const val REPLY_SCHEME = "textentry"
        private const val GRANT_FLAGS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        // shared by every client in the process, since the provider and receiver can't reach instances
        val sessions = TextEntrySessions<Session>()

        private fun authority(context: Context) = "${context.packageName}.textentry"

        fun onReply(reply: TextEntryIntents.Reply) {
            sessions.find(reply.sessionId)?.reply?.complete(reply.actionId)
        }
    }
}

/**
 * Serves the files behind a [TextEntryClient]. Not exported: the keyboard can only reach the URIs
 * it was granted for the open entry. See [TextEntryClient] for the manifest declaration.
 */
class TextEntryClientProvider : TextEntryFileProvider<TextEntryClient.Session>() {
    override fun findSession(sessionId: String) = TextEntryClient.sessions.find(sessionId)
}

/** receives the keyboard's reply for a [TextEntryClient]. See there for the manifest declaration. */
class TextEntryClientReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TextEntryIntents.parseReply(intent, TextEntryClient.REPLY_SCHEME)?.let(TextEntryClient::onReply)
    }
}
