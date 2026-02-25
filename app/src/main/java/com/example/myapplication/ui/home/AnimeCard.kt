package com.example.myapplication.ui.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.CupertinoMaterials
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.myapplication.data.models.Anime
import com.example.myapplication.isAppInDarkTheme
import com.example.myapplication.ui.shared.fluidClickable
import com.example.myapplication.ui.shared.theme.SnProFamily
import com.example.myapplication.ui.shared.theme.getRatingColor

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.OneUiAnimeCard(
    anime: Anime,
    animatedVisibilityScope: AnimatedVisibilityScope,
    getImgPath: (String?) -> java.io.File?,
    getGenreName: (String) -> String,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localHazeState = remember { HazeState() }
    val isDark = isAppInDarkTheme()
    val borderStroke = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.4f)
    val cardBg = if (isDark) Color(0xFF1C1F28) else Color.White
    val cardShadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.08f)
    val subtitleColor = if (isDark) Color(0xFF9898A0) else Color(0xFF8E8E93)
    val chipBg = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .sharedBounds(
                sharedContentState = rememberSharedContentState(key = "anime_${anime.id}_bounds"),
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(24.dp))
            )
            .fillMaxWidth()
            .fluidClickable(scaleDown = 0.975f, onClick = onClick)
            .shadow(
                elevation = if (isDark) 8.dp else 4.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = cardShadowColor
            )
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                RoundedCornerShape(24.dp)
            )
            .animateContentSize(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Постер с Shared Element (картинка плавно перелетит на AddEditScreen)
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 126.dp)
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "anime_${anime.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .hazeSource(state = localHazeState)
                    .background(if (isDark) Color(0xFF2C2C34) else Color(0xFFE8E8ED)),
                contentAlignment = Alignment.Center
            ) {
                // Fallback-подложка
                Text(
                    text = anime.title.take(1).uppercase(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White.copy(alpha = 0.3f)
                    else Color.Black.copy(alpha = 0.15f),
                    fontFamily = SnProFamily
                )

                val imgFile = remember(anime.imageFileName) { getImgPath(anime.imageFileName) }
                if (imgFile != null) {
                    val context = LocalContext.current
                    val filePath = imgFile.absolutePath
                    val imageRequest = remember(filePath) {
                        ImageRequest.Builder(context)
                            .data(imgFile)
                            .crossfade(true)
                            .memoryCacheKey(filePath)
                            .diskCacheKey(filePath)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = anime.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Content column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                // Title + Rating row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = anime.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            lineHeight = 22.sp,
                            fontFamily = SnProFamily,
                            lineBreak = LineBreak.Heading,
                            hyphens = Hyphens.Auto
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    // Rating chip on the right
                    if (anime.rating > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .hazeEffect(
                                    state = localHazeState,
                                    style = CupertinoMaterials.thin()
                                )
                                .border(0.5.dp, borderStroke, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = getRatingColor(anime.rating),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${anime.rating}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = getRatingColor(anime.rating),
                                    fontFamily = SnProFamily
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Genre tags row
                val genreTexts = anime.tags.take(2).mapNotNull { tag ->
                    val name = getGenreName(tag)
                    name.ifBlank { null }
                }
                if (genreTexts.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        genreTexts.forEach { genreText ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(chipBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = genreText,
                                    fontSize = 12.sp,
                                    color = subtitleColor,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = SnProFamily
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                // Episodes + category
                val categoryLabel = anime.categoryType.ifBlank { null }
                Text(
                    text = buildString {
                        append("${anime.episodes} episodes")
                        if (categoryLabel != null) append(" · $categoryLabel")
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontFamily = SnProFamily
                    ),
                    color = subtitleColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(12.dp))

                // Details Button — M3 Semantic Colors + Expressive Contrast
                Box(
                    modifier = Modifier
                        .fluidClickable(scaleDown = 0.92f, onClick = onDetailsClick)
                        .clip(RoundedCornerShape(100))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Details",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Details",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = SnProFamily,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
