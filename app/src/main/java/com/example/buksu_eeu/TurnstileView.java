package com.example.buksu_eeu;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class TurnstileView extends WebView {

    private OnTokenResolvedListener onTokenResolvedListener;
    private OnFailureListener onFailureListener;
    private String siteKey;

    public interface OnTokenResolvedListener {
        void onTokenResolved(String token);
    }

    public interface OnFailureListener {
        void onFailure(Exception e);
    }

    public TurnstileView(Context context) {
        super(context);
        init();
    }

    public TurnstileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        addJavascriptInterface(new TurnstileInterface(), "Android");
        
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });
    }

    public void setSiteKey(String siteKey) {
        this.siteKey = siteKey;
        loadDataWithBaseURL("https://buksu-eeu.firebaseapp.com", getHtml(), "text/html", "UTF-8", null);
    }

    private String getHtml() {
        return "<html>\n" +
                "  <head>\n" +
                "    <script src=\"https://challenges.cloudflare.com/turnstile/v0/api.js\" async defer></script>\n" +
                "  </head>\n" +
                "  <body style=\"margin:0;padding:0;display:flex;justify-content:center;\">\n" +
                "    <div class=\"cf-turnstile\" data-sitekey=\"" + siteKey + "\" data-callback=\"onSuccess\" data-error-callback=\"onError\"></div>\n" +
                "    <script>\n" +
                "      function onSuccess(token) {\n" +
                "        Android.onSuccess(token);\n" +
                "      }\n" +
                "      function onError() {\n" +
                "        Android.onError();\n" +
                "      }\n" +
                "    </script>\n" +
                "  </body>\n" +
                "</html>";
    }

    public void reset() {
        loadDataWithBaseURL("https://buksu-eeu.firebaseapp.com", getHtml(), "text/html", "UTF-8", null);
    }

    public void setOnTokenResolvedListener(OnTokenResolvedListener listener) {
        this.onTokenResolvedListener = listener;
    }

    public void setOnFailureListener(OnFailureListener listener) {
        this.onFailureListener = listener;
    }

    private class TurnstileInterface {
        @JavascriptInterface
        public void onSuccess(String token) {
            if (onTokenResolvedListener != null) {
                post(() -> onTokenResolvedListener.onTokenResolved(token));
            }
        }

        @JavascriptInterface
        public void onError() {
            if (onFailureListener != null) {
                post(() -> onFailureListener.onFailure(new Exception("Turnstile verification failed")));
            }
        }
    }
}