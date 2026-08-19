package com.inferno.gallery.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
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

import com.inferno.gallery.ui.theme.MotionTokens

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
@OptIn(ExperimentalSharedTransitionApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NavigationGraph(
    isLoading: Boolean,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    
    val startDest = "gallery"

    if (isLoading) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }

    val galleryViewModel: GalleryViewModel = viewModel()

    SharedTransitionLayout(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = startDest,
            // ── Forward navigation: slide in from right ──
            enterTransition = {
                slideInHorizontally(
                    animationSpec = tween(MotionTokens.Durations.Long, easing = MotionTokens.EmphasizedEasing),
                    initialOffsetX = { (it * 0.25f).toInt() }
                ) + fadeIn(
                    animationSpec = tween(MotionTokens.Durations.Medium, MotionTokens.Durations.Short, easing = MotionTokens.EmphasizedDecelerateEasing)
                ) + scaleIn(
                    animationSpec = tween(MotionTokens.Durations.Long, easing = MotionTokens.EmphasizedAccelerateEasing),
                    initialScale = 0.94f
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(MotionTokens.Durations.Long, easing = MotionTokens.EmphasizedEasing),
                    targetOffsetX = { -(it * 0.12f).toInt() }
                ) + fadeOut(
                    animationSpec = tween(MotionTokens.Durations.Short, easing = MotionTokens.EmphasizedDecelerateEasing)
                ) + scaleOut(
                    animationSpec = tween(MotionTokens.Durations.Long, easing = MotionTokens.EmphasizedAccelerateEasing),
                    targetScale = 0.94f
                )
            },
            // ── Back navigation: slide in from left ──
            popEnterTransition = {
                slideInHorizontally(
                    animationSpec = tween(MotionTokens.Durations.Long, easing = MotionTokens.EmphasizedEasing),
                    initialOffsetX = { -(it * 0.12f).toInt() }
                ) + fadeIn(
                    animationSpec = tween(MotionTokens.Durations.Medium, MotionTokens.Durations.Short, easing = MotionTokens.EmphasizedDecelerateEasing)
                ) + scaleIn(
                    animationSpec = tween(MotionTokens.Durations.Long, easing = MotionTokens.EmphasizedAccelerateEasing),
                    initialScale = 0.94f
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(MotionTokens.Durations.Long, easing = MotionTokens.EmphasizedEasing),
                    targetOffsetX = { (it * 0.25f).toInt() }
                ) + fadeOut(
                    animationSpec = tween(MotionTokens.Durations.Short, easing = MotionTokens.EmphasizedDecelerateEasing)
                ) + scaleOut(
                    animationSpec = tween(MotionTokens.Durations.Long, easing = MotionTokens.EmphasizedAccelerateEasing),
                    targetScale = 0.94f
                )
            }
        ) {

            composable(
                route = "gallery",
                enterTransition = { fadeIn(tween(MotionTokens.Durations.Short)) },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(MotionTokens.Durations.Short)) },
                popExitTransition = { fadeOut(tween(150)) }
            ) {
                MainAppLayout(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onPhotoClick = { mediaId, bucket, query ->
                        var route = "detail/$mediaId"
                        if (bucket != null) route += "?bucket=${android.net.Uri.encode(bucket)}"
                        if (query != null && bucket == "search_text") {
                            route += "&highlight=${android.net.Uri.encode(query)}"
                        }
                        navController.navigate(route)
                    },
                    onCreateCollage = { uriStrings ->
                        val joined = uriStrings.joinToString("|")
                        val encoded = android.util.Base64.encodeToString(
                            joined.toByteArray(Charsets.UTF_8),
                            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                        )
                        navController.navigate("collage?uris=$encoded")
                    },
                    onCreateStitch = { uriStrings ->
                        val joined = uriStrings.joinToString("|")
                        val encoded = android.util.Base64.encodeToString(
                            joined.toByteArray(Charsets.UTF_8),
                            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                        )
                        navController.navigate("stitch?uris=$encoded")
                    },
                    onNavigateToVault = { navController.navigate("vault") },
                    viewModel = galleryViewModel
                )
            }

            composable(
                route = "detail/{mediaId}?bucket={bucketName}&highlight={highlightText}",
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.StringType },
                    navArgument("bucketName") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("highlightText") { type = NavType.StringType; nullable = true; defaultValue = null }
                ),
                enterTransition = { fadeIn(tween(MotionTokens.Durations.Short)) },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(MotionTokens.Durations.Short)) },
                popExitTransition = { fadeOut(tween(150)) }
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getString("mediaId") ?: return@composable
                val bucketName = backStackEntry.arguments?.getString("bucketName")
                val highlightText = backStackEntry.arguments?.getString("highlightText")
                
                androidx.compose.runtime.LaunchedEffect(mediaId, bucketName) {
                    galleryViewModel.loadDetailMedia(mediaId, bucketName)
                }
                
                val useFullScreen by settingsViewModel.useFullScreen.collectAsState()
                DetailScreen(
                    mediaId = mediaId,
                    bucketName = bucketName,
                    highlightText = highlightText,
                    useFullScreenGlobal = useFullScreen,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onBack = { navController.popBackStack() },
                    onNavigateToEditor = { editUri ->
                        val encoded = android.net.Uri.encode(editUri.toString())
                        navController.navigate("editor?uri=$encoded")
                    },
                    viewModel = galleryViewModel
                )
            }

            composable(
                route = "collage?uris={uris}",
                arguments = listOf(
                    navArgument("uris") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString("uris") ?: ""
                val joined = try {
                    String(
                        android.util.Base64.decode(encoded, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP),
                        Charsets.UTF_8
                    )
                } catch (e: Exception) { "" }
                val uris = joined.split("|").filter { it.isNotBlank() }
                CollageScreen(
                    initialUris = uris,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "stitch?uris={uris}",
                arguments = listOf(
                    navArgument("uris") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString("uris") ?: ""
                val joined = try {
                    String(
                        android.util.Base64.decode(encoded, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP),
                        Charsets.UTF_8
                    )
                } catch (e: Exception) { "" }
                val uris = joined.split("|").filter { it.isNotBlank() }
                StitchScreen(
                    initialUris = uris,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "editor?uri={uri}",
                arguments = listOf(
                    navArgument("uri") { type = NavType.StringType }
                ),
                enterTransition = {
                    slideInVertically(
                        animationSpec = tween(MotionTokens.Durations.Long, easing = MotionTokens.EmphasizedEasing),
                        initialOffsetY = { (it * 0.3f).toInt() }
                    ) + fadeIn(tween(200))
                },
                exitTransition = {
                    slideOutVertically(
                        animationSpec = tween(MotionTokens.Durations.Medium, easing = MotionTokens.EmphasizedEasing),
                        targetOffsetY = { (it * 0.3f).toInt() }
                    ) + fadeOut(tween(150))
                },
                popEnterTransition = { fadeIn(tween(200)) },
                popExitTransition = {
                    slideOutVertically(
                        animationSpec = tween(MotionTokens.Durations.Medium, easing = MotionTokens.EmphasizedEasing),
                        targetOffsetY = { (it * 0.3f).toInt() }
                    ) + fadeOut(tween(150))
                }
            ) { backStackEntry ->
                val uriString = backStackEntry.arguments?.getString("uri") ?: ""
                val uri = android.net.Uri.parse(android.net.Uri.decode(uriString))
                ImageEditorScreen(
                    imageUri = uri,
                    onBack = { navController.popBackStack() },
                    onSaved = { }
                )
            }

            composable("vault") {
                com.inferno.gallery.ui.vault.PrivateSpaceScreen(
                    viewModel = galleryViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

        }
    }
}
