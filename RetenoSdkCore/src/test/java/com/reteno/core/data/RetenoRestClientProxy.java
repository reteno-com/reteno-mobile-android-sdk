package com.reteno.core.data;

import com.reteno.core.data.remote.api.RetenoRestClient;

import org.powermock.reflect.Whitebox;

public class RetenoRestClientProxy {

    static int TIMEOUT;
    static int READ_TIMEOUT;
    static String HEADER_DEBUG;
    static String HEADER_KEY;
    static String HEADER_VERSION;
    static String HEADER_CONTENT;
    static String HEADER_CONTENT_VALUE;
    static String HEADER_ENCODING;
    static String HEADER_CONTENT_ENCODING;
    static String HEADER_ENCODING_VALUE;
    static String HEADER_ACCEPT;
    static String HEADER_ACCEPT_VALUE;

    static {
        try {
            TIMEOUT = (int) getField("TIMEOUT");
            READ_TIMEOUT = (int) getField("READ_TIMEOUT");
            HEADER_DEBUG = (String) getField("HEADER_DEBUG");
            HEADER_KEY = (String) getField("HEADER_KEY");
            HEADER_VERSION = (String) getField("HEADER_VERSION");
            HEADER_CONTENT = (String) getField("HEADER_CONTENT");
            HEADER_CONTENT_VALUE = (String) getField("HEADER_CONTENT_VALUE");
            HEADER_ENCODING = (String) getField("HEADER_ENCODING");
            HEADER_CONTENT_ENCODING = (String) getField("HEADER_CONTENT_ENCODING");
            HEADER_ENCODING_VALUE = (String) getField("HEADER_ENCODING_VALUE");
            HEADER_ACCEPT = (String) getField("HEADER_ACCEPT");
            HEADER_ACCEPT_VALUE = (String) getField("HEADER_ACCEPT_VALUE");
        } catch (IllegalAccessException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    private static Object getField(String fieldName) throws IllegalAccessException {
        return Whitebox.getField(RetenoRestClient.class, fieldName).get(RetenoRestClient.class);
    }
}
