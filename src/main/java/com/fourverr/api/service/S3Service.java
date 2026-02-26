package com.fourverr.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest; // sube
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest; // borra

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

    // metodo IMAGEN PERFIL ---USUSARIO---
    public String subirImagenPerfil(MultipartFile archivo, String username) {
        String usernameLimpio = username.replaceAll("\\s+", "_");
    
        String fileName = "perfiles/" + usernameLimpio + "/" + UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();
        return enviarAS3(archivo, fileName);
    }

    // metodo PRODUCTOS ---VENDEDOR---
    public String subirImagenProducto(MultipartFile archivo, String username) {
        String usernameLimpio = username.replaceAll("\\s+", "_");
    try {
        String fileName = "productos/" + usernameLimpio + "/" + UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();
        
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(archivo.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(archivo.getInputStream(), archivo.getSize()));
    
        return "https://" + bucketName + ".s3.amazonaws.com/" + fileName;
    } catch (IOException e) {
        throw new RuntimeException("Error al procesar el archivo para S3: " + e.getMessage());
    }
}

    // Subida general
    private String enviarAS3(MultipartFile archivo, String fileName) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(archivo.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(archivo.getInputStream(), archivo.getSize()));
        return "https://" + bucketName + ".s3.amazonaws.com/" + fileName;
    } catch (IOException e) {
        throw new RuntimeException("Error al procesar archivo en S3: " + e.getMessage());
    }
}

    // metodo para borrar de Amazon S3
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