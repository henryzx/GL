package zx.gl.texture

import android.graphics.PointF
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.TextureView
import timber.log.Timber
import zx.gl.GLCircle
import zx.gl.R
import zx.gl.viewToBitmap
import kotlin.concurrent.thread

/**
 * Created by zx on 2017/9/23.
 */
class VSyncTextureActivity : AppCompatActivity() {
    lateinit var textureView: TextureView
    lateinit var renderThread: VSyncRenderThread

    lateinit var fpsThread: Thread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textureView = TextureView(this)
        textureView.isOpaque = false
        setContentView(textureView)

        val bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_sample, null, false)
        val bitmap = viewToBitmap(bubbleView)
        val glCircle = GLCircle(this, PointF(0f, 0f), bitmap, 0.5f) { }
        glCircle.start()

        renderThread = VSyncRenderThread(glCircle)
        renderThread.start()
        textureView.surfaceTextureListener = renderThread

        fpsThread = thread {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    Timber.i("fps: ${renderThread.fps}")
                    Thread.sleep(1000L)
                }
            } catch (e: Throwable) {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fpsThread.interrupt()
        renderThread.done()
    }
}