package com.unicar.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "bloqueio_usuario",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "usuario_bloqueado_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloqueioUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_bloqueado_id", nullable = false)
    private Usuario usuarioBloqueado;

    @CreationTimestamp
    @Column(name = "data_bloqueio", nullable = false, updatable = false)
    private LocalDateTime dataBloqueio;
}