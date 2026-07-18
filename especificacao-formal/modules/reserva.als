module modules/reserva

open modules/usuario
open modules/carona

abstract sig StatusReserva {}
one sig ReservaPendente, ReservaAceita, ReservaRecusada, ReservaCancelada,
        ReservaRemovida, ReservaConcluida extends StatusReserva {}

sig Reserva {
    carona: one Carona,
    passageiro: one Usuario,
    quantidadePassageiros: one Int,
    origemEmbarque: one Ponto,
    valorContribuicao: one ValorMonetario,
    status: one StatusReserva
}

fact IntegridadeReserva {
    all r: Reserva | {
        r.passageiro != r.carona.motorista
        r.quantidadePassageiros > 0
    }
    // Uma pessoa não pode criar nova reserva para a mesma carona,
    // mesmo após a reserva anterior alcançar um estado terminal.
    all disj r1, r2: Reserva |
        r1.carona != r2.carona or r1.passageiro != r2.passageiro
    all c: Carona | vagasOcupadas[c] <= c.vagasTotais
    all c: Carona | c.status = CaronaFinalizada implies
        no r: Reserva | r.carona = c and r.status = ReservaAceita
    all c: Carona | c.status = CaronaCancelada implies
        no r: Reserva | r.carona = c and r.status in ReservaPendente + ReservaAceita
}

fun reservasAtivas: set Reserva {
    { r: Reserva | r.status in ReservaPendente + ReservaAceita }
}

fun reservasDaCarona[c: Carona]: set Reserva {
    { r: Reserva | r.carona = c }
}

fun reservasDoPassageiro[u: Usuario]: set Reserva {
    { r: Reserva | r.passageiro = u }
}

fun historicoPassageiro[u: Usuario]: set Reserva {
    { r: reservasDoPassageiro[u] |
        r.status in ReservaRecusada + ReservaCancelada + ReservaRemovida + ReservaConcluida }
}

fun reservasEnviadas[u: Usuario]: set Reserva {
    passageiro.u
}

fun reservasRecebidas[m: Usuario]: set Reserva {
    { r: Reserva | r.carona.motorista = m }
}

fun vagasOcupadas[c: Carona]: Int {
    sum r: { x: Reserva | x.carona = c and x.status = ReservaAceita } |
        r.quantidadePassageiros
}

fun passageirosConfirmados[c: Carona]: set Usuario {
    { u: Usuario | some r: Reserva |
        r.carona = c and r.passageiro = u and r.status = ReservaAceita }
}

fun proximosStatus[s: StatusReserva]: set StatusReserva {
    s = ReservaPendente => ReservaAceita + ReservaRecusada + ReservaCancelada + ReservaRemovida
    else s = ReservaAceita => ReservaCancelada + ReservaRemovida + ReservaConcluida
    else none
}

pred transicaoReservaPermitida[antes, depois: StatusReserva] {
    depois in proximosStatus[antes]
}

pred podeSolicitarReserva[c: Carona, u: Usuario, qtd: Int] {
    c.status = CaronaCriada
    u != c.motorista
    u.ativo = True
    not bloqueioEntre[u, c.motorista]
    qtd > 0
    add[vagasOcupadas[c], qtd] <= c.vagasTotais
    no r: Reserva | r.carona = c and r.passageiro = u
}

pred podeConsultarReserva[r: Reserva, u: Usuario] {
    u in r.passageiro + r.carona.motorista
}

pred podeSimularReserva[c: Carona, u: Usuario, qtd: Int] {
    c.status = CaronaCriada
    u != c.motorista
    qtd > 0
    add[vagasOcupadas[c], qtd] <= c.vagasTotais
}

pred podeAceitarReserva[r: Reserva, m: Usuario] {
    r.carona.motorista = m
    r.status = ReservaPendente
    add[vagasOcupadas[r.carona], r.quantidadePassageiros] <= r.carona.vagasTotais
}

pred podeRecusarReserva[r: Reserva, m: Usuario] {
    r.carona.motorista = m
    r.status = ReservaPendente
}

pred podeCancelarReserva[r: Reserva, u: Usuario] {
    (u = r.passageiro and r.status in ReservaPendente + ReservaAceita)
    or
    (u = r.carona.motorista and r.status = ReservaAceita)
}

// No endpoint de remoção, uma reserva aceita passa a CANCELADA.
// REMOVIDA permanece no enum por ser um estado exposto pelo contrato.
pred podeRemoverReserva[r: Reserva, m: Usuario] {
    r.carona.motorista = m
    r.status = ReservaAceita
}

pred remocaoReservaPermitida[antes, depois: StatusReserva] {
    antes = ReservaAceita
    depois = ReservaCancelada
}

assert LotacaoRespeitada {
    all c: Carona | vagasOcupadas[c] <= c.vagasTotais
}

assert TerceiroNaoCancelaReserva {
    all r: Reserva, u: Usuario |
        podeCancelarReserva[r, u] implies u in r.passageiro + r.carona.motorista
}

assert SomenteReservaAceitaPodeSerRemovida {
    all r: Reserva, m: Usuario |
        podeRemoverReserva[r, m] implies r.status = ReservaAceita
}

assert RemocaoResultaEmCancelamento {
    all antes, depois: StatusReserva |
        remocaoReservaPermitida[antes, depois] implies
        antes = ReservaAceita and depois = ReservaCancelada
}

assert EstadoTerminalNaoTransiciona {
    all s: ReservaRecusada + ReservaCancelada + ReservaRemovida + ReservaConcluida |
        no proximosStatus[s]
}

assert BloqueadoNaoSolicitaReserva {
    all c: Carona, u: Usuario, qtd: Int |
        bloqueioEntre[u, c.motorista] implies not podeSolicitarReserva[c, u, qtd]
}

assert AceitacaoNaoExcedeVagas {
    all r: Reserva, m: Usuario |
        podeAceitarReserva[r, m] implies
        add[vagasOcupadas[r.carona], r.quantidadePassageiros] <= r.carona.vagasTotais
}

assert ApenasParticipantesConsultamReserva {
    all r: Reserva, u: Usuario |
        podeConsultarReserva[r, u] implies u in r.passageiro + r.carona.motorista
}

check LotacaoRespeitada for 5 but 8 Int
check TerceiroNaoCancelaReserva for 5 but 8 Int
check SomenteReservaAceitaPodeSerRemovida for 5 but 8 Int
check RemocaoResultaEmCancelamento for 5 but 8 Int
check EstadoTerminalNaoTransiciona for 5 but 8 Int
check BloqueadoNaoSolicitaReserva for 5 but 8 Int
check AceitacaoNaoExcedeVagas for 5 but 8 Int
check ApenasParticipantesConsultamReserva for 5 but 8 Int
run { some r: Reserva | r.status = ReservaAceita } for 4 but 8 Int
