package com.vaitri.ghostmode

import android.R.attr.textStyle
import android.R.color
import android.content.Context
import android.os.Bundle
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vaitri.ghostmode.screens.SplashScreen
import com.vaitri.ghostmode.ui.theme.GhostModeTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import androidx.compose.ui.graphics.Brush


class MainActivity : androidx.fragment.app.FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GhostModeTheme {

                var showSplash by remember {
                    mutableStateOf(true)
                }

                if (showSplash) {

                    SplashScreen(
                        onFinished = {
                            showSplash = false
                        }
                    )

                } else {

                    GhostModeApp(
                        context = this
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()

        getSharedPreferences(
            "ghostmode_session",
            Context.MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                "needs_relogin",
                true
            )
            .apply()
    }
}

/* ---------------- PASSWORD SECURITY ---------------- */

private const val PREFS_NAME = "ghostmode_security"
private const val PIN_HASH = "pin_hash"
private const val PIN_SALT = "pin_salt"
private const val DECOY_PIN = "decoy_pin"

private fun hashPin(pin: String, salt: ByteArray): ByteArray {

    val spec = PBEKeySpec(
        pin.toCharArray(),
        salt,
        120_000,
        256
    )

    val factory =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")

    return factory.generateSecret(spec).encoded
}

private fun savePin(context: Context, pin: String) {

    val salt = ByteArray(16)
    SecureRandom().nextBytes(salt)

    val hash = hashPin(pin, salt)

    val prefs = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    prefs.edit()
        .putString(
            PIN_HASH,
            Base64.encodeToString(hash, Base64.NO_WRAP)
        )
        .putString(
            PIN_SALT,
            Base64.encodeToString(salt, Base64.NO_WRAP)
        )
        .apply()
}
private fun saveDecoyPin(
    context: Context,
    pin: String
) {
    val prefs = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    prefs.edit()
        .putString(DECOY_PIN, pin)
        .apply()
}

private fun verifyDecoyPin(
    context: Context,
    pin: String
): Boolean {
    val prefs = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    val savedPin = prefs.getString(
        DECOY_PIN,
        null
    )

    return savedPin == pin
}

private fun verifyPin(context: Context, pin: String): Boolean {

    val prefs = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    val savedHashString = prefs.getString(PIN_HASH, null)
        ?: return false

    val savedSaltString = prefs.getString(PIN_SALT, null)
        ?: return false

    val salt = Base64.decode(
        savedSaltString,
        Base64.NO_WRAP
    )

    val savedHash = Base64.decode(
        savedHashString,
        Base64.NO_WRAP
    )

    val enteredHash = hashPin(pin, salt)

    return MessageDigest.isEqual(
        savedHash,
        enteredHash
    )
}

private fun pinExists(context: Context): Boolean {

    val prefs = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    return prefs.contains(PIN_HASH)
}

/* ---------------- MAIN APP ---------------- */

@Composable
fun GhostModeApp(context: Context) {

    val sessionPrefs = remember {
        context.getSharedPreferences(
            "ghostmode_session",
            Context.MODE_PRIVATE
        )
    }

    var screen by remember {

        mutableStateOf(
            if (pinExists(context)) {
                "login"
            } else {
                "welcome"
            }
        )
    }

    // Background ke baad app dobara open hone par Login
    LaunchedEffect(Unit) {

        if (
            pinExists(context) &&
            sessionPrefs.getBoolean(
                "needs_relogin",
                false
            )
        ) {

            screen = "login"

            sessionPrefs.edit()
                .putBoolean(
                    "needs_relogin",
                    false
                )
                .apply()
        }
    }

    when (screen) {

        "welcome" -> {

            WelcomeScreen {

                screen = "createPin"
            }
        }

        "createPin" -> {

            CreatePinScreen(
                onPinCreated = { pin ->

                    savePin(
                        context,
                        pin
                    )

                    sessionPrefs.edit()
                        .putBoolean(
                            "needs_relogin",
                            false
                        )
                        .apply()

                    screen = "home"
                }
            )
        }

        "login" -> {

            LoginScreen(
                onLoginSuccess = { isDecoy ->

                    sessionPrefs.edit()
                        .putBoolean(
                            "needs_relogin",
                            false
                        )
                        .apply()

                    if (isDecoy) {
                        screen = "decoy"
                    } else {
                        screen = "home"
                    }
                }
            )
        }

        "decoy" -> {

            DecoyScreen {

                screen = "login"
            }
        }

        "home" -> {

            HomeScreen(
                onNotesClick = {
                    screen = "notes"
                },
                onVaultClick = {
                    screen = "vault"
                },
                onLockClick = {

                    sessionPrefs.edit()
                        .putBoolean(
                            "needs_relogin",
                            false
                        )
                        .apply()

                    screen = "login"
                },
                onChangePinClick = {
                    screen = "changePin"
                }
            )
        }

        "changePin" -> {

            ChangePinScreen(
                onBack = {
                    screen = "home"
                },
                onPinChanged = {
                    screen = "home"
                }
            )
        }

        "notes" -> {

            NotesScreen {

                screen = "home"
            }
        }

        "vault" -> {

            VaultScreen {

                screen = "home"
            }
        }
    }
}

/* ---------------- WELCOME ---------------- */

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            "👻",
            fontSize = 70.sp
        )

        Text(
            "GHOSTMODE",
            fontSize = 34.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            "Your privacy. Your control."
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Button(
            onClick = onGetStarted
        ) {

            Text("GET STARTED")
        }
    }
}

