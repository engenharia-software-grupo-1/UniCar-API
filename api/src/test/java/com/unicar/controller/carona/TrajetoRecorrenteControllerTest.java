package com.unicar.controller.carona;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicar.domain.Usuario;
import com.unicar.dto.carona.EnderecoDTO;
import com.unicar.dto.trajeto.TrajetoRecorrenteDTO;
import com.unicar.dto.trajeto.TrajetoRecorrenteDetalhesDTO;
import com.unicar.dto.trajeto.TrajetoRecorrenteRecriarRequestDTO;
import com.unicar.dto.trajeto.TrajetoRecorrenteRecriarResponseDTO;
import com.unicar.enums.StatusCarona;
import com.unicar.exception.TrajetoRecorrenteNaoEncontradoException;
import com.unicar.security.JwtAuthenticationFilter;
import com.unicar.security.UsuarioDetails;
import com.unicar.service.carona.TrajetoRecorrenteService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrajetoRecorrenteController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrajetoRecorrenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrajetoRecorrenteService trajetoRecorrenteService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private static final Long USUARIO_ID = 1L;
    private static final String TRAJETO_ID = "3f1c9c3a-0000-0000-0000-000000000000";

    @BeforeEach
    void setup() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setEmail("teste@email.com");

        UsuarioDetails usuarioDetails = new UsuarioDetails(usuario);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        usuarioDetails,
                        null,
                        usuarioDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /trajetos-recorrentes")
    class Listar {

        @Test
        @DisplayName("deve retornar a lista de trajetos recorrentes do motorista autenticado")
        void deveListar() throws Exception {
            List<TrajetoRecorrenteDTO> trajetos = List.of(new TrajetoRecorrenteDTO(
                    TRAJETO_ID,
                    new EnderecoDTO("Bodocongó", new BigDecimal("-7.21456"), new BigDecimal("-35.90872")),
                    new EnderecoDTO("UFCG", new BigDecimal("-7.21590"), new BigDecimal("-35.90950")),
                    8,
                    LocalDateTime.of(2026, 6, 20, 8, 0)));

            when(trajetoRecorrenteService.listar(USUARIO_ID)).thenReturn(trajetos);

            mockMvc.perform(get("/trajetos-recorrentes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(TRAJETO_ID))
                    .andExpect(jsonPath("$[0].origem.descricao").value("Bodocongó"))
                    .andExpect(jsonPath("$[0].quantidadeViagens").value(8));

            verify(trajetoRecorrenteService).listar(USUARIO_ID);
        }
    }

    @Nested
    @DisplayName("GET /trajetos-recorrentes/{id}")
    class Buscar {

        @Test
        @DisplayName("deve retornar os detalhes de um trajeto recorrente")
        void deveBuscarDetalhes() throws Exception {
            TrajetoRecorrenteDetalhesDTO detalhes = new TrajetoRecorrenteDetalhesDTO(
                    TRAJETO_ID,
                    new EnderecoDTO("Bodocongó", new BigDecimal("-7.21456"), new BigDecimal("-35.90872")),
                    new EnderecoDTO("UFCG", new BigDecimal("-7.21590"), new BigDecimal("-35.90950")),
                    8,
                    LocalDateTime.of(2026, 1, 15, 7, 0),
                    LocalDateTime.of(2026, 6, 20, 8, 0));

            when(trajetoRecorrenteService.buscar(TRAJETO_ID, USUARIO_ID)).thenReturn(detalhes);

            mockMvc.perform(get("/trajetos-recorrentes/{id}", TRAJETO_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TRAJETO_ID))
                    .andExpect(jsonPath("$.quantidadeViagens").value(8));

            verify(trajetoRecorrenteService).buscar(TRAJETO_ID, USUARIO_ID);
        }

        @Test
        @DisplayName("deve retornar 404 quando o trajeto não existir")
        void deveRetornar404QuandoNaoExistir() throws Exception {
            when(trajetoRecorrenteService.buscar(TRAJETO_ID, USUARIO_ID))
                    .thenThrow(new TrajetoRecorrenteNaoEncontradoException("Trajeto recorrente não encontrado"));

            mockMvc.perform(get("/trajetos-recorrentes/{id}", TRAJETO_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Trajeto recorrente não encontrado"));
        }
    }

    @Nested
    @DisplayName("POST /trajetos-recorrentes/{id}/recriar")
    class Recriar {

        @Test
        @DisplayName("deve retornar 201 ao recriar uma carona a partir do trajeto")
        void deveRecriar() throws Exception {
            TrajetoRecorrenteRecriarRequestDTO request = new TrajetoRecorrenteRecriarRequestDTO(
                    1L, LocalDateTime.now().plusDays(5), 4, new BigDecimal("5.00"), "Portão principal");

            TrajetoRecorrenteRecriarResponseDTO response =
                    new TrajetoRecorrenteRecriarResponseDTO(50L, StatusCarona.CRIADA);

            when(trajetoRecorrenteService.recriar(eq(TRAJETO_ID), any(), eq(USUARIO_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/trajetos-recorrentes/{id}/recriar", TRAJETO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.caronaId").value(50))
                    .andExpect(jsonPath("$.status").value("CRIADA"));

            verify(trajetoRecorrenteService).recriar(eq(TRAJETO_ID), any(), eq(USUARIO_ID));
        }

        @Test
        @DisplayName("deve retornar 400 quando o payload for inválido")
        void deveRetornar400QuandoPayloadInvalido() throws Exception {
            TrajetoRecorrenteRecriarRequestDTO request = new TrajetoRecorrenteRecriarRequestDTO(
                    null, null, null, null, null);

            mockMvc.perform(post("/trajetos-recorrentes/{id}/recriar", TRAJETO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 404 quando o trajeto não existir")
        void deveRetornar404QuandoTrajetoNaoExistir() throws Exception {
            TrajetoRecorrenteRecriarRequestDTO request = new TrajetoRecorrenteRecriarRequestDTO(
                    1L, LocalDateTime.now().plusDays(5), 4, new BigDecimal("5.00"), "Portão principal");

            when(trajetoRecorrenteService.recriar(eq(TRAJETO_ID), any(), eq(USUARIO_ID)))
                    .thenThrow(new TrajetoRecorrenteNaoEncontradoException("Trajeto recorrente não encontrado"));

            mockMvc.perform(post("/trajetos-recorrentes/{id}/recriar", TRAJETO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }
}
