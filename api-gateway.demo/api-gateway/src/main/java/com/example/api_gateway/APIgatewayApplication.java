package com.example.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import com.example.api_gateway.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class APIgatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(APIgatewayApplication.class, args);
	}


	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder, AuthenticationFilter authenticationFilter) {
		return builder.routes()
				.route("department-service", r -> r.path("/department-service/**")
						.filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
						.uri("lb://DEPARTMENT-SERVICE"))

				.route("employee-service", r -> r.path("/employee-service/**")
						.filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
						.uri("http://localhost:8081")) // Direct local routing target to bypass network lookup mismatches

				.route("leave-service", r -> r.path("/leave-service/**")
						.filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
						.uri("lb://LEAVE-SERVICE"))

				.route("notification-service", r -> r.path("/notification-service/**")
						.filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
						.uri("lb://NOTIFICATION-SERVICE"))

				.route("audit-service", r -> r.path("/audit-service/**")
						.filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
						.uri("lb://AUDIT-SERVICE"))

				.build();
	}

}
