package com.fourverr.api.controller;

import com.fourverr.api.model.Ilustracion;
import com.fourverr.api.model.Usuario;
import com.fourverr.api.repository.IlustracionRepository;
import com.fourverr.api.repository.UsuarioRepository;
import com.fourverr.api.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/ilustraciones")
public class IlustracionController {

    @Autowired
    private IlustracionRepository ilustracionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private S3Service s3Service;

    // GET: Ver todas las obras (Catálogo)
    @GetMapping
    public List<Ilustracion> obtenerTodas() {
        return ilustracionRepository.findAll();
    }

    // POST: Subir una nueva obra
    // NOTA: No usamos @RequestBody porque estamos enviando un archivo binario (Multipart)
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> subirObra(
            @RequestParam("imagen") MultipartFile imagen,
            @RequestParam("titulo") String titulo,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("precio") BigDecimal precio,
            @RequestParam("nombreUsuario") String nombreUsuario) {

        // 1. Validar que el usuario exista
        Optional<Usuario> ilustradorOpt = usuarioRepository.findByNombreUsuario(nombreUsuario);
        if (ilustradorOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("El usuario no existe");
        }

        try {
            // 2. Subir imagen a AWS S3
            String urlImagen = s3Service.subirImagen(imagen);

            // 3. Crear el objeto Ilustracion y guardar en BD
            Ilustracion obra = new Ilustracion();
            obra.setTitulo(titulo);
            obra.setDescripcion(descripcion);
            obra.setPrecio(precio);
            obra.setUrlImagen(urlImagen); // Guardamos el link de AWS
            obra.setIlustrador(ilustradorOpt.get());

            Ilustracion obraGuardada = ilustracionRepository.save(obra);
            return ResponseEntity.ok(obraGuardada);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al subir la obra: " + e.getMessage());
        }
    }
}