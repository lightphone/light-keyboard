package com.thelightphone.lp3keyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
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
import com.thelightphone.lp3Keyboard.ui.DarkKeyboardColors
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardTheme
import com.thelightphone.lp3Keyboard.ui.lightFontFamily
import com.thelightphone.lp3Keyboard.ui.layout.LayoutRegistryItem

// Based on https://github.com/THEAccess/compose-keyboard-ime

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .padding(24.dp),
    ) {
        Text(
            text = "KEYBOARD",
            color = Color.White,
            fontFamily = akkurat,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Lp3Button(
            text = "1. Enable Keyboard",
            onClick = {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        Lp3Button(
            text = "2. Select Keyboard",
            onClick = {
                val imm = context.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                imm.showInputMethodPicker()
            }
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "CHOOSE LAYOUT",
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
            text = "TEST INPUT",
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
                    "Type here...",
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
    val akkurat = lightFontFamily(ctx)
    var selected by remember { mutableStateOf(LayoutPreferences.getActiveLayout(ctx)) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        LayoutRegistryItem.entries.forEach { item ->
            val isSelected = item == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .selectable(
                        selected = isSelected,
                        onClick = {
                            selected = item
                            LayoutPreferences.setActiveLayout(ctx, item)
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .border(1.dp, Color.White)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = item.label,
                    color = Color.White,
                    fontFamily = akkurat,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
