package com.intellitrack.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${aws.access-key-id:}")
    private String accessKey;

    @Value("${aws.secret-access-key:}")
    private String secretKey;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${aws.s3.bucket-name:}")
    private String bucketName;

    private AmazonS3 s3Client;
    private final String localUploadDir = "uploads";

    /** Allowed MIME types for submission file uploads. */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "image/png",
            "image/jpeg",
            "image/gif");

    /** Allowed file extensions (secondary check, defence-in-depth). */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "png", "jpg", "jpeg", "gif");

    @PostConstruct
    public void init() {
        if (!accessKey.isEmpty() && !secretKey.isEmpty() && !bucketName.isEmpty()) {
            BasicAWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);
            s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(creds))
                    .withRegion(region)
                    .build();
            System.out.println("AWS S3 Storage initialized.");
        } else {
            File dir = new File(localUploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            System.out.println("Local File Storage initialized in directory: " + localUploadDir);
        }
    }

    public String storeFile(MultipartFile file) throws IOException {
        validateFile(file);
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        // Strip any path components from the original name to prevent injection
        String safeName = Paths.get(originalName).getFileName().toString();
        String fileName = UUID.randomUUID().toString() + "_" + safeName;

        if (s3Client != null) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            s3Client.putObject(new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata));
            return s3Client.getUrl(bucketName, fileName).toString();
        } else {
            Path targetPath = Paths.get(localUploadDir).resolve(fileName);
            Files.copy(file.getInputStream(), targetPath);
            return targetPath.toAbsolutePath().toString();
        }
    }

    public byte[] getFile(String fileName) throws IOException {
        if (s3Client != null) {
            return s3Client.getObject(bucketName, fileName).getObjectContent().readAllBytes();
        } else {
            // Normalise and verify the resolved path stays inside the uploads directory
            Path uploadRoot = Paths.get(localUploadDir).toAbsolutePath().normalize();
            Path target = uploadRoot.resolve(fileName).normalize();
            if (!target.startsWith(uploadRoot)) {
                throw new SecurityException("Access denied: path traversal attempt detected");
            }
            return Files.readAllBytes(target);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "File type not allowed: " + contentType
                            + ". Accepted types: PDF, Word, Excel, PowerPoint, plain text, and common image formats.");
        }
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException(
                    "File extension not allowed: ." + ext);
        }
    }
}
