module modules/reserva

open modules/usuario
open modules/carona

abstract sig StatusReserva {}
// Mantido no enum do OpenAPI; remover uma reserva resulta em CANCELADA.
one sig ReservaPendente, ReservaAceita, ReservaRecusada,
        ReservaCancelada, ReservaRemovida, ReservaConcluida
        extends StatusReserva {}

sig Reserva {
    carona: one Carona,
    passageiro: one Usuario,
    origemEmbarque: one Ponto,
    vagasSolicitadas: some Vaga,
    quantidadePassageiros: one Int,
    valorContribuicao: one ValorMonetario,
    status: one StatusReserva
}

fact IntegridadeReserva {
    all r: Reserva | {
        r.passageiro != r.carona.motorista
        r.vagasSolicitadas in r.carona.vagas
        r.quantidadePassageiros = #r.vagasSolicitadas
        r.status = ReservaPendente implies
            r.carona.status = CaronaCriada
        r.status = ReservaConcluida implies
            r.carona.status = CaronaFinalizada
        r.status = ReservaAceita implies
            r.carona.status in CaronaCriada + CaronaEmAndamento
    }
    all disj r1, r2: Reserva |
        r1.carona != r2.carona or r1.passageiro != r2.passageiro
    all disj r1, r2: Reserva |
        r1.carona = r2.carona
        and r1.status = ReservaAceita
        and r2.status = ReservaAceita
        implies no r1.vagasSolicitadas & r2.vagasSolicitadas
    all c: Carona | c.status = CaronaCancelada implies
        no r: Reserva |
            r.carona = c
            and r.status in ReservaPendente + ReservaAceita
    no r: Reserva | r.status = ReservaRemovida
}

fun vagasAlocadas[c: Carona]: set Vaga {
    { v: c.vagas |
        some r: Reserva |
            r.carona = c
            and r.status = ReservaAceita
            and v in r.vagasSolicitadas }
}

fun vagasDisponiveis[c: Carona]: set Vaga {
    c.vagas - vagasAlocadas[c]
}

fun vagasDisponiveisPara[r: Reserva]: set Vaga {
    r.carona.vagas
    - { v: Vaga |
        some outra: Reserva - r |
            outra.carona = r.carona
            and outra.status = ReservaAceita
            and v in outra.vagasSolicitadas }
}

fun proximosStatus[s: StatusReserva]: set StatusReserva {
    s = ReservaPendente =>
        ReservaAceita + ReservaRecusada
        + ReservaCancelada
    else s = ReservaAceita => ReservaCancelada + ReservaConcluida
    else none
}

pred podeSolicitarReserva[nova: Reserva, c: Carona,
                          u: Usuario, o: Ponto, qtd: Int] {
    autenticado[u]
    c in caronasDisponiveis
    u != c.motorista
    not bloqueioEntre[u, c.motorista]
    qtd > 0
    qtd <= #vagasDisponiveis[c]
    nova.carona = c
    nova.passageiro = u
    nova.origemEmbarque = o
    nova.quantidadePassageiros = qtd
    nova.vagasSolicitadas in vagasDisponiveis[c]
    nova.status = ReservaPendente
    no outra: Reserva - nova |
        outra.carona = c and outra.passageiro = u
}

pred motoristaPodeResponder[r: Reserva, m: Usuario] {
    autenticado[m]
    r.carona.motorista = m
    r.status = ReservaPendente
}

pred podeAceitarReserva[r: Reserva, m: Usuario] {
    motoristaPodeResponder[r, m]
    r.vagasSolicitadas in vagasDisponiveisPara[r]
}

pred podeRecusarReserva[r: Reserva, m: Usuario] {
    motoristaPodeResponder[r, m]
}

pred podeConsultarReserva[r: Reserva, u: Usuario] {
    autenticado[u]
    u in r.passageiro + r.carona.motorista
}

pred podeCancelarReserva[r: Reserva, u: Usuario] {
    autenticado[u]
    (u = r.passageiro
     and r.status in ReservaPendente + ReservaAceita)
    or
    (u = r.carona.motorista and r.status = ReservaAceita)
}

assert LotacaoRespeitada {
    all c: Carona | #vagasAlocadas[c] <= c.vagasTotais
}

check LotacaoRespeitada for 5 but 8 Int
run { some nova: Reserva, c: Carona, u: Usuario, o: Ponto |
    podeSolicitarReserva[nova, c, u, o, 1] } for 5 but 8 Int
