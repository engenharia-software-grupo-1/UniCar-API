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

fact IntegridadeAvaliacao {
    all a: Avaliacao | {
        a.carona.status = CaronaFinalizada
        a.avaliador != a.avaliado
        a.nota >= 1 and a.nota <= 5
        participante[a.carona, a.avaliador]
        participante[a.carona, a.avaliado]
        not (passageiroDaCarona[a.carona, a.avaliador]
             and passageiroDaCarona[a.carona, a.avaliado])
    }
    all disj a1, a2: Avaliacao |
        a1.carona != a2.carona or a1.avaliador != a2.avaliador or a1.avaliado != a2.avaliado
}

pred passageiroDaCarona[c: Carona, u: Usuario] {
    some r: Reserva |
        r.carona = c and r.passageiro = u and r.status = ReservaConcluida
}

pred participante[c: Carona, u: Usuario] {
    u = c.motorista or passageiroDaCarona[c, u]
}

fun avaliacoesRecebidas[u: Usuario]: set Avaliacao {
    avaliado.u
}

fun avaliacoesFeitas[u: Usuario]: set Avaliacao { avaliador.u }

fun quantidadeAvaliacoes[u: Usuario]: Int { #(avaliacoesRecebidas[u]) }

fun notasRecebidas[u: Usuario]: set Int { (avaliacoesRecebidas[u]).nota }

fun pendentesPara[c: Carona, u: Usuario]: set Usuario {
    { alvo: Usuario |
        participante[c, u]
        and participante[c, alvo]
        and alvo != u
        and not (passageiroDaCarona[c, u] and passageiroDaCarona[c, alvo])
        and no a: Avaliacao |
            a.carona = c and a.avaliador = u and a.avaliado = alvo
    }
}

pred podeAvaliar[c: Carona, autor, alvo: Usuario, notaInformada: Int] {
    c.status = CaronaFinalizada
    notaInformada >= 1 and notaInformada <= 5
    autor != alvo
    participante[c, autor]
    participante[c, alvo]
    not (passageiroDaCarona[c, autor] and passageiroDaCarona[c, alvo])
    no a: Avaliacao |
        a.carona = c and a.avaliador = autor and a.avaliado = alvo
}

assert NotaSempreValida {
    all a: Avaliacao | a.nota >= 1 and a.nota <= 5
}

assert PassageirosNaoSeAvaliam {
    all a: Avaliacao |
        not (passageiroDaCarona[a.carona, a.avaliador]
             and passageiroDaCarona[a.carona, a.avaliado])
}

assert AvaliacaoUnicaPorParECarona {
    all disj a1, a2: Avaliacao |
        a1.carona != a2.carona or a1.avaliador != a2.avaliador or a1.avaliado != a2.avaliado
}

assert PendenciaEhElegivel {
    all c: Carona, u: Usuario, alvo: pendentesPara[c, u] | {
        participante[c, alvo]
        alvo != u
        no a: Avaliacao |
            a.carona = c and a.avaliador = u and a.avaliado = alvo
    }
}

assert NaoParticipanteNuncaAvalia {
    all c: Carona, autor, alvo: Usuario, n: Int |
        not participante[c, autor] implies not podeAvaliar[c, autor, alvo, n]
}

check NotaSempreValida for 5 but 8 Int
check PassageirosNaoSeAvaliam for 5 but 8 Int
check AvaliacaoUnicaPorParECarona for 5 but 8 Int
check PendenciaEhElegivel for 5 but 8 Int
check NaoParticipanteNuncaAvalia for 5 but 8 Int
run { some Avaliacao } for 4 but 8 Int
