package zx.gl.texture

import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.view.Choreographer
import android.view.TextureView
import gles.EglCore
import gles.WindowSurface
import timber.log.Timber
import zx.gl.utils.Fps
import java.util.concurrent.SynchronousQueue

/**
 * 使用 SynchronousQueue，与 VSync 信号同步
 * Created by zx on 2017/9/23.
 */
class VSyncRenderThread(val renderer: GLSurfaceView.Renderer) : TextureView.SurfaceTextureListener, Thread(), Choreographer.FrameCallback {

    val sync = SynchronousQueue<Long>()

    override fun doFrame(frameTimeNanos: Long) {
        Choreographer.getInstance().postFrameCallback(this)
        if(!sync.offer(frameTimeNanos)){
            Timber.w("frame dropped at $frameTimeNanos")
        }
    }

    val lock = Object()
    var surfaceTexture: SurfaceTexture? = null
    var done = false

    var width = 0
    var height = 0

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        Timber.i("surface texture available. start choreographer")
        synchronized(lock) {
            surfaceTexture = surface
            this.width = width
            this.height = height
            lock.notify()
        }
        renderer.onSurfaceCreated(null, null)
        renderer.onSurfaceChanged(null, width, height)
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        Timber.i("surface texture size changed")
        this.width = width
        this.height = height
        renderer.onSurfaceChanged(null, width, height)

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        Timber.i("surface texture destroyed")
        Choreographer.getInstance().removeFrameCallback(this)

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

                sync.take()
                renderer.onDrawFrame(null)

                // calcFps
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