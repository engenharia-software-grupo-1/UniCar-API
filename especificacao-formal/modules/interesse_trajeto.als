module modules/interesse_trajeto

open modules/usuario
open modules/carona

sig InteresseTrajeto {
    usuario: one Usuario,
    origem: one Coordenada,
    destino: one Coordenada
}

fact IntegridadeInteresse {
    all i: InteresseTrajeto | i.origem != i.destino
    all disj i1, i2: InteresseTrajeto |
        i1.usuario != i2.usuario
        or i1.origem != i2.origem
        or i1.destino != i2.destino
}

pred podeCadastrarInteresse[novo: InteresseTrajeto,
                            u: Usuario, o, d: Coordenada] {
    autenticado[u]
    novo.usuario = u
    novo.origem = o
    novo.destino = d
    no outro: InteresseTrajeto - novo |
        outro.usuario = u and outro.origem = o and outro.destino = d
}

pred podeRemoverInteresse[u: Usuario, i: InteresseTrajeto] {
    autenticado[u]
    i.usuario = u
}

run { some novo: InteresseTrajeto, u: Usuario, o, d: Coordenada |
    podeCadastrarInteresse[novo, u, o, d] } for 5
