package com.leonlima.notificacoes.modelo;

import com.leonlima.notificacoes.enums.StatusNotificacao;
import com.leonlima.notificacoes.enums.TipoNotificacao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Documento MongoDB para notificações.
 *
 * No MongoDB, @Document é equivalente ao @Entity do JPA.
 * A diferença: em vez de uma tabela com colunas fixas,
 * cada documento é um objeto JSON que pode ter campos diferentes.
 *
 * O campo "metadados" (Map<String, Object>) é um exemplo perfeito:
 * um EMAIL pode ter { "assunto": "..." }, um SMS pode ter { "ddd": "92" }.
 * Em SQL isso exigiria colunas nulas ou tabelas separadas.
 *
 * @Indexed: cria índice no MongoDB para acelerar buscas por usuarioId.
 * Equivalente ao @Index do JPA ou ao CREATE INDEX do SQL.
 */
@Document(collection = "notificacoes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notificacao {

    @Id
    private String id;

    @Indexed
    private String usuarioId;

    private String destinatario;
    private String titulo;
    private String conteudo;

    private TipoNotificacao tipo;
    private StatusNotificacao status;

    // Campos extras específicos de cada tipo de notificação
    // Ex: EMAIL → { "assunto": "...", "templateId": "..." }
    //     SMS   → { "ddd": "92", "operadora": "claro" }
    //     PUSH  → { "deviceToken": "...", "badge": 1 }
    private Map<String, Object> metadados;

    private LocalDateTime criadoEm;
    private LocalDateTime enviadoEm;
    private LocalDateTime lidoEm;
}
