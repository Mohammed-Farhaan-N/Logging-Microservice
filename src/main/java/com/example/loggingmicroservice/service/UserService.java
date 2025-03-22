package com.example.loggingmicroservice.service;

import com.amazonaws.services.s3.AmazonS3;
import com.example.loggingmicroservice.model.User;
import com.example.loggingmicroservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class UserService {

    private final Map<String, User> users;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public UserService(UserRepository userRepository, MongoTemplate mongoTemplate, AmazonS3 s3Client) {
        this.users = new HashMap<>();
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
        this.s3Client = s3Client;
    }

    /**
     * Asynchronously register a new user.
     *
     * @param appName The name of the application to register.
     * @return A CompletableFuture representing the user registration process.
     *         It will eventually complete with the registered User object.
     * @throws IllegalArgumentException if the given appName is already taken.
     */
    @Async
    public CompletableFuture<User> registerAsync(String appName) {
        return CompletableFuture.supplyAsync(() -> {
                User existingUser = userRepository.findByAppName(appName);
                if (existingUser != null) {
                    throw new IllegalArgumentException("Name has already been taken. Try Again.");
                }

                User user = new User(appName, UUID.randomUUID().toString(), generateApiKey());
                userRepository.save(user);

                String logFileName = user.getAppName() + "_" + user.getAppId();
                mongoTemplate.createCollection(logFileName);
                AWStemplete("user" ,new File(logFileName));
                return user;

        });
    }

    /**
     * Asynchronously post a log entry for a specific user.
     *
     * @param apiKey    The API key of the user.
     * @param appId     The app ID of the user.
     * @param message   The log message.
     * @param logLevel  The log level (optional, defaults to "info").
     * @param className The class name associated with the log entry.
     * @return A CompletableFuture representing the log posting process.
     */
    @Async
    public CompletableFuture<Void> postLogAsync(String apiKey, String appId, String message, String logLevel, String className) {
        return CompletableFuture.runAsync(() -> {
            User user1 = userRepository.findByApiKeyAndAppId(apiKey, appId);
            if (user1 == null) {
                throw new IllegalArgumentException("Invalid API Key or App Id.");
            }

            String appName = user1.getAppName();
            String collectionName = appName + "_" + appId;

            File logFile = new File(collectionName);

            try (FileWriter fileWriter = new FileWriter(logFile, true);
                 BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                LocalDate currentDate = LocalDate.now();
                LocalTime currentTime = LocalTime.now();

                String formattedDate = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
                String formattedTime = currentTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
                // Create a log entry map
                Map<String, Object> logEntry = new HashMap<>();
                logEntry.put("message", sanitizeInput(message));
                logEntry.put("ClassName:", sanitizeInput(className));
                logEntry.put("time", LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
                logEntry.put("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                logEntry.put("logLevel", sanitizeInput(logLevel));

                mongoTemplate.insert(logEntry, collectionName);

                //String logEntryJson = objectMapper.writeValueAsString(logEntry);

               // bufferedWriter.write(logEntryJson);
                bufferedWriter.newLine();
                AWStemplete(collectionName,logFile);

            } catch (IOException e) {
                e.printStackTrace();
            }

            // Insert the log entry into the corresponding collection
            //mongoTemplate.insert(logEntry, collectionName);



        });
    }

    /**
     * Asynchronously retrieve log entries for a specific user.
     *
     * @param apiKey The API key of the user.
     * @param appId  The app ID of the user.
     * @param date   The date to filter the log entries (optional).
     * @return A CompletableFuture representing the log retrieval process.
     * It will eventually complete with a list of log entries.
     */
    @Async
    public CompletableFuture<List<Map<String, Object>>> getLogAsync(String apiKey, String appId, String date) {
        return CompletableFuture.supplyAsync(() -> {

            User user1 = userRepository.findByApiKeyAndAppId(apiKey, appId);
            if (user1 == null) {
                throw new IllegalArgumentException("Invalid API Key or App Id.");
            }
            String collectionName = user1.getAppName() + "_" + user1.getAppId();

            MongoCollection<Document> collection = mongoTemplate.getCollection(collectionName);

            Bson filter = null;
            if (date != null) {
                LocalDate searchDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
                LocalDate nextDate = searchDate.plusDays(1);
                filter = Filters.and(
                        Filters.gte("date", searchDate.toString()),
                        Filters.lt("date", nextDate.toString())
                );
            }

            FindIterable<Document> result;
            if (filter != null) {
                result = collection.find(filter);
            } else {
                result = collection.find();
            }

            // Step 7: Process the search results
            List<Map<String, Object>> logEntries = new ArrayList<>();
            for (Document document : result) {
                Map<String, Object> logEntry = new HashMap<>();
                logEntry.put("message", document.getString("message"));
                logEntry.put("ClassName", document.getString("ClassName:"));
                logEntry.put("time", document.getString("time"));
                logEntry.put("date", document.getString("date"));
                logEntry.put("logLevel", document.getString("logLevel"));
                logEntries.add(logEntry);
            }

            return logEntries;

        });
    }


    // Helper methods for generating API Key, saving users to JSON, and creating initial log file

    /**
     * Generate a random API key.
     *
     * @return The generated API key.
     */
    private String generateApiKey() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    /**
     * Sanitize the input by removing potential malicious content and limiting the length.
     *
     * @param input The input string to sanitize.
     * @return The sanitized input string.
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        // Remove leading and trailing whitespace
        String sanitizedInput = input.trim();

        // Remove potential malicious HTML tags and attributes
        sanitizedInput = sanitizedInput.replaceAll("(?i)<(\\/)?[a-z][^>]*?>", "");

        // Remove JavaScript event handlers
        sanitizedInput = sanitizedInput.replaceAll("(?i)\\s*on\\w+\\s*=\\s*(['\"](?:\\\\.|[^\\\\])*?['\"])", "");

        // Remove potential SQL injection characters
        sanitizedInput = sanitizedInput.replaceAll("'", "''");

        // Remove potential command injection characters
        sanitizedInput = sanitizedInput.replaceAll("[;&|`$]", "");
        // +

        sanitizedInput = sanitizedInput.replaceAll("<script>", "");
        sanitizedInput = sanitizedInput.replaceAll("</script>", "");
        sanitizedInput = sanitizedInput.replaceAll("<.*?>", "");

        int maxLength = 1000;
        if (sanitizedInput.length() > maxLength) {
            sanitizedInput = sanitizedInput.substring(0, maxLength);
        }

        return sanitizedInput;
    }
    @Value("${application.bucket.name}")
    private String bucketName;

    private AmazonS3 s3Client;


    public User AWStemplete(String filename, File file) {
        s3Client.putObject(bucketName, filename , file );
        return null;
    }

}
