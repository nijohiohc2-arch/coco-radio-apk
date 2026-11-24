package com.jaehui.cocoradio;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves tokenized KBS/MBC/SBS stream URLs over HttpURLConnection.
 * Does not send an Origin header (broadcasters often block browser Origins).
 */
public final class StreamResolver {
    private static final int TIMEOUT_MS = 8000;
    private static final Pattern URL_IN_TEXT = Pattern.compile("https?://\\S+");

    public static final String UA_MOBILE =
            "Mozilla/5.0 (Linux; Android 10; SM-G973N) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 CocoRadio/1.3";
    public static final String UA_SBS =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_16_0) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) GOREALRA/1.2.1 Chrome/85.0.4183.121 "
                    + "Electron/10.1.3 Safari/537.36";

    private StreamResolver() {}

    public static List<String> resolve(Station st) throws Exception {
        ArrayList<String> urls = new ArrayList<>();
        if (st == null) throw new Exception("방송 정보가 없어요");

        if ("KBS".equals(st.broadcaster) && st.kbsCode != null) {
            addAll(urls, resolveKbs(st.kbsCode));
        } else if ("SBS".equals(st.broadcaster)) {
            add(urls, resolveSbs(st.id));
        } else if ("MBC".equals(st.broadcaster)) {
            add(urls, resolveMbc(st.id));
        }
        add(urls, st.url);
        if (st.fallbacks != null) {
            for (int i = 0; i < st.fallbacks.length; i++) add(urls, st.fallbacks[i]);
        }
        if (urls.isEmpty()) throw new Exception("주소가 없어요");
        return urls;
    }

    public static String userAgentFor(Station st) {
        if (st != null && "SBS".equals(st.broadcaster)) return UA_SBS;
        return UA_MOBILE;
    }

    public static String refererFor(Station st) {
        if (st == null) return null;
        if ("KBS".equals(st.broadcaster)) return "https://onair.kbs.co.kr/";
        if ("SBS".equals(st.broadcaster)) return "https://gorealraplayer.radio.sbs.co.kr/main.html?v=1.2.1";
        if ("MBC".equals(st.broadcaster)) return "https://mini.imbc.com/";
        return null;
    }

    private static List<String> resolveKbs(String code) throws Exception {
        String[] apis = new String[] {
                "https://cfpwwwapi.kbs.co.kr/api/v1/landing/live/channel_code/" + code,
                "https://pwwwapi.kbs.co.kr/api/v1/landing/live/channel_code/" + code
        };
        Exception last = null;
        for (int a = 0; a < apis.length; a++) {
            try {
                String body = httpGet(apis[a], UA_MOBILE, "https://onair.kbs.co.kr/");
                List<String> parsed = parseKbs(body);
                if (!parsed.isEmpty()) return parsed;
            } catch (Exception e) {
                last = e;
            }
        }
        if (last != null) throw last;
        throw new Exception("KBS 해석 실패");
    }

    private static List<String> parseKbs(String body) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONArray items = root.optJSONArray("channel_item");
        if (items == null) {
            JSONObject ch = root.optJSONObject("channel");
            if (ch != null) items = ch.optJSONArray("item");
        }
        if (items == null) {
            JSONObject data = root.optJSONObject("data");
            if (data != null) {
                JSONObject ch = data.optJSONObject("channel");
                if (ch != null) items = ch.optJSONArray("item");
            }
        }
        ArrayList<String> radio = new ArrayList<>();
        ArrayList<String> other = new ArrayList<>();
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject it = items.optJSONObject(i);
                if (it == null) continue;
                String u = it.optString("service_url", "");
                if (u == null || u.length() < 8) continue;
                String media = (it.optString("media_type", "") + " "
                        + it.optString("type", "")).toLowerCase(Locale.US);
                boolean isRadio = media.contains("radio") && !media.contains("bora");
                if (isRadio || u.contains("_192_") || u.contains("_128_")) radio.add(u);
                else other.add(u);
            }
        }
        radio.addAll(other);
        return radio;
    }

    private static String resolveSbs(String id) throws Exception {
        String path;
        if ("sbs-power".equals(id)) path = "powerpc/powerfm";
        else if ("sbs-love".equals(id)) path = "lovepc/lovefm";
        else throw new Exception("SBS 채널이 없어요");
        String api = "https://apis.sbs.co.kr/play-api/1.0/livestream/"
                + path + "?protocol=hls&ssl=Y";
        String body = httpGet(api, UA_SBS, "https://gorealraplayer.radio.sbs.co.kr/main.html?v=1.2.1");
        return firstUrl(body);
    }

    private static String resolveMbc(String id) throws Exception {
        String ch;
        if ("mbc-fm4u".equals(id)) ch = "mfm";
        else if ("mbc-standard".equals(id)) ch = "sfm";
        else if ("mbc-allthat".equals(id)) ch = "chm";
        else throw new Exception("MBC 채널이 없어요");
        String api = "https://sminiplay.imbc.com/aacplay.ashx?agent=webapp&channel=" + ch;
        String body = httpGet(api, UA_MOBILE, "https://mini.imbc.com/");
        return firstUrl(body);
    }

    private static String firstUrl(String body) throws Exception {
        if (body == null) throw new Exception("빈 응답");
        String trimmed = body.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed.split("\\s+")[0].replace("\"", "").replaceAll("[>,;]+$", "");
        }
        Matcher m = URL_IN_TEXT.matcher(trimmed);
        if (m.find()) {
            return m.group(0).replace("\"", "").replaceAll("[>,;]+$", "");
        }
        throw new Exception("스트림 주소를 못 찾았어요");
    }

    private static String httpGet(String url, String ua, String referer) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", ua);
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8");
            conn.setRequestProperty("Accept-Encoding", "identity");
            if (referer != null) conn.setRequestProperty("Referer", referer);
            // Intentionally no Origin header.
            int code = conn.getResponseCode();
            InputStream in = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (in == null) throw new Exception("HTTP " + code);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            int total = 0;
            while ((n = in.read(buf)) != -1) {
                bos.write(buf, 0, n);
                total += n;
                if (total > 1024 * 1024) break;
            }
            in.close();
            if (code < 200 || code >= 400) throw new Exception("HTTP " + code);
            return new String(bos.toByteArray(), "UTF-8");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void add(List<String> list, String url) {
        if (url != null && url.length() > 5 && !list.contains(url)) list.add(url);
    }

    private static void addAll(List<String> list, List<String> more) {
        if (more == null) return;
        for (int i = 0; i < more.size(); i++) add(list, more.get(i));
    }
}
