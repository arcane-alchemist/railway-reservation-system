package com.railway.model;

/**
 * Station model representing a railway junction or terminal.
 */
public class Station {
    private int id;
    private String stationCode;
    private String stationName;
    private String city;
    private String state;

    public Station(int id, String stationCode, String stationName, String city, String state) {
        this.id = id;
        this.stationCode = stationCode;
        this.stationName = stationName;
        this.city = city;
        this.state = state;
    }

    public Station(String stationCode, String stationName, String city, String state) {
        this(0, stationCode, stationName, city, state);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return stationCode + " - " + stationName + " (" + city + ")";
    }
}
