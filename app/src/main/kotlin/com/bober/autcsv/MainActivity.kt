package com.bober.autcsv

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.rememberNavController
import com.bober.autcsv.core.utils.BiometricLockStore
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.presentation.navigation.Navigation
import com.bober.autcsv.ui.theme.AppColors
import com.bober.autcsv.ui.theme.AutCSVTheme
import com.bober.autcsv.ui.theme.MD
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * AppCompatActivity (наследник FragmentActivity — BiometricPrompt работает):
 * AppCompatDelegate применяет per-app locales и автоматически пересоздает
 * Activity при смене языка на всех поддерживаемых API (32+).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var biometricLock: BiometricLockStore

    /** Есть ли на устройстве готовая биометрия (отпечаток/лицо). */
    private fun canAuthenticate(): Boolean =
        BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS

    /** Показывает системный биометрический диалог. */
    private fun authenticate(onSuccess: () -> Unit, onError: (String) -> Unit) {
        // Fail-open: если биометрия отвязана после включения блокировки,
        // приложение не должно стать неоткрываемым
        if (!canAuthenticate()) {
            onSuccess()
            return
        }
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                onSuccess()

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                onError(errString.toString())
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback)

        // BIOMETRIC_WEAK без DEVICE_CREDENTIAL: negative-кнопка обязательна,
        // иначе PromptInfo.Builder.build() бросает IllegalArgumentException
        // (падало при холодном старте с включённой блокировкой).
        // Fail-open целиком: любая ошибка сборки/запуска диалога на конкретном
        // OEM-устройстве не должна оставлять приложение неоткрываемым.
        runCatching {
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_locked_title))
                .setSubtitle(getString(R.string.biometric_locked_subtitle))
                .setNegativeButtonText(getString(R.string.biometric_cancel))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .setConfirmationRequired(false)
                .build()
            prompt.authenticate(info)
        }.onFailure { error ->
            LlmLogger.logError("Биометрический диалог не удалось показать", error)
            onSuccess()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() вызывается ДО super.onCreate() —
        // это системный сплэш из темы Theme.AutCSV.Splash (иконка на весь экран)
        val splashScreen = installSplashScreen()

        // Держим системный сплэш только до момента, пока Compose не отрисует
        // самый первый кадр (то есть наш собственный SplashScreen). Как только
        // он готов — системное окно исчезает мгновенно, без задержки старта,
        // и пользователь видит уже наш SplashScreen с логотипом и лоадером.
        var isFirstFrameDrawn = false
        splashScreen.setKeepOnScreenCondition { !isFirstFrameDrawn }

        super.onCreate(savedInstanceState)
        // Иконки системных баров подстраиваются под системную тему:
        // тёмные — на светлой, светлые — на тёмной
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            AutCSVTheme {
                // Биометрическая блокировка (№36): экран входа при старте
                // и после возвращения из фона, если включена в настройках
                var isLocked by remember { mutableStateOf(biometricLock.enabled) }
                var lockError by remember { mutableStateOf<String?>(null) }

                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP && biometricLock.enabled) {
                            isLocked = true
                            lockError = null
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val activity = LocalContext.current as? MainActivity
                LaunchedEffect(isLocked) {
                    if (isLocked && activity != null) {
                        activity.authenticate(
                            onSuccess = { isLocked = false },
                            onError = { lockError = it },
                        )
                    }
                }

                if (isLocked && activity != null) {
                    BiometricLockScreen(
                        errorText = lockError,
                        onRetry = {
                            lockError = null
                            activity.authenticate(
                                onSuccess = { isLocked = false },
                                onError = { lockError = it },
                            )
                        },
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        Navigation(navController = navController)
                    }
                }
            }
            // Сигнализируем, что первый кадр Compose-дерева отрисован —
            // системный сплэш можно скрывать.
            LaunchedEffect(Unit) {
                isFirstFrameDrawn = true
            }
        }
    }
}

/**
 * Экран блокировки: иконка замка, сообщение и кнопка повторного запроса
 * биометрии (после отмены диалога).
 */
@Composable
private fun BiometricLockScreen(
    errorText: String?,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.BackgroundSoft)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.biometric_locked_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = errorText ?: stringResource(R.string.biometric_locked_subtitle),
            fontSize = 14.sp,
            color = AppColors.TextDim,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(MD),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Accent,
                contentColor = AppColors.AccentInk,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.biometric_unlock), fontWeight = FontWeight.Bold)
        }
    }
}
