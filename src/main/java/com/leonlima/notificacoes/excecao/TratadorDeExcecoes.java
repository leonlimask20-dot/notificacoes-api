package com.leonlima.notificacoes.excecao;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class TratadorDeExcecoes {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro(ex.getMessage(), 404));
    }

    @ExceptionHandler(RateLimitExcedidoException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExcedidoException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(erro(ex.getMessage(), 429));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            erros.put(campo, error.getDefaultMessage());
        });
        Map<String, Object> corpo = erro("Dados inválidos", 400);
        corpo.put("campos", erros);
        return ResponseEntity.badRequest().body(corpo);
    }

    private Map<String, Object> erro(String mensagem, int status) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("mensagem", mensagem);
        corpo.put("status", status);
        corpo.put("timestamp", LocalDateTime.now());
        return corpo;
    }
}
