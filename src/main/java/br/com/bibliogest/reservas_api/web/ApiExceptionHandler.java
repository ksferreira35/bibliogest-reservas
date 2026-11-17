package br.com.bibliogest.reservas_api.web;

import br.com.bibliogest.reservas_api.acervo.AcervoIndisponivelException;
import br.com.bibliogest.reservas_api.acervo.ObraNaoEncontradaException;
import br.com.bibliogest.reservas_api.reserva.ExemplarIndisponivelException;
import br.com.bibliogest.reservas_api.reserva.ExemplarNaoEncontradoException;
import br.com.bibliogest.reservas_api.reserva.ReservaNaoEncontradaException;
import br.com.bibliogest.reservas_api.reserva.TransicaoInvalidaException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
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
