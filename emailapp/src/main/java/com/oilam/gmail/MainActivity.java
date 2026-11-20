package com.oilam.gmail;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.common.api.Scope;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int AUTH_REQUEST = 4102;
    private static final String GMAIL_MODIFY = "https://www.googleapis.com/auth/gmail.modify";
    private static final String GMAIL_SEND = "https://www.googleapis.com/auth/gmail.send";
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private AuthorizationClient authClient;
    private String accessToken;
    private LinearLayout root, content;
    private TextView status;
    private final ArrayList<JSONObject> messages = new ArrayList<>();
    private int selected = 0;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF174EA6);
        getWindow().setNavigationBarColor(0xFF174EA6);
        authClient = Identity.getAuthorizationClient(this);
        buildShell();
        if (accessToken == null) connectGmail();
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(10, 8, 10, 8);
        root.setBackgroundColor(0xFFF5F7FA);
        TextView title = new TextView(this);
        title.setText("Gmail");
        title.setTextSize(23);
        title.setTypeface(null, 1);
        title.setTextColor(0xFF174EA6);
        root.addView(title, new LinearLayout.LayoutParams(-1, 38));
        status = new TextView(this);
        status.setText("Connect your Google account");
        status.setTextSize(13);
        status.setTextColor(0xFF5F6368);
        root.addView(status, new LinearLayout.LayoutParams(-1, 28));
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView foot = new TextView(this);
        foot.setText("UP/DOWN: move   OK: select   BACK: return");
        foot.setTextSize(11);
        foot.setGravity(Gravity.CENTER);
        root.addView(foot, new LinearLayout.LayoutParams(-1, 26));
        setContentView(root);
        showHome();
    }

    private void connectGmail() {
        status.setText("Requesting Google account permission…");
        AuthorizationRequest request = AuthorizationRequest.builder()
            .setRequestedScopes(Arrays.asList(new Scope(GMAIL_MODIFY), new Scope(GMAIL_SEND)))
            .build();
        authClient.authorize(request).addOnSuccessListener(result -> handleAuthorization(result))
            .addOnFailureListener(e -> {
                status.setText("Google sign-in unavailable");
                new AlertDialog.Builder(this).setTitle("Connect Gmail")
                    .setMessage("Google account authorization could not start: " + e.getMessage())
                    .setPositiveButton("Try again", (d,w) -> connectGmail())
                    .setNegativeButton("Cancel", null).show();
            });
    }

    private void handleAuthorization(AuthorizationResult result) {
        if (result.hasResolution()) {
            try {
                startIntentSenderForResult(result.getPendingIntent().getIntentSender(), AUTH_REQUEST, null, 0, 0, 0);
            } catch (Exception e) { showError("Could not open Google permission screen"); }
        } else {
            accessToken = result.getAccessToken();
            status.setText("Connected to Gmail");
            loadInbox();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTH_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    AuthorizationResult result = authClient.getAuthorizationResultFromIntent(data);
                    accessToken = result.getAccessToken();
                    status.setText("Connected to Gmail");
                    loadInbox();
                } catch (Exception e) { showError("Google authorization was not completed"); }
            } else status.setText("Gmail not connected");
        }
    }

    private void showHome() {
        content.removeAllViews();
        String[] labels = {"Inbox", "Compose", "Sent", "Drafts", "Starred", "Search", "Connect account"};
        for (int i=0;i<labels.length;i++) {
            final int n=i;
            Button b = new Button(this);
            b.setText(labels[i]); b.setAllCaps(false); b.setTextSize(16);
            b.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
            b.setMinHeight(48); b.setFocusable(true);
            b.setOnClickListener(v -> { selected=n; if(n==0) loadInbox(); else if(n==1) compose(); else if(n==6) connectGmail(); else loadLabel(labels[n].toUpperCase(Locale.US)); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 50); p.bottomMargin=4;
            content.addView(b,p);
        }
        selected=0;
        content.getChildAt(0).requestFocus();
    }

    private void loadLabel(String label) {
        if (!ensureConnected()) return;
        status.setText("Loading " + label.toLowerCase(Locale.US) + "…");
        String labelId = label.equals("SENT") ? "SENT" : label.equals("DRAFTS") ? "DRAFT" : "STARRED";
        fetchList(labelId);
    }
    private void loadInbox() { if (!ensureConnected()) return; status.setText("Loading inbox…"); fetchList("INBOX"); }
    private boolean ensureConnected() {
        if (accessToken == null) { connectGmail(); return false; }
        return true;
    }

    private void fetchList(String label) {
        io.execute(() -> {
            try {
                JSONObject response = request("GET", "https://gmail.googleapis.com/gmail/v1/users/me/messages?labelIds="+label+"&maxResults=25", null);
                JSONArray arr = response.optJSONArray("messages");
                ArrayList<JSONObject> loaded = new ArrayList<>();
                if (arr != null) for (int i=0;i<arr.length();i++) {
                    JSONObject item=arr.getJSONObject(i);
                    JSONObject full=request("GET","https://gmail.googleapis.com/gmail/v1/users/me/messages/"+item.getString("id")+"?format=metadata&metadataHeaders=From&metadataHeaders=Subject&metadataHeaders=Date",null);
                    loaded.add(full);
                }
                runOnUiThread(() -> showMessages(loaded, label));
            } catch(Exception e) { runOnUiThread(() -> showError("Could not load mail: "+e.getMessage())); }
        });
    }

    private void showMessages(ArrayList<JSONObject> list, String label) {
        messages.clear(); messages.addAll(list); content.removeAllViews(); selected=0;
        status.setText(label+" · "+list.size()+" messages");
        if(list.isEmpty()) { addText("No messages found."); return; }
        for(int i=0;i<list.size();i++) {
            JSONObject msg=list.get(i); String from=header(msg,"From"), subject=header(msg,"Subject");
            Button b=new Button(this); b.setAllCaps(false); b.setTextSize(14);
            b.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
            b.setText((from.isEmpty()?"(unknown sender)":from)+"\n"+(subject.isEmpty()?"(no subject)":subject)+"\n"+msg.optString("snippet"));
            b.setMinHeight(72); b.setFocusable(true);
            final int index=i; b.setOnClickListener(v -> openMessage(index));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=4; content.addView(b,p);
        }
        content.getChildAt(0).requestFocus();
    }

    private void openMessage(int index) {
        JSONObject m=messages.get(index); String id=m.optString("id");
        status.setText("Opening message…");
        io.execute(() -> {
            try {
                JSONObject full=request("GET","https://gmail.googleapis.com/gmail/v1/users/me/messages/"+id+"?format=full",null);
                String body=extractBody(full.optJSONObject("payload"));
                runOnUiThread(() -> {
                    content.removeAllViews();
                    addText(header(full,"From"));
                    addText("To: "+header(full,"To"));
                    addText(header(full,"Date"));
                    addText(header(full,"Subject"));
                    addText("");
                    addText(body.isEmpty()?full.optString("snippet"):body);
                    addButton("Reply", v -> composeReply(full));
                    addButton("Back to inbox", v -> loadInbox());
                    status.setText("Message");
                });
            } catch(Exception e) { runOnUiThread(() -> showError("Could not open message")); }
        });
    }

    private void compose() { composeDialog(null); }
    private void composeReply(JSONObject msg) { composeDialog(msg); }
    private void composeDialog(JSONObject reply) {
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        EditText to=new EditText(this); to.setSingleLine(true); to.setHint("To");
        EditText subject=new EditText(this); subject.setSingleLine(true); subject.setHint("Subject");
        EditText body=new EditText(this); body.setHint("Message"); body.setMinLines(5);
        if(reply!=null) { to.setText(header(reply,"Reply-To").isEmpty()?header(reply,"From"):header(reply,"Reply-To")); subject.setText("Re: "+header(reply,"Subject")); }
        form.addView(to); form.addView(subject); form.addView(body);
        new AlertDialog.Builder(this).setTitle("Compose email").setView(form)
            .setPositiveButton("Send",(d,w)->sendMail(to.getText().toString(),subject.getText().toString(),body.getText().toString()))
            .setNegativeButton("Cancel",null).show();
    }

    private void sendMail(String to,String subject,String body) {
        if(to.trim().isEmpty()) { showError("Enter a recipient"); return; }
        status.setText("Sending…");
        io.execute(() -> {
            try {
                String raw="To: "+to+"\r\nSubject: "+subject+"\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n"+body;
                String encoded=android.util.Base64.encodeToString(raw.getBytes(StandardCharsets.UTF_8),android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING);
                JSONObject payload=new JSONObject(); payload.put("raw",encoded);
                request("POST","https://gmail.googleapis.com/gmail/v1/users/me/messages/send",payload);
                runOnUiThread(()->{ status.setText("Email sent"); showHome(); });
            } catch(Exception e) { runOnUiThread(()->showError("Send failed: "+e.getMessage())); }
        });
    }

    private JSONObject request(String method,String url,JSONObject body) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setRequestMethod(method); c.setConnectTimeout(20000); c.setReadTimeout(20000);
        c.setRequestProperty("Authorization","Bearer "+accessToken);
        c.setRequestProperty("Accept","application/json");
        if(body!=null) { c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json; charset=UTF-8"); try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));} }
        int code=c.getResponseCode(); InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
        ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[4096]; int n; while((n=in.read(buf))>0)out.write(buf,0,n); c.disconnect();
        String text=new String(out.toByteArray(),StandardCharsets.UTF_8);
        if(code<200||code>=300) throw new IOException("Google API "+code+": "+text);
        return text.isEmpty()?new JSONObject():new JSONObject(text);
    }

    private String header(JSONObject msg,String name) {
        JSONObject p=msg.optJSONObject("payload"); if(p==null)return "";
        JSONArray hs=p.optJSONArray("headers"); if(hs==null)return "";
        for(int i=0;i<hs.length();i++){JSONObject h=hs.optJSONObject(i);if(h!=null&&name.equalsIgnoreCase(h.optString("name")))return h.optString("value");}
        return "";
    }
    private String extractBody(JSONObject part) {
        if(part==null)return "";
        String mime=part.optString("mimeType");
        if(mime.startsWith("text/plain")) {
            String data=part.optJSONObject("body")==null?"":part.optJSONObject("body").optString("data");
            if(!data.isEmpty()) try { return new String(android.util.Base64.decode(data,android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP),StandardCharsets.UTF_8); } catch(Exception ignored) {}
        }
        JSONArray parts=part.optJSONArray("parts");
        if(parts!=null) for(int i=0;i<parts.length();i++){String s=extractBody(parts.optJSONObject(i));if(!s.isEmpty())return s;}
        return "";
    }
    private void addText(String value) { TextView t=new TextView(this); t.setText(value); t.setTextSize(15); t.setTextColor(0xFF202124); t.setPadding(6,8,6,8); content.addView(t,new LinearLayout.LayoutParams(-1,-2)); }
    private void addButton(String label,View.OnClickListener click) { Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setOnClickListener(click); content.addView(b,new LinearLayout.LayoutParams(-1,-2)); }
    private void showError(String message) { status.setText(message); Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
    @Override public boolean onKeyDown(int key,KeyEvent event) {
        if(key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_UP) {
            View focused=getCurrentFocus(); if(focused==null)return super.onKeyDown(key,event);
            View next=focused.focusSearch(key==KeyEvent.KEYCODE_DPAD_DOWN?View.FOCUS_DOWN:View.FOCUS_UP);
            if(next!=null)next.requestFocus(); return true;
        }
        if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER) {
            View focused=getCurrentFocus(); if(focused!=null){focused.performClick();return true;}
        }
        return super.onKeyDown(key,event);
    }
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}
