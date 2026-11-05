package com.thelightphone.lp3Keyboard.textentry

/**
 * The intent protocol between apps and the keyboard app's full-screen text entry.
 *
 * Text never travels in the intent itself. The caller hands the keyboard two content URIs: a
 * readable [EXTRA_SOURCE_URI] with the starting text and a writable [EXTRA_RESULT_URI] for the
 * entered text. It also describes the screen: a [EXTRA_TITLE], a [EXTRA_MODE] and the
 * [EXTRA_ACTIONS] to show (see [TextEntryRequest], which reads and writes these).
 *
 * When the user hits an action, the keyboard writes the result and sends the [EXTRA_REPLY]
 * PendingIntent with [EXTRA_ACTION_ID] set to that action's id. When the user leaves without one
 * (system back, the reserved [ACTION_ID_CANCEL], or another session replacing this one), it sends
 * the reply without [EXTRA_ACTION_ID] and writes nothing.
 *
 * The keyboard keeps a single reused instance shared by every caller, so it must treat each launch
 * as a new session: a new launch cancels the previous one, and nothing from one session may be
 * shown to the next.
 */
object TextEntryProtocol {
    /** bumped whenever the protocol changes incompatibly; the keyboard must keep accepting older versions */
    const val VERSION = 1

    /**
     * the stock LP3 keyboard app. callers don't need to target it: any app handling
     * [ACTION_ENTER_TEXT] can do text entry, and the Light SDK asks LightOS which one to use
     */
    const val LP3_KEYBOARD_PACKAGE = "com.thelightphone.lp3keyboard"

    // action and extra names are fixed whichever app handles text entry
    private const val NAMESPACE = "com.thelightphone.lp3keyboard"
    const val ACTION_ENTER_TEXT = "$NAMESPACE.action.ENTER_TEXT"

    const val EXTRA_PROTOCOL_VERSION = "$NAMESPACE.extra.PROTOCOL_VERSION"
    const val EXTRA_SOURCE_URI = "$NAMESPACE.extra.SOURCE_URI"
    const val EXTRA_RESULT_URI = "$NAMESPACE.extra.RESULT_URI"
    const val EXTRA_REPLY = "$NAMESPACE.extra.REPLY"

    /** String: shown at the top of the screen */
    const val EXTRA_TITLE = "$NAMESPACE.extra.TITLE"

    /** String: a [TextEntryMode.wireName] */
    const val EXTRA_MODE = "$NAMESPACE.extra.MODE"

    /** ArrayList<Bundle>: one per action, with the ACTION_KEY_* keys */
    const val EXTRA_ACTIONS = "$NAMESPACE.extra.ACTIONS"

    /** String: in [TextEntryMode.Simple], the id of the action the return key fires */
    const val EXTRA_RETURN_ACTION_ID = "$NAMESPACE.extra.RETURN_ACTION_ID"

    /** String, on the reply: the id of the action the user hit. absent if they cancelled. */
    const val EXTRA_ACTION_ID = "$NAMESPACE.extra.ACTION_ID"

    // keys of each action's Bundle in EXTRA_ACTIONS. exactly one of ICON, ICON_URI and TEXT is set
    const val ACTION_KEY_ID = "id"
    const val ACTION_KEY_POSITION = "position" // a TextEntryPosition.wireName
    const val ACTION_KEY_ICON = "icon" // a TextEntryIcon.wireName
    const val ACTION_KEY_ICON_URI = "icon_uri" // Uri: content:// (granted to the keyboard) or android.resource://
    const val ACTION_KEY_TEXT = "text"
    const val ACTION_KEY_LABEL = "label" // optional accessibility label for icons

    /** an action with this id cancels instead of returning its id, e.g. for a back button */
    const val ACTION_ID_CANCEL = "$NAMESPACE.cancel"
}
