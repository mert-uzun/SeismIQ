package com.seismiq.common.model;

/**
 * Represents a category that can be added to the emergency reports in the SeismIQ system.
 * Each category has an identifier and a type.
 * Categories include: food, water, medical, equipment, and other.
 * 
 * @author Ayşe Ece Bilgi
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
}
