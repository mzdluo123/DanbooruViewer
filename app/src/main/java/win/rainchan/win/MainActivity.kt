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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {
    lateinit var webView: WebView
    lateinit var process: ProgressBar
    lateinit var refresh: SwipeRefreshLayout

    private lateinit var preferences: SharedPreferences

    companion object {
        val TAG = "web"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        process = findViewById(R.id.progress)
        refresh = findViewById(R.id.refresh)
        preferences = this.getPreferences(Context.MODE_PRIVATE)
        initWebView()
        refresh.setOnRefreshListener {
            webView.reload()
            refresh.isRefreshing = false
        }
        if (postViewer) {
            webView.loadUrl(intent.data.toString())
        } else {
            webView.loadUrl(preferences.getString("now_url", "https://danbooru.donmai.us/")!!)
        }
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
        webView.setBackgroundColor(0);

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
                if (!postViewer) {  // 从其他页面打开的新窗口不覆盖浏览记录
                    preferences.edit().putString("now_url", url).apply()
                }

            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest
            ): Boolean {
                if (request.url.toString()
                        .startsWith("https://danbooru.donmai.us/posts/")
                ) { // 是post查看界面
                    val intent = Intent(baseContext, MainActivity::class.java)
                    intent.data = request.url
                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                                Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS
                    )
                    startActivity(intent)
                    return true
                }
                if (".donmai.us" !in request.url.toString()) {
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
        webView.setDownloadListener { url, _, _, _, _ ->
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
    }

    private val postViewer: Boolean
        get() = intent.data != null

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            if (postViewer) {
                finishAndRemoveTask()
            }
            finish()
        }
    }

    private fun bypass(request: WebResourceRequest): WebResourceResponse {
        var url = request.url.toString()

        url = url.replace("danbooru.donmai.us", "cdn.donmai.us")
        Log.d(TAG, "bypassed: $url")
        val builder = Request.Builder()
        builder.url(url)
        builder.addHeader("host", "danbooru.donmai.us")
        request.requestHeaders.asSequence().filter { it.key.lowercase() != "host" }
            .forEach {
                builder.addHeader(it.key, it.value)
            }
        try {
            val response = DanbooruApp.INSTANCE.okHttpClient.newCall(builder.build()).execute()
            if (response.isSuccessful) {
                val content = response.body!!.bytes()
                response.body!!.close()
                val mime =
                    "${response.body!!.contentType()!!.type}/${response.body!!.contentType()!!.subtype}"
                return WebResourceResponse(
                    mime,
                    "utf-8",
                    200,
                    "ok",
                    response.headers.toMap(),
                    ByteArrayInputStream(content)
                )
            }
            Log.e(TAG, "load error:${response.request.url}")
            // 我也不知道为什么有个405的请求 但是电脑浏览器没出现过

            if (response.code == 302) { // 手动处理重定向，让重定向后的地址也能绕过
                runOnUiThread {
                    webView.loadUrl(response.header("location", "https://danbooru.donmai.us")!!)
                }
                return WebResourceResponse(
                    "text/html",
                    "utf-8",
                    ByteArrayInputStream("跳转中".toByteArray())
                )

            }
            if (response.code != 405) {
                runOnUiThread {
                    Toast.makeText(this, "加载失败，状态码:${response.code}", Toast.LENGTH_SHORT).show()
                }
            }
            return WebResourceResponse(
                "text/html",
                "utf-8",
                200,
                "ok",
                response.headers.toMap(),
                ByteArrayInputStream(ByteArray(0))
            )

        } catch (e: IOException) {
            return WebResourceResponse(
                "text/html",
                "utf-8",
                ByteArrayInputStream("加载失败,error:$e".toByteArray())
            )
        }

    }

}