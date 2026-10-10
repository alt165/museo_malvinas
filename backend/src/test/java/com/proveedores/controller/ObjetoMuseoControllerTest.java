package com.proveedores.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.dto.ReciboEscaneadoObjetoMuseoResponseDTO;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.GlobalExceptionHandler;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.security.KeycloakJwtAuthenticationConverter;
import com.proveedores.service.ComodatoPrestamoService;
import com.proveedores.service.FotoObjetoMuseoService;
import com.proveedores.service.ObjetoMuseoExportService;
import com.proveedores.service.ObjetoMuseoFichaPdfService;
import com.proveedores.service.ObjetoMuseoService;
import com.proveedores.service.ReciboEscaneadoObjetoMuseoService;
import com.proveedores.service.ReciboIngresoObjetoService;
import com.proveedores.service.RelacionObjetoService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ObjetoMuseoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ObjetoMuseoControllerTest {

    @Test
    void listarAnexarYDescargarReciboEscaneadoPorId() throws Exception {
        var recibo = new ReciboEscaneadoObjetoMuseoResponseDTO(42L, 7L, "acta.pdf", "application/pdf", 8L,
                java.time.LocalDateTime.of(2026, 10, 9, 12, 0), "tester");
        when(reciboEscaneadoObjetoMuseoService.listar(7L)).thenReturn(java.util.List.of(recibo));
        when(reciboEscaneadoObjetoMuseoService.agregar(eq(7L), any(), isNull())).thenReturn(recibo);
        when(reciboEscaneadoObjetoMuseoService.descargar(7L, 42L)).thenReturn(
                new ReciboEscaneadoObjetoMuseoService.ReciboEscaneadoArchivo(recibo,
                        new ByteArrayResource("%PDF-1.4".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        "application/pdf", "acta.pdf"));

        mockMvc.perform(get("/api/objetos/7/recibos-escaneados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42L));
        mockMvc.perform(multipart("/api/objetos/7/recibos-escaneados")
                        .file(new MockMultipartFile("archivo", "acta.pdf", "application/pdf", "%PDF-1.4".getBytes())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreArchivoOriginal").value("acta.pdf"));
        mockMvc.perform(get("/api/objetos/7/recibos-escaneados/42/archivo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PDF_VALUE)))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("acta.pdf")));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ObjetoMuseoService objetoMuseoService;

    @MockBean
    private ObjetoMuseoExportService objetoMuseoExportService;

    @MockBean
    private ObjetoMuseoFichaPdfService objetoMuseoFichaPdfService;

    @MockBean
    private ComodatoPrestamoService comodatoPrestamoService;

    @MockBean
    private FotoObjetoMuseoService fotoObjetoMuseoService;

    @MockBean
    private ReciboEscaneadoObjetoMuseoService reciboEscaneadoObjetoMuseoService;

    @MockBean
    private ReciboIngresoObjetoService reciboIngresoObjetoService;

    @MockBean
    private RelacionObjetoService relacionObjetoService;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Test
    void crearObjetoMuseoCorrectamenteDevuelveCreated() throws Exception {
        ObjetoMuseoResponseDTO response = new ObjetoMuseoResponseDTO(1L, "INV-1", "Casco", null, null, null, null, null, null, null, null, null, null, java.util.List.of(), java.util.List.of(), null);
        when(objetoMuseoService.crear(any(ObjetoMuseoRequestDTO.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/objetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ObjetoMuseoRequestDTO("INV-1", "Casco", null, null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.numeroInventario").value("INV-1"));
    }

    @Test
    void exportarPdfDevuelveArchivo() throws Exception {
        when(objetoMuseoExportService.exportarListadoPdf(eq("Casco"), eq("INV"), eq(java.util.List.of(2L)), isNull(), isNull(), isNull(), isNull(), any(), any()))
                .thenReturn("%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/objetos/export/pdf")
                        .param("nombre", "Casco")
                        .param("numeroInventario", "INV")
                        .param("categoriaIds", "2")
                        .param("sort", "numeroInventario,asc"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PDF_VALUE)))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("objetos_")));
    }

    @Test
    void descargarFichaPdfDevuelveArchivoConNombreSeguro() throws Exception {
        when(objetoMuseoFichaPdfService.generar(eq(12L), any()))
                .thenReturn(new ObjetoMuseoFichaPdfService.FichaPdf(
                        "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        "ficha-objeto-INV-12.pdf"));

        mockMvc.perform(get("/api/objetos/12/ficha-pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PDF_VALUE)))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("ficha-objeto-INV-12.pdf")));
    }

    @Test
    void buscarObjetosPaginadosDevuelvePage() throws Exception {
        ObjetoMuseoResponseDTO response = new ObjetoMuseoResponseDTO(1L, "INV-1", "Casco", null, null, null, null, null, null, null, null, null, null, java.util.List.of(), java.util.List.of(), null);
        when(objetoMuseoService.buscar(eq("Casco"), eq("INV"), eq(java.util.List.of(2L)), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(response)));

        mockMvc.perform(get("/api/objetos/buscar")
                        .param("nombre", "Casco")
                        .param("numeroInventario", "INV")
                        .param("categoriaIds", "2")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].numeroInventario").value("INV-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void requestInvalidoDevuelveBadRequest() throws Exception {
        mockMvc.perform(post("/api/objetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ObjetoMuseoRequestDTO("", "", null, null, null, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("La solicitud contiene errores de validacion"))
                .andExpect(jsonPath("$.path").value("/api/objetos"))
                .andExpect(jsonPath("$.validationErrors.denominacionONombreValida").value("La denominacion o nombre es obligatorio"));
    }

    @Test
    void temperaturaYLuzAceptanHasta200Caracteres() throws Exception {
        ObjetoMuseoResponseDTO response = new ObjetoMuseoResponseDTO(1L, "INV-1", "Casco", null, null, null, null, null, null, null, null, null, null, java.util.List.of(), java.util.List.of(), null);
        when(objetoMuseoService.crear(any(ObjetoMuseoRequestDTO.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/objetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "denominacionObjeto", "Casco",
                                "temperaturaConservacion", "T".repeat(200),
                                "luzConservacion", "L".repeat(200),
                                "cantidadPartes", 0
                        ))))
                .andExpect(status().isCreated());
    }

    @Test
    void temperaturaYLuzRechazanMasDe200Caracteres() throws Exception {
        mockMvc.perform(post("/api/objetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "denominacionObjeto", "Casco",
                                "temperaturaConservacion", "T".repeat(201),
                                "luzConservacion", "L".repeat(201),
                                "cantidadPartes", 0
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.temperaturaConservacion").value("La temperatura no puede superar 200 caracteres"))
                .andExpect(jsonPath("$.validationErrors.luzConservacion").value("La luz no puede superar 200 caracteres"));
    }

    @Test
    void resourceNotFoundDevuelve404() throws Exception {
        when(objetoMuseoService.obtenerPorId(99L)).thenThrow(new ResourceNotFoundException("Objeto de museo no encontrado"));

        mockMvc.perform(get("/api/objetos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Objeto de museo no encontrado"))
                .andExpect(jsonPath("$.path").value("/api/objetos/99"));
    }

    @Test
    void businessExceptionDevuelve400() throws Exception {
        when(objetoMuseoService.crear(any(ObjetoMuseoRequestDTO.class), any())).thenThrow(new BusinessException("Ya existe un objeto con ese numero de inventario"));

        mockMvc.perform(post("/api/objetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ObjetoMuseoRequestDTO("INV-1", "Casco", null, null, null, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Ya existe un objeto con ese numero de inventario"));
    }

    @Test
    void dataIntegrityViolationDevuelve409() throws Exception {
        when(objetoMuseoService.crear(any(ObjetoMuseoRequestDTO.class), any()))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        mockMvc.perform(post("/api/objetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ObjetoMuseoRequestDTO("INV-1", "Casco", null, null, null, null, null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("No se pudo completar la operacion porque viola una restriccion de datos"))
                .andExpect(jsonPath("$.path").value("/api/objetos"));
    }

    @Test
    void jsonInvalidoDevuelve400ConFormatoEstandar() throws Exception {
        mockMvc.perform(post("/api/objetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud es invalido o no tiene el formato esperado"))
                .andExpect(jsonPath("$.path").value("/api/objetos"));
    }

    @Test
    void exceptionGenericaDevuelve500() throws Exception {
        when(objetoMuseoService.obtenerPorId(1L)).thenThrow(new RuntimeException("fallo inesperado"));

        mockMvc.perform(get("/api/objetos/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Error interno del servidor"))
                .andExpect(jsonPath("$.path").value("/api/objetos/1"));
    }
}
