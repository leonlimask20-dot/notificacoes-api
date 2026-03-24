package com.leonlima.notificacoes.repositorio;

import com.leonlima.notificacoes.enums.StatusNotificacao;
import com.leonlima.notificacoes.enums.TipoNotificacao;
import com.leonlima.notificacoes.modelo.Notificacao;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório MongoDB para notificações.
 *
 * MongoRepository funciona igual ao JpaRepository — Spring Data
 * gera as queries automaticamente pelo nome dos métodos.
 *
 * Diferença do JPA: não tem JPQL, usa Query por derivação de nome
 * ou @Query com sintaxe MongoDB (JSON).
 *
 * findByUsuarioId → db.notificacoes.find({ usuarioId: "..." })
 * findByUsuarioIdAndStatus → db.notificacoes.find({ usuarioId: "...", status: "..." })
 */
@Repository
public interface NotificacaoRepositorio extends MongoRepository<Notificacao, String> {

    List<Notificacao> findByUsuarioId(String usuarioId);

    List<Notificacao> findByUsuarioIdAndStatus(String usuarioId, StatusNotificacao status);

    List<Notificacao> findByUsuarioIdAndTipo(String usuarioId, TipoNotificacao tipo);

    long countByUsuarioIdAndStatus(String usuarioId, StatusNotificacao status);
}
