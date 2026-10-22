package com.railway.model;

/**
 * Abstract base class representing a generic system user.
 * Demonstrates OOP concepts: Abstraction and Encapsulation.
 */
public abstract class User {
    private int id;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String role; // 'PASSENGER' or 'ADMIN'

    public User(int id, String username, String password, String fullName, String email, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public User(String username, String password, String fullName, String email, String role) {
        this(0, username, password, fullName, email, role);
    }

    // Abstract method implemented polymorphically by subclasses
    public abstract String getDashboardGreeting();

    // Polymorphic permission check
    public abstract boolean canManageTrains();

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
