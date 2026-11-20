package com.oilam.gmail;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final Button[] items = new Button[6];
    private int selected = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF174EA6);
        getWindow().setNavigationBarColor(0xFF174EA6);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(12, 10, 12, 10);
        root.setBackgroundColor(0xFFF5F7FA);

        TextView title = new TextView(this);
        title.setText("Gmail");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(0xFF174EA6);
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title, new LinearLayout.LayoutParams(-1, 42));

        TextView hint = new TextView(this);
        hint.setText("Use UP / DOWN and OK");
        hint.setTextSize(13);
        hint.setTextColor(0xFF5F6368);
        root.addView(hint, new LinearLayout.LayoutParams(-1, 28));

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        String[] labels = {"Inbox", "Compose email", "Sent mail", "Drafts", "Starred", "Search mail"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            Button b = new Button(this);
            items[i] = b;
            b.setText(labels[i]);
            b.setTextSize(17);
            b.setAllCaps(false);
            b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            b.setPadding(14, 4, 10, 4);
            b.setMinHeight(48);
            b.setFocusable(true);
            b.setOnClickListener(v -> open(index));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 52);
            lp.bottomMargin = 5;
            list.addView(b, lp);
        }
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView footer = new TextView(this);
        footer.setText("OK: open    BACK: exit");
        footer.setTextSize(12);
        footer.setTextColor(0xFF5F6368);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, new LinearLayout.LayoutParams(-1, 28));

        setContentView(root);
        updateSelection();
        items[0].requestFocus();
    }

    private void updateSelection() {
        for (int i = 0; i < items.length; i++) {
            items[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                i == selected ? 0xFFD2E3FC : 0xFFE8EAED));
            items[i].setTextColor(0xFF202124);
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            selected = (selected + 1) % items.length;
            items[selected].requestFocus();
            updateSelection();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            selected = (selected + items.length - 1) % items.length;
            items[selected].requestFocus();
            updateSelection();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            open(selected);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void open(int index) {
        if (index == 1) {
            Intent compose = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
            compose.setPackage("com.google.android.gm");
            try {
                startActivity(compose);
            } catch (ActivityNotFoundException e) {
                openUrl("https://mail.google.com/mail/u/0/#compose");
            }
            return;
        }
        String[] paths = {
            "https://mail.google.com/mail/u/0/#inbox",
            "",
            "https://mail.google.com/mail/u/0/#sent",
            "https://mail.google.com/mail/u/0/#drafts",
            "https://mail.google.com/mail/u/0/#starred",
            "https://mail.google.com/mail/u/0/#search"
        };
        openUrl(paths[index]);
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No web browser is installed", Toast.LENGTH_LONG).show();
        }
    }
}