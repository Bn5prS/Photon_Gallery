package com.inferno.gallery

import android.app.Application
import android.os.Build.VERSION.SDK_INT
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import coil3.video.VideoFrameDecoder

class GalleryApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                // Support GIFs, Animated WebP, and Animated HEIF
                if (SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                // Support SVGs
                add(SvgDecoder.Factory())
                // Support Video Frame extraction
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true) // Premium smooth loading transition
            .build()
    }
}
