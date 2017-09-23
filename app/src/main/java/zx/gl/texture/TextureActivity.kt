package zx.gl.texture

import android.graphics.PointF
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.TextureView
import android.widget.FrameLayout
import android.widget.TextView
import timber.log.Timber
import zx.gl.GLCircle
import zx.gl.R
import zx.gl.viewToBitmap
import kotlin.concurrent.thread

/**
 * Created by zx on 2017/9/23.
 */
class TextureActivity : AppCompatActivity() {
    lateinit var textureView: TextureView
    lateinit var textView: TextView
    lateinit var renderThread: RenderThread
    lateinit var fpsThread: Thread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frame = FrameLayout(this)
        textureView = TextureView(this)
        textureView.isOpaque = false
        textView = TextView(this)
        frame.addView(textView)
        frame.addView(textureView)
        setContentView(frame)

        val bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_sample, null, false)
        val bitmap = viewToBitmap(bubbleView)
        val glCircle = GLCircle(this, PointF(0f, 0f), bitmap, 0.5f) { }
        glCircle.start()

        renderThread = RenderThread(glCircle)
        renderThread.start()
        textureView.surfaceTextureListener = renderThread

        fpsThread = thread {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    //Timber.i("fps: ${renderThread.fps}")
                    runOnUiThread { textView.text = "fps ${renderThread.fps}" }
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