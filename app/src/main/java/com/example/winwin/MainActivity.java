package com.example.winwin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 1;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        // JavaScript & DOM
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // File access
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // Mixed content (HTTP inside HTTPS)
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Media
        settings.setMediaPlaybackRequiresUserGesture(false);

        // User-Agent: spoof desktop Chrome so sites don't show mobile-lite versions
        settings.setUserAgentString(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"
        );

        // Cookie persistence
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // Handle in-app navigation
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Open external links in browser, keep site in app
                if (!url.contains("com") &&
                    (url.startsWith("intent:") || url.startsWith("market:"))) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception e) { /* ignore */ }
                    return true;
                }
                return false;
            }
        });

        // File chooser & camera support
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> cb,
                                             FileChooserParams params) {
                filePathCallback = cb;
                Intent intent = params.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        // MutationObserver CSS class blocker + viewport inject
        String injectedJS =
            "(function(){" +
            "try{" +
            "var m=document.createElement('meta');" +
            "m.name='viewport';" +
            "m.content='width=1280,initial-scale=1.0,maximum-scale=1.0,user-scalable=no';" +
            "document.head.appendChild(m);" +
            "}catch(e){}" +
            "var SELECTORS='';" +
            "function hideBlocked(){" +
            "if(!SELECTORS)return;" +
            "try{document.querySelectorAll(SELECTORS).forEach(function(el){" +
            "if(el&&el.style)el.style.setProperty('display','none','important');" +
            "});}catch(e){}" +
            "}" +
            "var obs=new MutationObserver(hideBlocked);" +
            "obs.observe(document.documentElement,{childList:true,subtree:true});" +
            "hideBlocked();" +
            "})();true;";

        webView.evaluateJavascript(injectedJS, null);

        webView.loadUrl("https://link.prod.sekai.chat/wqryZu");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                results = new Uri[]{ data.getData() };
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
