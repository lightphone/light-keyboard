package com.thelightphone.lp3Keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thelightphone.lp3Keyboard.ui.composer.ComposerCandidate
import com.thelightphone.lp3Keyboard.ui.composer.ComposerState

/** Height of the candidate strip that sits directly above the key rows. */
const val CANDIDATE_BAR_HEIGHT_DP = 40

/**
 * The candidate bar: a horizontally scrollable strip showing the raw reading
 * the user is composing on the left, then the ranked candidates for it.
 * Tapping a candidate reports it via [onCandidate]; the host commits it and
 * clears the composition, which empties [state] and hides the bar again.
 *
 * Script-agnostic — it renders whatever [ComposerState] it is given, so any
 * layout with a composing buffer (bopomofo, kana, …) can reuse it as-is.
 *
 * Rendered by [Lp3KeyboardWrapper] only while [ComposerState.isActive], so
 * it takes zero vertical space (and never appears) for the English keyboard.
 */
@Composable
fun CandidateBar(
    state: ComposerState,
    onCandidate: (ComposerCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKeyboardColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CANDIDATE_BAR_HEIGHT_DP.dp)
            .background(colors.background),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Composing reading: the bopomofo typed so far, held apart from the
        // candidates so the eye reads "what I typed → what I can pick".
        Text(
            text = state.composing,
            color = colors.foreground.copy(alpha = 0.55f),
            fontFamily = LocalAkkuratFamily.current,
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 12.dp, end = 8.dp),
        )
        // Thin separator between reading and candidates.
        Box(
            Modifier
                .padding(vertical = 8.dp)
                .width(1.dp)
                .fillMaxHeight()
                .background(colors.foreground.copy(alpha = 0.2f)),
        )
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            for (candidate in state.candidates) {
                CandidateChip(candidate, onCandidate)
            }
        }
    }
}

@Composable
private fun CandidateChip(candidate: ComposerCandidate, onCandidate: (ComposerCandidate) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clickable { onCandidate(candidate) }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = candidate.text,
            color = LocalKeyboardColors.current.foreground,
            // CJK glyphs aren't in Akkurat; let the platform font handle them.
            fontFamily = FontFamily.Default,
            fontSize = 22.sp,
        )
    }
}

@Preview(name = "CandidateBar Dark", widthDp = (1080 / 3), heightDp = CANDIDATE_BAR_HEIGHT_DP)
@Composable
private fun CandidateBarDarkPreview() {
    Lp3KeyboardTheme(DarkKeyboardColors) {
        CandidateBar(
            state = ComposerState.of("ㄕˋ", listOf("是", "事", "世", "市", "示", "式")),
            onCandidate = {},
        )
    }
}

@Preview(name = "CandidateBar Light", widthDp = (1080 / 3), heightDp = CANDIDATE_BAR_HEIGHT_DP)
@Composable
private fun CandidateBarLightPreview() {
    Lp3KeyboardTheme(LightKeyboardColors) {
        CandidateBar(
            state = ComposerState.of("ㄋㄧˇ", listOf("你", "妳")),
            onCandidate = {},
        )
    }
}
