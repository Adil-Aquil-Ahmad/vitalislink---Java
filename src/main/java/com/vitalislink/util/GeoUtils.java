package com.vitalislink.util;

import org.springframework.stereotype.Component;

/**
 * Utility class for geographic calculations and operations.
 * Useful for finding nearby blood donation centers or calculating distances.
 */
@Component
public class GeoUtils {
    
    // Earth radius in kilometers
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    /**
     * Calculate distance between two points using the Haversine formula.
     * 
     * @param lat1 Latitude of first point in degrees
     * @param lon1 Longitude of first point in degrees
     * @param lat2 Latitude of second point in degrees
     * @param lon2 Longitude of second point in degrees
     * @return Distance in kilometers
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);
        
        // Haversine formula
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                  Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                  Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
    
    /**
     * Check if a location is within a certain radius of another location.
     * 
     * @param centerLat Center latitude in degrees
     * @param centerLon Center longitude in degrees
     * @param pointLat Target point latitude in degrees
     * @param pointLon Target point longitude in degrees
     * @param radiusKm Radius in kilometers
     * @return true if the point is within the radius, false otherwise
     */
    public boolean isWithinRadius(double centerLat, double centerLon, 
                                double pointLat, double pointLon, double radiusKm) {
        double distance = calculateDistance(centerLat, centerLon, pointLat, pointLon);
        return distance <= radiusKm;
    }
    
    /**
     * Validates if the given coordinates are valid geographic coordinates.
     * 
     * @param latitude Latitude in degrees (-90 to 90)
     * @param longitude Longitude in degrees (-180 to 180)
     * @return true if coordinates are valid, false otherwise
     */
    public boolean areValidCoordinates(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90 && 
               longitude >= -180 && longitude <= 180;
    }
    
    /**
     * Find the nearest location from a list of locations to the given coordinates.
     * 
     * @param targetLat Latitude of the target point
     * @param targetLon Longitude of the target point
     * @param locationLats Array of latitudes for the locations to check
     * @param locationLons Array of longitudes for the locations to check
     * @return The index of the nearest location, or -1 if arrays are empty or of different sizes
     */
    public int findNearestLocation(double targetLat, double targetLon, 
                                 double[] locationLats, double[] locationLons) {
        if (locationLats == null || locationLons == null || 
            locationLats.length == 0 || locationLats.length != locationLons.length) {
            return -1;
        }
        
        int nearestIdx = 0;
        double minDistance = calculateDistance(targetLat, targetLon, 
                                              locationLats[0], locationLons[0]);
        
        for (int i = 1; i < locationLats.length; i++) {
            double distance = calculateDistance(targetLat, targetLon, 
                                               locationLats[i], locationLons[i]);
            if (distance < minDistance) {
                minDistance = distance;
                nearestIdx = i;
            }
        }
        
        return nearestIdx;
    }
}