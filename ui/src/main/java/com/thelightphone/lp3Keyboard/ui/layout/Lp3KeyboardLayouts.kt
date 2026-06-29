package com.thelightphone.lp3Keyboard.ui.layout

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.thelightphone.lp3Keyboard.ui.DefaultRow
import com.thelightphone.lp3Keyboard.ui.FinalRow
import com.thelightphone.lp3Keyboard.ui.FirstRow
import com.thelightphone.lp3Keyboard.ui.ICON_KEY_WIDTH_DP
import com.thelightphone.lp3Keyboard.ui.IconKey
import com.thelightphone.lp3Keyboard.ui.Key
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardCallback
import com.thelightphone.lp3Keyboard.ui.MEDIUM_KEY_WIDTH_DP
import com.thelightphone.lp3Keyboard.ui.MultiLabelKey
import com.thelightphone.lp3Keyboard.ui.R
import com.thelightphone.lp3Keyboard.ui.SecondRow
import com.thelightphone.lp3Keyboard.ui.SpecialKey
import com.thelightphone.lp3Keyboard.ui.ThirdRow
import kotlinx.coroutines.flow.Flow

interface SwipeConfig {
    fun deriveLayout(): Triple<String, FloatArray, FloatArray>?
    fun report(code: Int, bounds: Rect)
    fun letterBoundsRect(): Rect?
    val boundsFlow: Flow<Int>
}
sealed interface Layout {
    @Composable
    fun ColumnScope.Render(options: KeyboardOptions, callback: Lp3KeyboardCallback)
    val isRootLayout: Boolean
        get() = false

    val swipeConfig: SwipeConfig?
        get() = null
}