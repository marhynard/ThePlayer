package com.fritzbang.theplayer;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class XMLHandler {
    private static final String ns = null;
    // private static final String DEBUG_TAG = "XMLHandler";

    String podcastTitle = "";
    String imageLink = "";
    String width = "";
    String height = "";
    List<PodcastEntry> podEntries;

    public void parse(InputStream in) throws XmlPullParserException,
            IOException {
        // Log.d(DEBUG_TAG, "Starting to parse");
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            readRSS(parser);
        } finally {
            in.close();
        }
    }

    private void readRSS(XmlPullParser parser) throws XmlPullParserException,
            IOException {

        // Log.d(DEBUG_TAG, "Starting to parse feed");
        parser.require(XmlPullParser.START_TAG, ns, "rss");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for Item tag
            if (name.equals("channel")) {
                podEntries = readChannel(parser);
            } else {
                skip(parser);
            }
        }
    }

    private List<PodcastEntry> readChannel(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        List<PodcastEntry> entries = new ArrayList<PodcastEntry>();
        // Log.d(DEBUG_TAG, "Starting to parse channel");
        parser.require(XmlPullParser.START_TAG, ns, "channel");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Log.d(DEBUG_TAG, name);
            // Starts by looking for Item tag
            if (name.equals("title")) {
                this.podcastTitle = readTitle(parser);
            } else if (name.equals("image")) {
                readImage(parser);
            } else if (name.equals("itunes:image")) {
                readItunesImage(parser);
            } else if (name.equals("item")) {
                entries.add(readItem(parser));

            } else {

                skip(parser);
            }
        }

        return entries;
    }

    private PodcastEntry readItem(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "item");

        String enclosure[] = new String[2];
        String pubDate = "";
        String title = "";
        // Log.d(DEBUG_TAG, "Starting to parse item");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for Item tag
            if (name.equals("title")) {
                title = readTitle(parser);
            } else if (name.equals("enclosure")) {
                enclosure = readEnclosure(parser);

            } else if (name.equals("pubDate")) {
                pubDate = readPubDate(parser);

            } else {

                skip(parser);
            }
        }
        // Log.d(DEBUG_TAG, "item parsed");
        return new PodcastEntry(title, pubDate, enclosure);

    }

    private String readPubDate(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        // Log.d(DEBUG_TAG, "Starting to parse pubDate");
        parser.require(XmlPullParser.START_TAG, ns, "pubDate");
        String pubDate = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "pubDate");
        // Log.d(DEBUG_TAG, "pubDate: " + pubDate);
        return pubDate;
    }

    private String[] readEnclosure(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        // Log.d(DEBUG_TAG, "Starting to parse enclosure");
        String[] linkInfo = new String[2];
        parser.require(XmlPullParser.START_TAG, ns, "enclosure");
        linkInfo[0] = parser.getAttributeValue(null, "url");
        // Log.d(DEBUG_TAG, "Starting to parse enclosure: " + linkInfo[0]);
        linkInfo[1] = parser.getAttributeValue(null, "length");
        // Log.d(DEBUG_TAG, "Starting to parse enclosure: " + linkInfo[1]);
        parser.nextTag();
        parser.require(XmlPullParser.END_TAG, ns, "enclosure");
        return linkInfo;
    }

    private void readImage(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        parser.require(XmlPullParser.START_TAG, ns, "image");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("url")) {
                imageLink = readUrl(parser);
            } else if (name.equals("width")) {
                width = readWidth(parser);
            } else if (name.equals("height")) {
                height = readHeight(parser);
            } else {
                skip(parser);
            }
        }

    }

    private void readItunesImage(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "itunes:image");

        imageLink = parser.getAttributeValue(null, "href");

        parser.nextTag();
        parser.require(XmlPullParser.END_TAG, ns, "itunes:image");

    }

    private String readHeight(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "height");
        String height = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "height");
        return height;
    }

    private String readWidth(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "width");
        String width = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "width");
        return width;
    }

    private String readUrl(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        parser.require(XmlPullParser.START_TAG, ns, "url");
        String url = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "url");
        return url;
    }

    private String readTitle(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        // Log.d(DEBUG_TAG, "Reading title");
        parser.require(XmlPullParser.START_TAG, ns, "title");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "title");
        // Log.d(DEBUG_TAG, "title: " + title);
        return title;

    }

    private String readText(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }

    }

}
