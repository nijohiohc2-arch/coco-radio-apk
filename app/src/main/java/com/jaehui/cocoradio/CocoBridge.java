package com.jaehui.cocoradio;

import android.content.Context;
import android.webkit.JavascriptInterface;

public class CocoBridge {
    private final Context app;

    public CocoBridge(Context context) {
        this.app = context.getApplicationContext();
    }

    @JavascriptInterface
    public boolean isApk() {
        return true;
    }

    @JavascriptInterface
    public String getUserAgentMarker() {
        return "CocoRadio/1.3";
    }

    @JavascriptInterface
    public void playStation(String json) {
        RadioService.enqueuePlayJson(app, json);
    }

    @JavascriptInterface
    public void playStationId(String id) {
        RadioService.enqueuePlayId(app, id);
    }

    @JavascriptInterface
    public void pause() {
        RadioService.enqueuePause(app);
    }

    @JavascriptInterface
    public void resume() {
        RadioService.enqueueResume(app);
    }

    @JavascriptInterface
    public void setVolume(float volume) {
        RadioService.enqueueVolume(app, volume);
    }

    @JavascriptInterface
    public void saveLastStation(String id) {
        RadioService.saveLastStation(app, id);
    }

    @JavascriptInterface
    public boolean isPlaying() {
        return RadioService.isPlaying();
    }

    @JavascriptInterface
    public String currentStationId() {
        String id = RadioService.getCurrentStationId();
        return id == null ? "" : id;
    }
}
