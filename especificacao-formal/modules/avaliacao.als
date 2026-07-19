module modules/avaliacao

open modules/usuario
open modules/carona
open modules/reserva

sig Avaliacao {
    carona: one Carona,
    avaliador: one Usuario,
    avaliado: one Usuario,
    nota: one Int
}

pred passageiroConcluiu[c: Carona, u: Usuario] {
    some r: Reserva |
        r.carona = c
        and r.passageiro = u
        and r.status = ReservaConcluida
}

pred participante[c: Carona, u: Usuario] {
    u = c.motorista or passageiroConcluiu[c, u]
}

pred parAvaliavel[c: Carona, u1, u2: Usuario] {
    (u1 = c.motorista and passageiroConcluiu[c, u2])
    or (u2 = c.motorista and passageiroConcluiu[c, u1])
}

fact IntegridadeAvaliacao {
    all a: Avaliacao | {
        a.carona.status = CaronaFinalizada
        a.avaliador != a.avaliado
        a.nota >= 1 and a.nota <= 5
        parAvaliavel[a.carona, a.avaliador, a.avaliado]
    }
    all disj a1, a2: Avaliacao |
        a1.carona != a2.carona
        or a1.avaliador != a2.avaliador
        or a1.avaliado != a2.avaliado
}

fun avaliacoesRecebidas[u: Usuario]: set Avaliacao {
    { a: Avaliacao | a.avaliado = u }
}

fun pendentesPara[c: Carona, u: Usuario]: set Usuario {
    { alvo: Usuario |
        c.status = CaronaFinalizada
        and participante[c, u]
        and parAvaliavel[c, u, alvo]
        and no a: Avaliacao |
            a.carona = c
            and a.avaliador = u
            and a.avaliado = alvo }
}

pred podeAvaliar[c: Carona, autor, alvo: Usuario,
                 notaInformada: Int] {
    autenticado[autor]
    c.status = CaronaFinalizada
    parAvaliavel[c, autor, alvo]
    notaInformada >= 1 and notaInformada <= 5
}

run { some Avaliacao } for 5 but 8 Int
