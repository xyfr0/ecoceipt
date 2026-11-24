package com.ecoceipt.app.ui

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable fun RegisterScreen(s: UiState, vm: EcoceiptViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val emailError = when {
        email.isBlank() -> "Email wajib diisi."
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Masukkan email yang valid."
        else -> null
    }
    val passwordError = when {
        password.isBlank() -> "Password wajib diisi."
        password.length < 6 -> "Password minimal 6 karakter."
        else -> null
    }
    val confirmationError = when {
        confirmation.isEmpty() -> "Ulangi password kamu."
        confirmation != password -> "Konfirmasi password tidak cocok."
        else -> null
    }
    AuthLayout("Buat Akun", "Daftar untuk mengelola nota dan stok usahamu.", back = vm::back) {
        OutlinedTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") }, placeholder = { Text("nama@email.com") },
            singleLine = true, enabled = !s.busy, shape = Round,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = submitted && emailError != null,
            supportingText = { if (submitted && emailError != null) Text(emailError) })
        OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") }, singleLine = true, enabled = !s.busy, shape = Round,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = submitted && passwordError != null,
            supportingText = { Text(if (submitted) passwordError ?: "Minimal 6 karakter." else "Minimal 6 karakter.") })
        OutlinedTextField(value = confirmation, onValueChange = { confirmation = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Konfirmasi Password") }, singleLine = true, enabled = !s.busy, shape = Round,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = submitted && confirmationError != null,
            supportingText = { if (submitted && confirmationError != null) Text(confirmationError) })
        Action("Daftar Akun", enabled = !s.busy, framed = true) {
            submitted = true
            if (emailError == null && passwordError == null && confirmationError == null) {
                vm.login(email.trim(), password, register = true)
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Link("Sudah punya akun? Sign In", vm::back)
        }
    }
}

@Composable fun AuthLayout(
    title: String,
    subtitle: String,
    back: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (back != null) BackButton(back) else Spacer(Modifier.height(48.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsyncImage("file:///android_asset/figma/8d105.png", "Ecoceipt", Modifier.size(112.dp))
            Label(title, 24, bold = true)
            Label(subtitle, 13, Muted)
        }
        content()
    }
}
