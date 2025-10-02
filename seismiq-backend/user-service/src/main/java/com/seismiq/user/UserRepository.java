package com.seismiq.user;

import java.util.HashMap;
import java.util.Map;

import com.seismiq.common.model.User;
import com.seismiq.common.repository.DynamoDBRepository;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

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
        
        return user;
    }

    public void updateUser(User user){
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
        
        putItem(item);
    } 

    public void deleteUser(String userId){
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("userId", AttributeValue.builder().s(userId).build());
        deleteItem(key);
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