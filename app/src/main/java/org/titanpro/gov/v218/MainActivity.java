package org.titanpro.gov.v218;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private static final int REQ_PERMS = 1001;
    private static final String CHANNEL_ID = "titan_pro_gov_alerts";
    private static final String DASHBOARD_URL = "http://127.0.0.1:8080/";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int loadAttempts = 0;
    private boolean pageOk = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();
        requestAllPermissions();
        webView = findViewById(R.id.webview);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            s.setSafeBrowsingEnabled(false);
        }
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageOk = true;
                injectNotificationBridge();
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) pageOk = false;
            }
        });
        webView.addJavascriptInterface(new NotifyBridge(), "TitanNative");
        showBootHtml("در حال روشن شدن موتور TITAN PRO GOV v2.1.8 CALIBRATED…\n۳۰–۹۰ ثانیه صبر کنید.\nمجوز دسترسی به همه فایل‌ها را تأیید کنید تا پوشه XXX ساخته شود.");
        startPythonServer();
        scheduleLoad();
    }

    private void showBootHtml(String msg) {
        String html = "<!DOCTYPE html><html lang='fa' dir='rtl'><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>body{font-family:sans-serif;background:#0b0f1a;color:#e8ecff;padding:28px;line-height:1.8}"
                + ".c{background:#141a2e;border-radius:16px;padding:20px;max-width:520px;margin:40px auto;border:1px solid #00f0ff44;"
                + "box-shadow:0 0 24px #00f0ff33}</style></head>"
                + "<body><div class='c'><h3 style='color:#00f0ff;text-shadow:0 0 12px #00f0ff'>Titan PRO GOV v2.1.8 Neon</h3><pre style='white-space:pre-wrap'>" + msg
                + "</pre></div></body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Titan PRO GOV Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("سیگنال Titan PRO GOV v2.1.8");
            channel.enableVibration(true);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void requestAllPermissions() {
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                list.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                list.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (!list.isEmpty()) {
            ActivityCompat.requestPermissions(this, list.toArray(new String[0]), REQ_PERMS);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                if (!Environment.isExternalStorageManager()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            } catch (Exception e) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                } catch (Exception ignored) {}
            }
        }
    }

    private void injectNotificationBridge() {
        if (webView == null) return;
        String js = "(function(){if(window.__titanNativeNotify)return;window.__titanNativeNotify=1;"
                + "function show(t,b){try{if(window.TitanNative)TitanNative.notify(String(t||'Titan'),String(b||''));}catch(e){}}"
                + "try{function N(t,o){o=o||{};show(t,o.body||'');}N.permission='granted';N.requestPermission=function(){return Promise.resolve('granted');};window.Notification=N;}catch(e){}})();";
        webView.evaluateJavascript(js, null);
    }

    public class NotifyBridge {
        @JavascriptInterface
        public void notify(String title, String body) {
            handler.post(() -> postNotification(title, body));
        }
    }

    private void postNotification(String title, String body) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title != null ? title : "Titan PRO GOV")
                .setContentText(body != null ? body : "")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body != null ? body : ""))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);
        NotificationManagerCompat.from(this).notify((int) (System.currentTimeMillis() % 100000), b.build());
    }

    private void startPythonServer() {
        new Thread(() -> {
            try {
                if (!Python.isStarted()) Python.start(new AndroidPlatform(getApplicationContext()));
                Python.getInstance().getModule("android_main").callAttr("start_server");
                handler.post(() -> Toast.makeText(MainActivity.this, "موتور Titan PRO GOV v2.1.8 شروع شد", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                final String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                handler.post(() -> {
                    Toast.makeText(MainActivity.this, "Server error: " + msg, Toast.LENGTH_LONG).show();
                    showBootHtml("خطای پایتون:\n" + msg);
                });
            }
        }, "titan-python").start();
    }

    private void scheduleLoad() { handler.postDelayed(this::tryLoad, 5000); }

    private void tryLoad() {
        loadAttempts++;
        if (!pageOk || loadAttempts <= 3) webView.loadUrl(DASHBOARD_URL);
        if (loadAttempts < 40) handler.postDelayed(this::tryLoad, 3000);
        else if (!pageOk) showBootHtml("سرور پاسخ نداد. Force Stop کنید و دوباره باز کنید.\nپوشه XXX را در حافظه داخلی بررسی کنید.");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
