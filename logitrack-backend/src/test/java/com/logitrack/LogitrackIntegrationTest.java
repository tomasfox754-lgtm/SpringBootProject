package com.logitrack;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LogitrackIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    String token;
    long origen, destino, producto;

    @BeforeEach
    void preparar() throws Exception {
        // Sin @Transactional: comprobamos commits y rollbacks reales del servicio.
        for (String tabla :
                List.of(
                        "detalle_movimiento",
                        "movimiento",
                        "inventario",
                        "auditoria",
                        "producto",
                        "bodega")) jdbc.update("DELETE FROM " + tabla);
        jdbc.update("DELETE FROM usuario WHERE username <> 'admin'");
        token = login("admin", "Admin12345!");
        origen =
                crear(
                        "/bodegas",
                        "{\"nombre\":\"Bogota\",\"ubicacion\":\"Bogota\",\"capacidad\":100,\"encargado\":\"Ana\"}");
        destino =
                crear(
                        "/bodegas",
                        "{\"nombre\":\"Medellin\",\"ubicacion\":\"Medellin\",\"capacidad\":100,\"encargado\":\"Luis\"}");
        producto =
                crear(
                        "/productos",
                        "{\"nombre\":\"Teclado\",\"categoria\":\"Tecnologia\",\"precio\":85000}");
    }

    String login(String nombre, String clave) throws Exception {
        var r =
                mvc.perform(
                                post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "username",
                                                                nombre,
                                                                "password",
                                                                clave))))
                        .andExpect(status().isOk())
                        .andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    long crear(String ruta, String body) throws Exception {
        var r =
                mvc.perform(
                                post(ruta)
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    String movimiento(String tipo, Long desde, Long hasta, int cantidad) throws Exception {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("tipo", tipo);
        dto.put("origenId", desde);
        dto.put("destinoId", hasta);
        dto.put("detalles", List.of(Map.of("productoId", producto, "cantidad", cantidad)));
        return mapper.writeValueAsString(dto);
    }

    ResultActions mover(String body) throws Exception {
        return mover(body, token);
    }

    ResultActions mover(String body, String jwt) throws Exception {
        return mvc.perform(
                post("/movimientos")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    long stock(long bodega) {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(cantidad),0) FROM inventario WHERE bodega_id=?",
                Long.class,
                bodega);
    }

    @Test
    void entradaTransferenciaSalidaYResumen() throws Exception {
        mover(movimiento("ENTRADA", null, origen, 20))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").value("admin"));
        mover(movimiento("TRANSFERENCIA", origen, destino, 7)).andExpect(status().isCreated());
        assertThat(stock(origen)).isEqualTo(13);
        assertThat(stock(destino)).isEqualTo(7);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT stock FROM producto WHERE id=?", Long.class, producto))
                .isEqualTo(20);
        mover(movimiento("SALIDA", destino, null, 2)).andExpect(status().isCreated());
        assertThat(stock(destino)).isEqualTo(5);
        mvc.perform(get("/reportes/resumen").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockPorBodega[0].stockTotal").value(13))
                .andExpect(jsonPath("$.productosMasMovidos[0].cantidadMovida").value(29));
        mvc.perform(get("/productos/stock-bajo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void stockInsuficienteNoDejaCambiosNiAuditoria() throws Exception {
        mover(movimiento("ENTRADA", null, origen, 4)).andExpect(status().isCreated());
        long antes = jdbc.queryForObject("SELECT COUNT(*) FROM auditoria", Long.class);
        mover(movimiento("TRANSFERENCIA", origen, destino, 5)).andExpect(status().isBadRequest());
        assertThat(stock(origen)).isEqualTo(4);
        assertThat(stock(destino)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria", Long.class))
                .isEqualTo(antes);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM movimiento", Long.class)).isEqualTo(1);
    }

    @Test
    void falloEnSegundoProductoReviertePrimero() throws Exception {
        long segundo =
                crear(
                        "/productos",
                        "{\"nombre\":\"Mouse\",\"categoria\":\"Tecnologia\",\"precio\":100}");
        mover(movimiento("ENTRADA", null, origen, 10)).andExpect(status().isCreated());
        long antes = jdbc.queryForObject("SELECT COUNT(*) FROM auditoria", Long.class);
        String dto =
                mapper.writeValueAsString(
                        Map.of(
                                "tipo",
                                "SALIDA",
                                "origenId",
                                origen,
                                "detalles",
                                List.of(
                                        Map.of("productoId", producto, "cantidad", 3),
                                        Map.of("productoId", segundo, "cantidad", 1))));
        mover(dto).andExpect(status().isBadRequest());
        assertThat(stock(origen)).isEqualTo(10);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT stock FROM producto WHERE id=?", Long.class, producto))
                .isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria", Long.class))
                .isEqualTo(antes);
    }

    @Test
    void capacidadYValidaciones() throws Exception {
        mover(movimiento("ENTRADA", null, origen, 101)).andExpect(status().isBadRequest());
        mover(movimiento("ENTRADA", null, origen, 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isMap());
        mover(movimiento("TRANSFERENCIA", origen, origen, 1)).andExpect(status().isBadRequest());
        mover(movimiento("SALIDA", null, destino, 1)).andExpect(status().isBadRequest());
        String repetidos =
                mapper.writeValueAsString(
                        Map.of(
                                "tipo",
                                "ENTRADA",
                                "destinoId",
                                origen,
                                "detalles",
                                List.of(
                                        Map.of("productoId", producto, "cantidad", 1),
                                        Map.of("productoId", producto, "cantidad", 2))));
        mover(repetidos).andExpect(status().isBadRequest());
        assertThat(stock(origen)).isZero();
    }

    @Test
    void auditoriaAutomaticaAntesDespuesYDelete() throws Exception {
        mvc.perform(
                        put("/productos/" + producto)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"nombre\":\"Teclado"
                                            + " nuevo\",\"categoria\":\"Tecnologia\",\"precio\":90000}"))
                .andExpect(status().isOk());
        String antes =
                jdbc.queryForObject(
                        "SELECT valores_anteriores FROM auditoria WHERE entidad='Producto' AND"
                                + " operacion='UPDATE'",
                        String.class);
        String despues =
                jdbc.queryForObject(
                        "SELECT valores_nuevos FROM auditoria WHERE entidad='Producto' AND"
                                + " operacion='UPDATE'",
                        String.class);
        assertThat(mapper.readTree(antes).get("nombre").asText()).isEqualTo("Teclado");
        assertThat(mapper.readTree(despues).get("nombre").asText()).isEqualTo("Teclado nuevo");
        mvc.perform(delete("/productos/" + producto).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(
                        get("/auditorias")
                                .param("usuario", "admin")
                                .param("operacion", "DELETE")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entidad").value("Producto"))
                .andExpect(jsonPath("$[0].valoresNuevos").doesNotExist());
    }

    @Test
    void auditoriaInventarioYProducto() throws Exception {
        mover(movimiento("ENTRADA", null, origen, 8)).andExpect(status().isCreated());
        mover(movimiento("SALIDA", origen, null, 3)).andExpect(status().isCreated());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM auditoria WHERE entidad='Inventario' AND"
                                        + " operacion='UPDATE'",
                                Long.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM auditoria WHERE entidad='Producto' AND"
                                        + " operacion='UPDATE'",
                                Long.class))
                .isEqualTo(2);
    }

    @Test
    void jwtRegistroYRoles() throws Exception {
        mvc.perform(get("/productos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
        mvc.perform(get("/productos").header("Authorization", "Bearer falso"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\"empleado\",\"password\":\"Empleado123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("EMPLEADO"))
                .andExpect(jsonPath("$.password").doesNotExist());
        String empleado = login("empleado", "Empleado123!");
        mvc.perform(get("/bodegas").header("Authorization", "Bearer " + empleado))
                .andExpect(status().isOk());
        mvc.perform(
                        post("/productos")
                                .header("Authorization", "Bearer " + empleado)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"A\",\"categoria\":\"B\",\"precio\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/auditorias").header("Authorization", "Bearer " + empleado))
                .andExpect(status().isForbidden());
        mover(movimiento("ENTRADA", null, origen, 5), empleado)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").value("empleado"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT password FROM usuario WHERE username='empleado'",
                                String.class))
                .startsWith("$2");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM auditoria WHERE usuario='empleado'",
                                Long.class))
                .isGreaterThanOrEqualTo(3);
        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\"hacker\",\"password\":\"Empleado123!\",\"rol\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tokenExpiradoRechazado() throws Exception {
        String expirado =
                Jwts.builder()
                        .subject("admin")
                        .issuer("logitrack")
                        .expiration(new Date(1))
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        "clave-pruebas-logitrack-12345678901234567890"
                                                .getBytes(StandardCharsets.UTF_8)))
                        .compact();
        mvc.perform(get("/productos").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void filtrosDeFechasYOpenApi() throws Exception {
        mover(movimiento("ENTRADA", null, origen, 5)).andExpect(status().isCreated());
        mvc.perform(
                        get("/movimientos")
                                .param("desde", "2000-01-01T00:00:00")
                                .param("hasta", "2099-01-01T00:00:00")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(
                        get("/movimientos")
                                .param("desde", "2000-01-01T00:00:00")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        get("/movimientos")
                                .param("desde", "2099-01-01T00:00:00")
                                .param("hasta", "2000-01-01T00:00:00")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/auth/login'].post.security").doesNotExist());
    }

    @Test
    void noEliminarHistoricosNiReducirCapacidadOEditarStock() throws Exception {
        mover(movimiento("ENTRADA", null, origen, 10)).andExpect(status().isCreated());
        mvc.perform(delete("/productos/" + producto).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/bodegas/" + origen).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        put("/bodegas/" + origen)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"nombre\":\"Bogota\",\"ubicacion\":\"Bogota\",\"capacidad\":5,\"encargado\":\"Ana\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        put("/productos/" + producto)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"nombre\":\"Teclado\",\"categoria\":\"Tecnologia\",\"precio\":1,\"stock\":999}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void errores404Y400() throws Exception {
        mvc.perform(get("/productos/9999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404));
        mvc.perform(get("/productos/abc").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/productos/-1").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/productos")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"\",\"categoria\":\"A\",\"precio\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dosSalidasConcurrentesNoGeneranStockNegativo() throws Exception {
        mover(movimiento("ENTRADA", null, origen, 10)).andExpect(status().isCreated());
        String body = movimiento("SALIDA", origen, null, 7);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        Callable<Integer> tarea =
                () -> {
                    inicio.await();
                    return mover(body).andReturn().getResponse().getStatus();
                };
        try {
            Future<Integer> a = pool.submit(tarea);
            Future<Integer> b = pool.submit(tarea);
            inicio.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 400);
            assertThat(stock(origen)).isEqualTo(3);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT stock FROM producto WHERE id=?", Long.class, producto))
                    .isEqualTo(3);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void falloDeAuditoriaDevuelve500YRevierteInventario() throws Exception {
        jdbc.execute("ALTER TABLE auditoria RENAME TO auditoria_temporal");
        try {
            mover(movimiento("ENTRADA", null, origen, 5))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.mensaje").value("Error interno del servidor"));
            assertThat(stock(origen)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM movimiento", Long.class)).isZero();
        } finally {
            jdbc.execute("ALTER TABLE auditoria_temporal RENAME TO auditoria");
        }
    }

    @Test
    void jwtSinRolesYPermisosActualizadosDesdeLaBaseDeDatos() throws Exception {
        var claims =
                Jwts.parser()
                        .verifyWith(
                                Keys.hmacShaKeyFor(
                                        "clave-pruebas-logitrack-12345678901234567890"
                                                .getBytes(StandardCharsets.UTF_8)))
                        .requireIssuer("logitrack")
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
        assertThat(claims.getSubject()).isEqualTo("admin");
        assertThat(claims).doesNotContainKeys("rol", "roles", "authorities");
        try {
            jdbc.update("UPDATE usuario SET rol='EMPLEADO' WHERE username='admin'");
            mvc.perform(get("/auditorias").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        } finally {
            jdbc.update("UPDATE usuario SET rol='ADMIN' WHERE username='admin'");
        }
        // Mismo token, sin volver a iniciar sesion: el permiso se toma de la BD.
        mvc.perform(get("/auditorias").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void unRolDeclaradoEnUnTokenNoConcedePermisos() throws Exception {
        String tokenConRol =
                Jwts.builder()
                        .subject("admin")
                        .issuer("logitrack")
                        .claim("rol", "ADMIN")
                        .expiration(new Date(System.currentTimeMillis() + 60000))
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        "clave-pruebas-logitrack-12345678901234567890"
                                                .getBytes(StandardCharsets.UTF_8)))
                        .compact();
        try {
            jdbc.update("UPDATE usuario SET rol='EMPLEADO' WHERE username='admin'");
            mvc.perform(get("/auditorias").header("Authorization", "Bearer " + tokenConRol))
                    .andExpect(status().isForbidden());
        } finally {
            jdbc.update("UPDATE usuario SET rol='ADMIN' WHERE username='admin'");
        }
    }
}
