package com.reteno.sample.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.reteno.sample.R

class RetenoView : LinearLayout {
    private var hint: String? = null
    private var text: String? = null
    private var tvHint: TextView? = null
    private var tvText: TextView? = null
    private val onClickListener = OnClickListener { v: View? -> copyToClipboard(getText()) }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        inflate(context, R.layout.view_reteno, this)
        tvHint = findViewById(R.id.tvRetenoHint)
        tvText = findViewById(R.id.tvRetenoText)
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.Reteno,
            0, 0
        )
        try {
            hint = a.getString(R.styleable.Reteno_retenoHint)
            text = a.getString(R.styleable.Reteno_retenoText)
        } finally {
            a.recycle()
        }
        tvHint?.text = hint
        tvText?.text = text
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setOnClickListener(onClickListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        setOnClickListener(null)
    }

    private fun copyToClipboard(text: String?) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Custom Data", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun getHint(): String? {
        return hint
    }

    fun setHint(hint: String?) {
        this.hint = hint
        tvHint!!.text = hint
    }

    fun setTextOrHide(text: String?) {
        visibility = if (TextUtils.isEmpty(text)) {
            GONE
        } else {
            VISIBLE
        }
        this.text = text
        tvText!!.text = text
    }

    fun setText(text: String?) {
        this.text = text
        tvText!!.text = text
    }

    private fun getText(): String? {
        return text
    }
}
