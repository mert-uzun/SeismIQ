package com.seismiq.common.model;

/**
 * Represents a category that can be added to the emergency reports in the SeismIQ system.
 * Each category has an identifier and a type.
 * Categories include: food, water, medical, equipment, and other.
 * 
 * @author Ayşe Ece Bilgi and Sıla Bozkurt
 */

public class Category {
    private String categoryType;
    private String categoryID;

    //consturctors
    public Category(){}

    public Category(String categoryID, String type){
        this.categoryID = categoryID;
        this.categoryType = type;
    }

    //getters and setters
    public String getCategoryID(){
        return categoryID;
    }

    public void setCategoryID(String categoryID){
        this.categoryID = categoryID;
    }
 
    public String getCategoryType(){
        return categoryType;
    }

    public void setCategoryType(String type){
        this.categoryType = type;
    }


    @Override
    public String toString(){
        return "Category: \n" + "Category ID: " + categoryID + "\n" + "Category Type: " + categoryType;
    }

    /**
     * Static factory method to create a Category object from a category type string.
     * This method is used when deserializing from storage or for convenient creation.
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
