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

        // Handle in-app navigation & Counter-Attack Blockers
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Hijack play store or market redirection attempts from the popup
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
                injectCounterBlocker(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectCounterBlocker(view);
            }

            private void injectCounterBlocker(WebView view) {
                String injectedJS =
                    "(function(){" +
                    "  function hijackAndAddCloseButton() {" +
                    "    try {" +
                    "      var allElements = document.querySelectorAll('div, section, dialog');" +
                    "      allElements.forEach(function(el) {" +
                    "        if (!el) return;" +
                    "        var text = el.innerText || el.textContent || '';" +
                    "        var isPopup = text.indexOf('Keep playing in Sekai?') !== -1 || text.indexOf('Download the Sekai app') !== -1;" +
                    "        " +
                    "        if (isPopup) {" +
                    "          var style = window.getComputedStyle(el);" +
                    "          if (style.position === 'fixed' || style.position === 'absolute' || parseInt(style.zIndex, 10) > 5) {" +
                    "            " +
                    "            el.setAttribute('data-target-popup', 'true');" +
                    "            " +
                    "            var downloadButtons = el.querySelectorAll('button, a, div');" +
                    "            downloadButtons.forEach(function(btn) {" +
                    "              if(btn.innerText && btn.innerText.indexOf('Download') !== -1) {" +
                    "                if(!btn.hasHijacked) {" +
                    "                  btn.hasHijacked = true;" +
                    "                  btn.style.setProperty('background-color', '#4CAF50', 'important');" +
                    "                  btn.addEventListener('click', function(e) {" +
                    "                    e.preventDefault();" +
                    "                    e.stopPropagation();" +
                    "                    el.remove();" +
                    "                  }, true);" +
                    "                }" +
                    "              }" +
                    "            });" +
                    "            " +
                    "            if (!el.querySelector('.custom-close-btn')) {" +
                    "              var closeBtn = document.createElement('div');" +
                    "              closeBtn.className = 'custom-close-btn';" +
                    "              closeBtn.innerText = 'X';" +
                    "              closeBtn.style.cssText = 'position:absolute;top:10px;right:10px;width:30px;height:30px;background:red;color:white;font-weight:bold;text-align:center;line-height:30px;border-radius:50%;cursor:pointer;z-index:99999;font-size:16px;';" +
                    "              closeBtn.addEventListener('click', function(e) {" +
                    "                e.preventDefault();" +
                    "                e.stopPropagation();" +
                    "                el.remove();" +
                    "              }, true);" +
                    "              el.appendChild(closeBtn);" +
                    "            }" +
                    "          }" +
                    "        }" +
                    "      });" +
                    "    } catch(e) {}" +
                    "  }" +
                    "  " +
                    "  if (!window.isCounterActive) {" +
                    "    window.isCounterActive = true;" +
                    "    var obs = new MutationObserver(hijackAndAddCloseButton);" +
                    "    obs.observe(document.documentElement, {childList: true, subtree: true, attributes: true});" +
                    "    window.addEventListener('input', hijackAndAddCloseButton, true);" +
                    "    window.addEventListener('keydown', hijackAndAddCloseButton, true);" +
                    "    setInterval(hijackAndAddCloseButton, 300);" +
                    "  }" +
                    "  hijackAndAddCloseButton();" +
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
        // Ultimate Back Button Defense
        // If the popup exists on screen, dismiss it first instead of leaving the page
        if (webView != null) {
            String dismissJS = 
                "(function(){" +
                "  var popup = document.querySelector('[data-target-popup=\"true\"]') || " +
                "              document.querySelector('.DownloadAppPopup') || " +
                "              document.querySelector('div[data-sentry-component=\"DownloadAppPopup\"]');" +
                "  if (popup) {" +
                "    popup.remove();" +
                "    return true;" +
                "  }" +
                "  return false;" +
                "})();";

            webView.evaluateJavascript(dismissJS, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    if ("true".equals(value)) {
                        // Popup was found and destroyed, do nothing else (consumed the back press)
                    } else {
                        // No popup found, proceed with normal web back navigation
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
