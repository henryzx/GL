package zx.gl.utils

/**
 * Created by zx on 2017/9/24.
 */
class Fps{
    var fps: Long = 0
    var lastFpsTime = 0L
    var frameCount = 0L

    fun calcFps() {
        val now = System.currentTimeMillis()
        if ((now - lastFpsTime) > 1000L) {
            // timeup, save fps
            lastFpsTime = now
            fps = frameCount
            frameCount = 0L
        } else {
            frameCount++
        }
    }

    override fun toString(): String = fps.toString()
}