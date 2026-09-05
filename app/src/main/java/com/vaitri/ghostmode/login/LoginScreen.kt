package com.vaitri.ghostmode.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "👻",
            fontSize = 70.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "GHOSTMODE",
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your PIN",
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(25.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                    pin = it
                    error = ""
                }
            },
            label = {
                Text("4-digit PIN")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        if (error.isNotEmpty()) {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = error,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(25.dp))

        Button(
            onClick = {

                if (pin.length == 4) {
                    onLoginSuccess()
                } else {
                    error = "Enter a valid 4-digit PIN"
                }

            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "UNLOCK",
                fontSize = 17.sp
            )
        }
    }
}