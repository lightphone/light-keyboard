package com.thelightphone.lp3Keyboard.textentry

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.DrawableRes
import com.thelightphone.lp3Keyboard.ui.R

/** how the text entry screen is laid out */
enum class TextEntryMode(val wireName: String) {
    /** a single line; the return key fires [TextEntryRequest.returnActionId] */
    Simple("simple"),

    /** multi-line, scrolling text; the return key inserts a newline
     * Think LightOS Notes
     **/
    Multi("multi");

    companion object {
        fun fromWireName(name: String?) = entries.firstOrNull { it.wireName == name }
    }
}

/** where an action sits on the text entry screen */
enum class TextEntryPosition(val wireName: String) {
    TopLeft("top_left"),
    TopRight("top_right"),
    BottomLeft("bottom_left"),
    BottomCenter("bottom_center"),
    BottomRight("bottom_right");

    companion object {
        fun fromWireName(name: String?) = entries.firstOrNull { it.wireName == name }
    }
}

/** icons the keyboard draws itself */
enum class TextEntryIcon(val wireName: String, @param:DrawableRes val drawable: Int) {
    Back("back", R.drawable.text_entry_back_lp3),
    Check("check", R.drawable.check_lp3);

    companion object {
        fun fromWireName(name: String?) = entries.firstOrNull { it.wireName == name }
    }
}

/**
 * Something the user can hit to finish text entry. The keyboard returns its [id], see
 * [TextEntryProtocol.ACTION_ID_CANCEL] for a cancel action.
 */
data class TextEntryAction(
    val id: String,
    val position: TextEntryPosition,
    val content: Content,
    /** accessibility label, mostly for icons */
    val label: String? = null,
) {
    sealed interface Content {
        data class Icon(val icon: TextEntryIcon) : Content

        /**
         * an icon from the caller: an android.resource:// URI (drawable or vector, readable by
         * any app) or a content:// URI of an image, which the caller must grant the keyboard
         */
        data class CustomIcon(val uri: Uri) : Content

        data class Text(val text: String) : Content
    }

    internal fun toBundle() = Bundle().apply {
        putString(TextEntryProtocol.ACTION_KEY_ID, id)
        putString(TextEntryProtocol.ACTION_KEY_POSITION, position.wireName)
        label?.let { putString(TextEntryProtocol.ACTION_KEY_LABEL, it) }
        when (content) {
            is Content.Icon -> putString(TextEntryProtocol.ACTION_KEY_ICON, content.icon.wireName)
            is Content.CustomIcon -> putParcelable(TextEntryProtocol.ACTION_KEY_ICON_URI, content.uri)
            is Content.Text -> putString(TextEntryProtocol.ACTION_KEY_TEXT, content.text)
        }
    }

    internal companion object {
        // null if the bundle is malformed or from a newer protocol (e.g. an unknown position)
        fun fromBundle(bundle: Bundle): TextEntryAction? {
            val id = bundle.getString(TextEntryProtocol.ACTION_KEY_ID) ?: return null
            val position = TextEntryPosition.fromWireName(bundle.getString(TextEntryProtocol.ACTION_KEY_POSITION))
                ?: return null
            val content = bundle.getString(TextEntryProtocol.ACTION_KEY_ICON)
                ?.let { Content.Icon(TextEntryIcon.fromWireName(it) ?: return null) }
                ?: bundle.getParcelable(TextEntryProtocol.ACTION_KEY_ICON_URI, Uri::class.java)
                    ?.let { Content.CustomIcon(it) }
                ?: bundle.getString(TextEntryProtocol.ACTION_KEY_TEXT)?.let { Content.Text(it) }
                ?: return null
            return TextEntryAction(id, position, content, bundle.getString(TextEntryProtocol.ACTION_KEY_LABEL))
        }
    }
}

/** what the text entry screen shows, and how it behaves */
data class TextEntryRequest(
    val title: String? = null,
    val mode: TextEntryMode = TextEntryMode.Simple,
    /** at most one per position, with distinct ids */
    val actions: List<TextEntryAction> = DEFAULT_ACTIONS,
    /**
     * in [TextEntryMode.Simple], the id of the action the return key fires. null to ignore return.
     * defaults to the [ACTION_ID_DONE] action if there is one, e.g. with [DEFAULT_ACTIONS]
     */
    val returnActionId: String? = defaultReturnActionId(actions),
) {
    init {
        require(actions.distinctBy { it.id }.size == actions.size) { "text entry action ids must be distinct" }
        require(actions.distinctBy { it.position }.size == actions.size) { "only one text entry action per position" }
    }

    /** the action at [position], if any */
    fun actionAt(position: TextEntryPosition) = actions.firstOrNull { it.position == position }

    fun writeTo(intent: Intent): Intent = intent
        .putExtra(TextEntryProtocol.EXTRA_TITLE, title)
        .putExtra(TextEntryProtocol.EXTRA_MODE, mode.wireName)
        .putParcelableArrayListExtra(TextEntryProtocol.EXTRA_ACTIONS, ArrayList(actions.map { it.toBundle() }))
        .putExtra(TextEntryProtocol.EXTRA_RETURN_ACTION_ID, returnActionId)

    companion object {
        /** the id [DEFAULT_ACTIONS] returns when the user is done */
        const val ACTION_ID_DONE = "done"

        private fun defaultReturnActionId(actions: List<TextEntryAction>) =
            ACTION_ID_DONE.takeIf { done -> actions.any { it.id == done } }

        /** back (cancel) at the top left, and a check returning [ACTION_ID_DONE] at the top right */
        val DEFAULT_ACTIONS = listOf(
            TextEntryAction(
                TextEntryProtocol.ACTION_ID_CANCEL,
                TextEntryPosition.TopLeft,
                TextEntryAction.Content.Icon(TextEntryIcon.Back),
                label = "Back",
            ),
            TextEntryAction(
                ACTION_ID_DONE,
                TextEntryPosition.TopRight,
                TextEntryAction.Content.Icon(TextEntryIcon.Check),
                label = "Done",
            ),
        )

        /**
         * the request described by [intent]. actions the keyboard doesn't understand, and any that
         * clash with an earlier one, are dropped; an intent without actions gets [DEFAULT_ACTIONS].
         */
        fun readFrom(intent: Intent): TextEntryRequest {
            val actions = intent.getParcelableArrayListExtra(TextEntryProtocol.EXTRA_ACTIONS, Bundle::class.java)
                ?.mapNotNull(TextEntryAction::fromBundle)
                ?.distinctBy { it.id }
                ?.distinctBy { it.position }
                ?: DEFAULT_ACTIONS
            return TextEntryRequest(
                title = intent.getStringExtra(TextEntryProtocol.EXTRA_TITLE),
                mode = TextEntryMode.fromWireName(intent.getStringExtra(TextEntryProtocol.EXTRA_MODE))
                    ?: TextEntryMode.Simple,
                actions = actions,
                // an explicit null (writeTo always sets it) still means return does nothing
                returnActionId = if (intent.hasExtra(TextEntryProtocol.EXTRA_RETURN_ACTION_ID)) {
                    intent.getStringExtra(TextEntryProtocol.EXTRA_RETURN_ACTION_ID)
                } else {
                    defaultReturnActionId(actions)
                },
            )
        }
    }
}
