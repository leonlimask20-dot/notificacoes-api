package com.leonlima.notificacoes.servico;

import com.leonlima.notificacoes.dto.NotificacaoDTO;
import com.leonlima.notificacoes.enums.StatusNotificacao;
import com.leonlima.notificacoes.enums.TipoNotificacao;
import com.leonlima.notificacoes.excecao.RateLimitExcedidoException;
import com.leonlima.notificacoes.excecao.RecursoNaoEncontradoException;
import com.leonlima.notificacoes.modelo.Notificacao;
import com.leonlima.notificacoes.modelo.PreferenciasUsuario;
import com.leonlima.notificacoes.repositorio.NotificacaoRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * ServicoNotificacao — lógica de negócio para envio e gestão de notificações.
 *
 * Fluxo de envio:
 * 1. Verifica rate limiting no Redis — bloqueia se excedeu o limite
 * 2. Busca preferências no Redis (cache) — fallback para padrão se não encontrado
 * 3. Valida se o canal está habilitado nas preferências
 * 4. Persiste a notificação no MongoDB
 * 5. Simula o envio (em produção integraria com SendGrid, Twilio, FCM etc.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServicoNotificacao {

    private final NotificacaoRepositorio repositorio;
    private final ServicoRedis servicoRedis;

    public NotificacaoDTO.Resposta enviar(NotificacaoDTO.Requisicao req) {

        // 1. Rate limiting — Redis verifica se está dentro do limite
        if (!servicoRedis.verificarRateLimit(req.getUsuarioId())) {
            throw new RateLimitExcedidoException(
                "Limite de notificações por minuto excedido para o usuário: " + req.getUsuarioId()
            );
        }

        // 2. Preferências — busca no Redis (cache) ou usa padrão
        PreferenciasUsuario prefs = servicoRedis
            .buscarPreferencias(req.getUsuarioId())
            .orElse(preferenciasDefault(req.getUsuarioId()));

        // 3. Valida canal habilitado
        if (!prefs.isNotificacoesAtivas()) {
            log.info("Notificações desativadas para o usuário {}", req.getUsuarioId());
            return persistirComStatus(req, StatusNotificacao.FALHOU);
        }

        if (!prefs.getCanaisHabilitados().contains(req.getTipo())) {
            log.info("Canal {} desabilitado para o usuário {}", req.getTipo(), req.getUsuarioId());
            return persistirComStatus(req, StatusNotificacao.FALHOU);
        }

        // 4. Persiste como PENDENTE
        Notificacao notificacao = Notificacao.builder()
            .usuarioId(req.getUsuarioId())
            .destinatario(req.getDestinatario())
            .titulo(req.getTitulo())
            .conteudo(req.getConteudo())
            .tipo(req.getTipo())
            .status(StatusNotificacao.PENDENTE)
            .metadados(req.getMetadados())
            .criadoEm(LocalDateTime.now())
            .build();

        notificacao = repositorio.save(notificacao);

        // 5. Simula envio — em produção integraria com SendGrid, Twilio, FCM
        notificacao.setStatus(StatusNotificacao.ENVIADA);
        notificacao.setEnviadoEm(LocalDateTime.now());
        notificacao = repositorio.save(notificacao);

        log.info("Notificação {} enviada para {}", notificacao.getId(), req.getDestinatario());
        return mapearResposta(notificacao);
    }

    public List<NotificacaoDTO.Resposta> listarPorUsuario(String usuarioId) {
        return repositorio.findByUsuarioId(usuarioId)
            .stream().map(this::mapearResposta).toList();
    }

    public List<NotificacaoDTO.Resposta> listarPorStatus(String usuarioId, StatusNotificacao status) {
        return repositorio.findByUsuarioIdAndStatus(usuarioId, status)
            .stream().map(this::mapearResposta).toList();
    }

    public NotificacaoDTO.Resposta marcarComoLida(String id) {
        Notificacao notificacao = repositorio.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada: " + id));

        notificacao.setStatus(StatusNotificacao.LIDA);
        notificacao.setLidoEm(LocalDateTime.now());
        return mapearResposta(repositorio.save(notificacao));
    }

    public NotificacaoDTO.ResumoUsuario resumo(String usuarioId) {
        return NotificacaoDTO.ResumoUsuario.builder()
            .usuarioId(usuarioId)
            .totalEnviadas(repositorio.countByUsuarioIdAndStatus(usuarioId, StatusNotificacao.ENVIADA))
            .totalPendentes(repositorio.countByUsuarioIdAndStatus(usuarioId, StatusNotificacao.PENDENTE))
            .totalFalharam(repositorio.countByUsuarioIdAndStatus(usuarioId, StatusNotificacao.FALHOU))
            .totalLidas(repositorio.countByUsuarioIdAndStatus(usuarioId, StatusNotificacao.LIDA))
            .build();
    }

    public void salvarPreferencias(String usuarioId, NotificacaoDTO.PreferenciasRequisicao req) {
        PreferenciasUsuario prefs = PreferenciasUsuario.builder()
            .usuarioId(usuarioId)
            .canaisHabilitados(req.getCanaisHabilitados())
            .notificacoesAtivas(req.isNotificacoesAtivas())
            .horaSilencioInicio(req.getHoraSilencioInicio())
            .horaSilencioFim(req.getHoraSilencioFim())
            .build();

        servicoRedis.salvarPreferencias(usuarioId, prefs);
    }

    // ── Métodos privados ──────────────────────────────────────────────────────

    private PreferenciasUsuario preferenciasDefault(String usuarioId) {
        return PreferenciasUsuario.builder()
            .usuarioId(usuarioId)
            .notificacoesAtivas(true)
            .canaisHabilitados(Set.of(TipoNotificacao.values()))
            .build();
    }

    private NotificacaoDTO.Resposta persistirComStatus(
            NotificacaoDTO.Requisicao req, StatusNotificacao status) {
        Notificacao n = Notificacao.builder()
            .usuarioId(req.getUsuarioId())
            .destinatario(req.getDestinatario())
            .titulo(req.getTitulo())
            .conteudo(req.getConteudo())
            .tipo(req.getTipo())
            .status(status)
            .metadados(req.getMetadados())
            .criadoEm(LocalDateTime.now())
            .build();
        return mapearResposta(repositorio.save(n));
    }

    private NotificacaoDTO.Resposta mapearResposta(Notificacao n) {
        return NotificacaoDTO.Resposta.builder()
            .id(n.getId())
            .usuarioId(n.getUsuarioId())
            .destinatario(n.getDestinatario())
            .titulo(n.getTitulo())
            .conteudo(n.getConteudo())
            .tipo(n.getTipo())
            .status(n.getStatus())
            .metadados(n.getMetadados())
            .criadoEm(n.getCriadoEm())
            .enviadoEm(n.getEnviadoEm())
            .build();
    }
}
