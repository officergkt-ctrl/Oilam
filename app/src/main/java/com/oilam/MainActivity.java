package com.oilam;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.net.Uri;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private WebView web;
    private LinearLayout root;
    private static final String[][] SITES = {
        {"Torah Anytime", "https://www.torahanytime.com/"},
        {"Mostly Music", "https://mostlymusic.com/"},
        {"MyZmanim", "https://www.myzmanim.com/"},
        {"Kol Halashon", "https://www.kolhalashon.com/"},
        {"OU", "https://www.ou.org/"},
        {"STAR-K", "https://www.star-k.org/"},
        {"cRc", "https://www.crcweb.org/"}
    };
    private final Set<String> allowed = new HashSet<>(Arrays.asList(
        "torahanytime.com", "mostlymusic.com", "myzmanim.com", "kolhalashon.com", "ou.org", "star-k.org", "crcweb.org"
    ));

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF5F1E8);
        setContentView(root);
        TextView heading = new TextView(this);
        heading.setText("OILAM"); heading.setTextSize(24); heading.setGravity(Gravity.CENTER);
        heading.setTextColor(0xFF263238); heading.setPadding(8, 14, 8, 14);
        root.addView(heading, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER); root.addView(tabs, new LinearLayout.LayoutParams(-1, -2));
        Button home = new Button(this); home.setText("Home"); home.setOnClickListener(v -> showHome()); tabs.addView(home);
        Button back = new Button(this); back.setText("Back"); back.setOnClickListener(v -> { if (web != null && web.canGoBack()) web.goBack(); else showHome(); }); tabs.addView(back);
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !isAllowed(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !isAllowed(Uri.parse(url));
            }
        });
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        showHome();
    }
    private boolean isAllowed(Uri uri) {
        String host = uri.getHost(); if (host == null) return false;
        for (String domain : allowed) if (host.equals(domain) || host.endsWith("." + domain)) return true;
        return false;
    }
    private void showHome() {
        StringBuilder html = new StringBuilder("<html><meta name='viewport' content='width=device-width, initial-scale=1'><body style='font-family:sans-serif;background:#f5f1e8;color:#263238;text-align:center;padding:12px'><h2>Jewish Websites</h2><p>Choose a site</p>");
        for (String[] site : SITES) html.append("<a href='").append(site[1]).append("' style='display:block;background:#fff;padding:16px;margin:10px;border-radius:10px;color:#263238;text-decoration:none;font-size:18px;border:1px solid #d7cdbb'>").append(site[0]).append("</a>");
        html.append("</body></html>"); web.loadDataWithBaseURL("https://oilam.local/", html.toString(), "text/html", "UTF-8", null);
    }
    @Override public void onBackPressed() { if (web != null && web.canGoBack()) web.goBack(); else showHome(); }
}
