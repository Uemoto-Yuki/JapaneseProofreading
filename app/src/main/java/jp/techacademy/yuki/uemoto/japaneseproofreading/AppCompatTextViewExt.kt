package jp.techacademy.yuki.uemoto.japaneseproofreading

import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP
import android.widget.TextView
import androidx.core.text.HtmlCompat

class ClickListenableLinkMovementMethod(
    private val onClick: ((url: String) -> Boolean)) :
    LinkMovementMethod() {
    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        val url = getUrl(widget, buffer, event)
        return when {
            event.action == ACTION_UP && url != null && onClick(url) -> true
            else -> super.onTouchEvent(widget, buffer, event)
        }
    }

    companion object {
        private fun getUrl(widget: TextView, buffer: Spannable, event: MotionEvent): String? {
            val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
            val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
            val off =
                widget.layout.run { getOffsetForHorizontal(getLineForVertical(y), x.toFloat()) }
            return (buffer.getSpans(off, off, ClickableSpan::class.java)
                .getOrNull(0) as? URLSpan)?.url
        }
    }
}

fun TextView.setLinkClickListenable(html: String, onLinkClick: (url: String) -> Boolean) {
        text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        movementMethod = ClickListenableLinkMovementMethod(onLinkClick)

}


