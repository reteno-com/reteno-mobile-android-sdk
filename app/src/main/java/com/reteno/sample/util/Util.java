package com.reteno.sample.util;

import android.widget.EditText;

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
}
