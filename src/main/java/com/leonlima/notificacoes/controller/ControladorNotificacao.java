package com.leonlima.notificacoes.controller;

import com.leonlima.notificacoes.dto.NotificacaoDTO;
import com.leonlima.notificacoes.enums.StatusNotificacao;
import com.leonlima.notificacoes.servico.ServicoNotificacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
@Tag(name = "Notificações", description = "Envio e gestão de notificações")
public class ControladorNotificacao {

    private final ServicoNotificacao servico;

    @PostMapping
    @Operation(summary = "Enviar notificação")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificacaoDTO.Resposta enviar(@Valid @RequestBody NotificacaoDTO.Requisicao req) {
        return servico.enviar(req);
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Listar notificações do usuário")
    public List<NotificacaoDTO.Resposta> listar(@PathVariable String usuarioId) {
        return servico.listarPorUsuario(usuarioId);
    }

    @GetMapping("/usuario/{usuarioId}/status/{status}")
    @Operation(summary = "Listar notificações por status")
    public List<NotificacaoDTO.Resposta> listarPorStatus(
            @PathVariable String usuarioId,
            @PathVariable StatusNotificacao status) {
        return servico.listarPorStatus(usuarioId, status);
    }

    @PatchMapping("/{id}/lida")
    @Operation(summary = "Marcar notificação como lida")
    public NotificacaoDTO.Resposta marcarLida(@PathVariable String id) {
        return servico.marcarComoLida(id);
    }

    @GetMapping("/usuario/{usuarioId}/resumo")
    @Operation(summary = "Resumo de notificações do usuário")
    public NotificacaoDTO.ResumoUsuario resumo(@PathVariable String usuarioId) {
        return servico.resumo(usuarioId);
    }

    @PutMapping("/usuario/{usuarioId}/preferencias")
    @Operation(summary = "Salvar preferências de notificação no Redis")
    public ResponseEntity<Void> salvarPreferencias(
            @PathVariable String usuarioId,
            @Valid @RequestBody NotificacaoDTO.PreferenciasRequisicao req) {
        servico.salvarPreferencias(usuarioId, req);
        return ResponseEntity.noContent().build();
    }
}
