module modules/interesse_trajeto

open modules/usuario
open modules/carona

sig InteresseTrajeto {
    usuario: one Usuario,
    origem: one Ponto,
    destino: one Ponto
}

fact InteresseUnicoPorUsuarioETrajeto {
    all disj i1, i2: InteresseTrajeto |
        i1.usuario != i2.usuario or i1.origem != i2.origem or i1.destino != i2.destino
}

fun interessesDe[u: Usuario]: set InteresseTrajeto {
    usuario.u
}

fun usuariosInteressados[o, d: Ponto]: set Usuario {
    { u: Usuario | some i: InteresseTrajeto |
        i.usuario = u and i.origem = o and i.destino = d }
}

pred podeCadastrarInteresse[u: Usuario, o, d: Ponto] {
    u.ativo = True
    no i: InteresseTrajeto |
        i.usuario = u and i.origem = o and i.destino = d
}

pred podeRemoverInteresse[u: Usuario, i: InteresseTrajeto] {
    i.usuario = u
}

assert InteresseDuplicadoImpossivel {
    all disj i1, i2: InteresseTrajeto |
        i1.usuario != i2.usuario or i1.origem != i2.origem or i1.destino != i2.destino
}

assert ApenasDonoRemoveInteresse {
    all u: Usuario, i: InteresseTrajeto |
        podeRemoverInteresse[u, i] implies i in interessesDe[u]
}

// Indireta: a consulta inversa e a consulta por proprietário são consistentes.
assert ConsultaDeInteresseConsistente {
    all i: InteresseTrajeto |
        i.usuario in usuariosInteressados[i.origem, i.destino]
        and i in interessesDe[i.usuario]
}

check InteresseDuplicadoImpossivel for 5
check ApenasDonoRemoveInteresse for 5
check ConsultaDeInteresseConsistente for 5
run { some InteresseTrajeto } for 4
