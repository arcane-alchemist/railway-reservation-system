package com.railway.service;

import com.railway.dao.UserDAO;
import com.railway.model.Passenger;
import com.railway.model.User;

import java.util.Optional;

/**
 * Service managing user authentication, sessions, and registration.
 */
public class AuthService {

    private final UserDAO userDAO;
    private static User currentUser;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public Optional<User> login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return Optional.empty();
        }

        Optional<User> userOpt = userDAO.authenticate(username.trim(), password.trim());
        userOpt.ifPresent(user -> currentUser = user);
        return userOpt;
    }

    public boolean registerPassenger(String username, String password, String fullName, String email,
                                     String phone, int age, String gender) {
        if (userDAO.isUsernameTaken(username)) {
            return false;
        }

        Passenger passenger = new Passenger(username, password, fullName, email, phone, age, gender);
        boolean success = userDAO.registerPassenger(passenger);
        if (success) {
            currentUser = passenger;
        }
        return success;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        currentUser = null;
    }
}
