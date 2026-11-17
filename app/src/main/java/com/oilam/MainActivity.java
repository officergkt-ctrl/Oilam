package com.oilam;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private LinearLayout root;
    private LinearLayout toolbar;
    private FrameLayout browserArea;
    private TextView homeButton, backButton, forwardButton, tabsButton;
    private final ArrayList<BrowserTab> tabs = new ArrayList<>();
    private int activeTab = -1;
    private static final int MAX_TABS = 6;
    private boolean numberHintsActive = false;
    private boolean textEntryFocused = false;
    private String numberBuffer = "";
    private final Handler numberHandler = new Handler(Looper.getMainLooper());
    private final Runnable selectNumberRunnable = () -> selectNumberHint();

    private static class Site {
        final String name, url, domain;
        Site(String name, String url, String domain) {
            this.name = name; this.url = url; this.domain = domain;
        }
    }

    private static final Site[] JEWISH_SITES = {
        new Site("Torah Anytime", "https://www.torahanytime.com/", "torahanytime.com"),
        new Site("Mostly Music", "https://mostlymusic.com/", "mostlymusic.com"),
        new Site("MyZmanim", "https://www.myzmanim.com/", "myzmanim.com"),
        new Site("Kol Halashon", "https://www.kolhalashon.com/en/regularSite/home?English=false", "kolhalashon.com"),
        new Site("OU", "https://www.ou.org/", "ou.org"),
        new Site("STAR-K", "https://www.star-k.org/", "star-k.org"),
        new Site("cRc", "https://www.crcweb.org/newsite/", "crcweb.org"),
        new Site("Kollel.org", "https://www.kollel.org/", "kollel.org"),
        new Site("HebrewBooks", "https://www.hebrewbooks.org/", "hebrewbooks.org")
    };

    private static final Site[] SERVICES = {
        new Site("Gett", "https://www.gett.com/il", "gett.com"),
        new Site("Moovit", "https://moovitapp.com/", "moovitapp.com"),
        new Site("Waze", "https://www.waze.com/", "waze.com"),
        new Site("Pango", "https://www.pango.co.il/", "pango.co.il")
    };

    private static class BrowserTab {
        WebView web;
        String title = "New tab";
        BrowserTab(WebView web) { this.web = web; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);

        buildToolbar();
        browserArea = new FrameLayout(this);
        root.addView(browserArea, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        addTab(true);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView toolbarButton(String label, int widthDp) {
        TextView button = new TextView(this);
        button.setText(label);
        button.setTextSize(14);
        button.setTextColor(Color.rgb(35, 48, 58));
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setPadding(dp(5), 0, dp(5), 0);
        button.setBackgroundColor(Color.TRANSPARENT);
        toolbar.addView(button, new LinearLayout.LayoutParams(
            widthDp == 0 ? 0 : dp(widthDp), ViewGroup.LayoutParams.MATCH_PARENT,
            widthDp == 0 ? 1f : 0f));
        return button;
    }

    private void buildToolbar() {
        toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(4), 0, dp(4), 0);
        toolbar.setBackgroundColor(Color.rgb(248, 247, 244));
        root.addView(toolbar, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        homeButton = toolbarButton("⌂ Home", 62);
        toolbarButton("", 0);
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        toolbar.addView(nav, new LinearLayout.LayoutParams(dp(94), ViewGroup.LayoutParams.MATCH_PARENT));
        backButton = navButton("‹");
        forwardButton = navButton("›");
        nav.addView(backButton, new LinearLayout.LayoutParams(dp(46), ViewGroup.LayoutParams.MATCH_PARENT));
        nav.addView(forwardButton, new LinearLayout.LayoutParams(dp(46), ViewGroup.LayoutParams.MATCH_PARENT));
        toolbarButton("", 0);
        tabsButton = toolbarButton("Tabs 1", 64);

        homeButton.setOnClickListener(v -> showHome());
        backButton.setOnClickListener(v -> {
            WebView web = currentWeb();
            if (web != null && web.canGoBack()) web.goBack();
        });
        forwardButton.setOnClickListener(v -> {
            WebView web = currentWeb();
            if (web != null && web.canGoForward()) web.goForward();
        });
        tabsButton.setOnClickListener(v -> showTabsDialog());
        updateToolbar();
    }

    private TextView navButton(String label) {
        TextView button = new TextView(this);
        button.setText(label);
        button.setTextSize(27);
        button.setTextColor(Color.rgb(35, 48, 58));
        button.setGravity(Gravity.CENTER);
        button.setBackgroundColor(Color.TRANSPARENT);
        return button;
    }

    private WebView currentWeb() {
        if (activeTab < 0 || activeTab >= tabs.size()) return null;
        return tabs.get(activeTab).web;
    }

    // Keypad: 1-9 open the nine Jewish sites; 0 toggles numbered link hints.
    // While hints are on, digit sequences choose a numbered clickable item.
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            int key = event.getKeyCode();
            // Let the website receive keypad input normally while a text field is active.
            if (textEntryFocused) return super.dispatchKeyEvent(event);
            if (key == KeyEvent.KEYCODE_ENDCALL) {
                showHome();
                return true;
            }
            if (key == KeyEvent.KEYCODE_0 || key == KeyEvent.KEYCODE_NUMPAD_0) {
                toggleNumberHints();
                return true;
            }
            int digit = -1;
            switch (key) {
                case KeyEvent.KEYCODE_1: case KeyEvent.KEYCODE_NUMPAD_1: digit = 1; break;
                case KeyEvent.KEYCODE_2: case KeyEvent.KEYCODE_NUMPAD_2: digit = 2; break;
                case KeyEvent.KEYCODE_3: case KeyEvent.KEYCODE_NUMPAD_3: digit = 3; break;
                case KeyEvent.KEYCODE_4: case KeyEvent.KEYCODE_NUMPAD_4: digit = 4; break;
                case KeyEvent.KEYCODE_5: case KeyEvent.KEYCODE_NUMPAD_5: digit = 5; break;
                case KeyEvent.KEYCODE_6: case KeyEvent.KEYCODE_NUMPAD_6: digit = 6; break;
                case KeyEvent.KEYCODE_7: case KeyEvent.KEYCODE_NUMPAD_7: digit = 7; break;
                case KeyEvent.KEYCODE_8: case KeyEvent.KEYCODE_NUMPAD_8: digit = 8; break;
                case KeyEvent.KEYCODE_9: case KeyEvent.KEYCODE_NUMPAD_9: digit = 9; break;
            }
            if (digit >= 0) {
                if (numberHintsActive) {
                    numberBuffer += digit;
                    numberHandler.removeCallbacks(selectNumberRunnable);
                    numberHandler.postDelayed(selectNumberRunnable, 700);
                    return true;
                }
                if (digit <= JEWISH_SITES.length) {
                    openSite(JEWISH_SITES[digit - 1]);
                    return true;
                }
            }
            WebView web = currentWeb();
            if (web != null && key == KeyEvent.KEYCODE_STAR) {
                web.zoomOut();
                return true;
            }
            if (web != null && key == KeyEvent.KEYCODE_POUND) {
                web.zoomIn();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void toggleNumberHints() {
        WebView web = currentWeb();
        if (web == null) return;
        numberHintsActive = !numberHintsActive;
        numberBuffer = "";
        numberHandler.removeCallbacks(selectNumberRunnable);
        String script = numberHintsActive ? NUMBER_HINTS_ON_JS : NUMBER_HINTS_OFF_JS;
        web.evaluateJavascript(script, null);
    }

    private void selectNumberHint() {
        if (!numberHintsActive || numberBuffer.isEmpty()) return;
        String number = numberBuffer;
        numberBuffer = "";
        WebView web = currentWeb();
        if (web != null) web.evaluateJavascript(
            "window.oilamClickNumber && window.oilamClickNumber(" + number + ");", null);
    }

    private static final String NUMBER_HINTS_OFF_JS =
        "(function(){if(window.oilamHintCleanup)window.oilamHintCleanup();})();";

    private static final String NUMBER_HINTS_ON_JS =
        "(function(){try{" +
        "if(window.oilamHintCleanup)window.oilamHintCleanup();" +
        "var items=Array.from(document.querySelectorAll('a,button,input,select,textarea,[role=button],[role=link],[onclick],[tabindex]'));" +
        "items=items.filter(function(e){var r=e.getBoundingClientRect(),s=getComputedStyle(e);" +
        "return r.width>0&&r.height>0&&s.display!=='none'&&s.visibility!=='hidden'&&e.getAttribute('aria-hidden')!=='true';});" +
        "var layer=document.createElement('div');layer.id='oilam-number-hints';" +
        "layer.style.cssText='position:fixed;inset:0;z-index:2147483647;pointer-events:none;';" +
        "var badges=[];items.forEach(function(e,i){var b=document.createElement('span');b.textContent=String(i+1);" +
        "b.style.cssText='position:fixed;background:#111;color:#fff;border:2px solid #fff;border-radius:5px;" +
        "font:bold 14px Arial;padding:2px 5px;line-height:18px;box-shadow:0 1px 4px #000;';" +
        "layer.appendChild(b);badges.push({el:e,badge:b});});document.documentElement.appendChild(layer);" +
        "function place(){badges.forEach(function(x){var r=x.el.getBoundingClientRect();" +
        "x.badge.style.display=(r.width>0&&r.height>0&&r.bottom>0&&r.top<innerHeight)?'block':'none';" +
        "x.badge.style.left=Math.max(0,Math.min(innerWidth-28,r.left))+'px';" +
        "x.badge.style.top=Math.max(0,Math.min(innerHeight-26,r.top))+'px';});}" +
        "var timer;function schedule(){clearTimeout(timer);timer=setTimeout(place,60);}place();" +
        "addEventListener('scroll',schedule,true);addEventListener('resize',schedule);" +
        "window.oilamClickNumber=function(n){var x=badges[n-1];if(x){x.el.scrollIntoView({block:'center',inline:'nearest'});" +
        "setTimeout(function(){try{x.el.focus();x.el.click();}catch(e){}},80);}};" +
        "window.oilamHintCleanup=function(){clearTimeout(timer);removeEventListener('scroll',schedule,true);" +
        "removeEventListener('resize',schedule);if(layer.parentNode)layer.parentNode.removeChild(layer);" +
        "window.oilamClickNumber=null;window.oilamHintCleanup=null;};" +
        "}catch(e){}})();";

    private void addTab(boolean showMenu) {
        if (tabs.size() >= MAX_TABS) {
            new AlertDialog.Builder(this).setMessage("You can have up to " + MAX_TABS + " tabs open.")
                .setPositiveButton("OK", null).show();
            return;
        }
        WebView web = new WebView(this);
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public void setTextEntryFocused(boolean focused) {
                runOnUiThread(() -> textEntryFocused = focused);
            }
        }, "OilamBridge");
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        // Prefer responsive/mobile layouts and scale desktop pages to the narrow device.
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setVerticalScrollBarEnabled(true);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onReceivedTitle(WebView view, String title) {
                int index = findTab(view);
                if (index >= 0) {
                    tabs.get(index).title = (title == null || title.trim().isEmpty()) ? "Website" : title;
                    updateToolbar();
                }
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                numberHintsActive = false;
                textEntryFocused = false;
                numberBuffer = "";
                numberHandler.removeCallbacks(selectNumberRunnable);
                super.onPageFinished(view, url);
                // Add a mobile viewport only when a page has not supplied one.
                String script = "(function(){try{var m=document.querySelector('meta[name=viewport]');"
                    + "if(!m){m=document.createElement('meta');m.name='viewport';"
                    + "m.content='width=device-width, initial-scale=1';document.head.appendChild(m);}"
                    + "}catch(e){}})();";
                view.evaluateJavascript(script, null);
                // Track focused search boxes and other editable controls so 0 is typed, not treated as a hint toggle.
                view.evaluateJavascript("(function(){try{if(window.__oilamFocusTracking)return;window.__oilamFocusTracking=true;"
                    + "function report(){var e=document.activeElement;var f=!!(e&&(e.matches('input,textarea,select,[contenteditable=true],[role=textbox]')||e.isContentEditable));"
                    + "if(window.OilamBridge&&OilamBridge.setTextEntryFocused)OilamBridge.setTextEntryFocused(f);}"
                    + "document.addEventListener('focusin',report,true);document.addEventListener('focusout',function(){setTimeout(report,0);},true);report();"
                    + "}catch(e){}})();", null);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String scheme = request.getUrl().getScheme();
                return !( "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme) );
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !(url.startsWith("https://") || url.startsWith("http://"));
            }
        });
        BrowserTab tab = new BrowserTab(web);
        tabs.add(tab);
        browserArea.addView(web, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        switchTab(tabs.size() - 1);
        if (showMenu) web.loadDataWithBaseURL("https://oilam.local/", homeHtml(),
            "text/html", "UTF-8", null);
    }

    private int findTab(WebView web) {
        for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).web == web) return i;
        return -1;
    }

    private void switchTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        activeTab = index;
        for (int i = 0; i < tabs.size(); i++)
            tabs.get(i).web.setVisibility(i == index ? View.VISIBLE : View.GONE);
        updateToolbar();
    }

    private void showTabsDialog() {
        ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < tabs.size(); i++) {
            String name = tabs.get(i).title;
            labels.add((i == activeTab ? "✓ " : "") + (name.length() > 32 ? name.substring(0, 29) + "..." : name));
        }
        labels.add("+ New tab");
        String[] items = labels.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Open tabs")
            .setItems(items, (dialog, which) -> {
                if (which == tabs.size()) addTab(true);
                else switchTab(which);
            })
            .setNeutralButton("Close current tab", (dialog, which) -> closeCurrentTab())
            .setNegativeButton("Cancel", null).show();
    }

    private void closeCurrentTab() {
        if (tabs.size() <= 1) {
            WebView web = currentWeb();
            if (web != null) {
                web.clearHistory();
                web.loadDataWithBaseURL("https://oilam.local/", homeHtml(), "text/html", "UTF-8", null);
                tabs.get(0).title = "Oilam";
            }
            updateToolbar();
            return;
        }
        BrowserTab removed = tabs.remove(activeTab);
        browserArea.removeView(removed.web);
        removed.web.stopLoading();
        removed.web.destroy();
        switchTab(Math.max(0, activeTab - 1));
    }

    private void showHome() {
        WebView web = currentWeb();
        if (web != null) {
            web.loadDataWithBaseURL("https://oilam.local/", homeHtml(), "text/html", "UTF-8", null);
            tabs.get(activeTab).title = "Oilam";
            updateToolbar();
        }
    }

    private void openSite(Site site) {
        WebView web = currentWeb();
        if (web == null) return;
        tabs.get(activeTab).title = site.name;
        web.loadUrl(site.url);
        updateToolbar();
    }

    private String cardsHtml(Site[] sites) {
        StringBuilder html = new StringBuilder();
        for (Site site : sites) {
            html.append("<a class='tile' href='").append(site.url).append("'>")
                .append("<img src='https://www.google.com/s2/favicons?domain=")
                .append(site.domain).append("&sz=128' onerror=\"this.style.display='none'\">")
                .append("<span>").append(site.name).append("</span></a>");
        }
        return html.toString();
    }

    private String homeHtml() {
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>"
            + "<style>body{margin:0;padding:10px 9px 22px;background:#f5f1e8;color:#263238;font-family:Arial,sans-serif}"
            + "h2{font-size:17px;margin:8px 3px 9px;font-weight:700} .grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:9px}"
            + ".tile{box-sizing:border-box;min-height:91px;background:#fff;border:1px solid #ddd4c6;border-radius:11px;"
            + "display:flex;flex-direction:column;align-items:center;justify-content:center;gap:7px;padding:8px 3px;"
            + "color:#263238;text-decoration:none;text-align:center;font-size:12px;font-weight:600}"
            + ".tile img{width:34px;height:34px;object-fit:contain;border-radius:7px} .section{margin-bottom:17px}</style>"
            + "</head><body><section class='section'><h2>Jewish Websites</h2><div class='grid'>"
            + cardsHtml(JEWISH_SITES) + "</div></section><section class='section'><h2>Services</h2><div class='grid'>"
            + cardsHtml(SERVICES) + "</div></section></body></html>";
    }

    private void updateToolbar() {
        if (tabsButton == null) return;
        tabsButton.setText("Tabs " + tabs.size());
        WebView web = currentWeb();
        boolean canBack = web != null && web.canGoBack();
        boolean canForward = web != null && web.canGoForward();
        backButton.setTextColor(canBack ? Color.rgb(35,48,58) : Color.LTGRAY);
        forwardButton.setTextColor(canForward ? Color.rgb(35,48,58) : Color.LTGRAY);
    }

    @Override public void onBackPressed() {
        WebView web = currentWeb();
        if (web != null && web.canGoBack()) web.goBack();
        else showHome();
    }

    @Override protected void onDestroy() {
        for (BrowserTab tab : tabs) {
            tab.web.stopLoading();
            tab.web.destroy();
        }
        tabs.clear();
        super.onDestroy();
    }
}
