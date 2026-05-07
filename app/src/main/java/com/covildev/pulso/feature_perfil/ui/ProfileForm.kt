package com.covildev.pulso.feature_perfil.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ProfileForm(
    name: String,
    age: String,
    additionalInfo: String,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onAdditionalInfoChange: (String) -> Unit,
    submitLabel: String,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nome") },
            singleLine = true,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = age,
            onValueChange = { value ->
                onAgeChange(value.filter { it.isDigit() })
            },
            label = { Text("Idade") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = additionalInfo,
            onValueChange = onAdditionalInfoChange,
            label = { Text("Informacoes adicionais") },
            minLines = 3,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSubmit,
        ) {
            Text(submitLabel)
        }
    }
}
