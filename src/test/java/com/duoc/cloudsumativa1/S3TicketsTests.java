package com.duoc.cloudsumativa1;

import com.duoc.cloudsumativa1.model.Asset;
import com.duoc.cloudsumativa1.model.Evento;
import com.duoc.cloudsumativa1.model.Usuario;
import com.duoc.cloudsumativa1.repository.*;
import com.duoc.cloudsumativa1.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.s3.enabled=true", "app.s3.bucket=test-bucket",
        "spring.cloud.aws.s3.enabled=false"})
@ActiveProfiles("test")
class S3TicketsTests {
    @TempDir static Path directorio;
    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("tickets.storage.path", () -> directorio.toString());
    }

    @MockitoBean S3Repository s3;
    @Autowired TicketService service;
    @Autowired TicketRepository tickets;
    @Autowired EventoRepository eventos;
    @Autowired UsuarioRepository usuarios;
    @Autowired WebApplicationContext context;
    private final Map<String, byte[]> objetos = new HashMap<>();
    private Evento evento;
    private Usuario usuario;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        tickets.deleteAll();
        eventos.deleteAll();
        usuarios.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        when(s3.subir(anyString(), any(byte[].class), anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            byte[] contenido = inv.getArgument(1);
            objetos.put(key, contenido.clone());
            return new Asset(key, key.substring(key.lastIndexOf('/') + 1), inv.getArgument(2), contenido.length);
        });
        when(s3.existe(anyString())).thenAnswer(inv -> objetos.containsKey(inv.getArgument(0)));
        when(s3.descargar(anyString())).thenAnswer(inv -> objetos.get(inv.getArgument(0)));
        doAnswer(inv -> { objetos.remove(inv.getArgument(0)); return null; }).when(s3).eliminar(anyString());
        evento = new Evento();
        evento.setNombre("Concierto");
        evento.setFecha(LocalDateTime.of(2027, 1, 10, 20, 0));
        evento.setPrecio(new BigDecimal("15000"));
        evento.setCuposDisponibles(1);
        eventos.save(evento);
        usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setCorreo("ana@test.cl");
        usuarios.save(usuario);
    }

    @Test
    void subeAutomaticamentePermiteDescargarYSeSincroniza() throws Exception {
        var ticket = service.crear(evento.getId(), usuario.getId());
        String key = "evento" + evento.getId() + "/usuario" + usuario.getId()
                + "/ticket" + ticket.codigo() + ".txt";
        assertThat(objetos).containsOnlyKeys(key);
        Path local = Path.of(tickets.findById(ticket.id()).orElseThrow().getRutaArchivo());
        assertThat(objetos.get(key)).isEqualTo(Files.readAllBytes(local));
        mvc.perform(get("/tickets/" + ticket.id() + "/s3"))
                .andExpect(status().isOk()).andExpect(content().bytes(objetos.get(key)));
        objetos.clear();
        mvc.perform(post("/tickets/" + ticket.id() + "/s3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.key").value(key));
        service.modificar(ticket.id(), "Puerta norte");
        assertThat(new String(objetos.get(key), StandardCharsets.UTF_8)).contains("Puerta norte");
        service.eliminar(ticket.id());
        assertThat(objetos).isEmpty();
        assertThat(local).doesNotExist();
        assertThat(eventos.findById(evento.getId()).orElseThrow().getCuposDisponibles()).isEqualTo(1);
    }

    @Test
    void falloDeSubidaRevierteReservaYArchivoLocal() throws Exception {
        when(s3.subir(anyString(), any(byte[].class), anyString()))
                .thenThrow(new IllegalStateException("S3 no disponible"));
        mvc.perform(post("/eventos/" + evento.getId() + "/tickets")
                        .contentType("application/json").content("{\"usuarioId\":" + usuario.getId() + "}"))
                .andExpect(status().isBadGateway());
        assertThat(tickets.count()).isZero();
        assertThat(eventos.findById(evento.getId()).orElseThrow().getCuposDisponibles()).isEqualTo(1);
        try (var archivos = Files.walk(directorio.resolve("evento" + evento.getId()))) {
            assertThat(archivos.filter(Files::isRegularFile).toList()).isEmpty();
        }
    }
    @Test
    void falloAlEliminarEnS3ConservaTicketCupoYArchivo() throws Exception {
        var ticket = service.crear(evento.getId(), usuario.getId());
        Path local = Path.of(tickets.findById(ticket.id()).orElseThrow().getRutaArchivo());
        byte[] original = Files.readAllBytes(local);
        doThrow(new IllegalStateException("S3 no disponible")).when(s3).eliminar(anyString());
        mvc.perform(delete("/tickets/" + ticket.id())).andExpect(status().isBadGateway());
        assertThat(tickets.existsById(ticket.id())).isTrue();
        assertThat(eventos.findById(evento.getId()).orElseThrow().getCuposDisponibles()).isZero();
        assertThat(Files.readAllBytes(local)).isEqualTo(original);
        assertThat(objetos.values()).singleElement().isEqualTo(original);
    }
}
