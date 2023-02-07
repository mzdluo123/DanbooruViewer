package win.rainchan.win

import android.app.Application
import okhttp3.OkHttpClient

class DanbooruApp : Application() {
    lateinit var okHttpClient: OkHttpClient
        private set

    companion object {
        lateinit var INSTANCE: DanbooruApp
    }

    override fun onCreate() {
        okHttpClient = OkHttpClient.Builder().followRedirects(false).build()
        INSTANCE = this
        super.onCreate()
    }
}