package com;

import java.util.UUID;

import com.seismiq.common.model.User;
import com.seismiq.user.UserRepository;

/**
 * Service layer for managing user operations in the SeismIQ system.
 * Interacts with UserRepository to perform CRUD operations on user data.
 * 
 * @author Ayşe Ece Bilgi
 */

public class UserService {
    private final UserRepository userRepository;

    public UserService(){
        this.userRepository = new UserRepository();
    } 
    
    public void registerUser(User user){
        if(user.getUserId() == null){
            user.setUserId(UUID.randomUUID().toString());
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
}
