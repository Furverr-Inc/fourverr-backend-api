package com.fourverr.api.model;

public enum TipoProducto {
    SERVICIO_GIG,       // Una ilustración personalizada ✅
    CURSO_DIGITAL,      // Un video o curso    (3D)      ✅
    RECURSO_DESCARGABLE,// Un PDF, un Zip, Assets        ✅
    SUSCRIPCION         // ilustracion                   ✅
}

// roubd robin. LEast connections algoritmo de balanceo de carga.
//microservicio para la gestion de prodcutos CRUD
/*  
ilustracion
modelo 3d
paquete de assets   
seervicio tecnico   serviicio_gig

se tiene que crear un producto personalizado
usando un hasmap para poder aglomerarlos

*/