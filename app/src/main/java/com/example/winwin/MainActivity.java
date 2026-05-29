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

        // JavaScript & DOM Settings
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // File access support
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // Mixed Content rule
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Media autoplay bypass
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Standard Desktop Spoofing
        settings.setUserAgentString(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"
        );

        // Cookie management
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // WebView Routing & Absolute Antifreeze Engine
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Intercept any app store hijacking attempts from bad overlays
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
                injectThawEngine(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectThawEngine(view);
            }

            private void injectThawEngine(WebView view) {
                // Completely safe string structure for Java compiler
                String jsCode = 
                    "(function(){" +
                    "  function meltIcecaps() {" +
                    "    try {" +
                    "      var targets = document.querySelectorAll('div, section, dialog');" +
                    "      targets.forEach(function(el) {" +
                    "        if(!el) return;" +
                    "        var text = el.innerText || el.textContent || '';" +
                    "        if(text.indexOf('Keep playing in Sekai?') !== -1 || text.indexOf('Download the Sekai app') !== -1 || el.className.indexOf('DownloadApp') !== -1) {" +
                    "          if(el.parentNode && el !== document.body && el !== document.documentElement) {" +
                    "            el.remove();" +
                    "          }" +
                    "        }" +
                    "      });" +
                    "      " +
                    "      var forceStyles = function(element) {" +
                    "        if(!element) return;" +
                    "        if(element.style) {" +
                    "          element.style.setProperty('pointer-events', 'auto', 'important');" +
                    "          element.style.setProperty('overflow', 'auto', 'important');" +
                    "          element.style.setProperty('user-select', 'auto', 'important');" +
                    "        }" +
                    "      };" +
                    "      " +
                    "      forceStyles(document.body);" +
                    "      forceStyles(document.documentElement);" +
                    "      " +
                    "      document.querySelectorAll('*').forEach(function(node) {" +
                    "        if(node && node.shadowRoot) {" +
                    "          node.shadowRoot.querySelectorAll('div, section').forEach(function(sub){" +
                    "            var subText = sub.innerText || sub.textContent || '';" +
                    "            if(subText.indexOf('Keep playing in Sekai?') !== -1) sub.remove();" +
                    "          });" +
                    "        }" +
                    "      });" +
                    "    } catch(e) {}" +
                    "  }" +
                    "  " +
                    "  if(!window.isThawActive) {" +
                    "    window.isThawActive = true;" +
                    "    var css = document.createElement('style');" +
                    "    css.innerHTML = 'html, body, #root, #__next, main, [class*=\"layout\"] { pointer-events: auto !important; overflow: auto !important; user-select: auto !important; visibility: visible !important; }';" +
                    "    (document.head || document.documentElement).appendChild(css);" +
                    "    " +
                    "    var observer = new MutationObserver(meltIcecaps);" +
                    "    observer.observe(document.documentElement, {childList: true, subtree: true, attributes: true});" +
                    "    setInterval(meltIcecaps, 200);" +
                    "  }" +
                    "  meltIcecaps();" +
                    "})();true;";

                view.evaluateJavascript(jsCode, null);
            }
        });

        // File chooser support
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

        // Load targeted arena
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
            String backDefenseJS = 
                "(function(){" +
                "  document.body.style.setProperty('pointer-events', 'auto', 'important');" +
                "  document.body.style.setProperty('overflow', 'auto', 'important');" +
                "  return true;" +
                "})();";

            webView.evaluateJavascript(backDefenseJS, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        MainActivity.super.onBackPressed();
                    }
                }
            });
        } else {
            super.onBackPressed();
        }
    }
}

