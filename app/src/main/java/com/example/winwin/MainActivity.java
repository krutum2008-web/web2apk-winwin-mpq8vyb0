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

        // Handle in-app navigation & Concrete CSS Overrider
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
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
                injectConcreteBlocker(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectConcreteBlocker(view);
            }

            private void injectConcreteBlocker(WebView view) {
                String targetSelectors = 
                    ".DownloadAppPopup, " +
                    "div[data-sentry-component='DownloadAppPopup'], " +
                    ".DownloadAppTopBar, " +
                    "div[data-sentry-component='DownloadAppTopBar'], " +
                    "div[class*='bg-black'], " +
                    "div[class*='Fixed'], " +
                    "div[class*='fixed']";

                String injectedJS =
                    "(function(){" +
                    "  var SELECTORS = '" + targetSelectors + "';" +
                    "  " +
                    "  function concreteClean() {" +
                    "    try {" +
                    "      var id = 'ublock-concrete-layer';" +
                    "      var style = document.getElementById(id);" +
                    "      if(!style) {" +
                    "        style = document.createElement('style');" +
                    "        style.id = id;" +
                    "        style.innerHTML = " +
                    "          SELECTORS + ' { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; z-index: -9999 !important; } ' + " +
                    "          'body { overflow: auto !important; pointer-events: auto !important; } ' + " +
                    "          'html { overflow: auto !important; }';" +
                    "        (document.head || document.documentElement).appendChild(style);" +
                    "      }" +
                    "      " +
                    "      var elements = document.querySelectorAll('*');" +
                    "      elements.forEach(function(el) {" +
                    "        if (el && el.shadowRoot) {" +
                    "          var subStyle = el.shadowRoot.getElementById('sub-concrete-layer');" +
                    "          if (!subStyle) {" +
                    "            subStyle = document.createElement('style');" +
                    "            subStyle.id = 'sub-concrete-layer';" +
                    "            subStyle.innerHTML = SELECTORS + ' { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; z-index: -9999 !important; }';" +
                    "            el.shadowRoot.appendChild(subStyle);" +
                    "          }" +
                    "        }" +
                    "        var txt = el.innerText || el.textContent || '';" +
                    "        if (txt.indexOf('Keep playing in Sekai?') !== -1 || txt.indexOf('Download the Sekai app') !== -1) {" +
                    "          el.style.setProperty('display', 'none', 'important');" +
                    "          el.style.setProperty('z-index', '-9999', 'important');" +
                    "          if (el.parentNode) el.remove();" +
                    "        }" +
                    "      });" +
                    "    } catch(e) {}" +
                    "  }" +
                    "  " +
                    "  try {" +
                    "    var m = document.createElement('meta');" +
                    "    m.name = 'viewport';" +
                    "    m.content = 'width=1280,initial-scale=1.0,maximum-scale=1.0,user-scalable=no';" +
                    "    (document.head || document.documentElement).appendChild(m);" +
                    "  } catch(e) {}" +
                    "  " +
                    "  if (!window.isConcreteActive) {" +
                    "    window.isConcreteActive = true;" +
                    "    var obs = new MutationObserver(concreteClean);" +
                    "    obs.observe(document.documentElement, {childList: true, subtree: true, attributes: true});" +
                    "    window.addEventListener('input', concreteClean, true);" +
                    "    window.addEventListener('keydown', concreteClean, true);" +
                    "    setInterval(concreteClean, 200);" +
                    "  }" +
                    "  concreteClean();" +
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
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

