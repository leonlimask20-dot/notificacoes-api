package com.leonlima.notificacoes.servico;

import com.leonlima.notificacoes.modelo.PreferenciasUsuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * ServicoRedis — encapsula todas as operações com o Redis.
 *
 * O Redis é usado aqui para duas finalidades:
 *
 * 1. CACHE DE PREFERÊNCIAS:
 *    Preferências são lidas a cada envio de notificação.
 *    Buscar no MongoDB a cada vez seria lento. O Redis mantém
 *    em memória com TTL de 5 minutos — depois busca no MongoDB de novo.
 *
 * 2. RATE LIMITING:
 *    Controla quantas notificações um usuário pode enviar por minuto.
 *    Usa um contador com expiração de 60 segundos no Redis.
 *    Muito mais eficiente do que queries no banco para isso.
 *
 * RedisTemplate: API de baixo nível para operações no Redis.
 * Equivalente ao EntityManager do JPA.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServicoRedis {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${notificacoes.rate-limit.max-por-minuto}")
    private int maxPorMinuto;

    private static final String PREFIXO_PREFERENCIAS = "preferencias:";
    private static final String PREFIXO_RATE_LIMIT    = "rate_limit:";

    // ── Preferências ──────────────────────────────────────────────────────────

    public void salvarPreferencias(String usuarioId, PreferenciasUsuario preferencias) {
        String chave = PREFIXO_PREFERENCIAS + usuarioId;
        // TTL de 5 minutos — depois o cache expira e busca do banco de novo
        redisTemplate.opsForValue().set(chave, preferencias, Duration.ofMinutes(5));
        log.info("Preferências do usuário {} salvas no Redis", usuarioId);
    }

    public Optional<PreferenciasUsuario> buscarPreferencias(String usuarioId) {
        String chave = PREFIXO_PREFERENCIAS + usuarioId;
        Object valor = redisTemplate.opsForValue().get(chave);
        if (valor instanceof PreferenciasUsuario preferencias) {
            log.info("Cache hit — preferências do usuário {} encontradas no Redis", usuarioId);
            return Optional.of(preferencias);
        }
        log.info("Cache miss — preferências do usuário {} não estão no Redis", usuarioId);
        return Optional.empty();
    }

    public void invalidarPreferencias(String usuarioId) {
        redisTemplate.delete(PREFIXO_PREFERENCIAS + usuarioId);
        log.info("Cache de preferências do usuário {} invalidado", usuarioId);
    }

    // ── Rate Limiting ─────────────────────────────────────────────────────────

    /**
     * Verifica se o usuário está dentro do limite de envios por minuto.
     *
     * Estratégia: INCR + EXPIRE no Redis
     * 1. Incrementa o contador do usuário
     * 2. Se é o primeiro acesso, define expiração de 60 segundos
     * 3. Se o contador ultrapassou o limite, bloqueia
     *
     * Isso é thread-safe porque INCR no Redis é atômico.
     */
    public boolean verificarRateLimit(String usuarioId) {
        String chave = PREFIXO_RATE_LIMIT + usuarioId;

        Long contador = redisTemplate.opsForValue().increment(chave);

        if (contador == null) return false;

        // Define TTL de 60 segundos na primeira chamada
        if (contador == 1) {
            redisTemplate.expire(chave, Duration.ofMinutes(1));
        }

        boolean dentro = contador <= maxPorMinuto;
        if (!dentro) {
            log.warn("Rate limit excedido para usuário {} — {} envios no último minuto", usuarioId, contador);
        }
        return dentro;
    }

    public Long contadorAtual(String usuarioId) {
        String chave = PREFIXO_RATE_LIMIT + usuarioId;
        Object valor = redisTemplate.opsForValue().get(chave);
        return valor != null ? Long.parseLong(valor.toString()) : 0L;
    }
}
