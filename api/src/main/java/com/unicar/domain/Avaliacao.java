package com.unicar.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@Table(name = "avaliacao")
@AllArgsConstructor
@NoArgsConstructor
public class Avaliacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "carona_id")
    private Carona carona;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliador_id")
    private Usuario avaliador;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliado_id")
    private Usuario avaliado;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer nota;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "data_avaliacao", nullable = false)
    private LocalDateTime dataAvaliacao;
}
