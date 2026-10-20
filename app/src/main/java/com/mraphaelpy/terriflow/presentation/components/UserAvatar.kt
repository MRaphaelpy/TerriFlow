package com.mraphaelpy.terriflow.presentation.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mraphaelpy.terriflow.domain.model.User

val LocalUserDirectory = staticCompositionLocalOf<Map<String, User>> { emptyMap() }

@Composable
fun UserAvatar(
    user: User?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val current = user?.let { LocalUserDirectory.current[it.id] } ?: user
    AvatarImage(name = current?.name.orEmpty(), photoUrl = current?.photoUrl, size = size, modifier = modifier)
}

@Composable
fun UserAvatarByName(
    name: String,
    photoUrl: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    userId: String? = null
) {
    val directoryUser = userId?.let { LocalUserDirectory.current[it] }
    AvatarImage(
        name = directoryUser?.name ?: name,
        photoUrl = directoryUser?.photoUrl?.takeIf { it.isNotEmpty() } ?: photoUrl,
        size = size,
        modifier = modifier
    )
}

@Composable
private fun AvatarImage(name: String, photoUrl: String?, size: Dp, modifier: Modifier) {
    if (photoUrl.isNullOrEmpty()) {
        DefaultAvatar(initial = name.trim().take(1).uppercase().ifEmpty { "?" }, size = size, modifier = modifier)
        return
    }
    val description = "Foto de perfil de $name"
    val inlineBitmap = remember(photoUrl) { decodeDataUri(photoUrl) }
    if (inlineBitmap != null) {
        Image(
            bitmap = inlineBitmap,
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape)
        )
    } else {
        AsyncImage(
            model = photoUrl,
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape)
        )
    }
}

private fun decodeDataUri(photoUrl: String): ImageBitmap? {
    if (!photoUrl.startsWith("data:image")) return null
    return runCatching {
        val bytes = Base64.decode(photoUrl.substringAfter("base64,"), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}

@Composable
private fun DefaultAvatar(initial: String, size: Dp, modifier: Modifier) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            val textStyle = when {
                size < 24.dp -> MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                size < 36.dp -> MaterialTheme.typography.labelMedium
                else -> MaterialTheme.typography.titleMedium
            }
            Text(text = initial, style = textStyle, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
        }
    }
}
