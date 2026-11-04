package com.duoc.cloudsumativa1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class EventosUsuariosApiTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void creaYConsultaEvento() throws Exception {
        String ruta = mvc.perform(post("/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Concierto","fecha":"2027-01-10T20:00:00",
                                 "precio":15000,"cuposDisponibles":10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(ruta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Concierto"))
                .andExpect(jsonPath("$.cuposDisponibles").value(10));
        mvc.perform(get("/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Concierto"));
    }

    @Test
    void creaYConsultaUsuario() throws Exception {
        String ruta = mvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","correo":"ana@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(ruta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@example.com"));
        mvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Ana"));
    }

    @Test
    void rechazaCorreoDuplicadoSinDistinguirMayusculas() throws Exception {
        mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","correo":"ana@example.com"}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Otra Ana","correo":"ANA@EXAMPLE.COM"}
                                """))
                .andExpect(status().isConflict());

        mvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Ana"));
    }

    @Test
    void rechazaDatosInvalidos() throws Exception {
        mvc.perform(post("/eventos").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":" ","precio":-1,"cuposDisponibles":-1}
                                """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":" ","correo":"correo-invalido"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devuelve404SiNoExiste() throws Exception {
        mvc.perform(get("/eventos/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/usuarios/999999")).andExpect(status().isNotFound());
    }
}
