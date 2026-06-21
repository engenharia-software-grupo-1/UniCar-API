package com.unicar.controller;

import com.unicar.domain.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuários")
public class UsuarioController {

    @GetMapping
    @Operation(summary = "Lista usuários")
    public List<Usuario> listar() {
        return List.of();
    }

}
