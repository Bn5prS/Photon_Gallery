package com.inferno.gallery.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * Top-level navigation graph for Photon Gallery.
 *
 * CRITICAL M3 EXPRESSIVE RULE: The entire [NavHost] is wrapped inside a
 * [SharedTransitionLayout] so that shared element transitions (shape-morphing)
 * work seamlessly between the gallery grid and the detail screen.
 *
 * Both [SharedTransitionScope] and [AnimatedVisibilityScope] are forwarded
 * into each screen composable, enabling [Modifier.sharedElement()] to
 * coordinate the cross-destination animation.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavigationGraph(
    isLoading: Boolean,

    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    if (isLoading) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }

    val galleryViewModel: GalleryViewModel = viewModel()

    SharedTransitionLayout(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = "gallery"
        ) {

            composable("gallery") {
                MainAppLayout(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onPhotoClick = { mediaId, bucket, query ->
                        var route = "detail/$mediaId"
                        if (bucket != null) route += "?bucket=${android.net.Uri.encode(bucket)}"
                        if (query != null) {
                            route += if (bucket != null) "&highlight=${android.net.Uri.encode(query)}" 
                                     else "?highlight=${android.net.Uri.encode(query)}"
                        }
                        navController.navigate(route)
                    },
                    viewModel = galleryViewModel
                )
            }

            composable(
                route = "detail/{mediaId}?bucket={bucketName}&highlight={highlightText}",
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.StringType },
                    navArgument("bucketName") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("highlightText") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getString("mediaId") ?: return@composable
                val bucketName = backStackEntry.arguments?.getString("bucketName")
                val highlightText = backStackEntry.arguments?.getString("highlightText")
                val useFullScreen by settingsViewModel.useFullScreen.collectAsState()
                DetailScreen(
                    mediaId = mediaId,
                    bucketName = bucketName,
                    highlightText = highlightText,
                    useFullScreenGlobal = useFullScreen,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onBack = { navController.popBackStack() },
                    viewModel = galleryViewModel
                )
            }
        }
    }
}
