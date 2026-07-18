package com.unicar.service.carona;

import com.unicar.domain.BloqueioUsuario;
import com.unicar.domain.Carona;
import com.unicar.domain.ReservaCarona;
import com.unicar.domain.Usuario;
import com.unicar.enums.StatusCarona;
import com.unicar.enums.StatusReserva;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CaronaSpecifications {

    public static Specification<Carona> comStatusCriada() {
        return (root, query, cb) -> cb.equal(root.get("status"), StatusCarona.CRIADA);
    }

    public static Specification<Carona> comDataFutura() {
        return (root, query, cb) -> cb.greaterThan(root.get("dataHoraPartida"), LocalDateTime.now());
    }

    public static Specification<Carona> comCursoMotorista(String curso) {
        return (root, query, cb) -> {
            if (curso == null) return cb.conjunction();
            Join<Carona, Usuario> motorista = root.join("motorista");
            return cb.like(cb.upper(motorista.get("curso")), "%" + curso.toUpperCase() + "%");
        };
    }

    public static Specification<Carona> comBoundingBox(BigDecimal lat, BigDecimal lon, double raioKm) {
        return (root, query, cb) -> {
            if (lat == null || lon == null) return cb.conjunction();
            double deltaLat = raioKm / 111.045; // graus de latitude por km
            double deltaLon = raioKm / (111.045 * Math.cos(Math.toRadians(lat.doubleValue())));
            return cb.and(
                    cb.between(root.get("origemLatitude"), lat.subtract(BigDecimal.valueOf(deltaLat)), lat.add(BigDecimal.valueOf(deltaLat))),
                    cb.between(root.get("origemLongitude"), lon.subtract(BigDecimal.valueOf(deltaLon)), lon.add(BigDecimal.valueOf(deltaLon)))
            );
        };
    }

    public static Specification<Carona> comGeneroMotorista(String genero) {
        return (root, query, cb) -> {
            if (genero == null) return cb.conjunction();
            Join<Carona, Usuario> motorista = root.join("motorista");
            return cb.equal(motorista.get("genero"), genero);
        };
    }

    public static Specification<Carona> semBloqueioBidirecional(Long usuarioAutenticadoId) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<BloqueioUsuario> bloqueio = subquery.from(BloqueioUsuario.class);
            subquery.select(bloqueio.get("id"));
            subquery.where(cb.or(
                    cb.and(
                            cb.equal(bloqueio.get("usuario").get("id"), usuarioAutenticadoId),
                            cb.equal(bloqueio.get("usuarioBloqueado").get("id"), root.get("motorista").get("id"))
                    ),
                    cb.and(
                            cb.equal(bloqueio.get("usuario").get("id"), root.get("motorista").get("id")),
                            cb.equal(bloqueio.get("usuarioBloqueado").get("id"), usuarioAutenticadoId)
                    )
            ));
            return cb.not(cb.exists(subquery));
        };
    }

    public static Specification<Carona> comVagasDisponiveis() {
        return (root, query, cb) -> {
            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<ReservaCarona> reserva = subquery.from(ReservaCarona.class);
            subquery.select(cb.sum(reserva.get("quantidadePassageiros")));
            subquery.where(
                    cb.equal(reserva.get("carona"), root),
                    cb.equal(reserva.get("status"), StatusReserva.ACEITA)
            );

            Expression<Integer> vagasOcupadas = cb.coalesce(subquery, 0);
            return cb.greaterThan(cb.diff(root.get("vagasTotais"), vagasOcupadas), 0);
        };
    }

    public static Specification<Carona> comDataHoraSaida(LocalDateTime dataHoraSaida) {
        return (root, query, cb) -> {
            if (dataHoraSaida == null) return cb.conjunction();
            LocalDateTime inicioDia = dataHoraSaida.toLocalDate().atStartOfDay();
            LocalDateTime fimDia = dataHoraSaida.toLocalDate().atTime(23, 59, 59, 999_000_000);
            return cb.and(
                    cb.greaterThanOrEqualTo(root.get("dataHoraPartida"), inicioDia),
                    cb.lessThanOrEqualTo(root.get("dataHoraPartida"), fimDia)
            );
        };
    }
}