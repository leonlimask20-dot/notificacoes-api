package com.leonlima.notificacoes.servico;

import com.leonlima.notificacoes.enums.TipoNotificacao;
import com.leonlima.notificacoes.modelo.PreferenciasUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("ServicoRedis — testes unitários")
class ServicoRedisTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ServicoRedis servicoRedis;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(servicoRedis, "maxPorMinuto", 10);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("deve retornar preferências quando encontradas no Redis")
    void buscarPreferencias_cacheHit_retornaPreferencias() {
        PreferenciasUsuario prefs = PreferenciasUsuario.builder()
            .usuarioId("usuario-123")
            .notificacoesAtivas(true)
            .canaisHabilitados(Set.of(TipoNotificacao.EMAIL))
            .build();

        when(valueOperations.get("preferencias:usuario-123")).thenReturn(prefs);

        Optional<PreferenciasUsuario> resultado = servicoRedis.buscarPreferencias("usuario-123");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getUsuarioId()).isEqualTo("usuario-123");
    }

    @Test
    @DisplayName("deve retornar Optional vazio quando não encontrado no Redis")
    void buscarPreferencias_cacheMiss_retornaVazio() {
        when(valueOperations.get("preferencias:usuario-123")).thenReturn(null);

        Optional<PreferenciasUsuario> resultado = servicoRedis.buscarPreferencias("usuario-123");

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("deve retornar Optional vazio quando valor não é PreferenciasUsuario")
    void buscarPreferencias_valorInvalido_retornaVazio() {
        when(valueOperations.get("preferencias:usuario-123")).thenReturn("string-invalida");

        Optional<PreferenciasUsuario> resultado = servicoRedis.buscarPreferencias("usuario-123");

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("deve salvar preferências no Redis com TTL")
    void salvarPreferencias_salvaNaChaveCorreta() {
        PreferenciasUsuario prefs = PreferenciasUsuario.builder()
            .usuarioId("usuario-123")
            .notificacoesAtivas(true)
            .canaisHabilitados(Set.of(TipoNotificacao.EMAIL))
            .build();

        servicoRedis.salvarPreferencias("usuario-123", prefs);

        verify(valueOperations).set(eq("preferencias:usuario-123"), eq(prefs), any());
    }

    @Test
    @DisplayName("deve invalidar cache de preferências do usuário")
    void invalidarPreferencias_deletaChaveCorreta() {
        servicoRedis.invalidarPreferencias("usuario-123");

        verify(redisTemplate).delete("preferencias:usuario-123");
    }

    @Test
    @DisplayName("deve retornar contador atual quando chave existe no Redis")
    void contadorAtual_chaveExiste_retornaContador() {
        when(valueOperations.get("rate_limit:usuario-123")).thenReturn("7");

        Long resultado = servicoRedis.contadorAtual("usuario-123");

        assertThat(resultado).isEqualTo(7L);
    }

    @Test
    @DisplayName("deve retornar zero quando chave não existe no Redis")
    void contadorAtual_chaveNaoExiste_retornaZero() {
        when(valueOperations.get("rate_limit:usuario-123")).thenReturn(null);

        Long resultado = servicoRedis.contadorAtual("usuario-123");

        assertThat(resultado).isEqualTo(0L);
    }

    @Test
    @DisplayName("deve retornar false quando Redis retorna null no increment")
    void verificarRateLimit_redisRetornaNull_retornaFalse() {
        when(valueOperations.increment("rate_limit:usuario-123")).thenReturn(null);

        boolean resultado = servicoRedis.verificarRateLimit("usuario-123");

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("deve permitir envio quando dentro do rate limit")
    void verificarRateLimit_dentroDoLimite_retornaTrue() {
        when(valueOperations.increment("rate_limit:usuario-123")).thenReturn(5L);

        boolean resultado = servicoRedis.verificarRateLimit("usuario-123");

        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("deve bloquear envio quando rate limit excedido")
    void verificarRateLimit_limiteExcedido_retornaFalse() {
        when(valueOperations.increment("rate_limit:usuario-123")).thenReturn(11L);

        boolean resultado = servicoRedis.verificarRateLimit("usuario-123");

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("deve definir TTL na primeira chamada do rate limit")
    void verificarRateLimit_primeiraChama_defineTTL() {
        when(valueOperations.increment("rate_limit:usuario-123")).thenReturn(1L);

        servicoRedis.verificarRateLimit("usuario-123");

        verify(redisTemplate).expire(eq("rate_limit:usuario-123"), any());
    }

    @Test
    @DisplayName("não deve definir TTL quando não é a primeira chamada")
    void verificarRateLimit_naoEPrimeiraChama_naoDefineTTL() {
        when(valueOperations.increment("rate_limit:usuario-123")).thenReturn(5L);

        servicoRedis.verificarRateLimit("usuario-123");

        verify(redisTemplate, never()).expire(any(), any());
    }
}
