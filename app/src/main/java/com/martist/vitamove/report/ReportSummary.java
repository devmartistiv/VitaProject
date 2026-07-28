package com.martist.vitamove.report;

public class ReportSummary {
    private final String id;
    private final String title;
    private final String subtitle;
    private final String content;
    private final long createdAt;

    public ReportSummary(String id, String title, String subtitle, String content, long createdAt) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getContent() {
        return content;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
