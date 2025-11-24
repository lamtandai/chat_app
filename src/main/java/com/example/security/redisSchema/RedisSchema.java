package com.example.security.redisSchema;

public class RedisSchema {
    public static String getCategoryId(String id){
        return KeyHelper.getKey(getCategory() + ":" + id);
    }

    public static String getArticleId(String c_id, String a_id){
        return getCategoryId(c_id) + ":articles:" + a_id;
    }

    public static String getArticlesOfCategoryId(String c_id){
        return getCategoryId(c_id) + ":articles";

    }
    
    public static String getCategory(){
        return "category";
    }

    public static String getSessionKey(){
        return KeyHelper.getKey("sessions");
    }
}
