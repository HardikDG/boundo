package com.madness.collision.unit.themed_wallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Handler
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.madness.collision.R
import com.madness.collision.util.P
import com.madness.collision.util.ThemeUtil

class ThemedWallpaperService : WallpaperService(){

    private val wallpaperDrawable: Drawable
        get() = ThemedWallpaperEasyAccess.background!!
    private var frameRate: Float = 0.1f
        set(value) {
            frameGap = (1000f / value).toLong()
            field = value
        }
    private var frameGap: Long = 10000L
    private lateinit var prefSettings: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        prefSettings = getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        val keyApplyDarkPlan = resources.getString(R.string.prefExteriorKeyDarkPlan)
        val planValue = prefSettings.getString(keyApplyDarkPlan, resources.getString(R.string.prefExteriorDefaultDarkPlan)) ?: ""
        frameRate = if(planValue == resources.getString(R.string.prefExteriorDarkPlanValueSchedule)) 0.05f else 1f
        if (ThemedWallpaperEasyAccess.isDead) {
            ThemedWallpaperEasyAccess.wallpaperTimestamp = System.currentTimeMillis()
            ThemedWallpaperEasyAccess.isDead = false
        }
    }

    override fun onCreateEngine(): Engine {
        val shouldChange = ThemeUtil.shouldChangeTheme4ThemedWallpaper(applicationContext, prefSettings)
        // so as to trigger change in order to get initial state
        if (!shouldChange) {
            ThemedWallpaperEasyAccess.isDark = !ThemedWallpaperEasyAccess.isDark
            ThemeUtil.shouldChangeTheme4ThemedWallpaper(applicationContext, prefSettings)
        }
        return ThemedEngine()
    }

    inner class ThemedEngine: Engine(){
        private var wallpaperTimestamp = 0L
        private val handler = Handler()
        private val drawThread = Thread{
            val changed = wallpaperTimestamp != ThemedWallpaperEasyAccess.wallpaperTimestamp || updateWallpaperRes()
            wallpaperTimestamp = ThemedWallpaperEasyAccess.wallpaperTimestamp
            if (changed) {
                themedWallpaper.updateWallpaper(wallpaperDrawable)
                themedWallpaper.completeTranslate()
            }
            updateFrame(themedWallpaper.isTranslating || changed, true)
            // todo sleep?
//            try { Thread.sleep(frameGap) }catch (e: Exception){}
        }
        private var offsetRatio: Float = 0f
        private var offsetStepRatio: Float = 1f
        private val themedWallpaper = ThemedWallpaper(wallpaperDrawable).apply {
            setSize(surfaceHolder.surfaceFrame.width(), surfaceHolder.surfaceFrame.height())
        }

        override fun onDestroy() {
            super.onDestroy()
            cease()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) updateFrame(false)
            else cease()
        }

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            offsetRatio = xOffset
            offsetStepRatio = xOffsetStep
            updateFrame(true)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            themedWallpaper.setSize(width, height)
            updateFrame(true)
        }

        private fun updateFrame(change: Boolean, shouldTranslate: Boolean = false){
            if (change) {
                if (shouldTranslate && themedWallpaper.isTranslateCompleted) themedWallpaper.startTranslate()
                drawFrame()
            }
            cease()
            if (isVisible) handler.postDelayed(drawThread, if (themedWallpaper.isTranslating) ThemedWallpaper.FRAME_GAP else frameGap)
        }

        private fun drawFrame(){
            val holder: SurfaceHolder = surfaceHolder
            val canvas = holder.lockCanvas()
            if (themedWallpaper.isTranslating) {
                themedWallpaper.updateTranslateProgress()
                if (!themedWallpaper.isTranslateCompleted) themedWallpaper.makeMotion(this@ThemedWallpaperService)
                else themedWallpaper.destroyMotion()
            }
            themedWallpaper.translate(canvas, offsetRatio, offsetStepRatio)
            holder.unlockCanvasAndPost(canvas)
        }

        private fun cease(){
            handler.removeCallbacks(drawThread)
        }

        private fun updateWallpaperRes(): Boolean {
            // update theme
//        val shouldChange = ThemeUtil.shouldChangeTheme4ThemedWallpaper(applicationContext, prefSettings)
//        if (shouldChange){
//            val context = ContextThemeWrapper(applicationContext, themeId)
//            appTheme = context.theme
//            wallpaperDrawable = ThemedWallpaperEasyAccess.background?.constantState?.newDrawable()
//                    ?: ColorDrawable(if (ThemedWallpaperEasyAccess.isDark) Color.BLACK else Color.WHITE)
//        }
            return ThemeUtil.shouldChangeTheme4ThemedWallpaper(applicationContext, prefSettings)
        }
    }
}
