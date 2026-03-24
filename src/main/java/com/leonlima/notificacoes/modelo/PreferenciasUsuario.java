package com.leonlima.notificacoes.modelo;

import com.leonlima.notificacoes.enums.TipoNotificacao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * Preferências de notificação do usuário — armazenadas no Redis.
 *
 * Por que Redis e não MongoDB para isso?
 * - Preferências são lidas em TODA requisição de envio de notificação
 * - Redis é ~10x mais rápido que MongoDB para leituras simples
 * - Dados ficam em memória com TTL configurável
 *
 * Implementa Serializable porque o Redis serializa objetos Java
 * para armazená-los como bytes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciasUsuario implements Serializable {

    private String usuarioId;

    // Canais que o usuário habilitou para receber notificações
    private Set<TipoNotificacao> canaisHabilitados;

    // Se false, não envia nenhuma notificação
    private boolean notificacoesAtivas;

    // Horário de silêncio — não envia entre horaInicio e horaFim
    private Integer horaSilencioInicio;
    private Integer horaSilencioFim;
}
