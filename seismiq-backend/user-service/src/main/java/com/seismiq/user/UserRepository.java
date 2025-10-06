package com.seismiq.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.seismiq.common.model.User;
import com.seismiq.common.repository.DynamoDBRepository;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

/**
 * Repository class for managing user data in DynamoDB.
 * Handles CRUD operations for user records in the SeismIQ system.
 * 
 * Existing methods maintained by Sıla Bozkurt.
 * Additional methods added by Ayşe Ece Bilgi.
 *
 * @author Sıla Bozkurt
 * @author Ayşe Ece Bilgi
 */
public class UserRepository extends DynamoDBRepository {
    public UserRepository() {
        super("seismiq-Users"); // Use CloudFormation managed table
    }

    public void saveUser(User user) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.builder().s(user.getUserId()).build());
        item.put("name", AttributeValue.builder().s(user.getName()).build());
        item.put("address", AttributeValue.builder().s(user.getAddress()).build());
        item.put("isVolunteer", AttributeValue.builder().bool(user.isVolunteer()).build());
        item.put("isSocialWorker", AttributeValue.builder().bool(user.isSocialWorker()).build());
        if (user.getEmail() != null) 
            item.put("email", AttributeValue.builder().s(user.getEmail()).build());
        if (user.getPasswordHash() != null) 
            item.put("passwordHash", AttributeValue.builder().s(user.getPasswordHash()).build());
        if (user.getDeviceToken() != null) 
            item.put("deviceToken", AttributeValue.builder().s(user.getDeviceToken()).build());
        if (user.getLatitude() != 0.0) 
            item.put("latitude", AttributeValue.builder().n(String.valueOf(user.getLatitude())).build());
        if (user.getLongitude() != 0.0) 
            item.put("longitude", AttributeValue.builder().n(String.valueOf(user.getLongitude())).build());
        putItem(item);
    }

    public User getUser(String userId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("userId", AttributeValue.builder().s(userId).build());
        
        GetItemResponse response = getItem(key);
        if (!response.hasItem()) {
            return null;
        }

        Map<String, AttributeValue> item = response.item();
        User user = new User();
        user.setUserId(item.get("userId").s());
        user.setName(item.get("name").s());
        user.setAddress(item.get("address").s());
        user.setVolunteer(item.get("isVolunteer").bool());
        user.setSocialWorker(item.get("isSocialWorker").bool());

        if (item.containsKey("email")) 
            user.setEmail(item.get("email").s());
        if (item.containsKey("passwordHash")) 
            user.setPasswordHash(item.get("passwordHash").s());
        if (item.containsKey("deviceToken")) 
            user.setDeviceToken(item.get("deviceToken").s());
        if (item.containsKey("latitude")) 
            user.setLatitude(Double.parseDouble(item.get("latitude").n()));
        if (item.containsKey("longitude")) 
            user.setLongitude(Double.parseDouble(item.get("longitude").n()));
        
        return user;
    }

    public void updateUser(User user){
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.builder().s(user.getUserId()).build());
        
        // Only update non-null/non-default values to preserve existing data
        if (user.getName() != null) 
            item.put("name", AttributeValue.builder().s(user.getName()).build());
        if (user.getAddress() != null) 
            item.put("address", AttributeValue.builder().s(user.getAddress()).build());
        if (user.getEmail() != null) 
            item.put("email", AttributeValue.builder().s(user.getEmail()).build());
        if (user.getPasswordHash() != null) 
            item.put("passwordHash", AttributeValue.builder().s(user.getPasswordHash()).build());
        if (user.getDeviceToken() != null) 
            item.put("deviceToken", AttributeValue.builder().s(user.getDeviceToken()).build());
        if (user.getLatitude() != 0.0) 
            item.put("latitude", AttributeValue.builder().n(String.valueOf(user.getLatitude())).build());
        if (user.getLongitude() != 0.0) 
            item.put("longitude", AttributeValue.builder().n(String.valueOf(user.getLongitude())).build());
        
        // Include boolean fields (they have default values)
        item.put("isVolunteer", AttributeValue.builder().bool(user.isVolunteer()).build());
        item.put("isSocialWorker", AttributeValue.builder().bool(user.isSocialWorker()).build());
        
        putItem(item);
    } 

    public void deleteUser(String userId){
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("userId", AttributeValue.builder().s(userId).build());
        deleteItem(key);
    }
    
    /**
     * Get all users for notification purposes
     * @return List of all users
     */
    public List<User> getAllUsers() {
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(this.tableName)
                .build();
                
        ScanResponse response = this.dynamoDbClient.scan(scanRequest);
        List<User> users = new ArrayList<>();
        
        for (Map<String, AttributeValue> item : response.items()) {
            User user = new User();
            user.setUserId(item.get("userId").s());
            
            if (item.containsKey("name")) 
                user.setName(item.get("name").s());
            if (item.containsKey("address")) 
                user.setAddress(item.get("address").s());
            if (item.containsKey("email")) 
                user.setEmail(item.get("email").s());
            if (item.containsKey("deviceToken")) 
                user.setDeviceToken(item.get("deviceToken").s());
            if (item.containsKey("latitude")) 
                user.setLatitude(Double.parseDouble(item.get("latitude").n()));
            if (item.containsKey("longitude")) 
                user.setLongitude(Double.parseDouble(item.get("longitude").n()));
            if (item.containsKey("isVolunteer")) 
                user.setVolunteer(item.get("isVolunteer").bool());
            if (item.containsKey("isSocialWorker")) 
                user.setSocialWorker(item.get("isSocialWorker").bool());
                
            users.add(user);
        }
        
        return users;
    }

    public User getUserByEmail(String email) {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":email", AttributeValue.builder().s(email).build());

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(this.tableName)
                .indexName("EmailIndex")
                .keyConditionExpression("email = :email")
                .expressionAttributeValues(expressionValues)
                .build();

        QueryResponse result = this.dynamoDbClient.query(queryRequest);

        if (result.count() == 0) return null;

        Map<String, AttributeValue> item = result.items().get(0);
        User user = new User();
        user.setUserId(item.get("userId").s());
        user.setName(item.get("name").s());
        user.setAddress(item.get("address").s());
        user.setVolunteer(item.get("isVolunteer").bool());
        user.setSocialWorker(item.get("isSocialWorker").bool());
        if (item.containsKey("email")) user.setEmail(item.get("email").s());
        if (item.containsKey("passwordHash")) user.setPasswordHash(item.get("passwordHash").s());

        return user;
    }

}