package com.reteno.sample.util;

import android.util.Log;
import android.widget.EditText;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

public class Util {

    public static String getTextOrNull(EditText editText) {
        String rawText = editText.getText().toString().trim();
        if (rawText.isEmpty()) return null;
        return rawText;
    }

    public static List<String> getListFromEditText(EditText editText) {
        String rawText = editText.getText().toString().trim();
        if (rawText.isEmpty()) return null;
        return Arrays.asList(rawText.split(","));
    }

    public static Integer saveParseInt(@Nullable String text) {
        Integer result = null;
        if (text == null || text.isEmpty()) return result;
        try {
            result = Integer.parseInt(text);
        } catch (Exception e) {
            Log.d("saveParseInt", e.getMessage());
        }
        return result;
    }
}
