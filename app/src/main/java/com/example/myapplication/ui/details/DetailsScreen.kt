package com.example.myapplication.ui.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import com.example.myapplication.data.models.Anime
import com.example.myapplication.network.AppLanguage
import com.example.myapplication.isAppInDarkTheme
import com.example.myapplication.ui.shared.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    animeId: String,
    navController: NavController,
    onBackClick: () -> Unit = { navController.popBackStack() },
    viewModel: DetailsViewModel = koinViewModel()
) {
    val anime by viewModel.currentAnime.collectAsStateWithLifecycle()
    val language by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isAppInDarkTheme()
    val screenBg = if (isDark) Color(0xFF141419) else Color(0xFFF2F2F7)

    anime?.let { currentAnime ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(screenBg)
        ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    AnimeDetailsSheet(
                        viewModel = viewModel,
                        anime = currentAnime,
                        language = language,
                        getImgPath = { viewModel.getImgPath(it) },
                        onDismiss = onBackClick,
                        embedded = true
                    )
                }
        }
    } ?: run {
        LaunchedEffect(Unit) { onBackClick() }
    }
}

@Composable
fun AnimeDetailsSheet(
    viewModel: DetailsViewModel,
    anime: Anime,
    language: AppLanguage,
    getImgPath: (String?) -> File?,
    onDismiss: () -> Unit,
    embedded: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isAppInDarkTheme()
    var visible by remember { mutableStateOf(embedded) }

    if (!embedded) LaunchedEffect(Unit) { visible = true }

    fun triggerDismiss() {
        visible = false
    }

    if (!embedded) {
        LaunchedEffect(visible) {
            if (!visible) {
                delay(280)
                onDismiss()
            }
        }
        BackHandler { triggerDismiss() }
    }

    val contentAlpha = remember { Animatable(if (embedded) 1f else 0f) }
    val contentOffsetY = remember { Animatable(if (embedded) 0f else 20f) }
    if (!embedded) {
        LaunchedEffect(visible) {
            if (visible) {
                delay(200)
                launch { contentAlpha.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = 300f)) }
                launch { contentOffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 300f)) }
            }
        }
    }

    val panelBg = if (isDark) Color(0xFF1A1D26) else Color.White
    val subtitleColor = if (isDark) Color(0xFF9898A0) else Color(0xFF8E8E93)

    if (embedded) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            DetailsSheetBody(
                anime = anime,
                uiState = uiState,
                language = language,
                isDark = isDark,
                panelBg = panelBg,
                subtitleColor = subtitleColor,
                getImgPath = getImgPath,
                contentAlpha = contentAlpha,
                contentOffsetY = contentOffsetY,
                showCloseButton = false,
                onDismiss = onDismiss
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { triggerDismiss() }
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ) + fadeIn(animationSpec = tween(250)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = tween(200))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 620.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = Color.Black.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = panelBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    DetailsSheetBody(
                        anime = anime,
                        uiState = uiState,
                        language = language,
                        isDark = isDark,
                        panelBg = panelBg,
                        subtitleColor = subtitleColor,
                        getImgPath = getImgPath,
                        contentAlpha = contentAlpha,
                        contentOffsetY = contentOffsetY,
                        showCloseButton = true,
                        onDismiss = { triggerDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsSheetBody(
    anime: Anime,
    uiState: DetailsUiState,
    language: AppLanguage,
    isDark: Boolean,
    panelBg: Color,
    subtitleColor: Color,
    getImgPath: (String?) -> File?,
    contentAlpha: Animatable<Float, *>,
    contentOffsetY: Animatable<Float, *>,
    showCloseButton: Boolean,
    onDismiss: () -> Unit
) {
    // Hero image
    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    ) {
                        val imgFile = getImgPath(anime.imageFileName)
                        if (imgFile != null && imgFile.exists()) {
                            AsyncImage(
                                model = imgFile,
                                contentDescription = anime.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isDark) Color(0xFF2C2C34) else Color(0xFFE8E8ED)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Movie,
                                    contentDescription = null,
                                    tint = subtitleColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        // Gradient overlay at bottom of image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, panelBg)
                                    )
                                )
                        )

                        if (showCloseButton) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Content with stagger animation
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = contentAlpha.value
                                translationY = contentOffsetY.value
                            }
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        // Title
                        Text(
                            text = anime.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = SnProFamily,
                                lineHeight = 28.sp,
                                lineBreak = LineBreak.Heading,
                                hyphens = Hyphens.Auto
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(12.dp))

                        // Info chips row - always visible from local data
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (anime.rating > 0) {
                                InfoChip(
                                    icon = Icons.Rounded.Star,
                                    text = "${anime.rating}/5",
                                    color = getRatingColor(anime.rating),
                                    isDark = isDark
                                )
                            }
                            InfoChip(
                                icon = Icons.Default.Tv,
                                text = "${anime.episodes} ep",
                                color = EpisodesColor,
                                isDark = isDark
                            )
                            if (anime.categoryType.isNotBlank()) {
                                InfoChip(
                                    icon = Icons.Outlined.Movie,
                                    text = anime.categoryType,
                                    color = BrandBlue,
                                    isDark = isDark
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // API details
                        when (uiState) {
                            is DetailsUiState.Idle, DetailsUiState.Loading -> {
                                // Loading shimmer
                                Column {
                                    repeat(3) { i ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(if (i == 2) 0.6f else 1f)
                                                .height(14.dp)
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(
                                                    if (isDark) Color.White.copy(alpha = 0.06f)
                                                    else Color.Black.copy(alpha = 0.06f)
                                                )
                                        )
                                        Spacer(Modifier.height(10.dp))
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = BrandBlue,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            if (language == AppLanguage.RU) "Загрузка..." else "Fetching details...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = subtitleColor,
                                            fontFamily = SnProFamily
                                        )
                                    }
                                }
                            }

                            is DetailsUiState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isDark) BrandRed.copy(alpha = 0.1f)
                                            else BrandRed.copy(alpha = 0.06f)
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ErrorOutline,
                                            null,
                                            tint = BrandRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            if (language == AppLanguage.RU) "Не удалось загрузить" else "Could not load details",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = BrandRed,
                                            fontFamily = SnProFamily
                                        )
                                    }
                                }
                            }

                            is DetailsUiState.Success -> {
                                val details = (uiState as DetailsUiState.Success).details

                                // Alt title
                                val altTitleText = details.altTitle
                                if (!altTitleText.isNullOrBlank()) {
                                    Text(
                                        text = altTitleText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = SnProFamily
                                        ),
                                        color = subtitleColor
                                    )
                                    Spacer(Modifier.height(12.dp))
                                }

                                // Type / Status chips from API
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (details.type.isNotBlank()) {
                                        AssistChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    details.type,
                                                    fontSize = 12.sp,
                                                    fontFamily = SnProFamily
                                                )
                                            }
                                        )
                                    }
                                    if (details.status.isNotBlank()) {
                                        AssistChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    details.status,
                                                    fontSize = 12.sp,
                                                    fontFamily = SnProFamily
                                                )
                                            }
                                        )
                                    }
                                    if (details.episodesTotal != null) {
                                        AssistChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    "${details.episodesAired}/${details.episodesTotal} ep",
                                                    fontSize = 12.sp,
                                                    fontFamily = SnProFamily
                                                )
                                            }
                                        )
                                    }
                                }

                                // API genres
                                if (details.genres.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        details.genres.take(3).forEach { genre ->
                                            SuggestionChip(
                                                onClick = {},
                                                label = {
                                                    Text(
                                                        genre,
                                                        fontSize = 11.sp,
                                                        fontFamily = SnProFamily
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }

                                // Description (guard against literal "null" string)
                                val descText = details.description.takeIf { it.isNotBlank() && it != "null" }
                                if (descText != null) {
                                    Spacer(Modifier.height(16.dp))
                                    HorizontalDivider(
                                        color = if (isDark) Color.White.copy(alpha = 0.06f)
                                        else Color.Black.copy(alpha = 0.06f)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = descText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = SnProFamily,
                                            lineHeight = 22.sp,
                                            lineBreak = LineBreak.Paragraph,
                                            hyphens = Hyphens.Auto
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        maxLines = 10,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Source attribution
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Source: ${details.source}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = SnProFamily
                                    ),
                                    color = subtitleColor.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = if (isDark) 0.15f else 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = color,
                fontFamily = SnProFamily
            )
        }
    }
}
