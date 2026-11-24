package com.jaehui.cocoradio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.webkit.WebView;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RadioService extends Service {
    public static final String ACTION_PLAY = "com.jaehui.cocoradio.PLAY";
    public static final String ACTION_PLAY_ID = "com.jaehui.cocoradio.PLAY_ID";
    public static final String ACTION_PAUSE = "com.jaehui.cocoradio.PAUSE";
    public static final String ACTION_RESUME = "com.jaehui.cocoradio.RESUME";
    public static final String ACTION_AUTO_PLAY = "com.jaehui.cocoradio.AUTO_PLAY";
    public static final String ACTION_SET_VOLUME = "com.jaehui.cocoradio.SET_VOLUME";
    public static final String EXTRA_STATION_JSON = "station_json";
    public static final String EXTRA_STATION_ID = "station_id";
    public static final String EXTRA_VOLUME = "volume";

    private static final String CHANNEL_ID = "coco_radio_channel";
    private static final int NOTIF_ID = 1;
    private static final String PREFS = "coco_radio";
    private static final String KEY_LAST = "last_station_id";
    private static final String KEY_VOL = "volume";

    private static volatile RadioService sInstance;
    private static volatile String sCurrentStationId;
    private static volatile boolean sPlaying;
    private static WeakReference<WebView> sUi;

    private final Handler main = new Handler(Looper.getMainLooper());
    private ExoPlayer player;
    private DefaultHttpDataSource.Factory httpFactory;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    private Station currentStation;
    private List<String> currentUrls;
    private int urlIndex;
    private boolean retriedResolve;
    private float volume = 0.8f;

    public static void setUiWebView(WebView webView) {
        sUi = webView == null ? null : new WeakReference<>(webView);
    }

    public static String getCurrentStationId() {
        return sCurrentStationId;
    }

    public static boolean isPlaying() {
        return sPlaying;
    }

    public static void saveLastStation(Context ctx, String id) {
        if (id == null || id.length() == 0) return;
        ctx.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_LAST, id).apply();
    }

    public static String loadLastStation(Context ctx) {
        return ctx.getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_LAST, null);
    }

    public static void enqueuePlayJson(Context ctx, String json) {
        startWith(ctx, ACTION_PLAY, json, null, -1f);
    }

    public static void enqueuePlayId(Context ctx, String id) {
        startWith(ctx, ACTION_PLAY_ID, null, id, -1f);
    }

    public static void enqueuePause(Context ctx) {
        startWith(ctx, ACTION_PAUSE, null, null, -1f);
    }

    public static void enqueueResume(Context ctx) {
        startWith(ctx, ACTION_RESUME, null, null, -1f);
    }

    public static void enqueueAutoPlay(Context ctx) {
        startWith(ctx, ACTION_AUTO_PLAY, null, null, -1f);
    }

    public static void enqueueVolume(Context ctx, float volume) {
        startWith(ctx, ACTION_SET_VOLUME, null, null, volume);
    }

    private static void startWith(Context ctx, String action, String json, String id, float volume) {
        Intent i = new Intent(ctx, RadioService.class);
        i.setAction(action);
        if (json != null) i.putExtra(EXTRA_STATION_JSON, json);
        if (id != null) i.putExtra(EXTRA_STATION_ID, id);
        if (volume >= 0f) i.putExtra(EXTRA_VOLUME, volume);
        Context app = ctx.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(i);
        } else {
            app.startService(i);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        volume = getSharedPreferences(PREFS, MODE_PRIVATE).getFloat(KEY_VOL, 0.8f);
        createChannel();
        startForegroundNotification("재희미용실 라디오", "코코가 방송을 준비하고 있어요");
        acquireLocks();
        initPlayer();
    }

    private void initPlayer() {
        httpFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(8000)
                .setReadTimeoutMs(8000)
                .setKeepPostFor302Redirects(false);
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(httpFactory))
                .build();
        player.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                true);
        player.setTrackSelectionParameters(
                player.getTrackSelectionParameters()
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                        .build());
        player.setVolume(volume);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY && player.getPlayWhenReady()) {
                    sPlaying = true;
                    notifyUi("playing", null);
                } else if (state == Player.STATE_ENDED) {
                    sPlaying = false;
                    notifyUi("paused", null);
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                sPlaying = isPlaying;
                notifyUi(isPlaying ? "playing" : "paused", null);
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                tryNextOrFail(error);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundNotification(
                currentStation != null ? currentStation.name : "재희미용실 라디오",
                sPlaying ? "코코가 방송을 이어가고 있어요" : "코코가 기다리고 있어요");
        if (intent == null) {
            autoPlayLast();
            return START_STICKY;
        }
        String action = intent.getAction();
        if (ACTION_PLAY.equals(action)) {
            playFromJson(intent.getStringExtra(EXTRA_STATION_JSON));
        } else if (ACTION_PLAY_ID.equals(action) || ACTION_AUTO_PLAY.equals(action)) {
            String id = intent.getStringExtra(EXTRA_STATION_ID);
            if (id == null || id.length() == 0) id = loadLastStation(this);
            if (id != null) playById(id);
        } else if (ACTION_PAUSE.equals(action)) {
            pausePlayback();
        } else if (ACTION_RESUME.equals(action)) {
            resumePlayback();
        } else if (ACTION_SET_VOLUME.equals(action)) {
            if (intent.hasExtra(EXTRA_VOLUME)) setVolume(intent.getFloatExtra(EXTRA_VOLUME, volume));
        }
        return START_STICKY;
    }

    private void playFromJson(String json) {
        Station st = null;
        if (json != null) {
            try {
                org.json.JSONObject o = new org.json.JSONObject(json);
                String id = o.optString("id", "");
                st = StationCatalog.get(id);
                if (st == null && id.length() > 0) {
                    st = new Station(
                            id,
                            o.optString("name", id),
                            o.optString("broadcaster", ""),
                            o.optString("url", null),
                            o.has("kbsCode") ? String.valueOf(o.opt("kbsCode")) : null,
                            null);
                }
            } catch (Exception ignored) {
            }
        }
        if (st == null) {
            notifyUi("error", "방송 정보를 못 읽었어요");
            return;
        }
        resolveAndPlay(st);
    }

    private void playById(String id) {
        Station st = StationCatalog.get(id);
        if (st == null) {
            notifyUi("error", "없는 방송이에요");
            return;
        }
        resolveAndPlay(st);
    }

    private void resolveAndPlay(final Station st) {
        currentStation = st;
        sCurrentStationId = st.id;
        saveLastStation(this, st.id);
        urlIndex = 0;
        retriedResolve = false;
        currentUrls = null;
        startForegroundNotification(st.name, "주소를 찾는 중...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<String> urls = StreamResolver.resolve(st);
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            currentUrls = urls;
                            urlIndex = 0;
                            playCurrentUrl();
                        }
                    });
                } catch (final Exception e) {
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            sPlaying = false;
                            String msg = st.name + " 방송은 지금 연결이 안 돼요.";
                            startForegroundNotification(st.name, msg);
                            notifyUi("error", msg);
                        }
                    });
                }
            }
        }, "coco-resolve").start();
    }

    private void playCurrentUrl() {
        if (player == null || currentUrls == null || urlIndex >= currentUrls.size()) {
            sPlaying = false;
            notifyUi("error", currentStation != null
                    ? currentStation.name + " 방송은 지금 연결이 안 돼요."
                    : "방송 연결이 안 돼요.");
            return;
        }
        String url = currentUrls.get(urlIndex);
        String ua = StreamResolver.userAgentFor(currentStation);
        String referer = StreamResolver.refererFor(currentStation);
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", ua);
        headers.put("Accept", "*/*");
        if (referer != null) headers.put("Referer", referer);
        httpFactory.setUserAgent(ua);
        httpFactory.setDefaultRequestProperties(headers);
        player.setVolume(volume);
        player.stop();
        player.clearMediaItems();
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
        player.prepare();
        player.play();
        startForegroundNotification(
                currentStation != null ? currentStation.name : "재희미용실 라디오",
                "코코가 방송을 이어가고 있어요");
    }

    private void tryNextOrFail(PlaybackException error) {
        if (currentUrls != null && urlIndex + 1 < currentUrls.size()) {
            urlIndex++;
            playCurrentUrl();
            return;
        }
        if (!retriedResolve && currentStation != null) {
            retriedResolve = true;
            resolveAndPlay(currentStation);
            return;
        }
        sPlaying = false;
        String msg = currentStation != null
                ? currentStation.name + " 방송은 지금 연결이 안 돼요."
                : "방송 연결이 안 돼요.";
        startForegroundNotification(
                currentStation != null ? currentStation.name : "재희미용실 라디오", msg);
        notifyUi("error", msg);
    }

    private void pausePlayback() {
        if (player != null) player.pause();
        sPlaying = false;
        notifyUi("paused", null);
        startForegroundNotification(
                currentStation != null ? currentStation.name : "재희미용실 라디오",
                "일시정지");
    }

    private void resumePlayback() {
        if (player != null && player.getPlaybackState() == Player.STATE_READY) {
            player.play();
            return;
        }
        if (currentStation != null) {
            resolveAndPlay(currentStation);
            return;
        }
        autoPlayLast();
    }

    private void autoPlayLast() {
        String id = loadLastStation(this);
        if (id != null && id.length() > 0) playById(id);
    }

    private void setVolume(float v) {
        if (v < 0f) v = 0f;
        if (v > 1f) v = 1f;
        volume = v;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putFloat(KEY_VOL, v).apply();
        if (player != null) player.setVolume(v);
    }

    private void notifyUi(final String type, final String message) {
        final WebView web = sUi == null ? null : sUi.get();
        if (web == null) return;
        final String escaped = message == null ? "" : message
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ");
        final String js = "window.__cocoNativeEvent&&window.__cocoNativeEvent({type:'"
                + type + "',message:'" + escaped + "'})";
        web.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT >= 19) web.evaluateJavascript(js, null);
                    else web.loadUrl("javascript:" + js);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void startForegroundNotification(String title, String text) {
        Intent launch = new Intent(this, MainActivity.class);
        launch.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0, launch, flags);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        builder.setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setContentIntent(pi)
                .setOngoing(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }
        Notification notif = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIF_ID, notif);
        }
    }

    private void acquireLocks() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cocoradio:playback");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire();
        }
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "cocoradio:wifi");
            wifiLock.setReferenceCounted(false);
            wifiLock.acquire();
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Coco Radio", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("네비에서 지도 켜도 소리 계속");
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public void onDestroy() {
        sInstance = null;
        sPlaying = false;
        if (player != null) {
            player.release();
            player = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
