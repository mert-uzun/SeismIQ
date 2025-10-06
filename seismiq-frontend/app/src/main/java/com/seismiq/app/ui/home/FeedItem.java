package com.seismiq.app.ui.home;

import com.seismiq.app.model.Earthquake;
import com.seismiq.app.model.Report;

import java.util.Date;

/**
 * Wrapper class for feed items that can be either a Report or an Earthquake
 */
public class FeedItem {
    public static final int TYPE_REPORT = 0;
    public static final int TYPE_EARTHQUAKE = 1;

    private final int type;
    private final Report report;
    private final Earthquake earthquake;
    private final Date timestamp;

    private FeedItem(int type, Report report, Earthquake earthquake, Date timestamp) {
        this.type = type;
        this.report = report;
        this.earthquake = earthquake;
        this.timestamp = timestamp;
    }

    public static FeedItem fromReport(Report report) {
        Date timestamp = report.getCreatedAt() != null 
                ? report.getCreatedAt()
                : new Date();
        return new FeedItem(TYPE_REPORT, report, null, timestamp);
    }

    public static FeedItem fromEarthquake(Earthquake earthquake) {
        Date timestamp = earthquake.getTimestamp() != null 
                ? earthquake.getTimestamp()
                : new Date();
        return new FeedItem(TYPE_EARTHQUAKE, null, earthquake, timestamp);
    }

    public int getType() {
        return type;
    }

    public Report getReport() {
        return report;
    }

    public Earthquake getEarthquake() {
        return earthquake;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}

