package com.seismiq.common.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

class ReportTest {
    @Test
    void testReportCreation() {
        String reportId = "test-id";
        User user = new User("user-1", "Test User", "Test Address", false, false);
        String description = "Test description";
        String location = "Test location";
        LocalDateTime now = LocalDateTime.now();
        Category category = new Category(UUID.randomUUID().toString(), "medical");

        Report report = new Report(reportId, user, category,
                                 description, location, true, Report.ReportStatus.PENDING, now);

        assertEquals(reportId, report.getReportId());
        assertEquals(user, report.getUser());
        assertEquals(category, report.getCategory());
        assertEquals(description, report.getDescription());
        assertEquals(location, report.getLocation());
        assertTrue(report.isCurrentLocation());
        assertEquals(Report.ReportStatus.PENDING, report.getStatus());
        assertEquals(now, report.getTimestamp());
        assertEquals(now, report.getLastUpdated());
    }

    @Test
    void testStatusUpdate() {
        Report report = new Report();
        LocalDateTime before = LocalDateTime.now();
        report.setStatus(Report.ReportStatus.IN_PROGRESS);
        LocalDateTime after = LocalDateTime.now();

        assertEquals(Report.ReportStatus.IN_PROGRESS, report.getStatus());
        assertNotNull(report.getLastUpdated());
        assertTrue(report.getLastUpdated().isAfter(before) || report.getLastUpdated().equals(before));
        assertTrue(report.getLastUpdated().isBefore(after) || report.getLastUpdated().equals(after));
    }
}
