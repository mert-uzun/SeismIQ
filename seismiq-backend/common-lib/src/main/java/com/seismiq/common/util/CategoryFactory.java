package com.seismiq.common.util;

import java.util.Arrays;
import java.util.List;

public class CategoryFactory {
    
    public enum Category {
        RESCUE_CALL("Rescue Call"),
        MEDICAL_EMERGENCY("Medical Emergency"),
        SUPPLY_REQUEST("Supply Request"),
        DANGER_NOTICE("Danger Notice"),
        INFORMATION_SHARING("Information Sharing"),
        OFFERING_HELP("Offering Help"),
        INFRASTRUCTURE_PROBLEM("Infrastructure Problem"),
        COORDINATION("Coordination"),
        CONTACT_INFO("Contact Info"),
        EMOTIONAL_SUPPORT("Emotional Support"),
        LOW_PRIORITY("Low Priority"),
        OTHER("Other");
        
        private final String displayName;
        
        Category(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public static List<Category> getAllCategories() {
        return Arrays.asList(Category.values());
    }
    
    public static Category getCategoryByName(String name) {
        for (Category category : Category.values()) {
            if (category.name().equalsIgnoreCase(name) || 
                category.getDisplayName().equalsIgnoreCase(name)) {
                return category;
            }
        }
        return Category.OTHER;
    }
    
    public static boolean isValidCategory(String categoryName) {
        return getCategoryByName(categoryName) != Category.OTHER || 
               "OTHER".equalsIgnoreCase(categoryName);
    }
}