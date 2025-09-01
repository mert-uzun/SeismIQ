package com.seismiq.report;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.seismiq.common.model.Landmark;
import com.seismiq.common.model.Report;
import com.seismiq.common.model.LandmarkCategory;
import com.seismiq.landmark-service.LandmarkRepository;
import com.seismiq.common.util.LocalDateTimeAdapter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for processing report-related API requests in the SeismIQ system.
 * This class implements AWS Lambda's RequestHandler interface to handle API Gateway events
 * for managing disaster response reports.
 *
 * @author Sıla Bozkurt
 */
public class ReportHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final ReportRepository reportRepository;
    private final LandmarkRepository landmarkRepository;
    private final Gson gson;

    /**
     * Default constructor that initializes with a new ReportRepository instance.
     */
    public ReportHandler() {
        this(new ReportRepository(), new LandmarkRepository());
    }

    /**
     * Constructor with dependency injection support for testing.
     * 
     * @param reportRepository The repository implementation for report data operations
     * @param landmarkRepository The repository implementation for landmark operations
     */
    public ReportHandler(ReportRepository reportRepository, LandmarkRepository landmarkRepository) {
        this.reportRepository = reportRepository;
        this.landmarkRepository = landmarkRepository;
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    }

    /**
     * Main entry point for handling API Gateway requests.
     * Routes requests to appropriate handlers based on HTTP method and path.
     * 
     * @param input The API Gateway request event containing HTTP method, path, and body
     * @param context The Lambda execution context
     * @return API Gateway response with appropriate status code and body
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String path = input.getPath();
        String httpMethod = input.getHttpMethod();

        if (path.equals("/reports")) {
            switch (httpMethod) {
                case "POST":
                    return createReport(input);
                case "GET":
                    return listReports(input);
                default:
                    return notFound();
            }
        }

        if (path.matches("/reports/[^/]+")) {
            String reportId = path.substring("/reports/".length());
            switch (httpMethod) {
                case "GET":
                    return getReport(reportId);
                case "PUT":
                    return updateReport(reportId, input);
                case "DELETE":
                    return deleteReport(reportId);
                default:
                    return notFound();
            }
        }

        if (path.matches("/reports/[^/]+/status")) {
            String reportId = path.substring("/reports/".length(), path.lastIndexOf("/status"));
            return httpMethod.equals("PUT") ? updateReportStatus(reportId, input) : notFound();
        }

        if (path.matches("/reports/[^/]+/location")) {
            String reportId = path.substring("/reports/".length(), path.lastIndexOf("/location"));
            return httpMethod.equals("PUT") ? updateReportLocation(reportId, input) : notFound();
        }

        if (path.matches("/users/[^/]+/reports")) {
            String userId = path.substring("/users/".length(), path.length() - "/reports".length());
            return httpMethod.equals("GET") ? getReportsByUser(userId) : notFound();
        }

        if (path.matches("/reports/category/[^/]+")) {
            String category = path.substring("/reports/category/".length());
            return httpMethod.equals("GET") ? getReportsByCategory(category) : notFound();
        }

        if (path.matches("/reports/status/[^/]+")) {
            String status = path.substring("/reports/status/".length());
            return httpMethod.equals("GET") ? getReportsByStatus(status) : notFound();
        }

        return notFound();
    }

    /**
     * Creates a new report in the system.
     * Processes POST requests to /reports endpoint.
     * 
     * @param input API Gateway request containing the report data in JSON format
     * @return 201 Created on success with the created report
     *         400 Bad Request if the input is invalid
     */
    private APIGatewayProxyResponseEvent createReport(APIGatewayProxyRequestEvent input) {
        try {
            Report report = gson.fromJson(input.getBody(), Report.class);
            report.setReportId(UUID.randomUUID().toString());
            report.setStatus(Report.ReportStatus.PENDING);
            report.setTimestamp(LocalDateTime.now());

            reportRepository.createReport(report);
            createLandmarkFromReport(report);

            return new APIGatewayProxyResponseEvent()
                .withStatusCode(201)
                .withBody(gson.toJson(report));
        } catch (JsonSyntaxException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody("Invalid report format: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody("Invalid report data: " + e.getMessage());
        }
    }

    /**
     * Lists reports with optional time-based filtering.
     * Processes GET requests to /reports endpoint.
     * Supports time range filtering through query parameters:
     * - timeRange=today
     * - timeRange=week
     * - timeRange=month
     * - timeRange=year
     * 
     * @param input API Gateway request with optional time range query parameter
     * @return 200 OK with list of reports
     *         400 Bad Request if the time range is invalid
     */
    private APIGatewayProxyResponseEvent listReports(APIGatewayProxyRequestEvent input) {
        try {
            Map<String, String> queryParams = input.getQueryStringParameters();
            List<Report> reports;

            if (queryParams != null && queryParams.containsKey("timeRange")) {
                String timeRange = queryParams.get("timeRange");
                LocalDateTime startTime = calculateStartTime(timeRange);
                reports = reportRepository.getReportsByTimeRange(startTime, LocalDateTime.now());
            } else {
                reports = reportRepository.getAllReports();
            }

            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(gson.toJson(reports));
        } catch (IllegalArgumentException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody("Invalid time range: " + e.getMessage());
        }
    }

    /**
     * Retrieves a specific report by its ID.
     * Processes GET requests to /reports/{reportId} endpoint.
     * 
     * @param reportId The unique identifier of the report to retrieve
     * @return 200 OK with the report details
     *         404 Not Found if the report doesn't exist
     */
    private APIGatewayProxyResponseEvent getReport(String reportId) {
        Report report = reportRepository.getReport(reportId);
        if (report == null) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withBody("Report not found");
        }

        return new APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withBody(gson.toJson(report));
    }

    /**
     * Updates an existing report's details.
     * Processes PUT requests to /reports/{reportId} endpoint.
     * Preserves the report ID and updates timestamp while updating other fields.
     * 
     * @param reportId The unique identifier of the report to update
     * @param input API Gateway request containing the updated report data
     * @return 200 OK with the updated report
     *         404 Not Found if the report doesn't exist
     *         400 Bad Request if the input is invalid
     */
    private APIGatewayProxyResponseEvent updateReport(String reportId, APIGatewayProxyRequestEvent input) {
        try {
            Report existingReport = reportRepository.getReport(reportId);
            if (existingReport == null) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(404)
                    .withBody("Report not found");
            }

            Report updatedReport = gson.fromJson(input.getBody(), Report.class);
            updatedReport.setReportId(reportId);
            updatedReport.setLastUpdated(LocalDateTime.now());
            
            reportRepository.saveReport(updatedReport);

            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(gson.toJson(updatedReport));
        } catch (JsonSyntaxException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody("Invalid report format: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody("Invalid report data: " + e.getMessage());
        }
    }

    /**
     * Deletes a report from the system.
     * Processes DELETE requests to /reports/{reportId} endpoint.
     * 
     * @param reportId The unique identifier of the report to delete
     * @return 204 No Content on successful deletion
     *         404 Not Found if the report doesn't exist
     */
    private APIGatewayProxyResponseEvent deleteReport(String reportId) {
        Report report = reportRepository.getReport(reportId);
        if (report == null) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withBody("Report not found");
        }

        reportRepository.deleteReport(reportId);
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(204)
            .withBody("");
    }

    /**
     * Updates the status of a report.
     * Processes PUT requests to /reports/{reportId}/status endpoint.
     * Accepts a JSON object with a 'status' field containing the new status.
     * 
     * @param reportId The unique identifier of the report
     * @param input API Gateway request containing the new status
     * @return 200 OK with the updated report
     *         404 Not Found if the report doesn't exist
     *         400 Bad Request if the status is invalid or missing
     */
    private APIGatewayProxyResponseEvent updateReportStatus(String reportId, APIGatewayProxyRequestEvent input) {
        try {
            Report report = reportRepository.getReport(reportId);
            if (report == null) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(404)
                    .withBody("Report not found");
            }

            @SuppressWarnings("unchecked")
            Map<String, String> statusUpdate = gson.fromJson(input.getBody(), Map.class);
            String newStatus = statusUpdate.get("status");
            
            if (newStatus == null) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Status not provided");
            }

            try {
                Report.ReportStatus status = Report.ReportStatus.valueOf(newStatus.toUpperCase());
                Report updatedReport = reportRepository.updateReportStatus(reportId, status);
                
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(updatedReport));
            } catch (IllegalArgumentException e) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Invalid status: " + newStatus);
            }
        } catch (JsonSyntaxException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody("Invalid JSON format: " + e.getMessage());
        }
    }

    /**
     * Retrieves all reports associated with a specific user.
     * Processes GET requests to /users/{userId}/reports endpoint.
     * 
     * @param userId The unique identifier of the user
     * @return 200 OK with list of user's reports
     *         400 Bad Request if the user ID is invalid
     */
    private APIGatewayProxyResponseEvent getReportsByUser(String userId) {
        try {
            List<Report> reports = reportRepository.getReportsByUser(userId);
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(gson.toJson(reports));
        } catch (IllegalArgumentException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody("Invalid user ID: " + e.getMessage());
        }
    }

    /**
     * Retrieves reports filtered by category.
     * Processes GET requests to /reports/category/{category} endpoint.
     * Category values are case-insensitive.
     * 
     * @param categoryStr The category to filter by (e.g., MEDICAL_HELP, SHELTER, etc.)
     * @return 200 OK with list of reports in the category
     *         400 Bad Request if the category is invalid
     */
    private APIGatewayProxyResponseEvent getReportsByCategory(String categoryStr) {
        try {
            Report.ReportCategory reportCategory = Report.ReportCategory.valueOf(categoryStr.toUpperCase());
            List<Report> reports = reportRepository.getReportsByCategory(reportCategory);
            
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(gson.toJson(reports));
        } catch (IllegalArgumentException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody("Invalid category: " + categoryStr);
        }
    }

    /**
     * Retrieves reports filtered by status.
     * Processes GET requests to /reports/status/{status} endpoint.
     * Status values are case-insensitive.
     * 
     * @param statusStr The status to filter by (e.g., PENDING, IN_PROGRESS, COMPLETED, etc.)
     * @return 200 OK with list of reports with the specified status
     *         400 Bad Request if the status is invalid
     */
    private APIGatewayProxyResponseEvent getReportsByStatus(String statusStr) {
        try {
            Report.ReportStatus reportStatus = Report.ReportStatus.valueOf(statusStr.toUpperCase());
            List<Report> reports = reportRepository.getReportsByStatus(reportStatus);
            
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(gson.toJson(reports));
        } catch (IllegalArgumentException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody("Invalid status: " + statusStr);
        }
    }

    /**
     * Updates the location information for a report.
     * Processes PUT requests to /reports/{reportId}/location endpoint.
     * 
     * @param reportId The unique identifier of the report
     * @param input API Gateway request containing the new location data
     * @return 501 Not Implemented
     */
    private APIGatewayProxyResponseEvent updateReportLocation(String reportId, APIGatewayProxyRequestEvent input) {
        try {
            Report report = reportRepository.getReport(reportId);
            if (report == null) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(404)
                    .withBody("Report not found");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> locationUpdate = gson.fromJson(input.getBody(), Map.class);
            
            // Validate required location fields
            if (!locationUpdate.containsKey("latitude") || !locationUpdate.containsKey("longitude")) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Both latitude and longitude must be provided");
            }

            // Parse and validate coordinates
            try {
                double latitude = ((Number) locationUpdate.get("latitude")).doubleValue();
                double longitude = ((Number) locationUpdate.get("longitude")).doubleValue();

                // Basic coordinate validation
                if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                    return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Invalid coordinates: latitude must be between -90 and 90, longitude between -180 and 180");
                }

                String description = locationUpdate.containsKey("description") ? 
                    (String) locationUpdate.get("description") : null;

                Report updatedReport = reportRepository.updateReportLocation(reportId, latitude, longitude, description);

                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(updatedReport));

            } catch (ClassCastException | NullPointerException e) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Invalid coordinate format: latitude and longitude must be numeric values");
            }

        } catch (JsonSyntaxException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody("Invalid JSON format: " + e.getMessage());
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withBody("Internal server error: " + e.getMessage());
        }
    }

    /**
     * Calculates the start time for time-based report filtering.
     * Supports predefined time ranges: today, week, month, year.
     * Defaults to last 24 hours if the range is not recognized.
     * 
     * @param timeRange The time range identifier (today, week, month, year)
     * @return LocalDateTime representing the start of the time range
     */
    private LocalDateTime calculateStartTime(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        return switch (timeRange.toLowerCase()) {
            case "today" -> now.toLocalDate().atStartOfDay();
            case "week" -> now.minusWeeks(1);
            case "month" -> now.minusMonths(1);
            case "year" -> now.minusYears(1);
            default -> now.minusDays(1); // Default to last 24 hours
        };
    }

    /**
     * Helper method to create a standardized 404 Not Found response.
     * Used when a requested resource is not found or an endpoint doesn't exist.
     * 
     * @return API Gateway response with 404 status code
     */
    private APIGatewayProxyResponseEvent notFound() {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(404)
            .withBody("Not Found");
    }
    private void createLandmarkFromReport(Report report) {
        if (report.getCategory() == Report.ReportCategory.SHELTER ||
            report.getCategory() == Report.ReportCategory.MEDICAL_HELP ||
            report.getCategory() == Report.ReportCategory.FOOD_WATER) {
            
            // Create request payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", "Emergency " + report.getCategory().name());
            payload.put("location", report.getLocation());
            payload.put("category", convertReportCategoryToLandmarkCategory(report.getCategory()));
            payload.put("reportId", report.getReportId());
            payload.put("userId", report.getUser().getUserId());
            payload.put("latitude", report.getLatitude());
            payload.put("longitude", report.getLongitude());
            
            // Invoke Lambda function
            invokeLandmarkLambda(payload);
        }
    }

    private void invokeLandmarkLambda(Map<String, Object> payload) {
        try {
            // Get the Lambda function name from environment variable
            String functionName = System.getenv("LANDMARK_FUNCTION_NAME");
            if (functionName == null) {
                functionName = "seismiq-LandmarkFunction";
            }
            
            // Create Lambda client
            AWSLambda lambdaClient = AWSLambdaClientBuilder.standard().build();
            
            // Convert payload to JSON
            String payloadJson = new Gson().toJson(payload);
            
            // Invoke Lambda function
            InvokeRequest request = new InvokeRequest()
                .withFunctionName(functionName)
                .withPayload(payloadJson);
            
            lambdaClient.invoke(request);
        } catch (Exception e) {
            // Log error but don't fail the report creation
            System.err.println("Failed to create landmark: " + e.getMessage());
        }
    }

    private Landmark.LandmarkCategory convertReportCategoryToLandmarkCategory(Report.ReportCategory category) {
        return switch (category) {
            case SHELTER -> Landmark.LandmarkCategory.SHELTER;
            case MEDICAL_HELP -> Landmark.LandmarkCategory.MEDICAL_STATION;
            case FOOD_WATER -> Landmark.LandmarkCategory.FOOD_DISTRIBUTION;
            default -> Landmark.LandmarkCategory.OTHER;
        };
    }
}
