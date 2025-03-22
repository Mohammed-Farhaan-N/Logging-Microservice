package com.example.loggingmicroservice.repository;

import org.springframework.stereotype.Repository;

import com.example.loggingmicroservice.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    User findByAppName(String appName);
    User findByApiKeyAndAppId(String apiKey, String appId);
}

