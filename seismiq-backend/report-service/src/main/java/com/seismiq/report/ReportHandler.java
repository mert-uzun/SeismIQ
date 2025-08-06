package com.seismiq.report;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.seismiq.common.model.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ReportHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final ReportRepository reportRepository;
    private final Gson gson;

    public ReportHandler() {
        this.reportRepository = new ReportRepository();
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String path = input.getPath();
        String httpMethod = input.getHttpMethod();

        if ("/reports".equals(path) && "POST".equals(httpMethod)) {
            return createReport(input);
        } else if (path.startsWith("/reports/") && "GET".equals(httpMethod)) {
            String reportId = path.substring("/reports/".length());
            return getReport(reportId);
        } else if (path.startsWith("/users/") && path.endsWith("/reports") && "GET".equals(httpMethod)) {
            String userId = path.substring("/users/".length(), path.length() - "/reports".length());
            return getReportsByUser(userId);
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withBody("Not Found");
    }

    private APIGatewayProxyResponseEvent createReport(APIGatewayProxyRequestEvent input) {
        try {
            Report report = gson.fromJson(input.getBody(), Report.class);
            report.setReportId(UUID.randomUUID().toString());
            report.setTimestamp(LocalDateTime.now());
            if (report.getStatus() == null) {
                report.setStatus(Report.ReportStatus.PENDING);
            }
            
            reportRepository.saveReport(report);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    .withBody(gson.toJson(report));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error creating report: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent getReport(String reportId) {
        try {
            Report report = reportRepository.getReport(reportId);
            if (report == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withBody("Report not found");
            }
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(report));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error retrieving report: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent getReportsByUser(String userId) {
        try {
            List<Report> reports = reportRepository.getReportsByUser(userId);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(reports));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error retrieving reports: " + e.getMessage());
        }
    }
}