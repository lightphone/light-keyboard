package com.thelightphone.lp3Keyboard.textentry

import android.net.Uri
import java.io.File
import java.util.UUID

/** the two files behind every text entry session, and what the keyboard may do with each */
enum class TextEntryFile(val pathSegment: String) {
    /** the starting text: read-only */
    Source("source"),

    /** where the entered text goes: write-only */
    Result("result");

    /** throws if a content provider shouldn't open this file with the given mode ("r", "w", "wt", ...) */
    fun checkMode(mode: String) {
        when (this) {
            Source -> if (mode != "r") throw SecurityException("text entry source is read-only")
            Result -> if ('r' in mode || 'w' !in mode) throw SecurityException("text entry result is write-only")
        }
    }

    /** the content URI for this file in session [sessionId], served by the provider at [authority] */
    fun uri(authority: String, sessionId: String): Uri =
        Uri.Builder()
            .scheme("content")
            .authority(authority)
            .apply { pathSegments(sessionId, this@TextEntryFile).forEach(::appendPath) }
            .build()

    companion object {
        /** the content URI path segments for [file] in session [sessionId] */
        fun pathSegments(sessionId: String, file: TextEntryFile): List<String> =
            listOf(sessionId, file.pathSegment)

        /** the reverse of [pathSegments], or null if the path isn't a text entry file */
        fun parse(pathSegments: List<String>): Pair<String, TextEntryFile>? {
            if (pathSegments.size != 2) return null
            val file = entries.firstOrNull { it.pathSegment == pathSegments[1] } ?: return null
            return pathSegments[0] to file
        }
    }
}

interface TextEntrySessionFiles {
    val source: File?
    val result: File
}

/**
 * Tracks the one text entry session a caller has open at a time. Each session gets a random id.
 */
class TextEntrySessions<T : Any> {

    class Opened<T : Any>(val id: String, val replaced: T?)

    private var currentId: String? = null
    private var current: T? = null

    @Synchronized
    fun open(create: (id: String) -> T): Opened<T> {
        val id = UUID.randomUUID().toString()
        val payload = create(id)
        val replaced = current
        currentId = id
        current = payload
        return Opened(id, replaced)
    }

    @Synchronized
    fun find(id: String): T? = if (id == currentId) current else null

    @Synchronized
    fun close(id: String): T? {
        if (id != currentId) return null
        return closeCurrent()
    }

    @Synchronized
    fun closeCurrent(): T? {
        val closed = current
        currentId = null
        current = null
        return closed
    }
}
