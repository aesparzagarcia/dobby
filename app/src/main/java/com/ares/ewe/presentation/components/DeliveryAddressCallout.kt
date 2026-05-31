package com.ares.ewe.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.ewe.core.theme.DobbyColors

private val CalloutBlue = DobbyColors.Primary

/** ~30% less motion than the original −10dp peak. */
private const val BobAmplitudeDp = 7f

/**
 * Blue callout: small **upward** tail on top (biased left), body below; bobs vertically.
 */
@Composable
fun DeliveryAddressCallout(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "address_callout_bob")
    val offsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = -BobAmplitudeDp,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob_y",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = offsetY.dp),
    ) {
        // Pico arriba, misma altura; espejo al lado izquierdo (alineado con el margen del texto de dirección).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .padding(start = 16.dp),
        ) {
            Canvas(
                modifier = Modifier
                    .width(21.dp)
                    .height(10.dp)
                    .align(Alignment.TopStart),
            ) {
                val w = size.width
                val h = size.height
                val tipX = w * 0.22f
                val path = Path().apply {
                    moveTo(tipX, 0f)
                    lineTo(0f, h)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = path, color = CalloutBlue)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CalloutBlue)
                .clickable(onClick = onClick)
                .padding(horizontal = 13.dp, vertical = 10.dp),
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 19.sp,
            )
        }
    }
}
