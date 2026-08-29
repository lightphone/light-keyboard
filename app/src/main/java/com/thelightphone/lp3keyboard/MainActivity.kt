package com.thelightphone.lp3keyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Button
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.material.TextFieldDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.thelightphone.lp3Keyboard.ui.DarkKeyboardColors
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardTheme
import com.thelightphone.lp3Keyboard.ui.lightFontFamily
import com.thelightphone.lp3Keyboard.ui.layout.LayoutRegistryItem

// Based on https://github.com/THEAccess/compose-keyboard-ime

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            Lp3KeyboardTheme(DarkKeyboardColors) {
                Options()
            }
        }
    }
}

@Composable
fun Lp3Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val akkurat = lightFontFamily(context)
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = androidx.compose.material.ButtonDefaults.buttonColors(
            backgroundColor = Color.White,
            contentColor = Color.Black
        ),
        elevation = null,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontFamily = akkurat,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun Options() {
    val context = LocalContext.current
    val akkurat = lightFontFamily(context)
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            color = Color.White,
            fontFamily = akkurat,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Lp3Button(
            text = stringResource(R.string.btn_enable_keyboard),
            onClick = {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        Lp3Button(
            text = stringResource(R.string.btn_select_keyboard),
            onClick = {
                val imm = context.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                imm.showInputMethodPicker()
            }
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = stringResource(R.string.layout_picker_title),
            color = Color.White,
            fontFamily = akkurat,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        LayoutPicker()
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = stringResource(R.string.test_input_title),
            color = Color.White,
            fontFamily = akkurat,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        var textValue by remember { mutableStateOf(TextFieldValue("")) }
        TextField(
            value = textValue,
            onValueChange = { textValue = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black),
            placeholder = {
                Text(
                    stringResource(R.string.test_input_placeholder),
                    color = Color.Gray,
                    fontFamily = akkurat
                )
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontFamily = akkurat,
                fontSize = 18.sp
            ),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                cursorColor = Color.White,
                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = Color.Gray
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
    }
}

@Composable
fun LayoutPicker() {
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf(LayoutPreferences.getActiveLayout(ctx)) }

    Column(modifier = Modifier.fillMaxWidth()) {
        LayoutRegistryItem.entries.forEach { item ->
            val isSelected = item == selected
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                factory = { context ->
                    LightToggle(context).apply {
                        setText(context.getString(item.labelResId))
                        setOnCheckedChangeListener { checked ->
                            if (checked) {
                                selected = item
                                LayoutPreferences.setActiveLayout(ctx, item)
                            } else if (selected == item) {
                                // Don't allow unchecking the active layout
                                isChecked = true
                            }
                        }
                    }
                },
                update = { toggle ->
                    toggle.isChecked = isSelected
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
