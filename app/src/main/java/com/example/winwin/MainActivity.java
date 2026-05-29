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

        // Handle in-app navigation & Deep Injected Overrider
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
                injectUltimateBlocker(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectUltimateBlocker(view);
            }

            private void injectUltimateBlocker(WebView view) {
                String targetSelectors = 
                    ".DownloadAppPopup, " +
                    "div[data-sentry-component='DownloadAppPopup'], " +
                    ".DownloadAppTopBar, " +
                    "div[data-sentry-component='DownloadAppTopBar'], " +
                    "div[class*='bg-black']";

                String injectedJS =
                    "(function(){" +
                    "  var SELECTORS = '" + targetSelectors + "';" +
                    "  " +
                    "  function nukeTargetElements() {" +
                    "    try {" +
                    "      if (SELECTORS) {" +
                    "        document.querySelectorAll(SELECTORS).forEach(function(el){" +
                    "          if(el) el.remove();" +
                    "        });" +
                    "      }" +
                    "      var allDivs = document.querySelectorAll('div, section, dialog');" +
                    "      allDivs.forEach(HardScanElement);" +
                    "    } catch(e) {}" +
                    "  }" +
                    "  " +
                    "  function HardScanElement(el) {" +
                    "    try {" +
                    "      if (!el || el === document.body || el === document.documentElement) return;" +
                    "      var style = window.getComputedStyle(el);" +
                    "      var isFixedOrAbsolute = style.position === 'fixed' || style.position === 'absolute';" +
                    "      if (!isFixedOrAbsolute) return;" +
                    "      " +
                    "      var text = el.innerText || el.textContent || '';" +
                    "      var hasTriggerWords = text.indexOf('Keep playing in Sekai?') !== -1 || " +
                    "                            text.indexOf('Download the Sekai app') !== -1 || " +
                    "                            text.indexOf('Download') !== -1;" +
                    "      " +
                    "      var zIndex = parseInt(style.zIndex, 10) || 0;" +
                    "      if (hasTriggerWords && (zIndex > 10 || style.position === 'fixed')) {" +
                    "        el.style.setProperty('display', 'none', 'important');" +
                    "        el.style.setProperty('visibility', 'hidden', 'important');" +
                    "        setTimeout(function(){ if(el && el.parentNode) el.remove(); }, 0);" +
                    "      }" +
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
                    "  if (!window.isUltimateIntercepted) {" +
                    "    window.isUltimateIntercepted = true;" +
                    "    " +
                    "    var orgCreate = Document.prototype.createElement;" +
                    "    Document.prototype.createElement = function(tag) {" +
                    "      var el = orgCreate.apply(this, arguments);" +
                    "      if (tag.toLowerCase() === 'div') {" +
                    "        var orgSet = el.setAttribute;" +
                    "        el.setAttribute = function(name, val) {" +
                    "          orgSet.apply(this, arguments);" +
                    "          if (name === 'class' || name === 'data-sentry-component') {" +
                    "            if (val.indexOf('DownloadApp') !== -1) {" +
                    "              el.style.setProperty('display', 'none', 'important');" +
                    "              setTimeout(function(){ el.remove(); }, 0);" +
                    "            }" +
                    "          }" +
                    "        };" +
                    "      }" +
                    "      return el;" +
                    "    };" +
                    "    " +
                    "    var cssStyle = document.createElement('style');" +
                    "    cssStyle.innerHTML = SELECTORS + ' { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; }';" +
                    "    (document.head || document.documentElement).appendChild(cssStyle);" +
                    "    " +
                    "    var obs = new MutationObserver(nukeTargetElements);" +
                    "    obs.observe(document.documentElement, {childList: true, subtree: true, attributes: true});" +
                    "    " +
                    "    window.addEventListener('input', nukeTargetElements, true);" +
                    "    window.addEventListener('keydown', nukeTargetElements, true);" +
                    "    setInterval(nukeTargetElements, 300);" +
                    "  }" +
                    "  nukeTargetElements();" +
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
​
