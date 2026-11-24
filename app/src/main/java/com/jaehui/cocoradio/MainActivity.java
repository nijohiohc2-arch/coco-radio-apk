package com.jaehui.cocoradio;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebView;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        Intent svc = new Intent(this, RadioService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }

        webView = RadioService.getOrCreateWebView(this);
        ViewParent parent = webView.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(webView);
        }

        getWindow().getDecorView().setKeepScreenOn(true);
        setContentView(webView);
    }

    @Override
    protected void onPause() {
        // Do not call webView.onPause() or pauseTimers() so radio can keep
        // playing when a map app comes to the foreground.
        if (webView != null) {
            webView.resumeTimers();
            webView.evaluateJavascript(
                    "(function(){var a=document.querySelector('audio');"
                            + "if(a && !a.paused){var p=a.play();"
                            + "if(p&&p.catch)p.catch(function(){});}"
                            + "})();",
                    null);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        // Detach only. The service keeps the WebView so audio can continue.
        if (webView != null) {
            ViewParent parent = webView.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(webView);
            }
        }
        webView = null;
        super.onDestroy();
    }
}
