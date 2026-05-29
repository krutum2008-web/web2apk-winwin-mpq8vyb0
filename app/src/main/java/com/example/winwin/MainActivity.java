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

        // Handle in-app navigation & Inject Anti-Freeze Blocker
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Block all app-store or download redirects trying to hijack the page
                if (url.contains("play.google.com") || url.contains("market://") || url.contains("sekai.chat/download")) {
                    return true; 
                }

                if (!url.contains("com") &&
                    (url.startsWith("intent:") || url.startsWith("market:"))) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception e) { /* ignore */ }
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                injectAntiFreezeBlocker(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectAntiFreezeBlocker(view);
            }

            private void injectAntiFreezeBlocker(WebView view) {
                String injectedJS =
                    "(function(){" +
                    "  function thawAndDestroy() {" +
                    "    try {" +
                    "      var targetSelectors = 'div[class*=\"DownloadApp\"], div[data-sentry-component*=\"DownloadApp\"]';" +
                    "      document.querySelectorAll(targetSelectors).forEach(function(el){" +
                    "        if(el) el.remove();" +
                    "      });" +
                    "      " +
                    "      var elements = document.querySelectorAll('*');" +
                    "      elements.forEach(function(el) {" +
                    "        if (!el) return;" +
                    "        " +
                    "        if (el.shadowRoot) {" +
                    "          el.shadowRoot.querySelectorAll('div, section').forEach(function(subEl){" +
                    "            var subTxt = subEl.innerText || subEl.textContent || '';" +
                    "            if (subTxt.indexOf('Keep playing in Sekai?') !== -1) {" +
                    "              subEl.remove();" +
                    "            }" +
                    "          });" +
                    "          " +
                    "          var subStyle = el.shadowRoot.getElementById('anti-freeze-style');" +
                    "          if (!subStyle) {" +
                    "            subStyle = document.createElement('style');" +
                    "            subStyle.id = 'anti-freeze-style';" +
                    "            subStyle.innerHTML = '* { pointer-events: auto !important; user-select: auto !important; }';" +
                    "            el.shadowRoot.appendChild(subStyle);" +
                    "          }" +
                    "        }" +
                    "        " +
                    "        var txt = el.innerText || el.textContent || '';" +
                    "        if (txt.indexOf('Keep playing in Sekai?') !== -1 || txt.indexOf('Download the Sekai app') !== -1) {" +
                    "          if (el.parentNode && el !== document.body && el !== document.documentElement) {" +
                    "            el.remove();" +
                    "          }" +
                    "        }" +
                    "      });" +
                    "      " +
                    "      var bodyStyle = window.getComputedStyle(document.body);" +
                    "      if (bodyStyle.pointerEvents === 'none' || bodyStyle.overflow === 'hidden') {" +
                    "        document.body.style.setProperty('pointer-events', 'auto', 'important');" +
                    "        document.body.style.setProperty('overflow', 'auto', 'important');" +
                    "      }" +
                    "      " +
                    "      var htmlStyle = window.getComputedStyle(document.documentElement);" +
                    "      if (htmlStyle.pointerEvents === 'none' || htmlStyle.overflow === 'hidden') {" +
                    "        document.documentElement.style.setProperty('pointer-events', 'auto', 'important');" +
                    "        document.documentElement.style.setProperty('overflow', 'auto', 'important');" +
                    "      }" +
                    "    } catch(e) {}" +
                    "  }" +
                    "  " +
                    "  if (!window.isAntiFreezeLoaded) {" +
                    "    window.isAntiFreezeLoaded = true;" +
                    "    " +
                    "    var globalStyle = document.createElement('style');" +
                    "    globalStyle.innerHTML = " +
                    "      'html, body, #root, #__next, main, [class*=\"layout\"], [class*=\"page\"] {' +
                    "      '  pointer-events: auto !important; ' +
                    "      '  overflow: auto !important; ' +
                    "      '  user-select: auto !important; ' +
                    "      '  visibility: visible !important;' +
                    "      '}';" +
                    "    (document.head || document.documentElement).appendChild(globalStyle);" +
                    "    " +
                    "    var obs = new MutationObserver(thawAndDestroy);" +
                    "    obs.observe(document.documentElement, {childList: true, subtree: true, attributes: true, attributeFilter: ['style', 'class']});" +
                    "    " +
                    "    window.addEventListener('input', thawAndDestroy, true);" +
                    "    window.addEventListener('keydown', thawAndDestroy, true);" +
                    "    setInterval(thawAndDestroy, 150);" + // เร่งความเร็วเต็มสูบสแกนทุก 0.15 วินาที
                    "  }" +
                    "  thawAndDestroy();" +
                    "})();true;";

                view.evaluateJavascript(injectedJS, null);
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

        // Load the target website after everything is properly configured
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
        if (webView != null) {
            String dismissJS = 
                "(function(){" +
                "  var popup = document.querySelector('div[class*=\"DownloadApp\"], div[data-sentry-component*=\"DownloadApp\"]');" +
                "  if (popup) {" +
                "    popup.remove();" +
                "    document.body.style.setProperty('pointer-events', 'auto', 'important');" +
                "    document.body.style.setProperty('overflow', 'auto', 'important');" +
                "    return true;" +
                "  }" +
                "  return false;" +
                "})();";

            webView.evaluateJavascript(dismissJS, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    if ("true".equals(value)) {
                        // Popup was destroyed and screen unfrozen, absorb the click
                    } else {
                        if (webView.canGoBack()) {
                            webView.goBack();
                        } else {
                            MainActivity.super.onBackPressed();
                        }
                    }
                }
            });
        } else {
            super.onBackPressed();
        }
    }
}

