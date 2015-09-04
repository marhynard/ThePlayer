package com.fritzbang.theplayer;

public class PodcastEntry {

    public String title;
    public String pubDate;
    public String link;
    public String length;

    public PodcastEntry(String title, String pubDate, String[] enclosure) {
        this.title = title;
        this.pubDate = pubDate;
        this.link = enclosure[0];
        this.length = enclosure[1];
    }

}
