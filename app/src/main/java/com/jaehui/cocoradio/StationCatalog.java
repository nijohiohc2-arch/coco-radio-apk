package com.jaehui.cocoradio;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StationCatalog {
    private static final Map<String, Station> MAP = new LinkedHashMap<>();

    static {
        add("kbs-cool", "KBS Cool FM", "KBS", null, "25", null);
        add("kbs-classic", "KBS 클래식FM", "KBS", null, "24", null);
        add("kbs-happy", "KBS 해피FM", "KBS", null, "22", null);
        add("kbs-1radio", "KBS 1라디오", "KBS", null, "21", null);
        add("kbs-3radio", "KBS 3라디오", "KBS", null, "23", null);
        add("kbs-hanminjok", "KBS 한민족방송", "KBS", null, "26", null);
        add("mbc-fm4u", "MBC FM4U", "MBC", null, null, null);
        add("mbc-standard", "MBC 표준FM", "MBC", null, null, null);
        add("mbc-allthat", "MBC 올댓뮤직", "MBC", null, null, null);
        add("sbs-power", "SBS 파워FM", "SBS", null, null, null);
        add("sbs-love", "SBS 러브FM", "SBS", null, null, null);
        add("cbs-music", "CBS 음악FM", "CBS",
                "https://m-aac.cbs.co.kr/mweb_cbs939/_definst_/cbs939.stream/playlist.m3u8", null, null);
        add("cbs-standard", "CBS 표준FM", "CBS",
                "https://m-aac.cbs.co.kr/mweb_cbs981/_definst_/cbs981.stream/playlist.m3u8", null, null);
        add("ytn-radio", "YTN 라디오", "YTN",
                "https://r2-16r-s10.radio.ytn.co.kr/ytn/fm/live.m3u8", null,
                new String[]{"http://r2-16r-s10.radio.ytn.co.kr/ytn/fm/live.m3u8"});
        add("tbs-fm", "tbs FM", "TBS",
                "https://cdnfm.tbs.seoul.kr/tbs/_definst_/tbs_fm_web_360.smil/chunklist.m3u8", null, null);
        add("tbs-efm", "tbs eFM", "TBS",
                "https://cdnefm.tbs.seoul.kr/tbs/_definst_/tbs_efm_web_360.smil/chunklist.m3u8", null, null);
        add("ebs-fm", "EBS FM", "EBS",
                "https://ebsonair.ebs.co.kr/fmradiofamilypc/familypc1m/playlist.m3u8", null, null);
        add("somafm-groove", "SomaFM Groove Salad", "WORLD",
                "https://ice1.somafm.com/groovesalad-128-mp3", null,
                new String[]{"https://ice2.somafm.com/groovesalad-128-mp3"});
        add("somafm-lush", "SomaFM Lush", "WORLD",
                "https://ice1.somafm.com/lush-128-mp3", null, null);
        add("rp-main", "Radio Paradise", "WORLD",
                "https://stream.radioparadise.com/rock-128", null, null);
        add("rp-mellow", "Radio Paradise Mellow", "WORLD",
                "https://stream.radioparadise.com/mellow-128", null, null);
        add("kexp", "KEXP 90.3", "WORLD",
                "https://kexp-mp3-128.streamguys1.com/kexp128.mp3", null, null);
        add("listenmoe-jpop", "Listen.moe J-Pop", "WORLD",
                "https://listen.moe/stream", null, null);
        add("listenmoe-kpop", "Listen.moe K-Pop", "WORLD",
                "https://listen.moe/kpop/stream", null, null);
    }

    private static void add(String id, String name, String b, String url, String kbs, String[] fb) {
        MAP.put(id, new Station(id, name, b, url, kbs, fb));
    }

    public static Station get(String id) {
        return MAP.get(id);
    }
}
