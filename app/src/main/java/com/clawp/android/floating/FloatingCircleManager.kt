package com.clawp.android.floating

import android.app.Application
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.clawp.android.R
import com.clawp.android.channel.Channel
import com.clawp.android.utils.KVUtils

/**
 * 圆形悬浮窗管理器
 * 简化版本，移除 EasyFloat 依赖，仅保留核心状态管理逻辑
 * 后续可根据需要重新实现悬浮窗 UI
 */
object FloatingCircleManager {

    private const val KEY_FLOAT_X = "floating_circle_x"
    private const val KEY_FLOAT_Y = "floating_circle_y"
    private const val AUTO_RESET_DELAY_MS = 5000L

    /**
     * 悬浮窗状态
     */
    enum class State {
        IDLE,           // 等待任务（默认）
        TASK_NOTIFY,    // 收到任务通知
        RUNNING,        // 任务执行中
        SUCCESS,        // 任务完成
        ERROR           // 任务失败
    }

    private var isShowing = false
    private var currentState: State = State.IDLE
    private var currentRound: Int = 0
    private var currentChannel: Channel? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoResetRunnable: Runnable? = null

    /**
     * 显示悬浮窗（占位实现）
     */
    fun show(
        application: Application,
        x: Int? = null,
        y: Int? = null
    ) {
        // TODO: 实现悬浮窗 UI
        isShowing = true
    }

    /**
     * 隐藏悬浮窗
     */
    fun hide() {
        isShowing = false
    }

    /**
     * 判断是否显示中
     */
    fun isShowing(): Boolean = isShowing

    /**
     * 切换到等待任务状态（默认）
     */
    fun setIdleState() {
        setState(State.IDLE)
    }

    /**
     * 显示任务通知
     */
    fun showTaskNotify(channel: Channel, taskText: String) {
        currentChannel = channel
        setState(State.TASK_NOTIFY)
    }

    /**
     * 切换到任务执行中状态
     */
    fun setRunningState(round: Int) {
        currentRound = round
        setState(State.RUNNING)
    }

    /**
     * 切换到任务成功状态
     */
    fun setSuccessState() {
        setState(State.SUCCESS)
        scheduleAutoReset()
    }

    /**
     * 切换到任务失败状态
     */
    fun setErrorState() {
        setState(State.ERROR)
        scheduleAutoReset()
    }

    private fun setState(state: State) {
        currentState = state
        // TODO: 更新 UI
    }

    private fun scheduleAutoReset() {
        cancelAutoReset()
        autoResetRunnable = Runnable {
            setIdleState()
        }
        mainHandler.postDelayed(autoResetRunnable!!, AUTO_RESET_DELAY_MS)
    }

    private fun cancelAutoReset() {
        autoResetRunnable?.let {
            mainHandler.removeCallbacks(it)
            autoResetRunnable = null
        }
    }

    /**
     * 保存位置
     */
    private fun savePosition(x: Int, y: Int) {
        KVUtils.putInt(KEY_FLOAT_X, x)
        KVUtils.putInt(KEY_FLOAT_Y, y)
    }

    /**
     * 获取保存的 X 坐标
     */
    private fun getSavedX(): Int? {
        val x = KVUtils.getInt(KEY_FLOAT_X, -1)
        return if (x == -1) null else x
    }

    /**
     * 获取保存的 Y 坐标
     */
    private fun getSavedY(): Int? {
        val y = KVUtils.getInt(KEY_FLOAT_Y, -1)
        return if (y == -1) null else y
    }

    /**
     * 点击回调，可以在外部设置
     */
    var onFloatClick: () -> Unit = {}
}
