package com.reteno.sample.view;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.sample.R;

public class RetenoView extends LinearLayout {

    private String hint;
    private String text;

    private TextView tvHint;
    private TextView tvText;

    private final OnClickListener onClickListener = v -> {
        copyToClipboard(getText());
    };

    public RetenoView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public RetenoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RetenoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.view_reteno, this);

        tvHint = findViewById(R.id.tvRetenoHint);
        tvText = findViewById(R.id.tvRetenoText);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.Reteno,
                0, 0);
        try {
            hint = a.getString(R.styleable.Reteno_retenoHint);
            text = a.getString(R.styleable.Reteno_retenoText);
        } finally {
            a.recycle();
        }

        tvHint.setText(hint);
        tvText.setText(text);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setOnClickListener(onClickListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setOnClickListener(null);
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Custom Data", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
        tvHint.setText(hint);
    }

    public void setTextOrHide(String text) {
        if (TextUtils.isEmpty(text)) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
        }
        this.text = text;
        tvText.setText(text);
    }

    public void setText(String text) {
        this.text = text;
        tvText.setText(text);
    }

    public String getText() {
        return text;
    }
}
