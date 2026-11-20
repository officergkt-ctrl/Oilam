package com.oilam.gmail;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

public class MainActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private String email = "", appPassword = "";
    private LinearLayout root, content;
    private TextView status;
    private final ArrayList<Message> messages = new ArrayList<>();
    private String currentFolder = "INBOX";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF174EA6);
        getWindow().setNavigationBarColor(0xFF174EA6);
        buildShell();
        showLogin();
    }

    private void buildShell() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(8, 6, 8, 6); root.setBackgroundColor(0xFFF5F7FA);
        TextView title = new TextView(this); title.setText("Gmail");
        title.setTextSize(22); title.setTypeface(null, 1); title.setTextColor(0xFF174EA6);
        root.addView(title, new LinearLayout.LayoutParams(-1, 36));
        status = new TextView(this); status.setText("Not connected");
        status.setTextSize(12); status.setTextColor(0xFF5F6368);
        root.addView(status, new LinearLayout.LayoutParams(-1, 25));
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView foot = new TextView(this); foot.setText("UP/DOWN: move   OK: select   BACK: return");
        foot.setTextSize(10); foot.setGravity(Gravity.CENTER);
        root.addView(foot, new LinearLayout.LayoutParams(-1, 24));
        setContentView(root);
    }

    private void showLogin() {
        content.removeAllViews();
        addText("Connect your Gmail account");
        addText("Use a Google App Password. Your normal Google password will not work.");
        EditText user = new EditText(this); user.setSingleLine(true);
        user.setHint("Gmail address"); user.setInputType(33);
        EditText pass = new EditText(this); pass.setSingleLine(true);
        pass.setHint("16-character App Password"); pass.setInputType(129);
        content.addView(user); content.addView(pass);
        addButton("✓", "Connect", v -> {
            String u = user.getText().toString().trim();
            String p = pass.getText().toString().replace(" ", "").trim();
            if (!u.contains("@") || p.isEmpty()) { showError("Enter your Gmail address and App Password"); return; }
            email = u; appPassword = p; connect();
        });
        addText("Create an App Password in your Google Account security settings. This app does not open a browser.");
        user.requestFocus();
    }

    private Properties mailProps() {
        Properties p = new Properties();
        p.put("mail.store.protocol", "imaps");
        p.put("mail.imaps.host", "imap.gmail.com");
        p.put("mail.imaps.port", "993");
        p.put("mail.imaps.ssl.enable", "true");
        p.put("mail.imaps.connectiontimeout", "20000");
        p.put("mail.imaps.timeout", "20000");
        p.put("mail.smtp.host", "smtp.gmail.com");
        p.put("mail.smtp.port", "465");
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.ssl.enable", "true");
        p.put("mail.smtp.connectiontimeout", "20000");
        p.put("mail.smtp.timeout", "20000");
        return p;
    }

    private Session session() {
        return Session.getInstance(mailProps(), new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, appPassword);
            }
        });
    }

    private void connect() {
        status.setText("Connecting…");
        io.execute(() -> {
            try {
                Store store = session().getStore("imaps");
                store.connect("imap.gmail.com", email, appPassword);
                store.close();
                runOnUiThread(() -> { status.setText("Connected: " + email); showHome(); loadFolder("INBOX"); });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("Connection failed");
                    new AlertDialog.Builder(this).setTitle("Gmail connection failed")
                        .setMessage(explain(e))
                        .setPositiveButton("Try again", (d,w) -> showLogin())
                        .setNegativeButton("OK", null).show();
                });
            }
        });
    }

    private String explain(Exception e) {
        String s = e.getMessage();
        if (s == null) s = e.toString();
        if (s.toLowerCase(Locale.US).contains("authentication") || s.toLowerCase(Locale.US).contains("invalid credentials"))
            return "Google rejected the login. Check the email and App Password. A normal Gmail password cannot be used.";
        return s;
    }

    private void showHome() {
        content.removeAllViews();
        String[] labels = {"▣", "➤", "☆", "✎", "⚙"};
        String[] actions = {"Inbox", "Sent", "Starred", "Compose", "Change account"};
        for (int i = 0; i < labels.length; i++) {
            final String action = actions[i];
            addButton(labels[i], action, v -> {
                if (action.equals("Compose")) compose();
                else if (action.equals("Change account")) { email = ""; appPassword = ""; showLogin(); }
                else if (action.equals("Inbox")) loadFolder("INBOX");
                else if (action.equals("Sent")) loadFolder("[Gmail]/Sent Mail");
                else loadFolder("STARRED");
            });
        }
        content.getChildAt(0).requestFocus();
    }

    private void loadFolder(String folderName) {
        currentFolder = folderName; status.setText("Loading " + folderName + "…");
        io.execute(() -> {
            try {
                Store store = session().getStore("imaps"); store.connect("imap.gmail.com", email, appPassword);
                Folder folder;
                if (folderName.equals("STARRED")) {
                    folder = store.getFolder("INBOX");
                } else folder = store.getFolder(folderName);
                folder.open(Folder.READ_ONLY);
                Message[] all = folder.getMessages();
                int start = Math.max(1, all.length - 29);
                Message[] recent = folder.getMessages(start, all.length);
                ArrayList<Message> list = new ArrayList<>();
                for (int i = recent.length - 1; i >= 0; i--) list.add(recent[i]);
                if (folderName.equals("STARRED")) {
                    ArrayList<Message> starred = new ArrayList<>();
                    for (Message m : list) if (m.isSet(Flags.Flag.FLAGGED)) starred.add(m);
                    list = starred;
                }
                folder.close(false); store.close();
                ArrayList<Message> result = list;
                runOnUiThread(() -> showMessages(result, folderName));
            } catch (Exception e) { runOnUiThread(() -> showError("Could not load mail: " + explain(e))); }
        });
    }

    private void showMessages(ArrayList<Message> list, String folder) {
        messages.clear(); messages.addAll(list); content.removeAllViews();
        status.setText(folder + " · " + list.size() + " recent messages");
        addButton("⌂", "Menu", v -> showHome());
        if (list.isEmpty()) { addText("No messages found."); return; }
        for (int i = 0; i < list.size(); i++) {
            Message m = list.get(i); final int index = i;
            String from = "(unknown sender)", subject = "(no subject)";
            try { Address[] a = m.getFrom(); if (a != null && a.length > 0) from = a[0].toString(); } catch (Exception ignored) {}
            try { if (m.getSubject() != null) subject = m.getSubject(); } catch (Exception ignored) {}
            String marker = "●";
            try { marker = m.isSet(Flags.Flag.SEEN) ? "○" : "●"; } catch (Exception ignored) {}
            addButton(marker + "  " + from + "\n" + subject, "Open email", v -> openMessage(index));
        }
        if (content.getChildCount() > 0) content.getChildAt(1).requestFocus();
    }

    private void openMessage(int index) {
        Message msg = messages.get(index); status.setText("Opening message…");
        io.execute(() -> {
            try {
                String from = address(msg.getFrom()), subject = msg.getSubject() == null ? "" : msg.getSubject();
                String date = msg.getSentDate() == null ? "" : msg.getSentDate().toString();
                String body = readBody(msg);
                runOnUiThread(() -> {
                    content.removeAllViews();
                    addButton("←", "Back to " + currentFolder, v -> loadFolder(currentFolder));
                    addText("From: " + from); addText("Date: " + date);
                    addText("Subject: " + subject); addText("");
                    addText(body);
                    addButton("↩", "Reply", v -> composeReply(from, subject));
                    addButton("◉", "Mark as unread", v -> markUnread(msg));
                    status.setText("Email");
                });
            } catch (Exception e) { runOnUiThread(() -> showError("Could not open email: " + explain(e))); }
        });
    }

    private String address(Address[] a) {
        if (a == null || a.length == 0) return "";
        return a[0].toString();
    }

    private String readBody(Part part) throws Exception {
        Object value = part.getContent();
        if (value instanceof String) return (String) value;
        if (value instanceof Multipart) {
            Multipart mp = (Multipart) value;
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) return readBody(bp);
            }
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/html")) return readBody(bp);
            }
        }
        return "(This email has no readable text body.)";
    }

    private void markUnread(Message selected) {
        status.setText("Marking unread…");
        io.execute(() -> {
            try {
                Store store = session().getStore("imaps"); store.connect("imap.gmail.com", email, appPassword);
                String folderName = currentFolder.equals("STARRED") ? "INBOX" : currentFolder;
                Folder folder = store.getFolder(folderName); folder.open(Folder.READ_WRITE);
                Message target = folder.getMessage(selected.getMessageNumber());
                target.setFlag(Flags.Flag.SEEN, false);
                folder.close(false); store.close();
                runOnUiThread(() -> { status.setText("Marked unread"); loadFolder(currentFolder); });
            } catch (Exception e) { runOnUiThread(() -> showError("Could not mark unread: " + explain(e))); }
        });
    }

    private void compose() { composeDialog("", ""); }
    private void composeReply(String to, String subject) { composeDialog(to, subject.startsWith("Re:") ? subject : "Re: " + subject); }

    private void composeDialog(String defaultTo, String defaultSubject) {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        EditText to = new EditText(this); to.setSingleLine(true); to.setHint("To"); to.setText(defaultTo);
        EditText subject = new EditText(this); subject.setSingleLine(true); subject.setHint("Subject"); subject.setText(defaultSubject);
        EditText body = new EditText(this); body.setHint("Message"); body.setMinLines(4);
        form.addView(to); form.addView(subject); form.addView(body);
        new AlertDialog.Builder(this).setTitle("Compose email").setView(form)
            .setPositiveButton("➤", (d,w) -> sendMail(to.getText().toString(), subject.getText().toString(), body.getText().toString()))
            .setNegativeButton("×", null).show();
    }

    private void sendMail(String to, String subject, String body) {
        if (to.trim().isEmpty()) { showError("Enter a recipient"); return; }
        status.setText("Sending…");
        io.execute(() -> {
            try {
                MimeMessage msg = new MimeMessage(session());
                msg.setFrom(new InternetAddress(email));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                msg.setSubject(subject, "UTF-8");
                msg.setText(body, "UTF-8");
                Transport.send(msg);
                runOnUiThread(() -> { status.setText("Email sent"); showHome(); });
            } catch (Exception e) { runOnUiThread(() -> showError("Send failed: " + explain(e))); }
        });
    }

    private void addText(String s) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(14);
        t.setTextColor(0xFF202124); t.setPadding(6, 7, 6, 7);
        content.addView(t, new LinearLayout.LayoutParams(-1, -2));
    }
    private void addButton(String icon, String description, View.OnClickListener click) {
        Button b = new Button(this); b.setText(icon); b.setContentDescription(description); b.setAllCaps(false);
        b.setTextSize(14); b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        b.setMinHeight(46); b.setFocusable(true); b.setOnClickListener(click);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = 3; content.addView(b, p);
    }
    private void addButton(String description, View.OnClickListener click) { addButton("•", description, click); }
    private void showError(String s) { status.setText(s); Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    @Override public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_DPAD_DOWN || key == KeyEvent.KEYCODE_DPAD_UP) {
            View f = getCurrentFocus(); if (f == null) return super.onKeyDown(key, event);
            View n = f.focusSearch(key == KeyEvent.KEYCODE_DPAD_DOWN ? View.FOCUS_DOWN : View.FOCUS_UP);
            if (n != null) n.requestFocus(); return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
            View f = getCurrentFocus(); if (f != null) { f.performClick(); return true; }
        }
        return super.onKeyDown(key, event);
    }
    @Override protected void onDestroy() { super.onDestroy(); io.shutdown(); appPassword = ""; }
}
