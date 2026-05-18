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
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

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
            Path filePath = Paths.get(localUploadDir).resolve(fileName);
            return Files.readAllBytes(filePath);
        }
    }
}
