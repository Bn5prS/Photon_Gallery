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

    SharedTransitionLayout(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = "gallery"
        ) {

            composable("gallery") {
                MainAppLayout(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onPhotoClick = { mediaId, bucket ->
                        val route = if (bucket != null) "detail/$mediaId?bucket=${android.net.Uri.encode(bucket)}" else "detail/$mediaId"
                        navController.navigate(route)
                    }
                )
            }

            composable(
                route = "detail/{mediaId}?bucket={bucketName}",
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.StringType },
                    navArgument("bucketName") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getString("mediaId") ?: return@composable
                val bucketName = backStackEntry.arguments?.getString("bucketName")
                val useFullScreen by settingsViewModel.useFullScreen.collectAsState()
                DetailScreen(
                    mediaId = mediaId,
                    bucketName = bucketName,
                    useFullScreenGlobal = useFullScreen,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
