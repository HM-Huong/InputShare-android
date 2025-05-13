package com.bhznjns.inputsharereporter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.bhznjns.inputsharereporter.utils.Direction

typealias TriggeredCallback = () -> Unit

class SideLineOverlay : View {
    private lateinit var triggerCallback: TriggeredCallback
    private lateinit var params: WindowManager.LayoutParams

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setIsDebug(isDebug: Boolean): SideLineOverlay {
        if (isDebug) this.setBackgroundColor(Color.RED)
        return this
    }

    fun setDirection(direction: String?): SideLineOverlay {
        val direction = parseDirection(direction)
        setParamWithDirection(direction)
        return this
    }

    fun setTriggeredCallback(callback: TriggeredCallback): SideLineOverlay {
        triggerCallback = callback
        return this
    }

    fun launch() {
        val windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(this, this.params)
    }

    fun close() {
        if (!isAttachedToWindow) return
        val windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        try {
            windowManager.removeView(this)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun setParamWithDirection(direction: Direction) {
        val edgeThickness = (EDGE_THICKNESS_DP * resources.displayMetrics.density).toInt()
            .coerceAtLeast(1)
        params = when (direction) {
            Direction.UP, Direction.DOWN -> WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                edgeThickness,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )
            Direction.LEFT, Direction.RIGHT -> WindowManager.LayoutParams(
                edgeThickness,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )
        }
        params.gravity = when (direction) {
            Direction.LEFT  -> Gravity.LEFT
            Direction.RIGHT -> Gravity.RIGHT
            Direction.UP    -> Gravity.TOP
            Direction.DOWN  -> Gravity.BOTTOM
        }

        // Accessibility overlays are inset below system bars on recent Android/One UI
        // versions unless they explicitly opt out. The edge detector must stay on the
        // physical display edge so a mouse can reliably enter it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.setFitInsetsTypes(0)
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun parseDirection(direction: String?): Direction {
        Log.d("SideLineOverlay", "Received direction: $direction")
        return when (direction) {
            "up"    -> Direction.UP
            "right" -> Direction.RIGHT
            "left"  -> Direction.LEFT
            "down"  -> Direction.DOWN
            else    -> Direction.LEFT
        }
    }

    private var triggered = false
    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_HOVER_ENTER) {
            Log.d("SideLineOverlay", "Mouse entered edge overlay")
            if (!triggered) {
                triggerCallback()
            }
            triggered = true
            return true
        } else if (event?.action == MotionEvent.ACTION_HOVER_EXIT) {
            Log.d("SideLineOverlay", "Mouse exited edge overlay")
            triggered = false
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    companion object {
        private const val EDGE_THICKNESS_DP = 4
    }
}
