package com.home.waterctl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

/**
 * Water Controller - native Android app.
 * Shows the ESP32's own control page full-screen. No third-party service involved:
 * the phone talks straight to the ESP32 over your home Wi-Fi.
 */
public class MainActivity extends Activity {

    private WebView web;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("wlc", MODE_PRIVATE);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // keeps the PIN between launches

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) showOffline();
            }
        });

        // The page uses confirm() for "Calibrate"; WebView needs this to show it.
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView v, String url, String msg, final JsResult res) {
                new AlertDialog.Builder(MainActivity.this)
                    .setMessage(msg)
                    .setPositiveButton(android.R.string.ok, (d, w) -> res.confirm())
                    .setNegativeButton(android.R.string.cancel, (d, w) -> res.cancel())
                    .setOnCancelListener(d -> res.cancel())
                    .show();
                return true;
            }
        });

        String addr = prefs.getString("addr", null);
        if (addr == null) askAddress(); else web.loadUrl(addr);
    }

    private void askAddress() {
        final EditText in = new EditText(this);
        in.setHint("192.168.1.50  or  watertank.local");
        in.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        in.setText(prefs.getString("addr", "").replace("http://", ""));
        new AlertDialog.Builder(this)
            .setTitle("Controller address")
            .setMessage("Enter the ESP32 address shown in the Arduino Serial Monitor.")
            .setView(in)
            .setPositiveButton("Connect", (d, w) -> {
                String a = in.getText().toString().trim();
                if (a.isEmpty()) { showOffline(); return; }
                if (!a.startsWith("http")) a = "http://" + a;
                while (a.endsWith("/")) a = a.substring(0, a.length() - 1);
                prefs.edit().putString("addr", a).apply();
                web.loadUrl(a);
            })
            .setNegativeButton(android.R.string.cancel, (d, w) -> {
                if (prefs.getString("addr", null) == null) showOffline();
            })
            .show();
    }

    private void showOffline() {
        String a = prefs.getString("addr", "");
        String html = "<body style='font-family:sans-serif;padding:24px'>"
            + "<h2>Controller not reachable</h2>"
            + "<p>Check that the phone is on the home Wi-Fi and the ESP32 is powered.</p>"
            + (a.isEmpty() ? "" : "<p><a href='" + a + "'>Try again</a></p>")
            + "<p>Menu (three dots) &gt; Change address.</p></body>";
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        m.add(0, 1, 0, "Reload");
        m.add(0, 2, 1, "Change address");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            String a = prefs.getString("addr", null);
            if (a != null) web.loadUrl(a); else askAddress();
            return true;
        }
        if (item.getItemId() == 2) { askAddress(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override protected void onPause()  { super.onPause();  web.onPause(); }
    @Override protected void onResume() { super.onResume(); web.onResume(); }
}
