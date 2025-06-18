package com.inigolabs;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Inigo {
    public static String LibraryPath = null;
    
    public static class Config {
        public Config() { }

        public Config(String Token) {
            this.Token = Token;
        }

        public Config(String Token, String Schema) {
            this.Token = Token;
            this.Schema = Schema;
        }

        public long LogLevel = 3; // 1 = Trace, 2 = Debug, 3 = Info
        public String Token = "";
        public String Schema = "";
        public String EgressURL = "";
        public String ServiceURL = "";
        public long Gateway;
        public boolean DisableResponseData;
        public boolean Federation;
        public boolean FederationExample;

        public String Name = "inigo-jv";
        public final String Runtime = "java" + System.getProperty("java.runtime.version");
    }

    public static void DownloadLibrary() {
        System.out.println("Downloading Inigo library...");

        try {
            // Detect operating system
            String osName = System.getProperty("os.name").toLowerCase();
            String os;
            if (osName.contains("windows")) {
                os = "windows";
            } else if (osName.contains("mac") || osName.contains("darwin")) {
                os = "darwin";
            } else {
                os = "linux";
            }
            
            // Detect architecture
            String arch = System.getProperty("os.arch").toLowerCase();
            String platform;
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                platform = "arm64";
            } else {
                platform = "amd64";
            }
            
            // Determine file extension
            String extension;
            switch (os) {
                case "darwin":
                    extension = "dylib";
                    break;
                case "windows":
                    extension = "dll";
                    break;
                default:
                    extension = "so";
            }
            
            var fileName = String.format("inigo-%s-%s.%s", os, platform, extension);
            var downloadUrl = "https://github.com/inigolabs/artifacts/releases/latest/download/" + fileName;
            
            System.out.println("Downloading from: " + downloadUrl);
            
            // Create temp directory if it doesn't exist
            var tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "inigo");
            Files.createDirectories(tempDir);
            var outputPath = tempDir.resolve(fileName);

            // Download the file
            var url = URI.create(downloadUrl).toURL();
            var connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            HttpURLConnection.setFollowRedirects(true);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream()) {
                    Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Downloaded successfully to: " + outputPath);
                }
                LibraryPath = outputPath.toString();
            } else {
                throw new IOException("Failed to download library. HTTP response code: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Error downloading Inigo library: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

