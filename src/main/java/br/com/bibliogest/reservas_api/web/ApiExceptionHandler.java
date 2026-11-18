package br.com.bibliogest.reservas_api.web;

import br.com.bibliogest.reservas_api.exception.AcervoIndisponivelException;
import br.com.bibliogest.reservas_api.exception.ExemplarIndisponivelException;
import br.com.bibliogest.reservas_api.exception.ExemplarNaoEncontradoException;
import br.com.bibliogest.reservas_api.exception.ObraNaoEncontradaException;
import br.com.bibliogest.reservas_api.exception.ReservaNaoEncontradaException;
import br.com.bibliogest.reservas_api.exception.TransicaoInvalidaException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = exception.getBody();
        problem.setDetail("Revise os campos informados.");
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList());
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    private record FieldErrorDetail(String field, String message) {}

    @ExceptionHandler({ExemplarIndisponivelException.class, TransicaoInvalidaException.class})
    ProblemDetail conflito(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concorrencia(OptimisticLockingFailureException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "A reserva foi alterada por outra operação. Atualize e tente novamente.");
    }

    @ExceptionHandler({ReservaNaoEncontradaException.class, ExemplarNaoEncontradoException.class,
        ObraNaoEncontradaException.class})
    ProblemDetail naoEncontrado(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AcervoIndisponivelException.class)
    ProblemDetail acervoIndisponivel(AcervoIndisponivelException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
            "Não foi possível confirmar o exemplar no Acervo. Tente novamente.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalido(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
