package com.unicar.domain;

import com.unicar.enums.StatusReserva;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reserva_carona")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaCarona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carona_id", nullable = false)
    private Carona carona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Builder.Default
    @Column(name = "quantidade_passageiros", nullable = false)
    @Positive
    private Integer quantidadePassageiros = 1;

    @Column(name = "origem_embarque_descricao", nullable = false, length = 255)
    private String origemEmbarqueDescricao;

    @Column(name = "origem_embarque_latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal origemEmbarqueLatitude;

    @Column(name = "origem_embarque_longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal origemEmbarqueLongitude;

    @Column(name = "valor_contribuicao", nullable = false, precision = 10, scale = 2)
    @PositiveOrZero
    private BigDecimal valorContribuicao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusReserva status;

    @Column(name = "data_reserva", nullable = false, updatable = false)
    private LocalDateTime dataReserva;

    @Column(name = "data_expiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    @Column(name = "data_resposta")
    private LocalDateTime dataResposta;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = StatusReserva.PENDENTE;
        }

        if (dataReserva == null) {
            dataReserva = LocalDateTime.now();
        }
    }
}