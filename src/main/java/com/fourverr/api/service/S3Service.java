package com.fourverr.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest; // Importante para borrar

import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    public S3Service(@Value("${aws.s3.bucket}") String bucketName,
                     @Value("${aws.s3.region}") String region,
                     @Value("${aws.access.key.id}") String accessKey,
                     @Value("${aws.secret.access.key}") String secretKey) {
        this.bucketName = bucketName;
        AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .build();
    }

    public String subirImagen(MultipartFile archivo) {
        try {
            String fileName = UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(archivo.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(archivo.getInputStream(), archivo.getSize()));
            return "https://" + bucketName + ".s3.amazonaws.com/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo: " + e.getMessage());
        }
    }

    // Nuevo método para borrar de Amazon S3
    public void eliminarImagen(String urlImagen) {
        try {
            // Extrae el nombre del archivo de la URL pública
            String key = urlImagen.substring(urlImagen.lastIndexOf("/") + 1);
            
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            System.out.println("DEBUG: Borrado de S3 exitoso: " + key);
        } catch (Exception e) {
            throw new RuntimeException("Error al borrar en S3: " + e.getMessage());
        }
    }
}