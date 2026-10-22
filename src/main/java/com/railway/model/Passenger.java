package com.railway.model;

/**
 * Passenger entity representing a customer booking tickets.
 * Demonstrates OOP: Inheritance and Polymorphism.
 */
public class Passenger extends User {
    private String phone;
    private int age;
    private String gender;

    public Passenger(int id, String username, String password, String fullName, String email,
                     String phone, int age, String gender) {
        super(id, username, password, fullName, email, "PASSENGER");
        this.phone = phone;
        this.age = age;
        this.gender = gender;
    }

    public Passenger(String username, String password, String fullName, String email,
                     String phone, int age, String gender) {
        this(0, username, password, fullName, email, phone, age, gender);
    }

    @Override
    public String getDashboardGreeting() {
        return "Welcome aboard, " + getFullName() + "! Plan your journey with ease.";
    }

    @Override
    public boolean canManageTrains() {
        return false;
    }

    // Senior citizen concession eligibility check
    public boolean isSeniorCitizen() {
        return age >= 60;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    @Override
    public String toString() {
        return "Passenger{" +
                "fullName='" + getFullName() + '\'' +
                ", phone='" + phone + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                "} " + super.toString();
    }
}
