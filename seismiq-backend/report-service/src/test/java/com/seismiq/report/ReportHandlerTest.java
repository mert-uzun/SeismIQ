package com.seismiq.report;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.seismiq.common.model.Category;
import com.seismiq.common.model.Report;
import com.seismiq.common.model.User;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ReportHandlerTest {
    private ReportHandler handler;
    private Gson gson;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
        handler = new ReportHandler(reportRepository);
    }

    @Test
    void testCreateReport() {
        // Create a sample report
        String reportId = UUID.randomUUID().toString();
        User user = new User("test-user", "Test User", "Test Address", true, false);
        Report report = new Report();
        report.setUser(user);
        report.setCategory(new Category(UUID.randomUUID().toString(), "medical"));
        report.setDescription("Test medical help needed");
        report.setLocation("Test Location");
        report.setCurrentLocation(true);

        // Mock repository behavior
        when(reportRepository.createReport(any(Report.class))).thenAnswer(invocation -> {
            Report savedReport = invocation.getArgument(0);
            savedReport.setReportId(reportId);
            savedReport.setStatus(Report.ReportStatus.PENDING);
            savedReport.setTimestamp(LocalDateTime.now());
            return savedReport;
        });

        // Create API Gateway request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
            .withPath("/reports")
            .withHttpMethod("POST")
            .withBody(gson.toJson(report));

        // Test the handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Verify response
        assertEquals(201, response.getStatusCode());
        Report createdReport = gson.fromJson(response.getBody(), Report.class);
        assertNotNull(createdReport.getReportId());
        assertEquals(Report.ReportStatus.PENDING, createdReport.getStatus());
        assertNotNull(createdReport.getTimestamp());

        // Verify repository was called
        verify(reportRepository).createReport(any(Report.class));
    }

    @Test
    void testGetReport() {
        // Create a sample report
        String reportId = UUID.randomUUID().toString();
        User user = new User("test-user", "Test User", "Test Address", true, false);
        Report report = new Report();
        report.setReportId(reportId);
        report.setUser(user);
        report.setCategory(new Category(UUID.randomUUID().toString(), "medical"));
        report.setDescription("Test medical help needed");
        report.setLocation("Test Location");
        report.setCurrentLocation(true);
        report.setStatus(Report.ReportStatus.PENDING);
        report.setTimestamp(LocalDateTime.now());

        // Mock repository behavior
        when(reportRepository.getReport(reportId)).thenReturn(report);

        // Create API Gateway request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
            .withPath("/reports/" + reportId)
            .withHttpMethod("GET");

        // Test the handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Verify response
        assertEquals(200, response.getStatusCode());
        Report retrievedReport = gson.fromJson(response.getBody(), Report.class);
        assertEquals(reportId, retrievedReport.getReportId());

        // Verify repository was called
        verify(reportRepository).getReport(reportId);
    }

    @Test
    void testUpdateReportStatus() {
        // Create a sample report
        String reportId = UUID.randomUUID().toString();
        User user = new User("test-user", "Test User", "Test Address", true, false);
        Report report = new Report();
        report.setReportId(reportId);
        report.setUser(user);
        report.setCategory(new Category(UUID.randomUUID().toString(), "medical"));
        report.setDescription("Test medical help needed");
        report.setLocation("Test Location");
        report.setCurrentLocation(true);
        report.setStatus(Report.ReportStatus.PENDING);
        report.setTimestamp(LocalDateTime.now().minusMinutes(5));

        // Mock repository behavior for getReport
        when(reportRepository.getReport(reportId)).thenReturn(report);

        // Mock repository behavior for updateReportStatus
        when(reportRepository.updateReportStatus(eq(reportId), any(Report.ReportStatus.class)))
            .thenAnswer(invocation -> {
                Report updatedReport = new Report(report);
                updatedReport.setStatus(invocation.getArgument(1));
                updatedReport.setLastUpdated(LocalDateTime.now());
                return updatedReport;
            });

        // Create status update request
        Map<String, String> statusUpdate = new HashMap<>();
        statusUpdate.put("status", "IN_PROGRESS");

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
            .withPath("/reports/" + reportId + "/status")
            .withHttpMethod("PUT")
            .withBody(gson.toJson(statusUpdate));

        // Test the handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Verify response
        assertEquals(200, response.getStatusCode());
        Report updatedReport = gson.fromJson(response.getBody(), Report.class);
        assertEquals(Report.ReportStatus.IN_PROGRESS, updatedReport.getStatus());
        assertTrue(updatedReport.getLastUpdated().isAfter(updatedReport.getTimestamp()));

        // Verify repository calls
        verify(reportRepository).getReport(reportId);
        verify(reportRepository).updateReportStatus(eq(reportId), eq(Report.ReportStatus.IN_PROGRESS));
    }

    @Test
    void testGetNonExistentReport() {
        // Mock repository behavior
        when(reportRepository.getReport(any())).thenReturn(null);

        // Create API Gateway request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
            .withPath("/reports/non-existent-id")
            .withHttpMethod("GET");

        // Test the handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Verify response
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("Report not found"));

        // Verify repository was called
        verify(reportRepository).getReport("non-existent-id");
    }

    @Test
    void testUpdateNonExistentReportStatus() {
        // Mock repository behavior
        when(reportRepository.getReport(any())).thenReturn(null);

        // Create status update request
        Map<String, String> statusUpdate = new HashMap<>();
        statusUpdate.put("status", "IN_PROGRESS");

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
            .withPath("/reports/non-existent-id/status")
            .withHttpMethod("PUT")
            .withBody(gson.toJson(statusUpdate));

        // Test the handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Verify response
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("Report not found"));

        // Verify repository was called
        verify(reportRepository).getReport("non-existent-id");
        verify(reportRepository, never()).updateReportStatus(any(), any());
    }

    @Test
    void testUpdateReportStatusInvalidStatus() {
        // Create a sample report
        String reportId = UUID.randomUUID().toString();
        Report report = new Report();
        report.setReportId(reportId);
        report.setStatus(Report.ReportStatus.PENDING);

        // Mock repository behavior
        when(reportRepository.getReport(reportId)).thenReturn(report);

        // Create status update request with invalid status
        Map<String, String> statusUpdate = new HashMap<>();
        statusUpdate.put("status", "INVALID_STATUS");

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
            .withPath("/reports/" + reportId + "/status")
            .withHttpMethod("PUT")
            .withBody(gson.toJson(statusUpdate));

        // Test the handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Verify response
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid status"));

        // Verify repository calls
        verify(reportRepository).getReport(reportId);
        verify(reportRepository, never()).updateReportStatus(any(), any());
    }
}
