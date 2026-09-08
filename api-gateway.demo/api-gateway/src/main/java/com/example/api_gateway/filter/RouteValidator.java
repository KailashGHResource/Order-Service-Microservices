package com.example.api_gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    // Define endpoints that bypass the Gateway JWT check
    public static final List<String> openApiEndpoints = List.of(
            "/api/v1/employees/login",
            "/eureka"
    );

    // If the request path does NOT contain any of the open endpoints, it is secured
    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndpoints
                    .stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}