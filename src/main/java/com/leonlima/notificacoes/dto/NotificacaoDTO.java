package com.leonlima.notificacoes.dto;

import com.leonlima.notificacoes.enums.StatusNotificacao;
import com.leonlima.notificacoes.enums.TipoNotificacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public class NotificacaoDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Dados para envio de uma notificação")
    public static class Requisicao {

        @NotBlank(message = "O ID do usuário é obrigatório")
        @Schema(example = "usuario-123")
        private String usuarioId;

        @NotBlank(message = "O destinatário é obrigatório")
        @Schema(example = "leon@email.com")
        private String destinatario;

        @NotBlank(message = "O título é obrigatório")
        @Schema(example = "Sua compra foi confirmada")
        private String titulo;

        @NotBlank(message = "O conteúdo é obrigatório")
        @Schema(example = "Seu pedido #12345 foi confirmado com sucesso.")
        private String conteudo;

        @NotNull(message = "O tipo é obrigatório")
        @Schema(example = "EMAIL")
        private TipoNotificacao tipo;

        @Schema(description = "Campos extras específicos do tipo de notificação")
        private Map<String, Object> metadados;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resposta {
        private String id;
        private String usuarioId;
        private String destinatario;
        private String titulo;
        private String conteudo;
        private TipoNotificacao tipo;
        private StatusNotificacao status;
        private Map<String, Object> metadados;
        private LocalDateTime criadoEm;
        private LocalDateTime enviadoEm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Preferências de notificação do usuário")
    public static class PreferenciasRequisicao {
        @NotNull
        private Set<TipoNotificacao> canaisHabilitados;
        private boolean notificacoesAtivas;
        private Integer horaSilencioInicio;
        private Integer horaSilencioFim;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumoUsuario {
        private String usuarioId;
        private long totalEnviadas;
        private long totalPendentes;
        private long totalFalharam;
        private long totalLidas;
    }
}
