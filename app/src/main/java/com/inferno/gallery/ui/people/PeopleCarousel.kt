@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.people

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.inferno.gallery.R
import com.inferno.gallery.data.db.PersonEntity
import com.inferno.gallery.ui.SectionHeader
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.utils.pressScale
import java.io.File

@Composable
fun PeopleCarousel(
    people: List<PersonEntity>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSeeAll: () -> Unit,
    onPersonClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (people.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
    ) {
        SectionHeader(
            title = "People",
            count = people.size,
            isExpanded = isExpanded,
            onToggle = onToggle,
            seeAllText = "Show all",
            onSeeAll = onSeeAll
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = MotionTokens.snappySpring()) + fadeIn(animationSpec = MotionTokens.snappySpring()),
            exit = shrinkVertically(animationSpec = MotionTokens.snappySpring()) + fadeOut(animationSpec = MotionTokens.snappySpring())
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = SpacingTokens.XS),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M)
            ) {
                items(
                    items = people,
                    key = { "person_${it.id}" }
                ) { person ->
                    PersonAvatarCard(
                        person = person,
                        onClick = { onPersonClick(person.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonAvatarCard(
    person: PersonEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(84.dp)
            .pressScale(pressedScale = 0.94f)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
    ) {
        Surface(
            shape = ShapeFull,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            modifier = Modifier.size(72.dp)
        ) {
            val coverFile = person.coverCropPath?.let { File(it) }
            if (coverFile != null && coverFile.exists()) {
                AsyncImage(
                    model = coverFile,
                    contentDescription = person.name ?: "Person",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!person.name.isNullOrBlank()) {
                        Text(
                            text = person.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                            contentDescription = "Person",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = person.name ?: "Unnamed",
            style = MaterialTheme.typography.labelLarge,
            color = if (person.name != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "${person.faceCount} ${if (person.faceCount == 1) "photo" else "photos"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
