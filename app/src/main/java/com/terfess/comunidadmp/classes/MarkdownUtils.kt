package com.terfess.comunidadmp.classes

import android.content.Context
import android.widget.TextView
import io.noties.markwon.Markwon
import io.noties.markwon.linkify.LinkifyPlugin

object MarkdownUtils {

    @Volatile
    private var markwonInstance: Markwon? = null

    fun getMarkwon(context: Context): Markwon {
        return markwonInstance ?: synchronized(this) {
            markwonInstance ?: Markwon.builder(context.applicationContext)
                .usePlugin(LinkifyPlugin.create())
                .build().also { markwonInstance = it }
        }
    }

    /**
     * Parsea y renderiza texto con sintaxis Markdown y URLs clickeables en un TextView.
     */
    fun renderMarkdown(textView: TextView, markdown: String) {
        if (markdown.isBlank()) {
            textView.text = ""
            return
        }
        val markwon = getMarkwon(textView.context)
        markwon.setMarkdown(textView, markdown)
    }
}
