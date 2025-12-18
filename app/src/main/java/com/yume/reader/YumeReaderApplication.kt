package com.yume.reader

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class YumeReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("YumeReaderApp", "Приложение запущено")
    }
}