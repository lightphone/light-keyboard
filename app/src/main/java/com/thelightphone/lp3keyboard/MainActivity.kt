package com.thelightphone.lp3keyboard

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Button
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.thelightphone.lp3Keyboard.textentry.TextEntryAction
import com.thelightphone.lp3Keyboard.textentry.TextEntryClient
import com.thelightphone.lp3Keyboard.textentry.TextEntryIcon
import com.thelightphone.lp3Keyboard.textentry.TextEntryMode
import com.thelightphone.lp3Keyboard.textentry.TextEntryPosition
import com.thelightphone.lp3Keyboard.textentry.TextEntryProtocol
import com.thelightphone.lp3Keyboard.textentry.TextEntryRequest
import com.thelightphone.lp3Keyboard.ui.layout.LayoutRegistryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Based on https://github.com/THEAccess/compose-keyboard-ime

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Options()
        }
    }
}

@Composable
fun Options() {
    Column(
        modifier = Modifier
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .background(Color.White)
            .fillMaxWidth(),
    ) {
        val ctx = LocalContext.current
        Text(text = "LP3 Keyboard")
        val (text, setValue) = remember { mutableStateOf(TextFieldValue("Try here")) }
        Spacer(modifier = Modifier.height(16.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }) {
            Text(text = "1. Enable IME")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            val imm = ctx.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
            imm.showInputMethodPicker()
        }) {
            Text(text = "2. Select IME")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "3. Choose layout")
        LayoutPicker()
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = text,
            onValueChange = setValue,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "4. Test text entry, starting from the text above")
        TextEntryTester(text = text.text, onResult = { setValue(TextFieldValue(it)) })
    }
}

/**
 * Opens [TextEntryActivity] the way another app would, through [TextEntryClient]: the text goes
 * through content URIs, the screen through the request extras, and the outcome comes back on the
 * reply PendingIntent. What the user saves replaces the text field's text.
 */
@Composable
fun TextEntryTester(text: String, onResult: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    // this app is the keyboard
    val client = remember { TextEntryClient(ctx) { ctx.packageName } }
    var status by remember { mutableStateOf("") }

    fun enter(request: TextEntryRequest) {
        scope.launch {
            status = try {
                // the stream version, so this exercises it end to end
                val result = client.enterTextStream(text, request)
                if (result == null) {
                    "cancelled"
                } else {
                    val saved = result.use { withContext(Dispatchers.IO) { it.reader().readText() } }
                    onResult(saved)
                    "saved with \"${result.actionId}\""
                }
            } catch (e: ActivityNotFoundException) {
                "no text entry activity: ${e.message}"
            }
        }
    }

    testRequests(ctx).forEach { (label, request) ->
        Spacer(modifier = Modifier.height(8.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = { enter(request) }) {
            Text(text = label)
        }
    }
    if (status.isNotEmpty()) Text(text = "Last entry: $status")
}

private fun testRequests(ctx: Context): List<Pair<String, TextEntryRequest>> {
    val back = TextEntryAction(
        TextEntryProtocol.ACTION_ID_CANCEL,
        TextEntryPosition.TopLeft,
        TextEntryAction.Content.Icon(TextEntryIcon.Back),
        "Back",
    )
    val submit = TextEntryAction("submit", TextEntryPosition.BottomCenter, TextEntryAction.Content.Text("SUBMIT"))
    // a caller's own icon, loaded from its resources like the SDK sends them
    fun resourceIcon(id: Int) = TextEntryAction.Content.CustomIcon(
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(ctx.packageName)
            .appendPath(id.toString())
            .build()
    )
    return listOf(
        // TextEntryRequest's defaults: a single line, back and a check in the top bar, and return
        // finishing like the check
        "Single line (defaults)" to TextEntryRequest(title = "Name"),
        "Multi line (default actions)" to TextEntryRequest(title = "Note", mode = TextEntryMode.Multi),
        // what LightTextInputEditor sends for a single line editor
        "Single line (SDK style)" to TextEntryRequest(
            title = "Name",
            mode = TextEntryMode.Simple,
            actions = listOf(back, submit),
            returnActionId = submit.id,
        ),
        // the SDK's left and right bottom bar items, as custom icons
        "Multi line, bottom icons" to TextEntryRequest(
            title = "Message",
            mode = TextEntryMode.Multi,
            actions = listOf(
                back,
                submit,
                TextEntryAction(
                    "left",
                    TextEntryPosition.BottomLeft,
                    resourceIcon(com.thelightphone.lp3Keyboard.ui.R.drawable.text_entry_back_lp3),
                    "Left",
                ),
                TextEntryAction(
                    "right",
                    TextEntryPosition.BottomRight,
                    resourceIcon(com.thelightphone.lp3Keyboard.ui.R.drawable.check_lp3),
                    "Right",
                ),
            ),
        ),
    )
}

@Composable
fun LayoutPicker() {
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf(LayoutPreferences.getActiveLayout(ctx)) }
    Column(modifier = Modifier.fillMaxWidth()) {
        LayoutRegistryItem.entries.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = item == selected,
                        onClick = {
                            selected = item
                            LayoutPreferences.setActiveLayout(ctx, item)
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = item == selected,
                    // Click is handled by the row's selectable modifier above.
                    onClick = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = item.label)
            }
        }
    }
}
