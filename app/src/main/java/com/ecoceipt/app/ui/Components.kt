package com.ecoceipt.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.Locale

val Mint = Color(0xFFDDF3EF)
val Teal = Color(0xFF117970)
val DarkTeal = Color(0xFF0F6865)
val Ink = Color(0xFF263238)
val Muted = Color(0xFF5A686C)
val Danger = Color(0xFF9C1C14)
val LightGray = Color(0xFFE6ECEC)
val Round = RoundedCornerShape(12.dp)
fun rupiah(amount: Long) = "Rp" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(amount)
fun decimal(number: Double) = String.format(Locale.forLanguageTag("id-ID"), "%.2f", number)

@Composable fun EcoceiptTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = DarkTeal, secondary = Teal, background = Mint,
        surface = Color.White, onSurface = Ink, onBackground = Ink, error = Danger,
        onPrimary = Color.White, onSecondary = Color.White,
        primaryContainer = Mint, onPrimaryContainer = DarkTeal,
        secondaryContainer = Mint, onSecondaryContainer = DarkTeal,
        surfaceVariant = LightGray, onSurfaceVariant = Muted,
        surfaceContainer = Color.White, surfaceContainerHigh = Color.White,
        outline = Muted, outlineVariant = LightGray,
        errorContainer = Color(0xFFFDE8E6), onErrorContainer = Danger), content = content)
}
@Composable fun Label(text: String, size: Int = 14, color: Color = Ink, bold: Boolean = false, modifier: Modifier = Modifier) {
    Text(text, modifier, color = color, fontSize = size.sp, lineHeight = (size + 4).sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
}
@Composable fun Heading(title: String, subtitle: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Label(title, 18, bold = true); if (subtitle.isNotEmpty()) Label(subtitle, 12, Muted) }
}
@Composable fun Panel(modifier: Modifier = Modifier, color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(color, Round).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}
@Composable fun Action(text: String, enabled: Boolean = true, secondary: Boolean = false, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled, shape = Round,
        colors = ButtonDefaults.buttonColors(containerColor = if (secondary) Color.White else DarkTeal,
            contentColor = if (secondary) DarkTeal else Color.White)) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
@Composable fun Link(text: String, onClick: () -> Unit, color: Color = DarkTeal) {
    TextButton(onClick, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Label(text, 12, color, true) }
}
@Composable fun Filters(labels: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        labels.forEach { label ->
            Box(Modifier.heightIn(min = 40.dp).clickable { onSelect(label) }, contentAlignment = Alignment.Center) {
                Box(Modifier.background(if (label == selected) Teal else Color.White, Round).padding(horizontal = 16.dp, vertical = 5.dp)) {
                    Label(label, 12, if (label == selected) Color.White else Ink)
                }
            }
        }
    }
}
@Composable fun KeyValue(key: String, value: String, color: Color = Ink) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Label(key, modifier = Modifier.weight(1f)); Label(value, color = color, bold = true)
    }
}
@Composable fun Badge(text: String, danger: Boolean = false) {
    Box(Modifier.background(if (danger) Color(0xFFFDE8E6) else LightGray, RoundedCornerShape(99.dp))
        .padding(horizontal = 10.dp, vertical = 3.dp)) { Label(text, 11, if (danger) Danger else Muted, true) }
}
@Composable fun Empty(title: String, message: String) {
    Panel { Label(title, 16, bold = true); Label(message, 13, Muted) }
}
@Composable fun Header(showLogo: Boolean = true) {
    Box(Modifier.fillMaxWidth().height(76.dp).background(Color.White), contentAlignment = Alignment.Center) {
        if (showLogo) AsyncImage("file:///android_asset/figma/8d105.png", "Ecoceipt", Modifier.size(57.dp))
    }
}
@Composable fun BackTitle(title: String, back: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(back, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(32.dp)) { Label("‹", 26) }
        Label(title, 18, bold = true)
    }
}
@Composable fun BottomNav(screen: Screen, navigate: (Screen) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White).padding(8.dp, 6.dp)) {
        listOf(Screen.HOME to "Beranda", Screen.HISTORY to "Riwayat", Screen.ANALYSIS to "Analisis", Screen.RECOMMENDATIONS to "Rekomendasi").forEach { (s, text) ->
            Box(Modifier.weight(1f).heightIn(min = 45.dp).background(if (screen == s) Mint else Color.White, Round)
                .clickable { navigate(s) }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                Text(text, fontSize = 12.sp, fontWeight = if (screen == s) FontWeight.SemiBold else FontWeight.Normal, color = if (screen == s) DarkTeal else Muted, textAlign = TextAlign.Center)
            }
        }
    }
}
