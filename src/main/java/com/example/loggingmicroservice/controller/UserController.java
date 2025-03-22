package com.example.loggingmicroservice.controller;

import com.example.loggingmicroservice.model.User;
import com.example.loggingmicroservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * API endpoint for registering a new user.
     * Creates a new user with the provided app name.
     *
     * @param appName The name of the application to register.
     * @return A CompletableFuture representing the asynchronous operation that resolves to the registered User.
     */
    @PostMapping("/register")
    public CompletableFuture<User> register(@RequestParam String appName) {
        return userService.registerAsync(appName);
    }

    /**
     * API endpoint for posting a log entry.
     * Logs the provided message, log level, and class name for the given app ID and API key.
     *
     * @param apiKey     The API key of the application.
     * @param appId      The ID of the application.
     * @param message    The log message to post.
     * @param logLevel   The log level (optional, default: "info").
     * @param className  The name of the class generating the log entry.
     * @return A CompletableFuture representing the asynchronous operation that resolves when the log entry is posted.
     */
    @PutMapping("/postlog")
    public CompletableFuture<Void> postLog(@RequestParam String apiKey,
                                           @RequestParam String appId,
                                           @RequestParam String message,
                                           @RequestParam(required = false, defaultValue = "info") String logLevel,
                                           @RequestParam String className) {
        return userService.postLogAsync(apiKey, appId, message, logLevel, className);
    }

    /**
     * API endpoint for retrieving logs.
     * Retrieves the log entries for the given app ID, API key, and optional date.
     *
     * @param apiKey The API key of the application.
     * @param appId  The ID of the application.
     * @param date   The date to filter the log entries (optional).
     * @return A CompletableFuture representing the asynchronous operation that resolves to a list of log entries.
     */
    @GetMapping("/logs")
    public CompletableFuture<List<Map<String, Object>>> getLog(@RequestParam String apiKey,
                                                               @RequestParam String appId,
                                                               @RequestParam(required = false) String date) {
        return userService.getLogAsync(apiKey, appId, date);
    }

}
