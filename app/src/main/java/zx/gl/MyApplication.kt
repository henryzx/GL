package zx.gl

import android.app.Application
import timber.log.Timber

/**
 * Created by zx on 2017/9/24.
 */
class MyApplication: Application(){
    override fun onCreate() {
        super.onCreate()
        if(BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}