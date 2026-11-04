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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import com.duoc.cloudsumativa1.service.TicketService;
import com.duoc.cloudsumativa1.dto.TicketResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
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
    @Autowired private TicketService service;
    @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
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
    @Test
    void filtraTicketsPorUsuarioYEvento() throws Exception {
        evento.setCuposDisponibles(3);
        eventos.save(evento);
        Usuario otroUsuario = new Usuario();
        otroUsuario.setNombre("Luis");
        otroUsuario.setCorreo("luis@example.com");
        usuarios.save(otroUsuario);
        Evento otroEvento = new Evento();
        otroEvento.setNombre("Teatro");
        otroEvento.setFecha(evento.getFecha());
        otroEvento.setPrecio(evento.getPrecio());
        otroEvento.setCuposDisponibles(2);
        eventos.save(otroEvento);
        TicketResponse primero = service.crear(evento.getId(), usuario.getId());
        TicketResponse segundo = service.crear(evento.getId(), otroUsuario.getId());
        TicketResponse tercero = service.crear(otroEvento.getId(), usuario.getId());

        mvc.perform(get("/usuarios/" + usuario.getId() + "/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.containsInAnyOrder(
                        primero.id().intValue(), tercero.id().intValue())));
        mvc.perform(get("/eventos/" + evento.getId() + "/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.containsInAnyOrder(
                        primero.id().intValue(), segundo.id().intValue())));

    }

    @Test
    void devuelveListaVaciaSiNoHayTickets() throws Exception {
        for (String ruta : new String[]{"/usuarios/" + usuario.getId(), "/eventos/" + evento.getId()}) {
            mvc.perform(get(ruta + "/tickets"))
                    .andExpect(status().isOk()).andExpect(content().json("[]"));
        }
        for (String ruta : new String[]{"/usuarios/0/tickets", "/eventos/0/tickets"}) {
            mvc.perform(get(ruta)).andExpect(status().isNotFound());
        }
    }

    @Test
    void modificaObservacionYEliminaDevolviendoCupo() throws Exception {
        TicketResponse ticket = service.crear(evento.getId(), usuario.getId());
        Path archivo = Path.of(tickets.findById(ticket.id()).orElseThrow().getRutaArchivo());
        mvc.perform(patch("/tickets/" + ticket.id()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacion\":\"Acceso por puerta norte\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacion").value("Acceso por puerta norte"));
        mvc.perform(get("/tickets/" + ticket.id() + "/archivo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Observación: Acceso por puerta norte")));
        mvc.perform(get("/tickets/" + ticket.id()))
                .andExpect(jsonPath("$.observacion").value("Acceso por puerta norte"));
        assertThat(eventos.findById(evento.getId()).orElseThrow().getCuposDisponibles()).isZero();
        mvc.perform(delete("/tickets/" + ticket.id())).andExpect(status().isNoContent());
        assertThat(archivo).doesNotExist();
        assertThat(tickets.existsById(ticket.id())).isFalse();
        assertThat(eventos.findById(evento.getId()).orElseThrow().getCuposDisponibles()).isEqualTo(1);
        mvc.perform(delete("/tickets/" + ticket.id())).andExpect(status().isNotFound());
        assertThat(eventos.findById(evento.getId()).orElseThrow().getCuposDisponibles()).isEqualTo(1);
    }

    @Test
    void validaObservacionYPermiteVaciarla() throws Exception {
        TicketResponse ticket = service.crear(evento.getId(), usuario.getId());
        mvc.perform(patch("/tickets/" + ticket.id()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/tickets/" + ticket.id()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacion\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/tickets/" + ticket.id()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacion\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.observacion").value(""));
        mvc.perform(patch("/tickets/0").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacion\":\"Prueba\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void restauraArchivoYDatosSiSeRevierteModificacionOEliminacion() throws Exception {
        TicketResponse ticket = service.crear(evento.getId(), usuario.getId());
        Path archivo = Path.of(tickets.findById(ticket.id()).orElseThrow().getRutaArchivo());
        byte[] original = Files.readAllBytes(archivo);
        var transaccion = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        transaccion.executeWithoutResult(status -> {
            service.modificar(ticket.id(), "Cambio temporal");
            status.setRollbackOnly();
        });
        assertThat(Files.readAllBytes(archivo)).isEqualTo(original);
        assertThat(service.consultar(ticket.id()).observacion()).isNull();
        transaccion.executeWithoutResult(status -> {
            service.eliminar(ticket.id());
            status.setRollbackOnly();
        });
        assertThat(Files.readAllBytes(archivo)).isEqualTo(original);
        assertThat(tickets.existsById(ticket.id())).isTrue();
        assertThat(eventos.findById(evento.getId()).orElseThrow().getCuposDisponibles()).isZero();
    }
    @Test
    void estadisticasReflejanReservasYEliminacionesDelEvento() throws Exception {
        evento.setPrecio(new BigDecimal("1250.50"));
        evento.setCuposDisponibles(3);
        eventos.save(evento);
        String ruta = "/eventos/" + evento.getId() + "/estadisticas";
        mvc.perform(get(ruta)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReservas").value(0))
                .andExpect(jsonPath("$.cuposDisponibles").value(3))
                .andExpect(jsonPath("$.montoTotalReservado").value(0));
        TicketResponse primero = service.crear(evento.getId(), usuario.getId());
        service.crear(evento.getId(), usuario.getId());
        Evento otro = new Evento();
        otro.setNombre("Otro evento");
        otro.setFecha(evento.getFecha());
        otro.setPrecio(new BigDecimal("999"));
        otro.setCuposDisponibles(1);
        eventos.save(otro);
        service.crear(otro.getId(), usuario.getId());
        mvc.perform(get(ruta)).andExpect(status().isOk())
                .andExpect(jsonPath("$.eventoId").value(evento.getId()))
                .andExpect(jsonPath("$.nombreEvento").value("Concierto"))
                .andExpect(jsonPath("$.totalReservas").value(2))
                .andExpect(jsonPath("$.cuposDisponibles").value(1))
                .andExpect(jsonPath("$.montoTotalReservado").value(2501.00));
        service.eliminar(primero.id());
        mvc.perform(get(ruta)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReservas").value(1))
                .andExpect(jsonPath("$.cuposDisponibles").value(2))
                .andExpect(jsonPath("$.montoTotalReservado").value(1250.50));
        mvc.perform(get("/eventos/0/estadisticas")).andExpect(status().isNotFound());
    }
}
