package com.leonlima.notificacoes.enums;

/**
 * Tipos de notificação suportados pelo sistema.
 *
 * Uma das vantagens do MongoDB sobre bancos relacionais:
 * cada tipo de notificação pode ter campos diferentes no mesmo documento,
 * sem precisar de tabelas separadas ou colunas nulas.
 */
public enum TipoNotificacao {
    EMAIL,
    SMS,
    PUSH,
    IN_APP
}
