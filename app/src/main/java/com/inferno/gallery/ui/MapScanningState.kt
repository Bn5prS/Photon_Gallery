@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui

import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import androidx.compose.ui.graphics.vector.ImageVector


import androidx.compose.material3.Surface
import com.inferno.gallery.ui.components.PhotonEmptyState
import com.inferno.gallery.ui.theme.ExpressiveShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsScanningAnimation(
    scanState: GalleryViewModel.GpsScanState,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Map") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBackClick) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_ms_arrow_back), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { paddingValues ->
        val state = scanState as? GalleryViewModel.GpsScanState.Scanning
        val total = state?.total ?: 0
        val processed = state?.processed ?: 0
        val progress = if (total > 0) processed.toFloat() / total else 0f
        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "gpsProgress"
        )

        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                // Scanning progress indicator container
                Surface(
                    shape = ExpressiveShapes.Clover8Leaf,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        androidx.compose.material3.ContainedLoadingIndicator(
                            modifier = Modifier.size(60.dp),
                            indicatorColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    "Scanning GPS data",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(8.dp))

                if (total > 0) {
                    Text(
                        "$processed / $total photos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    LinearWavyProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(10.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                } else {
                    Text(
                        "Preparing photo location index…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoGeotaggedPhotosState(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Map") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBackClick) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_ms_arrow_back), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            PhotonEmptyState(
                icon = ImageVector.vectorResource(R.drawable.ic_ms_location_on),
                title = "No location data",
                subtitle = "None of your photos have GPS coordinates embedded. Photos taken with location enabled will appear here.",
                shape = ExpressiveShapes.Cookie12Sided
            )
        }
    }
}
