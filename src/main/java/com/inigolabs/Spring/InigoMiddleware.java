package com.inigolabs.Spring;

import com.inigolabs.Foreign;
import com.inigolabs.Inigo;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;

import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;

public class InigoMiddleware implements WebGraphQlInterceptor {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final long instanceHandle;

    public InigoMiddleware(Inigo.Config config) {
        instanceHandle = initialize(config, false);
    }

    public InigoMiddleware(Inigo.Config config, Boolean downloadLibrary) {
        instanceHandle = initialize(config, downloadLibrary);
    }

    public InigoMiddleware(String token, String schema) {
        instanceHandle = initialize(new Inigo.Config(token, schema), false);
    }

    public InigoMiddleware(String token, String schema, Boolean downloadLibrary) {
        instanceHandle = initialize(new Inigo.Config(token, schema), downloadLibrary);
    }

    private long initialize(Inigo.Config config, Boolean downloadLibrary) {
        if (downloadLibrary) {
            Inigo.DownloadLibrary();
        }
        config.Name += " : spring";
        return Foreign.Create(config);
    }
    
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest gqlReq, Chain chain) {
        var query = gqlReq.getDocument().getBytes();
        // var variables = gqlReq.getVariables();

        var request = Foreign.ProcessRequest(instanceHandle, null, gqlReq.getHeaders(), query);
        if (request.Output() != null && !request.Output().isEmpty()) {
            // TODO: Block the request and return the output from Inigo
        }

        // TODO: Return response from Inigo if available
        return chain.next(gqlReq)
            .doOnSuccess(resp -> {
                byte[] jres = null;
                try {
                    jres = objectMapper.writeValueAsBytes(resp.toMap());
                } catch (Exception e) {
                    var errorResponse = java.util.Map.of(
                        "errors", java.util.List.of(
                            java.util.Map.of(
                                "message", "Failed to serialize response: " + e.getMessage(),
                                "extensions", java.util.Map.of("code", "INTERNAL_ERROR")
                            )
                        )
                    );
                    try {
                        jres = objectMapper.writeValueAsBytes(errorResponse);
                    } catch (Exception ex) {
                        System.err.println("ERROR: Failed to serialize error response: " + ex.getMessage());
                    }
                }

                Foreign.ProcessResponse(instanceHandle, request.Handle(), jres);
            })
            .doOnError(error -> {
                byte[] jres = null;
                try {
                    var errorResponse = java.util.Map.of(
                        "errors", java.util.List.of(
                            java.util.Map.of(
                                "message", error.getMessage() != null ? error.getMessage() : "Unknown error",
                                "extensions", java.util.Map.of("code", "INTERNAL_ERROR")
                            )
                        )
                    );
                    jres = objectMapper.writeValueAsBytes(errorResponse);
                } catch (Exception e) {
                    System.err.println("ERROR: Failed to serialize error response: " + e.getMessage());
                }

                Foreign.ProcessResponse(instanceHandle, request.Handle(), jres);
            });
    }
}
