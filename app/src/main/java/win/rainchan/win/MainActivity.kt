package win.rainchan.win


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream


class MainActivity : AppCompatActivity() {
    lateinit var webView: WebView
    lateinit var process: ProgressBar
    private val okHttpClient: OkHttpClient =
        OkHttpClient()

    private lateinit var preferences: SharedPreferences

    companion object {
        val TAG = "web"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        process = findViewById(R.id.progress)
        preferences = this.getPreferences(Context.MODE_PRIVATE)
        initWebView()
        webView.loadUrl(preferences.getString("now_url", "https://danbooru.donmai.us/")!!)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                Log.d(TAG, request.url.toString())
                val url = request.url.toString()

                if ("danbooru.donmai.us" in url) {
                    return bypass(request)
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                preferences.edit().putString("now_url", url).apply()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest
            ): Boolean {
                if (".donmai.us" !in request.url.toString()){
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = request.url
                    startActivity(i)
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                process.progress = newProgress
                if (newProgress == 100) {
                    process.visibility = View.GONE
                } else {
                    process.visibility = View.VISIBLE
                }
            }
        }
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            finish()
        }
    }

    private fun bypass(request: WebResourceRequest): WebResourceResponse {
        var url = request.url.toString();

        url = url.replace("danbooru.donmai.us", "cdn.donmai.us")

        val builder = Request.Builder()
        builder.url(url)
        builder.addHeader("host", "danbooru.donmai.us")
        request.requestHeaders.asSequence().filter { it.key.lowercase() != "host" }
            .forEach {
                builder.addHeader(it.key, it.value)
            }
        val response = okHttpClient.newCall(builder.build()).execute()
        val content = response.body!!.string();
        val mime =
            "${response.body!!.contentType()!!.type}/${response.body!!.contentType()!!.subtype}"
        return WebResourceResponse(
            mime, "utf-8", ByteArrayInputStream(
                content.toByteArray()
            )
        )

    }

}