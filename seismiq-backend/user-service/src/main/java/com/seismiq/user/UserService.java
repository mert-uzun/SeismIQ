package com.seismiq.user;

import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;

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
    
    public void registerUser(User user) throws Exception{
        if(user.getName() == null || user.getEmail() == null || user.getPasswordHash() == null){
            throw new Exception("Name, email, and password are required");
        }

        if(userRepository.getUserByEmail(user.getEmail()) != null){
            throw new Exception("Email already registered");
        }

        user.setUserId(UUID.randomUUID().toString());
        user.setPasswordHash(BCrypt.hashpw(user.getPasswordHash(), BCrypt.gensalt()));
        userRepository.saveUser(user);
    }

    public void registerUser(String name, String address, boolean isVolunteer, boolean isSocialWorker, String email, String password) throws Exception {
        User user = new User();
        user.setName(name);
        user.setAddress(address);
        user.setVolunteer(isVolunteer);
        user.setSocialWorker(isSocialWorker);
        user.setEmail(email);
        user.setPasswordHash(password);
        registerUser(user);
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
    
    public User loginUser(String email, String password) throws Exception {
        User user = userRepository.getUserByEmail(email);
        if(user == null || ! BCrypt.checkpw(password, user.getPasswordHash())){
            throw new Exception("Invalid emial or password");
        }
        return user;
    } 
}
