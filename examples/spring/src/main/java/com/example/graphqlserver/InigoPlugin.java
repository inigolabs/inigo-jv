package com.example.graphqlserver;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlInterceptor;

import com.inigolabs.Spring.InigoMiddleware;

@Configuration
public class InigoPlugin {
    @Bean
    public WebGraphQlInterceptor Main() {
        String token = "";
        String schema = "";
        
        try { token = System.getenv("INIGO_SERVICE_TOKEN"); } catch (Exception e) { }
        try { schema = new String(Files.readAllBytes(Paths.get("src/main/resources/graphql/schema.graphqls"))); } catch (Exception e) { }

        if (token == null || token.isEmpty()) {
            System.out.println("ERROR: INIGO_SERVICE_TOKEN environment variable is not set.");
            System.exit(1);
        }

        if (schema == null || schema.isEmpty()) {
            System.out.println("ERROR: Schema is empty. Please ensure the schema file exists and is not empty.");        
            System.exit(1);
        }

        return new InigoMiddleware(token, schema, true);
    }
}
