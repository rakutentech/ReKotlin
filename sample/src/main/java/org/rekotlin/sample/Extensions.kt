package org.rekotlin.sample

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.LayoutRes
import com.bumptech.glide.Glide

fun ViewGroup.attach(view: View) {
    this.addView(view)
}

fun ViewGroup.detach(view: View) {
    for (i in 0..this.childCount) {
        if (getChildAt(i) == view) {
            this.removeViewAt(i)
            break
        }
    }
}

fun ImageView.loadImageFromUrl(url: Uri) =
        Glide.with(this.context).load(url).into(this)

@Suppress("UNCHECKED_CAST")
fun <V : View> ViewGroup.inflate(@LayoutRes viewId: Int, attachToParent: Boolean = false): V {
    val inflater = LayoutInflater.from(this.context)
    return inflater.inflate(viewId, this, attachToParent) as V
}

fun String.asUri() = Uri.parse(this)
