package com.jaehui.cocoradio;

public final class Station {
    public final String id;
    public final String name;
    public final String broadcaster;
    public final String url;
    public final String kbsCode;
    public final String[] fallbacks;

    public Station(String id, String name, String broadcaster, String url, String kbsCode, String[] fallbacks) {
        this.id = id;
        this.name = name;
        this.broadcaster = broadcaster;
        this.url = url;
        this.kbsCode = kbsCode;
        this.fallbacks = fallbacks;
    }
}
