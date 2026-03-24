package com.leonlima.notificacoes.servico;

import com.leonlima.notificacoes.dto.NotificacaoDTO;
import com.leonlima.notificacoes.enums.StatusNotificacao;
import com.leonlima.notificacoes.enums.TipoNotificacao;
import com.leonlima.notificacoes.excecao.RateLimitExcedidoException;
import com.leonlima.notificacoes.excecao.RecursoNaoEncontradoException;
import com.leonlima.notificacoes.modelo.Notificacao;
import com.leonlima.notificacoes.modelo.PreferenciasUsuario;
import com.leonlima.notificacoes.repositorio.NotificacaoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do ServicoNotificacao.
 *
 * O MongoDB e o Redis são mockados — os testes rodam sem nenhuma
 * infraestrutura externa, apenas lógica de negócio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServicoNotificacao — testes unitários")
class ServicoNotificacaoTest {

    @Mock
    private NotificacaoRepositorio repositorio;

    @Mock
    private ServicoRedis servicoRedis;

    @InjectMocks
    private ServicoNotificacao servico;

    private NotificacaoDTO.Requisicao requisicaoValida;
    private Notificacao notificacaoSalva;

    @BeforeEach
    void setUp() {
        requisicaoValida = NotificacaoDTO.Requisicao.builder()
            .usuarioId("usuario-123")
            .destinatario("leon@email.com")
            .titulo("Teste")
            .conteudo("Conteúdo do teste")
            .tipo(TipoNotificacao.EMAIL)
            .build();

        notificacaoSalva = Notificacao.builder()
            .id("notif-001")
            .usuarioId("usuario-123")
            .destinatario("leon@email.com")
            .titulo("Teste")
            .conteudo("Conteúdo do teste")
            .tipo(TipoNotificacao.EMAIL)
            .status(StatusNotificacao.ENVIADA)
            .criadoEm(LocalDateTime.now())
            .enviadoEm(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("deve enviar notificação com sucesso quando dentro do rate limit")
    void enviar_dentroDoRateLimit_retornaNotificacaoEnviada() {
        when(servicoRedis.verificarRateLimit("usuario-123")).thenReturn(true);
        when(servicoRedis.buscarPreferencias("usuario-123")).thenReturn(Optional.empty());
        when(repositorio.save(any())).thenReturn(notificacaoSalva);

        NotificacaoDTO.Resposta resposta = servico.enviar(requisicaoValida);

        assertThat(resposta.getId()).isEqualTo("notif-001");
        assertThat(resposta.getStatus()).isEqualTo(StatusNotificacao.ENVIADA);
        verify(repositorio, times(2)).save(any()); // salva PENDENTE e depois ENVIADA
    }

    @Test
    @DisplayName("deve lançar RateLimitExcedidoException quando limite ultrapassado")
    void enviar_rateLimitExcedido_lancaExcecao() {
        when(servicoRedis.verificarRateLimit("usuario-123")).thenReturn(false);

        assertThatThrownBy(() -> servico.enviar(requisicaoValida))
            .isInstanceOf(RateLimitExcedidoException.class)
            .hasMessageContaining("usuario-123");

        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("deve usar preferências do Redis quando disponíveis no cache")
    void enviar_preferenciasNoCache_usaRedis() {
        PreferenciasUsuario prefs = PreferenciasUsuario.builder()
            .usuarioId("usuario-123")
            .notificacoesAtivas(true)
            .canaisHabilitados(Set.of(TipoNotificacao.EMAIL))
            .build();

        when(servicoRedis.verificarRateLimit("usuario-123")).thenReturn(true);
        when(servicoRedis.buscarPreferencias("usuario-123")).thenReturn(Optional.of(prefs));
        when(repositorio.save(any())).thenReturn(notificacaoSalva);

        servico.enviar(requisicaoValida);

        // Verifica que o Redis foi consultado (cache hit)
        verify(servicoRedis).buscarPreferencias("usuario-123");
    }

    @Test
    @DisplayName("deve salvar notificação com FALHOU quando canal está desabilitado")
    void enviar_canalDesabilitado_salvaComFalhou() {
        PreferenciasUsuario prefs = PreferenciasUsuario.builder()
            .usuarioId("usuario-123")
            .notificacoesAtivas(true)
            // Apenas SMS habilitado — EMAIL está desabilitado
            .canaisHabilitados(Set.of(TipoNotificacao.SMS))
            .build();

        Notificacao notifFalhou = Notificacao.builder()
            .id("notif-002")
            .status(StatusNotificacao.FALHOU)
            .criadoEm(LocalDateTime.now())
            .build();

        when(servicoRedis.verificarRateLimit("usuario-123")).thenReturn(true);
        when(servicoRedis.buscarPreferencias("usuario-123")).thenReturn(Optional.of(prefs));
        when(repositorio.save(any())).thenReturn(notifFalhou);

        NotificacaoDTO.Resposta resposta = servico.enviar(requisicaoValida);

        assertThat(resposta.getStatus()).isEqualTo(StatusNotificacao.FALHOU);
        // Salva apenas uma vez (não simula envio)
        verify(repositorio, times(1)).save(any());
    }

    @Test
    @DisplayName("deve salvar notificação com FALHOU quando notificações desativadas")
    void enviar_notificacoesDesativas_salvaComFalhou() {
        PreferenciasUsuario prefs = PreferenciasUsuario.builder()
            .usuarioId("usuario-123")
            .notificacoesAtivas(false) // desativadas
            .canaisHabilitados(Set.of(TipoNotificacao.values()))
            .build();

        Notificacao notifFalhou = Notificacao.builder()
            .id("notif-003")
            .status(StatusNotificacao.FALHOU)
            .criadoEm(LocalDateTime.now())
            .build();

        when(servicoRedis.verificarRateLimit("usuario-123")).thenReturn(true);
        when(servicoRedis.buscarPreferencias("usuario-123")).thenReturn(Optional.of(prefs));
        when(repositorio.save(any())).thenReturn(notifFalhou);

        NotificacaoDTO.Resposta resposta = servico.enviar(requisicaoValida);

        assertThat(resposta.getStatus()).isEqualTo(StatusNotificacao.FALHOU);
    }

    @Test
    @DisplayName("deve lançar RecursoNaoEncontradoException ao marcar notificação inexistente como lida")
    void marcarComoLida_notificacaoNaoEncontrada_lancaExcecao() {
        when(repositorio.findById("id-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.marcarComoLida("id-inexistente"))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessageContaining("id-inexistente");
    }

    @Test
    @DisplayName("deve retornar resumo correto com contagens por status")
    void resumo_retornaContagensCorretas() {
        when(repositorio.countByUsuarioIdAndStatus("usuario-123", StatusNotificacao.ENVIADA)).thenReturn(10L);
        when(repositorio.countByUsuarioIdAndStatus("usuario-123", StatusNotificacao.PENDENTE)).thenReturn(2L);
        when(repositorio.countByUsuarioIdAndStatus("usuario-123", StatusNotificacao.FALHOU)).thenReturn(1L);
        when(repositorio.countByUsuarioIdAndStatus("usuario-123", StatusNotificacao.LIDA)).thenReturn(7L);

        NotificacaoDTO.ResumoUsuario resumo = servico.resumo("usuario-123");

        assertThat(resumo.getTotalEnviadas()).isEqualTo(10L);
        assertThat(resumo.getTotalPendentes()).isEqualTo(2L);
        assertThat(resumo.getTotalFalharam()).isEqualTo(1L);
        assertThat(resumo.getTotalLidas()).isEqualTo(7L);
    }

    @Test
    @DisplayName("deve listar notificações do usuário")
    void listarPorUsuario_retornaListaCorreta() {
        when(repositorio.findByUsuarioId("usuario-123"))
            .thenReturn(List.of(notificacaoSalva));

        List<NotificacaoDTO.Resposta> resultado = servico.listarPorUsuario("usuario-123");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo("notif-001");
    }
}
