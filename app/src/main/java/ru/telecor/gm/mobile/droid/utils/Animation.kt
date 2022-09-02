package ru.telecor.gm.mobile.droid.utils

import android.view.View
import android.view.animation.TranslateAnimation
import android.content.Context
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import kotlinx.android.synthetic.main.fragment_garbage_load.*
import ru.telecor.gm.mobile.droid.R

object Animation {
    // slide the view from below itself to the current position
    fun slideUp(view: View) {
        view.visibility = View.VISIBLE
        val animate = TranslateAnimation(
            0f,  // fromXDelta
            0f,  // toXDelta
            view.height.toFloat(),  // fromYDelta
            0f
        ) // toYDelta
        animate.duration = 500
        animate.fillAfter = true
        view.startAnimation(animate)
    }

    // slide the view from its current position to below itself
    fun slideDown(view: View) {
        val animate = TranslateAnimation(
            0f,  // fromXDelta
            0f,  // toXDelta
            0f,  // fromYDelta
            view.height.toFloat()
        ) // toYDelta
        animate.duration = 500
        animate.fillAfter = true
        view.startAnimation(animate)
    }

    fun slideUp(view: View, boolean: Boolean , context: Context){
        val bottomUp: Animation
        if (boolean){
            bottomUp = AnimationUtils.loadAnimation(
                context, R.anim.bottom_up)
            view.startAnimation(bottomUp)
            view.visibility = View.VISIBLE
        }else{
            bottomUp = AnimationUtils.loadAnimation(
                context, R.anim.bottom_down)
            view.startAnimation(bottomUp)
            view.visibility = View.GONE
        }
    }
}