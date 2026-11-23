package com.ecoceipt.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.painterResource
import com.ecoceipt.app.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
@Composable fun Panel(modifier: Modifier = Modifier, color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(color, Round).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}
@Composable fun Action(text: String, enabled: Boolean = true, secondary: Boolean = false, framed: Boolean = false, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled, shape = Round,
        border = if (framed) BorderStroke(1.5.dp, if (enabled) DarkTeal else LightGray) else null,
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
@Composable fun BackButton(back: () -> Unit) {
    IconButton(onClick = back, modifier = Modifier.size(48.dp)) {
        Icon(painterResource(R.drawable.ic_back), "Kembali", tint = DarkTeal)
    }
}
@Composable fun BottomNav(screen: Screen, navigate: (Screen) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp, windowInsets = WindowInsets(0, 0, 0, 0)) {
        listOf(
            Triple(Screen.HOME, "Beranda", R.drawable.ic_home),
            Triple(Screen.HISTORY, "Riwayat", R.drawable.ic_history),
            Triple(Screen.STOCK, "Stok", R.drawable.ic_stock),
            Triple(Screen.ANALYSIS, "Analisis", R.drawable.ic_analysis),
            Triple(Screen.RECOMMENDATIONS, "Rekomendasi", R.drawable.ic_recommendations),
            Triple(Screen.ACCOUNT, "Akun", R.drawable.ic_account)
        ).forEach { (destination, label, icon) ->
            NavigationBarItem(selected = screen == destination, onClick = { navigate(destination) },
                icon = { Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(24.dp)) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = DarkTeal,
                    unselectedIconColor = Muted, indicatorColor = Mint))
        }
    }
}
