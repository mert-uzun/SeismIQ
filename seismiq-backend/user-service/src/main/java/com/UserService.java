package com;

import java.util.List;

import com.seismiq.common.model.User;
import com.seismiq.user.UserRepository;

/**
 * Service layer for managing user operations in the SeismIQ system.
 * Interacts with UserRepository to perform CRUD operations on user data.
 * Authentication is handled by AWS Cognito.
 * 
 * @author Ayşe Ece Bilgi
 */

public class UserService {
    private final UserRepository userRepository;

    public UserService(){
        this.userRepository = new UserRepository();
    } 
    
    /**
     * Register a new user with email/password authentication
     */
    public void registerUser(User user) throws Exception{
        if(userRepository.getUserByEmail(user.getEmail()) != null){
            throw new Exception("Email already registered");
        }
        user.setUserId(java.util.UUID.randomUUID().toString());
        user.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw(user.getPasswordHash(), org.mindrot.jbcrypt.BCrypt.gensalt()));
        userRepository.saveUser(user);
    }

    /**
     * Creates user profile in DynamoDB. 
     * User authentication/registration is handled by AWS Cognito.
     * This method is called after successful Cognito registration.
     */
    public void createUserProfile(User user) throws Exception{
        // userId comes from Cognito user sub (extracted from JWT token)
        if(user.getUserId() == null || user.getUserId().isEmpty()) {
            throw new Exception("User ID is required (from Cognito)");
        }
        
        // Check if user profile already exists
        if(userRepository.getUser(user.getUserId()) != null) {
            throw new Exception("User profile already exists");
        }
        
        userRepository.saveUser(user);
    }

    public User getUserProfile(String userId){
        return userRepository.getUser(userId);
    }

    public void updateUserProfile(User user){
        userRepository.updateUser(user);
    }

    public void deleteUserProfile(String userId){
        userRepository.deleteUser(userId);
    }
    
    /**
     * Get all users for notification purposes
     * @return List of all users
     */
    public List<User> getAllUsers() {
        return userRepository.getAllUsers();
    }

    /**
     * Authenticate user with email and password
     */
    public User loginUser(String email, String password) throws Exception {
        User user = userRepository.getUserByEmail(email);
        if(user == null || !org.mindrot.jbcrypt.BCrypt.checkpw(password, user.getPasswordHash())){
            throw new Exception("Invalid email or password");
        }
        return user;
    }
}
