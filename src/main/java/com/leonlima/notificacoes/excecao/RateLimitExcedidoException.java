package com.leonlima.notificacoes.excecao;

public class RateLimitExcedidoException extends RuntimeException {
    public RateLimitExcedidoException(String mensagem) {
        super(mensagem);
    }
}
