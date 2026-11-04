package com.duoc.cloudsumativa1;

import com.duoc.cloudsumativa1.model.Evento;
import com.duoc.cloudsumativa1.model.Usuario;
import com.duoc.cloudsumativa1.repository.EventoRepository;
import com.duoc.cloudsumativa1.repository.UsuarioRepository;
import com.duoc.cloudsumativa1.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class TicketsApiTests {
    @TempDir
    static Path directorio;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("tickets.storage.path", () -> directorio.toString());
    }

    @Autowired private WebApplicationContext context;
    @Autowired private EventoRepository eventos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private TicketRepository tickets;
    private MockMvc mvc;
    private Evento evento;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        tickets.deleteAll();
        eventos.deleteAll();
        usuarios.deleteAll();
        evento = new Evento();
        evento.setNombre("Concierto");
        evento.setFecha(LocalDateTime.of(2027, 1, 10, 20, 0));
        evento.setPrecio(new BigDecimal("15000"));
        evento.setCuposDisponibles(1);
        eventos.save(evento);
        usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setCorreo("ana@example.com");
        usuarios.save(usuario);
    }

    @Test
    void reservaConsultaYDescargaTxt() throws Exception {
        String ruta = mvc.perform(post("/eventos/" + evento.getId() + "/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + usuario.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").isString())
                .andExpect(jsonPath("$.eventoId").value(evento.getId()))
                .andReturn().getResponse().getHeader("Location");
        mvc.perform(get(ruta)).andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(usuario.getId()));
        mvc.perform(get(ruta + "/archivo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString(".txt")))
                .andExpect(content().string(containsString("Ana")))
                .andExpect(content().string(containsString("Concierto")))
                .andExpect(content().string(containsString("15000")));
        assertThat(eventos.findById(evento.getId()).orElseThrow().getCuposDisponibles()).isZero();
        Path archivo = Path.of(tickets.findAll().getFirst().getRutaArchivo());
        assertThat(archivo).exists();
        assertThat(archivo.getParent().getFileName().toString()).isEqualTo("usuario" + usuario.getId());

        mvc.perform(post("/eventos/" + evento.getId() + "/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + usuario.getId() + "}"))
                .andExpect(status().isConflict());
        assertThat(tickets.count()).isEqualTo(1);
    }

    @Test
    void rechazaReferenciasInvalidas() throws Exception {
        mvc.perform(post("/eventos/0/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + usuario.getId() + "}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/eventos/" + evento.getId() + "/tickets")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioId\":999999}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/eventos/" + evento.getId() + "/tickets")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/tickets/0")).andExpect(status().isNotFound());
        assertThat(tickets.count()).isZero();
        assertThat(eventos.findById(evento.getId()).orElseThrow().getCuposDisponibles()).isEqualTo(1);
    }

    @Test
    void revierteReservaSiNoPuedeGuardarArchivo() throws Exception {
        Path carpetaEvento = directorio.resolve("evento" + evento.getId());
        Files.writeString(carpetaEvento, "Esto impide crear la carpeta del evento");
        mvc.perform(post("/eventos/" + evento.getId() + "/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + usuario.getId() + "}"))
                .andExpect(status().isInternalServerError());
        assertThat(tickets.count()).isZero();
        assertThat(eventos.findById(evento.getId()).orElseThrow().getCuposDisponibles()).isEqualTo(1);
    }
}
