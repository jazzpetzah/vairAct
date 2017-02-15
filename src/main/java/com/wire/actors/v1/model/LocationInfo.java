package com.wire.actors.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LocationInfo {
    private Float lon = 13.0f;
    private Float lat = 52.0f;
    private String address = "Wire St, 1, WireTown, Wirestan";
    private Integer zoom = 7;

    public LocationInfo() {
    }

    @JsonProperty
    public Float getLon() {
        return lon;
    }

    @JsonProperty
    public void setLon(Float lon) {
        this.lon = lon;
    }

    @JsonProperty
    public Float getLat() {
        return lat;
    }

    @JsonProperty
    public void setLat(Float lat) {
        this.lat = lat;
    }

    @JsonProperty
    public String getAddress() {
        return address;
    }

    @JsonProperty
    public void setAddress(String address) {
        this.address = address;
    }

    @JsonProperty
    public Integer getZoom() {
        return zoom;
    }

    @JsonProperty
    public void setZoom(Integer zoom) {
        this.zoom = zoom;
    }

}
