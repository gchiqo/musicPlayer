package com.chiko.musicplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.chiko.musicplayer.image.SongArtFetcher
import com.chiko.musicplayer.image.SongKeyer
import com.chiko.musicplayer.youtube.YoutubeDownloader
import org.schabi.newpipe.extractor.NewPipe

class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        NewPipe.init(YoutubeDownloader())
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            add(SongKeyer())
            add(SongArtFetcher.Factory(applicationContext))
        }
        .memoryCache {
            MemoryCache.Builder(this).maxSizePercent(0.20).build()
        }
        .diskCache {
            DiskCache.Builder().directory(cacheDir.resolve("art_cache")).maxSizeBytes(64L * 1024 * 1024).build()
        }
        .crossfade(false)
        // Album art never needs an alpha channel — RGB_565 halves the bitmap
        // footprint, which keeps fast scrolling smooth on low-RAM phones.
        .allowRgb565(true)
        .build()
}
