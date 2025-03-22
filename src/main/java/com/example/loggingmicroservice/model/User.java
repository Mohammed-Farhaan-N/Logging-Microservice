package com.example.loggingmicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.UUID;
@Document(collection = "UserList")
public class User {

    @Id
    private String appName; // The name of the application
    @Field
    private String appId; // The unique identifier of the application

    @Field
    private String apiKey; // The API key associated with the application

    public User()
    {}

    public User(String appName,String appId,String apiKey ){
        this.apiKey = apiKey;
        this.appName = appName;
        this.appId = appId;
    }



    public String getAppName() {
        return appName;
    }

    public String getAppId() {
        return appId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
