package com.unicar.domain;

import com.unicar.enums.StatusCarona;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "carona")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Usuario motorista;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @Column(name = "origem_descricao", nullable = false, length = 255)
    private String origemDescricao;

    @Column(name = "origem_latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal origemLatitude;

    @Column(name = "origem_longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal origemLongitude;

    @Column(name = "destino_descricao", nullable = false, length = 255)
    private String destinoDescricao;

    @Column(name = "observacao", nullable = true, length = 255)
    private String observacao;

    @Column(name = "destino_latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal destinoLatitude;

    @Column(name = "destino_longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal destinoLongitude;

    @Column(name = "ponto_encontro_descricao", nullable = false, length = 255)
    private String pontoEncontroDescricao;

    @Column(name = "data_hora_partida", nullable = false)
    private LocalDateTime dataHoraPartida;

    @Column(name = "vagas_totais", nullable = false)
    @Positive
    private Integer vagasTotais;

    @Column(name = "valor_contribuicao", precision = 10, scale = 2)
    @PositiveOrZero
    private BigDecimal valorContribuicao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCarona status;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = StatusCarona.CRIADA;
        }
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }
}