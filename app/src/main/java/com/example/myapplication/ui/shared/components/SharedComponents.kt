package com.example.myapplication.ui.shared.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.models.*
import com.example.myapplication.data.repository.GenreRepository
import com.example.myapplication.network.AnimeDetails
import com.example.myapplication.network.AppLanguage
import com.example.myapplication.ui.shared.theme.*
import org.koin.compose.koinInject

// ==========================================
// AnimatedSaveFab
// ==========================================
@Composable
fun AnimatedSaveFab(isEnabled: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fabScale"
    )
    if (scale > 0f) {
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = BrandBlue,
            contentColor = Color.White,
            modifier = Modifier.size((56 * scale).dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = "Save")
        }
    }
}

// ==========================================
// AnimatedOneUiTextField
// ==========================================
@Composable
fun AnimatedOneUiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) BrandBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        label = "border"
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .onFocusChanged { isFocused = it.isFocused },
        singleLine = singleLine,
        maxLines = maxLines,
        textStyle = TextStyle(
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(BrandBlue),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontSize = 16.sp
                    )
                }
                innerTextField()
            }
        }
    )
}

// ==========================================
// AnimatedCopyButton
// ==========================================
@Composable
fun AnimatedCopyButton(textToCopy: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    IconButton(onClick = {
        clipboardManager.setText(AnnotatedString(textToCopy))
        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
    }) {
        Icon(
            Icons.Default.ContentCopy,
            contentDescription = "Copy",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ==========================================
// EpisodeSuggestions
// ==========================================
@Composable
fun EpisodeSuggestions(onSelect: (String) -> Unit) {
    val suggestions = listOf("12", "13", "24", "25", "26")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { ep ->
            SuggestionChip(
                onClick = { onSelect(ep) },
                label = { Text(ep) },
                modifier = Modifier.height(32.dp)
            )
        }
    }
}

// ==========================================
// StarRatingBar
// ==========================================
@Composable
fun StarRatingBar(rating: Int, onRatingChanged: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        for (i in 1..5) {
            IconButton(onClick = { onRatingChanged(i) }) {
                Icon(
                    imageVector = if (i <= rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Star $i",
                    tint = if (i <= rating) getRatingColor(i) else RateColorEmpty,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

// ==========================================
// GenreSelectionSection — collapsible categories
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenreSelectionSection(
    strings: UiStrings,
    currentLanguage: AppLanguage,
    selectedTags: List<String>,
    activeCategory: String,
    onTagToggle: (String, String) -> Unit
) {
    val genreRepository: GenreRepository = koinInject()
    val animeGenres = remember { genreRepository.getGenresForCategory(GenreCategory.ANIME) }
    val movieGenres = remember { genreRepository.getGenresForCategory(GenreCategory.MOVIE) }
    val seriesGenres = remember { genreRepository.getGenresForCategory(GenreCategory.SERIES) }

    var expandedCategory by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }

    val categories = listOf(
        Triple("Anime", strings.genreAnime, animeGenres),
        Triple("Movies", strings.genreMovies, movieGenres),
        Triple("Series", strings.genreSeries, seriesGenres)
    )

    val categoryIcons = mapOf(
        "Anime" to Icons.Outlined.Animation,
        "Movies" to Icons.Outlined.Movie,
        "Series" to Icons.Outlined.Tv
    )
    val categoryColors = mapOf(
        "Anime" to Color(0xFFFF2D55),
        "Movies" to Color(0xFF5AC8FA),
        "Series" to Color(0xFFFFCC00)
    )

    categories.forEach { (categoryType, label, genres) ->
        val isActive = activeCategory.isEmpty() || activeCategory == categoryType
        val hasSelectedTags = selectedTags.any { tag -> genres.any { it.id == tag } }
        val isExpanded = expandedCategory == categoryType

        if (isActive || hasSelectedTags) {
            val accentColor = categoryColors[categoryType] ?: BrandBlue
            val catIcon = categoryIcons[categoryType] ?: Icons.Outlined.Animation

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        expandedCategory = if (isExpanded) null else categoryType
                    }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    catIcon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (hasSelectedTags) {
                    val count = selectedTags.count { tag -> genres.any { it.id == tag } }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accentColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "$count",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp)
                ) {
                    genres.forEach { genreDef ->
                        val isSelected = selectedTags.contains(genreDef.id)
                        val displayName = if (currentLanguage == AppLanguage.RU) genreDef.ru else genreDef.en
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTagToggle(genreDef.id, categoryType) },
                            label = { Text(displayName, fontSize = 13.sp) },
                            enabled = isActive,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.15f),
                                selectedLabelColor = accentColor
                            )
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// DetailsContent (for DetailsScreen)
// ==========================================
@Composable
fun DetailsContent(
    details: AnimeDetails,
    currentLanguage: AppLanguage
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = details.title,
            style = MaterialTheme.typography.titleLarge.copy(
                lineBreak = LineBreak.Heading,
                hyphens = Hyphens.Auto
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        val altTitle = details.altTitle
        if (!altTitle.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = altTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text(details.type) })
            AssistChip(onClick = {}, label = { Text(details.status) })
            if (details.episodesTotal != null) {
                AssistChip(onClick = {}, label = { Text("${details.episodesAired}/${details.episodesTotal} ep") })
            }
        }
        if (details.genres.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                details.genres.take(3).forEach { genre ->
                    SuggestionChip(onClick = {}, label = { Text(genre, fontSize = 12.sp) })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = details.description,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineBreak = LineBreak.Paragraph,
                hyphens = Hyphens.Auto
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            maxLines = 8
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Source: ${details.source}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

// ==========================================
// StatsCard (expandable card for Settings)
// ==========================================
@Composable
fun StatsCard(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            if (isExpanded) {
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    content()
                }
            }
        }
    }
}

// ==========================================
// LanguageOption (for SettingsScreen)
// ==========================================
@Composable
fun LanguageOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) BrandBlue else Color.Transparent,
        label = "bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "content"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) BrandBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        label = "border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(1.dp, if (isSelected) Color.Transparent else borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Custom Radio Indicator
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color.White else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(BrandBlue)
                )
            }
        }
        
        Spacer(Modifier.width(14.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}

// ==========================================
// ThemeOptionItem (for SettingsScreen)
// ==========================================
@Composable
fun ThemeOptionItem(
    label: String,
    isSelected: Boolean,
    themeType: AppTheme,
    onClick: () -> Unit
) {
    val icon = when (themeType) {
        AppTheme.LIGHT -> Icons.Default.LightMode
        AppTheme.DARK -> Icons.Default.DarkMode
        AppTheme.SYSTEM -> Icons.Default.SettingsBrightness
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) BrandBlue.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surface
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) BrandBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = if (isSelected) BrandBlue else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) BrandBlue else MaterialTheme.colorScheme.onSurface
        )
    }
}

