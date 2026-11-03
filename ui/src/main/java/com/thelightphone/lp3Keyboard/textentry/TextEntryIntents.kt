package com.thelightphone.lp3Keyboard.textentry

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri

/** builds the intents a caller exchanges with the keyboard, see [TextEntryProtocol] */
object TextEntryIntents {

    /** launches [keyboardPackage]'s text entry on the given files, showing [request] */
    fun launch(
        keyboardPackage: String,
        sourceUri: Uri,
        resultUri: Uri,
        reply: PendingIntent,
        request: TextEntryRequest,
    ): Intent = request.writeTo(
        Intent(TextEntryProtocol.ACTION_ENTER_TEXT)
            .setPackage(keyboardPackage)
            .putExtra(TextEntryProtocol.EXTRA_PROTOCOL_VERSION, TextEntryProtocol.VERSION)
            .putExtra(TextEntryProtocol.EXTRA_SOURCE_URI, sourceUri)
            .putExtra(TextEntryProtocol.EXTRA_RESULT_URI, resultUri)
            .putExtra(TextEntryProtocol.EXTRA_REPLY, reply)
    )

    /**
     * the reply the keyboard sends to [receiver] when session [sessionId] ends. [scheme] tells the
     * caller's replies apart; read them back with [parseReply].
     */
    fun reply(
        context: Context,
        receiver: Class<out BroadcastReceiver>,
        scheme: String,
        sessionId: String,
    ): PendingIntent {
        // the session id lives in the data, which the keyboard can't overwrite when it fills in the status
        val intent = Intent(context, receiver).setData(Uri.fromParts(scheme, sessionId, null))
        // mutable so the keyboard can add the status extra
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
        )
    }

    /** [actionId] is the action the user hit, or null if they cancelled */
    class Reply(val sessionId: String, val actionId: String?)

    /** the session and outcome of a reply built by [reply], or null if [intent] isn't one */
    fun parseReply(intent: Intent, scheme: String): Reply? {
        val data = intent.data ?: return null
        if (data.scheme != scheme) return null
        return Reply(data.schemeSpecificPart, intent.getStringExtra(TextEntryProtocol.EXTRA_ACTION_ID))
    }
}
