package com.thelightphone.lp3Keyboard.textentry

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileNotFoundException

/**
 * Serves the files behind a caller's text entry sessions to the keyboard: only the open session's,
 * source read-only and result write-only. Subclasses find the session and may restrict callers.
 */
abstract class TextEntryFileProvider<S : TextEntrySessionFiles> : ContentProvider() {

    /** the files of session [sessionId], if it's the one open */
    protected abstract fun findSession(sessionId: String): S?

    /** throws SecurityException if the calling app may not open [session]'s files */
    protected open fun checkCaller(session: S) = Unit

    override fun onCreate() = true

    final override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val (sessionId, file) = TextEntryFile.parse(uri.pathSegments)
            ?: throw FileNotFoundException(uri.toString())
        val session = findSession(sessionId)
            ?: throw FileNotFoundException("no open text entry for $uri")
        checkCaller(session)
        file.checkMode(mode)

        return when (file) {
            TextEntryFile.Source -> session.source
                ?.let { ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY) }
                ?: emptySource()
            TextEntryFile.Result -> ParcelFileDescriptor.open(session.result, ParcelFileDescriptor.parseMode(mode))
        }
    }

    // the read end of a pipe whose write end is already closed reads as empty
    private fun emptySource(): ParcelFileDescriptor {
        val (read, write) = ParcelFileDescriptor.createPipe()
        write.close()
        return read
    }

    override fun getType(uri: Uri) = "text/plain"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException()

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = throw UnsupportedOperationException()
}
