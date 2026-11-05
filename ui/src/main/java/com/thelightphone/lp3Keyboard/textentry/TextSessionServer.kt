package com.thelightphone.lp3Keyboard.textentry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Something the keyboard can edit through [TextSessionServer], e.g. a new or existing note. The
 * keyboard only ever sees content URIs; targets decide which files sit behind them and what saving
 * means.
 */
interface TextSessionTarget {
    /** the text the keyboard starts with, or null to start empty. opened read-only. */
    fun sourceFile(context: Context, targetId: String?): File?

    /** where the keyboard writes its result. pick a location that makes [commit] cheap (e.g. a rename). */
    fun createResultFile(context: Context, sessionId: String): File

    /**
     * called off the main thread once the keyboard has written its result, with the id of the
     * action the user finished with
     */
    fun commit(context: Context, targetId: String?, actionId: String, result: File)
}

/**
 * Hands text to the keyboard and takes its result back for an app running as the system uid, like
 * LightOS, without the text going through an intent.
 *
 * Regular apps share files with URI permission grants, but Android refuses grants issued by the
 * system uid. So [TextSessionProvider] is exported and checks access itself: only the open session
 * (whose id is random), only the keyboard it was started for, source read-only and result
 * write-only.
 *
 * Nothing is declared for you: the app declares, in its own manifest,
 * ```
 * <provider
 *     android:name="com.thelightphone.lp3Keyboard.textentry.TextSessionProvider"
 *     android:authorities="${applicationId}.textsession"
 *     android:exported="true" />
 * <receiver
 *     android:name="com.thelightphone.lp3Keyboard.textentry.TextSessionReplyReceiver"
 *     android:exported="false" />
 * ```
 *
 * Register targets with [registerTarget] at startup, then launch the keyboard with the intent from
 * [startSession]. One session is open at a time; starting another cancels the previous one.
 */
object TextSessionServer {
    private const val TAG = "TextSessionServer"
    internal const val REPLY_SCHEME = "textsession"

    class Session internal constructor(
        internal val keyboardPackage: String,
        internal val target: TextSessionTarget,
        internal val targetId: String?,
        override val source: File?,
        override val result: File,
    ) : TextEntrySessionFiles

    private val targets = ConcurrentHashMap<String, TextSessionTarget>()
    internal val sessions = TextEntrySessions<Session>()
    private val commitExecutor = Executors.newSingleThreadExecutor()

    fun registerTarget(name: String, target: TextSessionTarget) {
        targets[name] = target
    }

    /**
     * opens a session on target [targetName] and returns the intent that launches [keyboardPackage]
     * on it, showing [request]. any custom icons must be android.resource:// URIs, since the system
     * uid can't grant the keyboard content:// ones.
     */
    fun startSession(
        context: Context,
        keyboardPackage: String,
        targetName: String,
        targetId: String?,
        request: TextEntryRequest = TextEntryRequest(),
    ): Intent {
        val target = requireNotNull(targets[targetName]) { "unknown text session target: $targetName" }
        val source = target.sourceFile(context, targetId)

        val opened = sessions.open { id ->
            Session(keyboardPackage, target, targetId, source, target.createResultFile(context, id))
        }
        opened.replaced?.result?.delete()

        val authority = authority(context)
        return TextEntryIntents.launch(
            keyboardPackage,
            TextEntryFile.Source.uri(authority, opened.id),
            TextEntryFile.Result.uri(authority, opened.id),
            TextEntryIntents.reply(context, TextSessionReplyReceiver::class.java, REPLY_SCHEME, opened.id),
            request,
        )
    }

    internal fun onReply(context: Context, reply: TextEntryIntents.Reply) {
        val session = sessions.close(reply.sessionId) ?: return
        commitExecutor.execute {
            try {
                reply.actionId?.let { session.target.commit(context, session.targetId, it, session.result) }
            } catch (e: Exception) {
                Log.e(TAG, "failed to commit text session", e)
            } finally {
                // a successful commit usually moves the result away, so this only cleans up leftovers
                session.result.delete()
            }
        }
    }

    private fun authority(context: Context) = "${context.packageName}.textsession"
}

/** serves [TextSessionServer]'s files. see there for the manifest declaration. */
class TextSessionProvider : TextEntryFileProvider<TextSessionServer.Session>() {
    override fun findSession(sessionId: String) = TextSessionServer.sessions.find(sessionId)

    override fun checkCaller(session: TextSessionServer.Session) {
        if (callingPackage != session.keyboardPackage) {
            throw SecurityException("text sessions are only available to the keyboard")
        }
    }
}

/** receives the keyboard's reply for [TextSessionServer]. see there for the manifest declaration. */
class TextSessionReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reply = TextEntryIntents.parseReply(intent, TextSessionServer.REPLY_SCHEME) ?: return
        TextSessionServer.onReply(context.applicationContext, reply)
    }
}