// ==========================================
// UpdateStateButton (for SettingsScreen)
// ==========================================
@Composable
fun UpdateStateButton(
    status: AppUpdateStatus,
    idleText: String,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (status) {
            AppUpdateStatus.IDLE -> MaterialTheme.colorScheme.secondary
            AppUpdateStatus.LOADING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            AppUpdateStatus.NO_UPDATE -> RateColor4
            AppUpdateStatus.UPDATE_AVAILABLE -> BrandBlue
            AppUpdateStatus.ERROR -> BrandRed
        },
        label = "btnBg"
    )
    val contentColor by animateColorAsState(
        targetValue = when (status) {
            AppUpdateStatus.IDLE -> MaterialTheme.colorScheme.onSecondary
            AppUpdateStatus.LOADING -> MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
            else -> Color.White
        },
        label = "btnContent"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(if (status == AppUpdateStatus.IDLE) 1f else 0.6f)
            .height(50.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(enabled = status != AppUpdateStatus.LOADING) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            AppUpdateStatus.IDLE -> Text(idleText, fontWeight = FontWeight.Bold, color = contentColor, fontSize = 16.sp)
            AppUpdateStatus.LOADING -> CircularProgressIndicator(modifier = Modifier.size(24.dp), color = contentColor, strokeWidth = 2.dp)
            AppUpdateStatus.NO_UPDATE -> Icon(Icons.Default.Check, "Up to date", tint = contentColor, modifier = Modifier.size(28.dp))
            AppUpdateStatus.UPDATE_AVAILABLE -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SystemUpdateAlt, "Update", tint = contentColor, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download", fontWeight = FontWeight.Bold, color = contentColor)
            }
            AppUpdateStatus.ERROR -> Icon(Icons.Default.Close, "Error", tint = contentColor, modifier = Modifier.size(28.dp))
        }
    }
}
