package win.rainchan.win

import android.util.Log
import android.webkit.WebResourceResponse
import okhttp3.*
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

const val PATCH_SIZE = 50 * 1000

class MultiThreadDownload(
    val url: String,
    private val finishCallback: (rsp: WebResourceResponse, header: Headers?) -> Unit
) {
    companion object{
        val TAG = MultiThreadDownload.javaClass.name
    }
    private lateinit var buffer: ByteArray
    private lateinit var headers: Headers
    private var error = false
    private lateinit var remainPatches: AtomicInteger


    fun startDownload() {
        val length = getLength()
        Log.i(TAG,"启动多线程下载 $length")
        if (length == -1) {
            throw java.lang.IllegalArgumentException("不支持多线程下载")
        }
        buffer = ByteArray(length)
        val lastPatch = (length - 1) / PATCH_SIZE
        for (i in 0 until lastPatch) {
            downloadPatch(i * PATCH_SIZE , (i + 1) * PATCH_SIZE -1)
        }
        val remain = (length - 1) % PATCH_SIZE
        if (remain != 0) {
            remainPatches = AtomicInteger(lastPatch+1)
            downloadPatch(lastPatch * PATCH_SIZE, lastPatch * PATCH_SIZE + remain)
        } else {
            remainPatches = AtomicInteger(lastPatch - 1)
        }

    }


    fun getLength(): Int {
        val rsp = DanbooruApp.INSTANCE.okHttpClient.newCall(
            Request.Builder().url(url).head().addHeader(
                "user-agent",
                "Mozilla/5.0 (Linux; Android 7.1.1; OPPO R9sk) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.90 Mobile Safari/537.36 EdgA/42.0.2.3819"
            ).build()
        ).execute()
        return rsp.header("content-length", "-1")!!.toInt()
    }

    fun downloadPatch(rangeStart: Int, rangeEnd: Int) {

        val rsp = DanbooruApp.INSTANCE.okHttpClient.newCall(
            Request.Builder().url(url).get().addHeader(
                "user-agent",
                "Mozilla/5.0 (Linux; Android 7.1.1; OPPO R9sk) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.90 Mobile Safari/537.36 EdgA/42.0.2.3819"
            ).addHeader("range", "bytes=${rangeStart}-${rangeEnd}").build()
        ).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                error = true
                finishCallback(
                    WebResourceResponse(
                        "text/html",
                        "utf-8",
                        ByteArrayInputStream("加载失败,error:$e".toByteArray())
                    ), null
                )
                throw java.lang.IllegalArgumentException("无法下载 bytes=${rangeStart}-${rangeEnd}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    finishCallback(
                        WebResourceResponse(
                            "text/html",
                            "utf-8",
                            ByteArrayInputStream("加载失败,status code:${response.code}".toByteArray())
                        ), null
                    )
                }
                val content = response.body!!.bytes()
                response.close()
                synchronized(buffer) {
                    System.arraycopy(content, 0, buffer, rangeStart, content.size)
                    headers = response.headers
                    if (remainPatches.decrementAndGet() == 0 && !error) {
                        val mime =
                            "${response.body!!.contentType()!!.type}/${response.body!!.contentType()!!.subtype}"

                        finishCallback(
                            WebResourceResponse(
                                mime,
                                "utf-8",
                                200,
                                "ok",
                                response.headers.toMap(),
                                ByteArrayInputStream(buffer)
                            ), headers
                        )
                    }
                }
            }
        })

    }

}


