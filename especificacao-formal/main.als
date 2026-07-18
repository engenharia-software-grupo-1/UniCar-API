module main

open modules/usuario
open modules/veiculo
open modules/carona
open modules/reserva
open modules/avaliacao
open modules/interesse_trajeto
open modules/comunicacao

// Integra os módulos do domínio e verifica propriedades que atravessam
// mais de um agregado. Detalhes de transporte HTTP não fazem parte do modelo.

fact ConsistenciaGlobal {
    all c: Carona | c.veiculo.dono = c.motorista
    all r: Reserva | r.passageiro != r.carona.motorista
    all a: Avaliacao | participante[a.carona, a.avaliador]
                         and participante[a.carona, a.avaliado]
    all ch: Chat | participantes[ch] = ch.reserva.passageiro + ch.reserva.carona.motorista
}

fun viagensEmCurso: set Carona { { c: Carona | c.status = CaronaEmAndamento } }
fun viagensEncerradas: set Carona {
    { c: Carona | c.status in CaronaFinalizada + CaronaCancelada }
}
fun participantesHistoricos[c: Carona]: set Usuario {
    c.motorista + { u: Usuario | some r: Reserva |
        r.carona = c and r.passageiro = u and r.status = ReservaConcluida }
}

pred podeBuscarCarona[u: Usuario, c: Carona] {
    u.ativo = True
    c in caronasDisponiveis
    u != c.motorista
    not bloqueioEntre[u, c.motorista]
    vagasOcupadas[c] < c.vagasTotais
}

pred podeExcluirVeiculo[u: Usuario, v: Veiculo] {
    v.dono = u
    no c: caronasAtivas | c.veiculo = v
}

pred cancelamentoConsistente[c: Carona] {
    c.status = CaronaCancelada
    no r: reservasDaCarona[c] | r.status in ReservaPendente + ReservaAceita
}

pred finalizacaoConsistente[c: Carona] {
    c.status = CaronaFinalizada
    no r: reservasDaCarona[c] | r.status = ReservaAceita
}

assert MotoristaNaoReservaPropriaCarona {
    all r: Reserva | r.passageiro != r.carona.motorista
}

assert BloqueioImpedeComunicacao {
    all ch: Chat, u: participantes[ch] |
        (some outro: participantes[ch] - u |
            existeBloqueio[u, outro] or existeBloqueio[outro, u])
        implies not podeEnviarMensagem[ch, u]
}

assert AvaliacaoSomenteAposFinalizacao {
    all a: Avaliacao | a.carona.status = CaronaFinalizada
}

assert BuscaSegura {
    all u: Usuario, c: Carona |
        podeBuscarCarona[u, c] implies {
            c.status = CaronaCriada
            vagasOcupadas[c] < c.vagasTotais
            u != c.motorista
            not bloqueioEntre[u, c.motorista]
        }
}

assert VeiculoDeCaronaAtivaNaoEhExcluivel {
    all c: caronasAtivas |
        not podeExcluirVeiculo[c.motorista, c.veiculo]
}

assert ConclusaoHabilitaAvaliacaoBilateral {
    all c: Carona, r: Reserva |
        c.status = CaronaFinalizada
        and r.carona = c
        and r.status = ReservaConcluida
        implies {
            participante[c, c.motorista]
            participante[c, r.passageiro]
        }
}

assert CaronaCanceladaSemReservasAtivas {
    all c: Carona | c.status = CaronaCancelada implies cancelamentoConsistente[c]
}

assert CaronaFinalizadaSemReservasAceitas {
    all c: Carona | c.status = CaronaFinalizada implies finalizacaoConsistente[c]
}

check MotoristaNaoReservaPropriaCarona for 5 but 8 Int
check BloqueioImpedeComunicacao for 5 but 8 Int
check AvaliacaoSomenteAposFinalizacao for 5 but 8 Int
check BuscaSegura for 5 but 8 Int
check VeiculoDeCaronaAtivaNaoEhExcluivel for 5 but 8 Int
check ConclusaoHabilitaAvaliacaoBilateral for 5 but 8 Int
check CaronaCanceladaSemReservasAtivas for 5 but 8 Int
check CaronaFinalizadaSemReservasAceitas for 5 but 8 Int

pred exemploCaronaFinalizada {
    some disj m, p: Usuario |
        some c: Carona |
            some r: Reserva |
                some a: Avaliacao |
                    some i: InteresseTrajeto {

                        m.ativo = True
                        p.ativo = True
                        not bloqueioEntre[m, p]

                        c.motorista = m
                        c.status = CaronaFinalizada

                        r.carona = c
                        r.passageiro = p
                        r.status = ReservaConcluida

                        a.carona = c
                        a.avaliador = m
                        a.avaliado = p

                        i.usuario = p
                    }
}

run exemploCaronaFinalizada for 2 but
    8 Int,
    exactly 2 Usuario,
    exactly 1 Veiculo,
    exactly 1 Carona,
    exactly 1 Reserva,
    exactly 1 Avaliacao,
    exactly 1 InteresseTrajeto

pred fluxoCaronaEmAndamento {
    some disj m, p: Usuario |
        some c: Carona |
            some v: Veiculo |
                some r: Reserva {

                    m.ativo = True
                    p.ativo = True
                    not bloqueioEntre[m, p]

                    c.motorista = m
                    c.veiculo = v
                    v.dono = m
                    c.status = CaronaEmAndamento

                    r.carona = c
                    r.passageiro = p
                    r.status = ReservaAceita

                    no a: Avaliacao | a.carona = c
                }
}

run fluxoCaronaEmAndamento for 2 but
    8 Int,
    exactly 2 Usuario,
    exactly 1 Veiculo,
    exactly 1 Carona,
    exactly 1 Reserva,
    exactly 0 Avaliacao,
    exactly 0 InteresseTrajeto