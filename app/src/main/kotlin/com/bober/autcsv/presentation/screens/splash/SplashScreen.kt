package com.bober.autcsv.presentation.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bober.autcsv.R
import com.bober.autcsv.ui.theme.AppColors
import kotlinx.coroutines.delay

/**
 * Сплэш-экран в стиле дизайн-макета: тёплое радиальное свечение по краям,
 * лого-плашка с инициалом, крупный заголовок и акцентный лоадер.
 */
@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "splashContentAlpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(200)
        onNavigateToMain()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.BackgroundSoft)
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AppColors.Accent.copy(alpha = 0.10f), Color.Transparent
                        ),
                        center = Offset(widthPx * 0.12f, heightPx * -0.05f),
                        radius = widthPx * 1.1f
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AppColors.Mint.copy(alpha = 0.09f), Color.Transparent
                        ),
                        center = Offset(widthPx * 0.95f, heightPx * 0.12f),
                        radius = widthPx * 0.9f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .graphicsLayerAlpha(contentAlpha)
            ) {
                // Лого-плашка вместо прежнего пустого бейджа с точкой
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    AppColors.Accent,
                                    AppColors.Accent.copy(alpha = 0.7f)
                                )
                            ),
                            RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.splash_logo_letter),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = AppColors.BackgroundSoft
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    letterSpacing = (-0.5).sp,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.create_prof_resume),
                    fontSize = 14.sp,
                    color = AppColors.TextDim,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = AppColors.Accent,
                    strokeWidth = 2.5.dp,
                    trackColor = AppColors.Surface3
                )
            }
        }
    }
}

/**
 * Небольшой хелпер, чтобы плавно проявлять контент сплэш-экрана через alpha.
 */
private fun Modifier.graphicsLayerAlpha(alpha: Float): Modifier =
    this.then(Modifier.graphicsLayer(alpha = alpha))