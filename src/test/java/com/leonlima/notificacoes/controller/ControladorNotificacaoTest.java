package com.leonlima.notificacoes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leonlima.notificacoes.dto.NotificacaoDTO;
import com.leonlima.notificacoes.enums.StatusNotificacao;
import com.leonlima.notificacoes.enums.TipoNotificacao;
import com.leonlima.notificacoes.excecao.RateLimitExcedidoException;
import com.leonlima.notificacoes.excecao.RecursoNaoEncontradoException;
import com.leonlima.notificacoes.servico.ServicoNotificacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ControladorNotificacao.class)
@DisplayName("ControladorNotificacao — testes de integração MockMvc")
class ControladorNotificacaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ServicoNotificacao servicoNotificacao;

    private NotificacaoDTO.Resposta respostaValida() {
        return NotificacaoDTO.Resposta.builder()
            .id("notif-001")
            .usuarioId("usuario-123")
            .destinatario("leon@email.com")
            .titulo("Teste")
            .conteudo("Conteudo")
            .tipo(TipoNotificacao.EMAIL)
            .status(StatusNotificacao.ENVIADA)
            .criadoEm(LocalDateTime.now())
            .enviadoEm(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("POST /api/notificacoes — deve retornar 201 com notificacao enviada")
    void enviar_requisicaoValida_retorna201() throws Exception {
        NotificacaoDTO.Requisicao req = NotificacaoDTO.Requisicao.builder()
            .usuarioId("usuario-123")
            .destinatario("leon@email.com")
            .titulo("Teste")
            .conteudo("Conteudo")
            .tipo(TipoNotificacao.EMAIL)
            .build();

        when(servicoNotificacao.enviar(any())).thenReturn(respostaValida());

        mockMvc.perform(post("/api/notificacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("notif-001"))
            .andExpect(jsonPath("$.status").value("ENVIADA"));
    }

    @Test
    @DisplayName("POST /api/notificacoes — deve retornar 400 quando dados invalidos")
    void enviar_dadosInvalidos_retorna400() throws Exception {
        NotificacaoDTO.Requisicao req = NotificacaoDTO.Requisicao.builder()
            .usuarioId("")
            .build();

        mockMvc.perform(post("/api/notificacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/notificacoes — deve retornar 429 quando rate limit excedido")
    void enviar_rateLimitExcedido_retorna429() throws Exception {
        NotificacaoDTO.Requisicao req = NotificacaoDTO.Requisicao.builder()
            .usuarioId("usuario-123")
            .destinatario("leon@email.com")
            .titulo("Teste")
            .conteudo("Conteudo")
            .tipo(TipoNotificacao.EMAIL)
            .build();

        when(servicoNotificacao.enviar(any()))
            .thenThrow(new RateLimitExcedidoException("Rate limit excedido"));

        mockMvc.perform(post("/api/notificacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.mensagem").value("Rate limit excedido"));
    }

    @Test
    @DisplayName("GET /api/notificacoes/usuario/{id} — deve retornar lista")
    void listar_usuarioValido_retornaLista() throws Exception {
        when(servicoNotificacao.listarPorUsuario("usuario-123"))
            .thenReturn(List.of(respostaValida()));

        mockMvc.perform(get("/api/notificacoes/usuario/usuario-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("notif-001"));
    }

    @Test
    @DisplayName("GET /api/notificacoes/usuario/{id}/status/{status} — deve filtrar por status")
    void listarPorStatus_retornaFiltrado() throws Exception {
        when(servicoNotificacao.listarPorStatus("usuario-123", StatusNotificacao.ENVIADA))
            .thenReturn(List.of(respostaValida()));

        mockMvc.perform(get("/api/notificacoes/usuario/usuario-123/status/ENVIADA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("ENVIADA"));
    }

    @Test
    @DisplayName("PATCH /api/notificacoes/{id}/lida — deve marcar como lida")
    void marcarLida_notificacaoExistente_retornaAtualizada() throws Exception {
        when(servicoNotificacao.marcarComoLida("notif-001")).thenReturn(respostaValida());

        mockMvc.perform(patch("/api/notificacoes/notif-001/lida"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("notif-001"));
    }

    @Test
    @DisplayName("PATCH /api/notificacoes/{id}/lida — deve retornar 404 quando nao encontrada")
    void marcarLida_notificacaoNaoEncontrada_retorna404() throws Exception {
        when(servicoNotificacao.marcarComoLida("id-invalido"))
            .thenThrow(new RecursoNaoEncontradoException("Notificacao nao encontrada: id-invalido"));

        mockMvc.perform(patch("/api/notificacoes/id-invalido/lida"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/notificacoes/usuario/{id}/resumo — deve retornar resumo")
    void resumo_retornaContagens() throws Exception {
        NotificacaoDTO.ResumoUsuario resumo = NotificacaoDTO.ResumoUsuario.builder()
            .usuarioId("usuario-123")
            .totalEnviadas(10L)
            .totalPendentes(2L)
            .totalFalharam(1L)
            .totalLidas(5L)
            .build();

        when(servicoNotificacao.resumo("usuario-123")).thenReturn(resumo);

        mockMvc.perform(get("/api/notificacoes/usuario/usuario-123/resumo"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalEnviadas").value(10));
    }

    @Test
    @DisplayName("PUT /api/notificacoes/usuario/{id}/preferencias — deve salvar preferencias")
    void salvarPreferencias_retorna204() throws Exception {
        NotificacaoDTO.PreferenciasRequisicao req = NotificacaoDTO.PreferenciasRequisicao.builder()
            .canaisHabilitados(Set.of(TipoNotificacao.EMAIL, TipoNotificacao.SMS))
            .notificacoesAtivas(true)
            .build();

        doNothing().when(servicoNotificacao).salvarPreferencias(eq("usuario-123"), any());

        mockMvc.perform(put("/api/notificacoes/usuario/usuario-123/preferencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isNoContent());
    }
}
