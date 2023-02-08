package win.rainchan.win

import android.app.Application
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

class DanbooruApp : Application() {
    lateinit var okHttpClient: OkHttpClient
        private set

    companion object {
        lateinit var INSTANCE: DanbooruApp
    }

    override fun onCreate() {
        val dispatcher = Dispatcher()

        dispatcher.maxRequests = 64
        dispatcher.maxRequestsPerHost = 16
        okHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .retryOnConnectionFailure(true)
            .dispatcher(dispatcher)
            .build()
        INSTANCE = this
        super.onCreate()
    }
}