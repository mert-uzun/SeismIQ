package com.seismiq.app.model;

/**
 * Represents a category that can be added to landmarks or emergency reports in the SeismIQ system.
 * Each category has an identifier and a type.
 * Categories include: hospital, medical aid station, shelter, etc.
 */
public class Category {
    private String categoryType;
    private String categoryID;

    // Default constructor for JSON deserialization
    public Category() {}

    public Category(String categoryID, String categoryType) {
        this.categoryID = categoryID;
        this.categoryType = categoryType;
    }

    // Getters and setters
    public String getCategoryID() {
        return categoryID;
    }

    public void setCategoryID(String categoryID) {
        this.categoryID = categoryID;
    }

    public String getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(String categoryType) {
        this.categoryType = categoryType;
    }

    @Override
    public String toString() {
        return categoryType;
    }

    /**
     * Static factory method to create a Category object from a category type string.
     * 
     * @param categoryType The category type string
     * @return A new Category object with the given type
     */
    public static Category valueOf(String categoryType) {
        if (categoryType == null || categoryType.isEmpty()) {
            throw new IllegalArgumentException("Category type cannot be null or empty");
        }
        // Generate a consistent ID based on the type
        String categoryID = categoryType.toLowerCase().replace(' ', '_');
        return new Category(categoryID, categoryType);
    }
}
