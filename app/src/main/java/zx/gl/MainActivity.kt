package zx.gl

import android.graphics.PixelFormat
import android.graphics.PointF
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater

class MainActivity : AppCompatActivity() {
    lateinit var surface: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_sample, null, false)
        val bitmap = viewToBitmap(bubbleView)
        val glCircle = GLCircle(this, PointF(0f,0f), bitmap, 0.5f){ }

        surface = findViewById(R.id.surfaceView) as GLSurfaceView

        surface.setEGLContextClientVersion(2)
        surface.setZOrderOnTop(true)
        surface.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        surface.holder.setFormat(PixelFormat.RGBA_8888)

        surface.setRenderer(glCircle)
        surface.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        surface.setOnTouchListener { v, event ->
            glCircle.setCenterByTouch(event.x, event.y);
//            glCircle.centerf.x = -(event.x - v.width / 2)/v.width * 2
//            glCircle.centerf.y = -(event.y - v.height / 2)/v.height * 2
            true
        }


        glCircle.start()

    }

    override fun onResume() {
        super.onResume()
        surface.onResume()
    }

    override fun onPause() {
        super.onPause()
        surface.onPause()
    }
}
