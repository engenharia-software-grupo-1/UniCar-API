module modules/carona

open modules/usuario
open modules/veiculo

abstract sig StatusCarona {}
one sig CaronaCriada, CaronaEmAndamento, CaronaFinalizada, CaronaCancelada extends StatusCarona {}

sig Ponto, Instante {}

abstract sig SituacaoTemporal {}
one sig Passada, Hoje, Futura extends SituacaoTemporal {}

sig Carona {
    motorista: one Usuario,
    veiculo: one Veiculo,
    origem: one Ponto,
    destino: one Ponto,
    partida: one Instante,
    situacaoPartida: one SituacaoTemporal,
    vagasTotais: one Int,
    valorContribuicao: lone Int,
    status: one StatusCarona
}

sig TrajetoRecorrente {
    motorista: one Usuario,
    origem: one Ponto,
    destino: one Ponto,
    ocorrencias: some Carona
}

fact IntegridadeCarona {
    all c: Carona | {
        c.veiculo.dono = c.motorista
        c.vagasTotais > 0
        some c.valorContribuicao implies c.valorContribuicao >= 0
    }
    all m: Usuario | lone c: Carona |
        c.motorista = m and c.status = CaronaEmAndamento
    all t: TrajetoRecorrente, c: t.ocorrencias | {
        c.motorista = t.motorista
        c.origem = t.origem
        c.destino = t.destino
        c.status = CaronaFinalizada
    }
}

fun caronasDe[m: Usuario]: set Carona { { c: Carona | c.motorista = m } }
fun caronasAtivas: set Carona {
    { c: Carona | c.status in CaronaCriada + CaronaEmAndamento }
}
fun historicoMotorista[m: Usuario]: set Carona {
    { c: caronasDe[m] | c.status in CaronaFinalizada + CaronaCancelada }
}

fun caronasDisponiveis: set Carona {
    { c: Carona | c.status = CaronaCriada and c.situacaoPartida = Futura }
}

fun caronasPorOrigemDestino[o, d: Ponto]: set Carona {
    { c: caronasDisponiveis | c.origem = o and c.destino = d }
}

fun trajetosRecorrentesDe[m: Usuario]: set TrajetoRecorrente {
    { t: TrajetoRecorrente | t.motorista = m }
}

pred podeCriarCarona[m: Usuario, v: Veiculo, vagas: Int] {
    m.ativo = True
    v.dono = m
    vagas > 0
    no c: Carona | c.motorista = m and c.status = CaronaEmAndamento
}

pred podeEditarCarona[c: Carona, m: Usuario, novasVagas, ocupadas: Int] {
    c.motorista = m
    c.status = CaronaCriada
    novasVagas > 0
    novasVagas >= ocupadas
}

pred podeCancelarCarona[c: Carona, m: Usuario] {
    c.motorista = m
    c.status = CaronaCriada
}

pred podeIniciarCarona[c: Carona, m: Usuario] {
    c.motorista = m
    c.status = CaronaCriada
    c.situacaoPartida = Hoje
}

pred podeAtualizarObservacao[c: Carona, m: Usuario] {
    c.motorista = m
    c.status not in CaronaFinalizada + CaronaCancelada
}

pred podeRecriarTrajeto[t: TrajetoRecorrente, m: Usuario, v: Veiculo, vagas: Int] {
    t.motorista = m
    podeCriarCarona[m, v, vagas]
}

pred podeFinalizarCarona[c: Carona, m: Usuario] {
    c.motorista = m
    c.status = CaronaEmAndamento
}

assert VeiculoPertenceAoMotorista {
    all c: Carona | c.veiculo.dono = c.motorista
}

assert CaronaFinalizadaNaoCancela {
    all c: Carona, m: Usuario |
        c.status = CaronaFinalizada implies not podeCancelarCarona[c, m]
}

// Direta: a busca pública nunca retorna carona iniciada ou encerrada.
assert BuscaSomenteCriadasFuturas {
    all c: caronasDisponiveis |
        c.status = CaronaCriada and c.situacaoPartida = Futura
}

// Indireta: uma carona disponível nunca é simultaneamente iniciável.
assert CaronaFuturaNaoPodeIniciar {
    all c: caronasDisponiveis, m: Usuario | not podeIniciarCarona[c, m]
}

assert ApenasMotoristaControlaCiclo {
    all c: Carona, u: Usuario |
        (podeCancelarCarona[c, u] or podeIniciarCarona[c, u] or podeFinalizarCarona[c, u])
        implies u = c.motorista
}

check VeiculoPertenceAoMotorista for 5
check CaronaFinalizadaNaoCancela for 5
check BuscaSomenteCriadasFuturas for 5
check CaronaFuturaNaoPodeIniciar for 5
check ApenasMotoristaControlaCiclo for 5
run { some c: Carona | c.status = CaronaEmAndamento } for 4 but 5 Int
