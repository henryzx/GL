package zx.gl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View

/**
 * Created by zx on 2017/9/17.
 */
fun viewToBitmap(textShadowView: View): Bitmap {
    textShadowView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
            .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
    textShadowView
            .layout(0, 0, textShadowView.measuredWidth, textShadowView.measuredHeight)
    val bitmap = Bitmap
            .createBitmap(textShadowView.measuredWidth, textShadowView.measuredHeight,
                    Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    textShadowView.draw(canvas)
    return bitmap
}