package com.mraphaelpy.terriflow.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import coil.compose.AsyncImage
import com.mraphaelpy.terriflow.domain.model.User
import androidx.compose.foundation.Image
import androidx.compose.runtime.remember

@Composable
fun UserAvatar(
    user: User?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    if (user == null) {
        DefaultAvatar(initial = "?", size = size, modifier = modifier)
        return
    }
    if (!user.photoUrl.isNullOrEmpty()) {
        val bitmap = remember(user.photoUrl) {
            try {
                if (user.photoUrl.startsWith("data:image")) {
                    val base64Image = user.photoUrl.substringAfter("base64,")
                    val decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                } else null
            } catch (e: Exception) { null }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Foto de perfil de ${user.name}",
                contentScale = ContentScale.Crop,
                modifier = modifier.size(size).clip(CircleShape)
            )
        } else {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = "Foto de perfil de ${user.name}",
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
            )
        }
    } else {
        DefaultAvatar(
            initial = if (user.name.isNotBlank()) user.name.take(1).uppercase() else "?",
            size = size,
            modifier = modifier
        )
    }
}

@Composable
fun UserAvatarByName(
    name: String,
    photoUrl: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    if (!photoUrl.isNullOrEmpty()) {
        val bitmap = remember(photoUrl) {
            try {
                if (photoUrl.startsWith("data:image")) {
                    val base64Image = photoUrl.substringAfter("base64,")
                    val decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                } else null
            } catch (e: Exception) { null }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Foto de perfil de $name",
                contentScale = ContentScale.Crop,
                modifier = modifier.size(size).clip(CircleShape)
            )
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Foto de perfil de $name",
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
            )
        }
    } else {
        DefaultAvatar(
            initial = if (name.isNotBlank()) name.take(1).uppercase() else "?",
            size = size,
            modifier = modifier
        )
    }
}

@Composable
private fun DefaultAvatar(initial: String, size: Dp, modifier: Modifier) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
