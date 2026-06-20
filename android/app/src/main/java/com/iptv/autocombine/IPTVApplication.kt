package com.iptv.autocombine

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Application class for IPTV Auto Combine.
 *
 * Configures Coil image loader with memory and disk caching
 * for efficient channel logo loading.
 */
class IPTVApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
    }

    /**
     * Creates a custom Coil ImageLoader with:
     * - Memory cache: 25% of available memory
     * - Disk cache: 50MB for logo images
     * - Crossfade animations enabled
     * - Error/placeholder handling
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }
            .crossfade(true)
            .crossfade(300)
            .respectCacheHeaders(false)
            .build()
    }
}
