package com.qytech.twoscreen

import android.app.Application
import timber.log.Timber

/**
 * Created by Jax on 2020-01-09.
 * Description :
 * Version : V1.0.0
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}