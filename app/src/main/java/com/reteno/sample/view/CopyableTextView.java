package com.reteno.sample.view;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;

import com.reteno.sample.R;

public class CopyableTextView extends AppCompatTextView {

    private static final int DRAWABLE_PADDING = 8;

    private final OnClickListener onClickListener = v -> {
        copyToClipboard(getText().toString());
    };

    public CopyableTextView(@NonNull Context context) {
        super(context);
        applyIcon(context);
    }

    public CopyableTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        applyIcon(context);
    }

    public CopyableTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        applyIcon(context);
    }

    private void applyIcon(Context context) {
        Drawable icon = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_content_copy, null);
        icon.setBounds(0, 0, 24, 24);
        setCompoundDrawables(null, null, icon, null);
        int drawablePadding = dpToPx(context, DRAWABLE_PADDING);
        setCompoundDrawablePadding(drawablePadding);
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

    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }
}