/* ---------------- CREATE PIN ---------------- */

@Composable
fun CreatePinScreen(
    onPinCreated: (String) -> Unit
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    var pin by remember {
        mutableStateOf("")
    }

    var confirmPin by remember {
        mutableStateOf("")
    }

    var decoyPin by remember {
        mutableStateOf("")
    }

    var confirmDecoyPin by remember {
        mutableStateOf("")
    }

    var error by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(20.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        // LOGO
        Card(
            modifier = Modifier.size(90.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            )
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "👻",
                    fontSize = 48.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Text(
            "Secure Your GhostMode",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            "Create your security and decoy PINs",
            fontSize = 14.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        // MAIN PIN CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    "🔐 MAIN SECURITY PIN",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    "This PIN unlocks your private GhostMode space",
                    fontSize = 12.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                OutlinedTextField(
                    value = pin,

                    onValueChange = {

                        if (
                            it.length <= 4 &&
                            it.all { c ->
                                c.isDigit()
                            }
                        ) {
                            pin = it
                            error = ""
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text("Main PIN")
                    },

                    singleLine = true,

                    shape =
                        RoundedCornerShape(15.dp)
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = confirmPin,

                    onValueChange = {

                        if (
                            it.length <= 4 &&
                            it.all { c ->
                                c.isDigit()
                            }
                        ) {
                            confirmPin = it
                            error = ""
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text("Confirm Main PIN")
                    },

                    singleLine = true,

                    shape =
                        RoundedCornerShape(15.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        // DECOY PIN CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    "🎭 DECOY PIN",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    "This PIN opens a harmless decoy screen",
                    fontSize = 12.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                OutlinedTextField(
                    value = decoyPin,

                    onValueChange = {

                        if (
                            it.length <= 4 &&
                            it.all { c ->
                                c.isDigit()
                            }
                        ) {
                            decoyPin = it
                            error = ""
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text("Decoy PIN")
                    },

                    singleLine = true,

                    shape =
                        RoundedCornerShape(15.dp)
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = confirmDecoyPin,

                    onValueChange = {

                        if (
                            it.length <= 4 &&
                            it.all { c ->
                                c.isDigit()
                            }
                        ) {
                            confirmDecoyPin = it
                            error = ""
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text("Confirm Decoy PIN")
                    },

                    singleLine = true,

                    shape =
                        RoundedCornerShape(15.dp)
                )
            }
        }

        if (error.isNotEmpty()) {

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text = error,
                color =
                    MaterialTheme
                        .colorScheme
                        .error,
                fontSize = 13.sp
            )
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Button(
            onClick = {

                when {

                    pin != confirmPin -> {
                        error =
                            "Main PINs do not match."
                    }

                    decoyPin != confirmDecoyPin -> {
                        error =
                            "Decoy PINs do not match."
                    }

                    pin == decoyPin -> {
                        error =
                            "Main and Decoy PIN must be different."
                    }

                    else -> {

                        saveDecoyPin(
                            context,
                            decoyPin
                        )

                        onPinCreated(pin)
                    }
                }
            },

            enabled =
                pin.length == 4 &&
                        confirmPin.length == 4 &&
                        decoyPin.length == 4 &&
                        confirmDecoyPin.length == 4,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(54.dp),

            shape =
                RoundedCornerShape(17.dp)
        ) {

            Text(
                "CREATE SECURE SPACE",
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Text(
            "🔒 Your private space is protected",
            fontSize = 12.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )
    }
}

/* ---------------- LOGIN ---------------- */
@Composable
fun LoginScreen(
    onLoginSuccess: (Boolean) -> Unit
) {

    var pin by remember {
        mutableStateOf("")
    }

    var error by remember {
        mutableStateOf("")
    }

    val context =
        androidx.compose.ui.platform.LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            // GHOST LOGO
            Card(
                shape =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(32.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                    ),
                modifier =
                    Modifier.size(115.dp)
            ) {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "👻",
                        fontSize = 60.sp
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(25.dp)
            )

            Text(
                text = "Welcome Back",
                fontSize = 30.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )

            Text(
                text = "Unlock your private space",
                fontSize = 15.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(35.dp)
            )

            // PIN CARD
            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(24.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .surface
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(22.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "ENTER YOUR PIN",
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )

                    Spacer(
                        modifier =
                            Modifier.height(18.dp)
                    )

                    OutlinedTextField(
                        value = pin,

                        onValueChange = {

                            if (
                                it.length <= 4 &&
                                it.all { character ->
                                    character.isDigit()
                                }
                            ) {

                                pin = it
                                error = ""
                            }
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        label = {
                            Text(
                                "4-digit PIN"
                            )
                        },

                        singleLine = true
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Button(
                        onClick = {

                            if (
                                verifyDecoyPin(
                                    context,
                                    pin
                                )
                            ) {

                                onLoginSuccess(true)

                            } else if (
                                verifyPin(
                                    context,
                                    pin
                                )
                            ) {

                                onLoginSuccess(false)

                            } else {

                                error =
                                    "Incorrect PIN"

                                pin = ""
                            }
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp),

                        shape =
                            androidx.compose.foundation.shape
                                .RoundedCornerShape(
                                    16.dp
                                ),

                        enabled =
                            pin.length == 4
                    ) {

                        Text(
                            "UNLOCK",
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            // FINGERPRINT BUTTON
            OutlinedButton(
                onClick = {

                    val activity =
                        context as
                                androidx.fragment.app
                                .FragmentActivity

                    val biometricManager =
                        androidx.biometric
                            .BiometricManager
                            .from(activity)

                    val result =
                        biometricManager
                            .canAuthenticate(
                                androidx.biometric
                                    .BiometricManager
                                    .Authenticators
                                    .BIOMETRIC_STRONG
                            )

                    if (
                        result ==
                        androidx.biometric
                            .BiometricManager
                            .BIOMETRIC_SUCCESS
                    ) {

                        val executor =
                            androidx.core.content
                                .ContextCompat
                                .getMainExecutor(
                                    activity
                                )

                        val biometricPrompt =
                            androidx.biometric
                                .BiometricPrompt(
                                    activity,
                                    executor,

                                    object :
                                        androidx.biometric
                                        .BiometricPrompt
                                        .AuthenticationCallback() {

                                        override fun
                                                onAuthenticationSucceeded(
                                            result:
                                            androidx.biometric
                                            .BiometricPrompt
                                            .AuthenticationResult
                                        ) {

                                            super
                                                .onAuthenticationSucceeded(
                                                    result
                                                )

                                            onLoginSuccess(
                                                false
                                            )
                                        }

                                        override fun
                                                onAuthenticationFailed() {

                                            super
                                                .onAuthenticationFailed()

                                            error =
                                                "Fingerprint not recognized"
                                        }
                                    }
                                )

                        val promptInfo =
                            androidx.biometric
                                .BiometricPrompt
                                .PromptInfo
                                .Builder()
                                .setTitle(
                                    "Unlock GhostMode"
                                )
                                .setSubtitle(
                                    "Verify your identity"
                                )
                                .setNegativeButtonText(
                                    "CANCEL"
                                )
                                .build()

                        biometricPrompt
                            .authenticate(
                                promptInfo
                            )

                    } else {

                        error =
                            "Fingerprint is not available"
                    }
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),

                shape =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(
                            16.dp
                        )
            ) {

                Text(
                    "👆  UNLOCK WITH FINGERPRINT",
                    fontWeight =
                        FontWeight.Bold
                )
            }

            if (error.isNotEmpty()) {

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

                Text(
                    text = error,
                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )
            }
        }

        // SECURITY TEXT AT BOTTOM
        Text(
            text = "🔒 Your private data stays protected",
            modifier =
                Modifier
                    .align(
                        Alignment.BottomCenter
                    ),
            fontSize = 12.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}


/* ---------------- HOME ---------------- */

@Composable
fun HomeScreen(
    onNotesClick: () -> Unit,
    onVaultClick: () -> Unit,
    onLockClick: () -> Unit,
    onChangePinClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090611),
                        Color(0xFF151027),
                        Color(0xFF090611)
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Card(
                    modifier = Modifier.size(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            "👻",
                            fontSize = 32.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        "GHOSTMODE",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )

                    Text(
                        "Your private space",
                        fontSize = 13.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onLockClick
                ) {
                    Text(
                        "🔒",
                        fontSize = 22.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(32.dp)
            )

            Text(
                "Privacy Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                "Keep your personal content safe",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            // NOTES CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onNotesClick
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF211A32)
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(
                                RoundedCornerShape(18.dp)
                            )
                            .background(
                                Color(0xFF332755)
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            "📝",
                            fontSize = 30.sp
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.width(16.dp)
                    )

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            "Private Notes",
                            fontSize = 20.sp,
                            fontWeight =
                                FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            "Write and manage your private notes",
                            fontSize = 13.sp,
                            color =
                                Color(0xFFBDB6CA)
                        )
                    }

                    Text(
                        "›",
                        fontSize = 30.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(15.dp)
            )

            // VAULT CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onVaultClick
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF211A32)
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(
                                RoundedCornerShape(18.dp)
                            )
                            .background(
                                Color(0xFF332755)
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            "🔐",
                            fontSize = 29.sp
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.width(16.dp)
                    )

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            "Private Vault",
                            fontSize = 20.sp,
                            fontWeight =
                                FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            "Your encrypted private photos",
                            fontSize = 13.sp,
                            color =
                                Color(0xFFBDB6CA)
                        )
                    }

                    Text(
                        "›",
                        fontSize = 30.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            Text(
                "SECURITY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFF181322)
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(8.dp)
                ) {

                    TextButton(
                        onClick =
                            onChangePinClick,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                "🔑",
                                fontSize = 20.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(12.dp)
                            )

                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(
                                    "Change PIN",
                                    color =
                                        Color.White,
                                    fontWeight =
                                        FontWeight.SemiBold
                                )

                                Text(
                                    "Update your security PIN",
                                    fontSize = 12.sp,
                                    color =
                                        Color(0xFFBDB6CA)
                                )
                            }

                            Text(
                                "›",
                                fontSize = 24.sp,
                                color = Color.White
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier =
                            Modifier.padding(
                                horizontal = 12.dp
                            ),
                        color =
                            Color(0xFF332755)
                    )

                    TextButton(
                        onClick =
                            onLockClick,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                "🔒",
                                fontSize = 20.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(12.dp)
                            )

                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(
                                    "Lock GhostMode",
                                    color =
                                        Color(0xFFFF8A80),
                                    fontWeight =
                                        FontWeight.SemiBold
                                )

                                Text(
                                    "Require authentication again",
                                    fontSize = 12.sp,
                                    color =
                                        Color(0xFFBDB6CA)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.weight(1f)
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    "●",
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,
                    fontSize = 12.sp
                )

                Spacer(
                    modifier =
                        Modifier.width(6.dp)
                )

                Text(
                    "Protected & encrypted",
                    fontSize = 12.sp,
                    color =
                        Color(0xFFBDB6CA)
                )
            }
        }
    }
}

/* ---------------- NOTES ---------------- */

@Composable
fun NotesScreen(
    onBack: () -> Unit
) {
    val context =
        androidx.compose.ui.platform.LocalContext.current

    val prefs = remember {
        context.getSharedPreferences(
            "ghostmode_notes",
            Context.MODE_PRIVATE
        )
    }

    val notes = remember {
        mutableStateListOf<String>()
    }

    var noteText by remember {
        mutableStateOf("")
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var editingIndex by remember {
        mutableStateOf(-1)
    }

    var deleteIndex by remember {
        mutableStateOf(-1)
    }

    LaunchedEffect(Unit) {

        val saved = prefs.getStringSet(
            "saved_notes",
            emptySet()
        ) ?: emptySet()

        notes.clear()
        notes.addAll(saved)
    }

    fun saveNotes() {

        prefs.edit()
            .putStringSet(
                "saved_notes",
                notes.toSet()
            )
            .apply()
    }

    val filteredNotes = notes.filter {
        it.contains(
            searchQuery,
            ignoreCase = true
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Card(
                modifier =
                    Modifier.size(48.dp),
                shape =
                    RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                    )
            ) {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clickable(
                                onClick = onBack
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        "←",
                        fontSize = 25.sp
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    "Private Notes",
                    fontSize = 25.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "Your personal thoughts",
                    fontSize = 13.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            Card(
                shape =
                    RoundedCornerShape(14.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                    )
            ) {

                Text(
                    "${notes.size}",
                    modifier =
                        Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 8.dp
                        ),
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(22.dp)
        )

        // SEARCH BAR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
            },
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(18.dp),
            leadingIcon = {
                Text(
                    "🔍",
                    fontSize = 18.sp
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                        }
                    ) {
                        Text("×")
                    }
                }
            },
            placeholder = {
                Text("Search your notes...")
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                color = androidx.compose.ui.graphics.Color.Black
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor =
                    androidx.compose.ui.graphics.Color.Black,
                unfocusedTextColor =
                    androidx.compose.ui.graphics.Color.Black,
                cursorColor = Color.Black
            ),
            singleLine = true
        )


        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        // ADD / EDIT NOTE CARD
        Card(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(22.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp)
            ) {

                Text(
                    if (editingIndex >= 0)
                        "✏️ Editing Note"
                    else
                        "✨ New Private Note",
                    fontWeight =
                        FontWeight.Bold,
                    fontSize = 17.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = {
                        noteText = it
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(115.dp),
                    shape =
                        RoundedCornerShape(16.dp),
                    placeholder = {
                        Text(
                            if (editingIndex >= 0)
                                "Update your note..."
                            else
                                "Write something private..."
                        )
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Button(
                    onClick = {

                        if (
                            noteText.isNotBlank()
                        ) {

                            if (
                                editingIndex >= 0
                            ) {

                                notes[editingIndex] =
                                    noteText.trim()

                                editingIndex = -1

                            } else {

                                notes.add(
                                    noteText.trim()
                                )
                            }

                            saveNotes()

                            noteText = ""
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    shape =
                        RoundedCornerShape(15.dp),
                    enabled =
                        noteText.isNotBlank()
                ) {

                    Text(
                        if (editingIndex >= 0)
                            "✓ SAVE CHANGES"
                        else
                            "＋ ADD PRIVATE NOTE",
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                if (editingIndex >= 0) {

                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )

                    TextButton(
                        onClick = {

                            editingIndex = -1
                            noteText = ""
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            "CANCEL EDIT",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        // NOTES TITLE
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                if (searchQuery.isEmpty())
                    "Your Notes"
                else
                    "Search Results",
                fontSize = 19.sp,
                fontWeight =
                    FontWeight.Bold,
                modifier =
                    Modifier.weight(1f)
            )

            Text(
                "${filteredNotes.size} items",
                fontSize = 12.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        // NOTES LIST
        if (filteredNotes.isEmpty()) {

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment =
                    Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        if (searchQuery.isEmpty())
                            "📝"
                        else
                            "🔍",
                        fontSize = 55.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Text(
                        if (searchQuery.isEmpty())
                            "No notes yet"
                        else
                            "No matching notes",
                        fontSize = 19.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )

                    Text(
                        if (searchQuery.isEmpty())
                            "Your private notes will appear here"
                        else
                            "Try searching something else",
                        fontSize = 13.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }

        } else {

            LazyColumn(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp),
                contentPadding =
                    PaddingValues(
                        bottom = 15.dp
                    )
            ) {

                items(
                    items = filteredNotes,
                    key = {
                        notes.indexOf(it)
                    }
                ) { note ->

                    val originalIndex =
                        notes.indexOf(note)

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(20.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surface
                            ),
                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation =
                                    3.dp
                            )
                    ) {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Box(
                                modifier =
                                    Modifier
                                        .size(42.dp)
                                        .background(
                                            MaterialTheme
                                                .colorScheme
                                                .primaryContainer,
                                            RoundedCornerShape(
                                                14.dp
                                            )
                                        ),
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(
                                    "📝",
                                    fontSize = 20.sp
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.width(12.dp)
                            )

                            Text(
                                note,
                                modifier =
                                    Modifier.weight(1f),
                                fontSize = 16.sp,
                                maxLines = 3,
                                overflow =
                                    androidx.compose.ui
                                        .text.style
                                        .TextOverflow
                                        .Ellipsis
                            )

                            Column {

                                TextButton(
                                    onClick = {

                                        editingIndex =
                                            originalIndex

                                        noteText = note

                                        searchQuery = ""
                                    }
                                ) {

                                    Text(
                                        "EDIT",
                                        fontSize = 11.sp
                                    )
                                }

                                TextButton(
                                    onClick = {

                                        deleteIndex =
                                            originalIndex
                                    }
                                ) {

                                    Text(
                                        "🗑",
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DELETE CONFIRMATION
    if (deleteIndex >= 0) {

        AlertDialog(
            onDismissRequest = {
                deleteIndex = -1
            },

            title = {
                Text("Delete note?")
            },

            text = {
                Text(
                    "This private note will be permanently deleted."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        if (
                            deleteIndex <
                            notes.size
                        ) {

                            notes.removeAt(
                                deleteIndex
                            )

                            saveNotes()
                        }

                        deleteIndex = -1
                    }
                ) {

                    Text(
                        "DELETE",
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        deleteIndex = -1
                    }
                ) {

                    Text("CANCEL")
                }
            }
        )
    }
}

/* ---------------- VAULT ---------------- */
@Composable
fun VaultScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val scope = rememberCoroutineScope()

    var photos by remember {
        mutableStateOf(emptyList<java.io.File>())
    }

    var thumbnails by remember {
        mutableStateOf(
            emptyMap<String, android.graphics.Bitmap>()
        )
    }

    var selectedPhoto by remember {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    var deletePhoto by remember {
        mutableStateOf<java.io.File?>(null)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    fun loadPhotos() {

        val folder = java.io.File(
            context.filesDir,
            "vault"
        )

        photos =
            if (folder.exists()) {

                folder
                    .listFiles()
                    ?.filter {
                        it.isFile &&
                                it.extension == "enc"
                    }
                    ?.sortedByDescending {
                        it.lastModified()
                    }
                    ?: emptyList()

            } else {
                emptyList()
            }

        scope.launch {

            val loaded =
                withContext(
                    kotlinx.coroutines.Dispatchers.IO
                ) {

                    val result =
                        mutableMapOf<
                                String,
                                android.graphics.Bitmap
                                >()

                    photos.forEach { file ->

                        try {

                            val bytes =
                                com.vaitri.ghostmode
                                    .security
                                    .VaultCrypto
                                    .decryptFile(
                                        context,
                                        file
                                    )

                            val bitmap =
                                android.graphics
                                    .BitmapFactory
                                    .decodeByteArray(
                                        bytes,
                                        0,
                                        bytes.size
                                    )

                            if (bitmap != null) {

                                result[
                                    file.absolutePath
                                ] = bitmap
                            }

                        } catch (e: Exception) {

                            e.printStackTrace()
                        }
                    }

                    result
                }

            thumbnails = loaded
        }
    }


    val photoPicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            if (uri != null) {

                scope.launch {

                    isLoading = true

                    try {

                        withContext(
                            kotlinx.coroutines.Dispatchers.IO
                        ) {

                            val folder =
                                java.io.File(
                                    context.filesDir,
                                    "vault"
                                )

                            if (!folder.exists()) {
                                folder.mkdirs()
                            }

                            val time =
                                System.currentTimeMillis()

                            val tempFile =
                                java.io.File(
                                    context.cacheDir,
                                    "temp_$time.jpg"
                                )

                            val encryptedFile =
                                java.io.File(
                                    folder,
                                    "photo_$time.enc"
                                )

                            context.contentResolver
                                .openInputStream(uri)
                                ?.use { input ->

                                    tempFile
                                        .outputStream()
                                        .use { output ->

                                            input.copyTo(output)
                                        }
                                }

                            com.vaitri.ghostmode
                                .security
                                .VaultCrypto
                                .encryptFile(
                                    context,
                                    tempFile,
                                    encryptedFile
                                )

                            tempFile.delete()
                        }

                        loadPhotos()

                    } catch (e: Exception) {

                        e.printStackTrace()

                    } finally {

                        isLoading = false
                    }
                }
            }
        }

    LaunchedEffect(Unit) {
        loadPhotos()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Card(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clickable(
                            onClick = onBack
                        ),
                shape =
                    RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                    )
            ) {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        "←",
                        fontSize = 25.sp
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    "Private Vault",
                    fontSize = 25.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "${photos.size} encrypted photos",
                    fontSize = 13.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            Button(
                onClick = {

                    photoPicker.launch(
                        androidx.activity.result
                            .PickVisualMediaRequest(
                                ActivityResultContracts
                                    .PickVisualMedia
                                    .ImageOnly
                            )
                    )
                },
                enabled = !isLoading,
                shape =
                    RoundedCornerShape(14.dp)
            ) {

                Text(
                    "＋ ADD",
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        // SECURITY STATUS CARD
        Card(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(22.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                )
        ) {

            Row(
                modifier =
                    Modifier.padding(16.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(50.dp)
                            .background(
                                MaterialTheme
                                    .colorScheme
                                    .primary,
                                RoundedCornerShape(
                                    16.dp
                                )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        "🔐",
                        fontSize = 26.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(14.dp)
                )

                Column {

                    Text(
                        "Encrypted Vault",
                        fontSize = 17.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )

                    Text(
                        "Your photos are protected with AES-GCM",
                        fontSize = 12.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onPrimaryContainer
                                .copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        // SECTION TITLE
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                "Your Private Photos",
                modifier =
                    Modifier.weight(1f),
                fontSize = 19.sp,
                fontWeight =
                    FontWeight.Bold
            )

            if (photos.isNotEmpty()) {

                Text(
                    "${photos.size} items",
                    fontSize = 12.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        // LOADING
        if (isLoading) {

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment =
                    Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    CircularProgressIndicator()

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Text(
                        "Securing your photo...",
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )

                    Text(
                        "Encrypting and moving it to your vault",
                        fontSize = 12.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }

        } else if (photos.isEmpty()) {

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment =
                    Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(105.dp)
                                .background(
                                    MaterialTheme
                                        .colorScheme
                                        .primaryContainer,
                                    RoundedCornerShape(
                                        32.dp
                                    )
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            "🔐",
                            fontSize = 52.sp
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    Text(
                        "Your vault is empty",
                        fontSize = 21.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(7.dp)
                    )

                    Text(
                        "Add photos and keep them safely encrypted",
                        fontSize = 13.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.height(22.dp)
                    )

                    Button(
                        onClick = {

                            photoPicker.launch(
                                androidx.activity.result
                                    .PickVisualMediaRequest(
                                        ActivityResultContracts
                                            .PickVisualMedia
                                            .ImageOnly
                                    )
                            )
                        },
                        shape =
                            RoundedCornerShape(16.dp)
                    ) {

                        Text(
                            "＋ ADD FIRST PHOTO",
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }

        } else {

            LazyVerticalGrid(
                columns =
                    GridCells.Fixed(2),

                modifier =
                    Modifier.weight(1f),

                contentPadding =
                    PaddingValues(
                        bottom = 16.dp
                    ),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = photos,
                    key = {
                        it.absolutePath
                    }
                ) { encryptedPhoto ->

                    val bitmap =
                        thumbnails[
                            encryptedPhoto.absolutePath
                        ]

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {

                                    if (
                                        bitmap != null
                                    ) {

                                        selectedPhoto =
                                            bitmap
                                    }
                                },

                        shape =
                            RoundedCornerShape(20.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surface
                            ),

                        elevation =
                            CardDefaults
                                .cardElevation(
                                    defaultElevation =
                                        4.dp
                                )
                    ) {

                        Column {

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(155.dp)
                                        .background(
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant
                                        ),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                if (
                                    bitmap != null
                                ) {

                                    AsyncImage(
                                        model = bitmap,

                                        contentDescription =
                                            "Private photo",

                                        modifier =
                                            Modifier
                                                .fillMaxSize(),

                                        contentScale =
                                            ContentScale.Crop
                                    )

                                } else {

                                    Column(
                                        horizontalAlignment =
                                            Alignment.CenterHorizontally
                                    ) {

                                        Text(
                                            "🔐",
                                            fontSize = 42.sp
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.height(5.dp)
                                        )

                                        Text(
                                            "Protected",
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 5.dp,
                                            vertical = 2.dp
                                        ),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                TextButton(
                                    onClick = {

                                        if (
                                            bitmap != null
                                        ) {

                                            selectedPhoto =
                                                bitmap
                                        }
                                    },

                                    modifier =
                                        Modifier.weight(1f)
                                ) {

                                    Text(
                                        "VIEW",
                                        fontSize = 11.sp,
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }

                                TextButton(
                                    onClick = {

                                        deletePhoto =
                                            encryptedPhoto
                                    }
                                ) {

                                    Text(
                                        "🗑",
                                        fontSize = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // PHOTO VIEWER
    selectedPhoto?.let { bitmap ->

        AlertDialog(
            onDismissRequest = {
                selectedPhoto = null
            },

            title = {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        "🔐",
                        fontSize = 22.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Text(
                        "Private Photo"
                    )
                }
            },

            text = {

                AsyncImage(
                    model = bitmap,

                    contentDescription =
                        "Private photo",

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .clip(
                                RoundedCornerShape(
                                    18.dp
                                )
                            ),

                    contentScale =
                        ContentScale.Fit
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        selectedPhoto = null
                    }
                ) {

                    Text(
                        "CLOSE",
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        )
    }

    // DELETE CONFIRMATION
    deletePhoto?.let { photo ->

        AlertDialog(
            onDismissRequest = {
                deletePhoto = null
            },

            title = {
                Text(
                    "Delete private photo?"
                )
            },

            text = {

                Text(
                    "This encrypted photo will be permanently removed from your vault. This action cannot be undone."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        photo.delete()

                        deletePhoto = null

                        loadPhotos()
                    }
                ) {

                    Text(
                        "DELETE",
                        color =
                            MaterialTheme
                                .colorScheme
                                .error,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        deletePhoto = null
                    }
                ) {

                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun DecoyScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences(
            "decoy_notes",
            Context.MODE_PRIVATE
        )
    }

    val notes = remember {
        mutableStateListOf<String>()
    }

    var noteText by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {

        val savedNotes =
            prefs.getStringSet(
                "saved_decoy_notes",
                emptySet()
            ) ?: emptySet()

        notes.clear()
        notes.addAll(savedNotes)
    }

    fun saveNotes() {

        prefs.edit()
            .putStringSet(
                "saved_decoy_notes",
                notes.toSet()
            )
            .apply()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // WALLPAPER BACKGROUND
        Image(
            painter = painterResource(
                id = R.drawable.decoy_wallpaper
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // DARK / LIGHT OVERLAY
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = 0.15f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(18.dp)
        ) {

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            // HEADER CARD
            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(28.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White.copy(
                                alpha = 0.90f
                            )
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(22.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            "📝",
                            fontSize = 40.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.width(12.dp)
                        )

                        Column {

                            Text(
                                "My Notes",
                                fontSize = 28.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    Color(
                                        0xFF34285A
                                    )
                            )

                            Text(
                                "Quick notes to keep your thoughts ✨",
                                fontSize = 13.sp,
                                color =
                                    Color(
                                        0xFF6E6485
                                    )
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    // NOTE INPUT
                    OutlinedTextField(
                        value = noteText,

                        onValueChange = {
                            noteText = it
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        label = {
                            Text(
                                "Write something..."
                            )
                        },

                        leadingIcon = {
                            Text(
                                "✏️",
                                fontSize = 20.sp
                            )
                        },

                        shape =
                            RoundedCornerShape(18.dp),

                        colors =
                            OutlinedTextFieldDefaults
                                .colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedContainerColor =
                                        Color.White.copy(
                                            alpha = 0.7f
                                        ),

                                    unfocusedContainerColor =
                                        Color.White.copy(
                                            alpha = 0.55f
                                        )
                                )
                    )

                    Spacer(
                        modifier =
                            Modifier.height(14.dp)
                    )

                    // SAVE BUTTON
                    Button(
                        onClick = {

                            if (
                                noteText
                                    .isNotBlank()
                            ) {

                                notes.add(
                                    noteText.trim()
                                )

                                saveNotes()

                                noteText = ""
                            }
                        },

                        enabled =
                            noteText.isNotBlank(),

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(54.dp),

                        shape =
                            RoundedCornerShape(18.dp),

                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        Color(
                                            0xFFE84E87
                                        )
                                )
                    ) {

                        Text(
                            "💾  SAVE NOTE",
                            fontSize = 16.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            // NOTES CARD
            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(28.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White.copy(
                                alpha = 0.90f
                            )
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(18.dp)
                ) {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically,

                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            "Your Notes",
                            fontSize = 23.sp,
                            fontWeight =
                                FontWeight.Bold,
                            color =
                                Color(
                                    0xFF34285A
                                )
                        )

                        Card(
                            shape =
                                RoundedCornerShape(14.dp),

                            colors =
                                CardDefaults
                                    .cardColors(
                                        containerColor =
                                            Color(
                                                0xFFFCE1EC
                                            )
                                    )
                        ) {

                            Text(
                                "${notes.size}",
                                modifier =
                                    Modifier.padding(
                                        horizontal = 13.dp,
                                        vertical = 7.dp
                                    ),
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    Color(
                                        0xFFE84E87
                                    )
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    if (notes.isEmpty()) {

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical = 35.dp
                                    ),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                "📭",
                                fontSize = 42.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(
                                "No notes yet",
                                fontSize = 17.sp,
                                fontWeight =
                                    FontWeight.Medium,
                                color =
                                    Color(
                                        0xFF5D5470
                                    )
                            )

                            Text(
                                "Write your first note above ✨",
                                fontSize = 13.sp,
                                color =
                                    Color(
                                        0xFF837A91
                                    )
                            )
                        }

                    } else {

                        notes.forEachIndexed {
                                index,
                                savedNote ->

                            val cardColor =
                                when (
                                    index % 3
                                ) {

                                    0 ->
                                        Color(
                                            0xFFFFF7E8
                                        )

                                    1 ->
                                        Color(
                                            0xFFF2ECFF
                                        )

                                    else ->
                                        Color(
                                            0xFFE9F7EF
                                        )
                                }

                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 6.dp
                                        ),

                                shape =
                                    RoundedCornerShape(
                                        20.dp
                                    ),

                                colors =
                                    CardDefaults
                                        .cardColors(
                                            containerColor =
                                                cardColor
                                        )
                            ) {

                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),

                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Text(
                                        when (
                                            index % 3
                                        ) {

                                            0 -> "⭐"
                                            1 -> "💜"
                                            else -> "🌿"
                                        },

                                        fontSize =
                                            26.sp
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.width(
                                                12.dp
                                            )
                                    )

                                    Text(
                                        text =
                                            savedNote,

                                        modifier =
                                            Modifier.weight(
                                                1f
                                            ),

                                        fontSize =
                                            16.sp,

                                        color =
                                            Color(
                                                0xFF3E3650
                                            )
                                    )

                                    TextButton(
                                        onClick = {

                                            notes.removeAt(
                                                index
                                            )

                                            saveNotes()
                                        }
                                    ) {

                                        Text(
                                            "🗑",
                                            fontSize =
                                                19.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            // BACK BUTTON
            OutlinedButton(
                onClick = onBack,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),

                shape =
                    RoundedCornerShape(18.dp),

                colors =
                    ButtonDefaults
                        .outlinedButtonColors(
                            containerColor =
                                Color.White.copy(
                                    alpha = 0.80f
                                ),

                            contentColor =
                                Color(
                                    0xFF71529A
                                )
                        )
            ) {

                Text(
                    "←   Back",
                    fontSize = 17.sp,
                    fontWeight =
                        FontWeight.Medium
                )
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )
        }
    }
}
@Composable
fun ChangePinScreen(
    onBack: () -> Unit,
    onPinChanged: () -> Unit
) {
    val context =
        androidx.compose.ui.platform.LocalContext.current

    var oldPin by remember {
        mutableStateOf("")
    }

    var newPin by remember {
        mutableStateOf("")
    }

    var confirmPin by remember {
        mutableStateOf("")
    }

    var error by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            "🔑",
            fontSize = 55.sp
        )

        Text(
            "Change PIN",
            fontSize = 28.sp
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        OutlinedTextField(
            value = oldPin,
            onValueChange = {
                if (
                    it.length <= 4 &&
                    it.all { c -> c.isDigit() }
                ) {
                    oldPin = it
                    error = ""
                }
            },
            label = {
                Text("Current PIN")
            },
            textStyle =
                androidx.compose.ui.text.TextStyle(
                    color =
                        androidx.compose.ui.graphics.Color.Black
                ),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor =
                        androidx.compose.ui.graphics.Color.Black,
                    unfocusedTextColor =
                        androidx.compose.ui.graphics.Color.Black,
                    cursorColor =
                        androidx.compose.ui.graphics.Color.Black
                ),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = newPin,
            onValueChange = {
                if (
                    it.length <= 4 &&
                    it.all { c -> c.isDigit() }
                ) {
                    newPin = it
                    error = ""
                }
            },
            label = {
                Text("New PIN")
            },
            textStyle =
                androidx.compose.ui.text.TextStyle(
                    color =
                        androidx.compose.ui.graphics.Color.Black
                ),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor =
                        androidx.compose.ui.graphics.Color.Black,
                    unfocusedTextColor =
                        androidx.compose.ui.graphics.Color.Black,
                    cursorColor =
                        androidx.compose.ui.graphics.Color.Black
                ),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = confirmPin,
            onValueChange = {
                if (
                    it.length <= 4 &&
                    it.all { c -> c.isDigit() }
                ) {
                    confirmPin = it
                    error = ""
                }
            },
            label = {
                Text("Confirm New PIN")
            },
            textStyle =
                androidx.compose.ui.text.TextStyle(
                    color =
                        androidx.compose.ui.graphics.Color.Black
                ),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor =
                        androidx.compose.ui.graphics.Color.Black,
                    unfocusedTextColor =
                        androidx.compose.ui.graphics.Color.Black,
                    cursorColor =
                        androidx.compose.ui.graphics.Color.Black
                ),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        if (error.isNotEmpty()) {

            Text(
                error,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )
        }

        Button(
            onClick = {

                when {

                    !verifyPin(
                        context,
                        oldPin
                    ) -> {
                        error =
                            "❌ Current PIN is incorrect."
                    }

                    newPin.length != 4 -> {
                        error =
                            "New PIN must be 4 digits."
                    }

                    newPin != confirmPin -> {
                        error =
                            "❌ New PINs do not match."
                    }

                    oldPin == newPin -> {
                        error =
                            "Choose a different PIN."
                    }

                    else -> {

                        savePin(
                            context,
                            newPin
                        )

                        onPinChanged()
                    }
                }
            },
            enabled =
                oldPin.length == 4 &&
                        newPin.length == 4 &&
                        confirmPin.length == 4,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SAVE NEW PIN")
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        TextButton(
            onClick = onBack
        ) {
            Text("CANCEL")
        }
    }
}