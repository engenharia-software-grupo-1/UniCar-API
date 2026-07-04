package com.unicar.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.unicar.enums.Genero;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String matricula;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    private String curso;

    @Column(nullable = false)
    private Boolean receberEmail;

    @Column(nullable = false)
    private Boolean ativo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Genero genero;

    @PrePersist
    protected void onCreate() {
        if (this.receberEmail == null) {
            this.receberEmail = true;
        }
        if (this.ativo == null) {
            this.ativo = true;
        }
        if (this.genero == null) {
            this.genero = Genero.NAO_INFORMADO;
        }
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }
}