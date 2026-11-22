package com.ecoceipt.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class EcoceiptApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.initializeApp(this) != null) {
            if (BuildConfig.DEBUG) {
                val klass = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                val provider = klass.getMethod("getInstance").invoke(null) as com.google.firebase.appcheck.AppCheckProviderFactory
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(provider)
            } else {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
            }
        }
    }
    override fun newImageLoader() = ImageLoader.Builder(this).components { add(SvgDecoder.Factory()) }.build()
}
