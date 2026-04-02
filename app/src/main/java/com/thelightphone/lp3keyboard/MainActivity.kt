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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

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
        TextField(
            value = text,
            onValueChange = setValue,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
    }
}
