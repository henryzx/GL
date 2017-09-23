package zx.gl.texture

import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.view.TextureView
import gles.EglCore
import gles.WindowSurface
import timber.log.Timber
import zx.gl.utils.Fps

/**
 * Created by zx on 2017/9/23.
 */
class RenderThread(val renderer: GLSurfaceView.Renderer) : TextureView.SurfaceTextureListener, Thread() {
    val lock = Object()
    var surfaceTexture: SurfaceTexture? = null
    var done = false

    var width = 0
    var height = 0

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        synchronized(lock) {
            surfaceTexture = surface
            this.width = width
            this.height = height
            lock.notify()
        }
        renderer.onSurfaceCreated(null, null)
        renderer.onSurfaceChanged(null, width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        this.width = width
        this.height = height
        renderer.onSurfaceChanged(null, width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        synchronized(lock) {
            surfaceTexture = null
        }
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
    }

    val fps = Fps()

    override fun run() {
        while (!done) {
            surfaceTexture = null

            Timber.i("waiting surfaceTexture")
            synchronized(lock) {
                while (!done && (surfaceTexture == null)) {
                    lock.wait()
                    if (done) break
                }
            }
            Timber.i("surfaceTexture is available")
            val eglCore = EglCore(null, EglCore.FLAG_TRY_GLES3)
            val windowSurface = WindowSurface(eglCore, surfaceTexture)
            windowSurface.makeCurrent()

            Timber.i("start render loop")

            while (!done && surfaceTexture != null) {

                renderer.onDrawFrame(null)

                windowSurface.swapBuffers()

                fps.calcFps()
            }

            Timber.i("exit render loop")

            windowSurface.release()
            eglCore.release()

            surfaceTexture?.release()
        }

        Timber.i("render thread exit")
    }

    fun done() {
        synchronized(lock) {
            done = true
            lock.notify()
        }
    }

}