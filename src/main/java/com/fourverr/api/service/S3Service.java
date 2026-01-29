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

import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    // Constructor: Aquí se conecta a AWS usando tus claves del application.properties
    public S3Service(@Value("${aws.s3.bucket}") String bucketName,
                     @Value("${aws.s3.region}") String region,
                     @Value("${aws.access.key.id}") String accessKey,
                     @Value("${aws.secret.access.key}") String secretKey) {
        this.bucketName = bucketName;
        
        // Creamos las credenciales
        AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);

        // Iniciamos el cliente de S3
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .build();
    }

    // Método para subir archivos
    public String subirImagen(MultipartFile archivo) {
        try {
            // 1. Generar un nombre único para que no se sobrescriban (ej: "uuid_imagen.png")
            String fileName = UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();

            // 2. Preparar la petición de subida
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(archivo.getContentType()) // Importante para que el navegador sepa que es imagen
                    .build();

            // 3. Subir el archivo
            s3Client.putObject(request, RequestBody.fromInputStream(archivo.getInputStream(), archivo.getSize()));

            // 4. Devolver la URL pública de la imagen
            return "https://" + bucketName + ".s3.amazonaws.com/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo: " + e.getMessage());
        }
    }
}