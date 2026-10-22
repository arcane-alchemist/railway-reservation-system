package com.railway.model;

/**
 * Admin entity with elevated privileges to view and manage train schedules and reservations.
 * Demonstrates OOP: Inheritance and Polymorphism.
 */
public class Admin extends User {
    private String department;

    public Admin(int id, String username, String password, String fullName, String email, String department) {
        super(id, username, password, fullName, email, "ADMIN");
        this.department = department;
    }

    public Admin(String username, String password, String fullName, String email, String department) {
        this(0, username, password, fullName, email, department);
    }

    @Override
    public String getDashboardGreeting() {
        return "Railway Operations Command Portal - Admin: " + getFullName() + " (" + department + ")";
    }

    @Override
    public boolean canManageTrains() {
        return true;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public String toString() {
        return "Admin{" +
                "department='" + department + '\'' +
                "} " + super.toString();
    }
}
