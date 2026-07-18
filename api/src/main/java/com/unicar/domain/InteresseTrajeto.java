package com.unicar.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "interesse_trajeto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteresseTrajeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "origem_latitude", nullable = false)
    private BigDecimal origemLatitude;

    @Column(name = "origem_longitude", nullable = false)
    private BigDecimal origemLongitude;

    @Column(name = "destino_latitude", nullable = false)
    private BigDecimal destinoLatitude;

    @Column(name = "destino_longitude", nullable = false)
    private BigDecimal destinoLongitude;

    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro;
}
