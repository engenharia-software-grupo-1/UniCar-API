module modules/carona

open modules/usuario
open modules/veiculo

abstract sig StatusCarona {}
one sig CaronaCriada, CaronaEmAndamento, CaronaFinalizada, CaronaCancelada extends StatusCarona {}

sig Ponto, Instante {}

// Alloy não possui tipo decimal. O valor monetário é decomposto sem perda:
// R$ 5,50 corresponde a reais = 5 e centavos = 50.
// Os comandos usam 8 Int para abranger toda a faixa de centavos (0..99).
sig ValorMonetario {
    reais: one Int,
    centavos: one Int
}

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
    valorContribuicao: lone ValorMonetario,
    status: one StatusCarona
}

sig TrajetoRecorrente {
    motorista: one Usuario,
    origem: one Ponto,
    destino: one Ponto,
    ocorrencias: some Carona
}

fact IntegridadeCarona {
    all v: ValorMonetario | {
        v.reais >= 0
        v.centavos >= 0
        v.centavos <= 99
    }
    all c: Carona | {
        c.veiculo.dono = c.motorista
        c.vagasTotais > 0
    }
    all m: Usuario | lone c: Carona |
        c.motorista = m and c.status = CaronaEmAndamento
    all t: TrajetoRecorrente, c: t.ocorrencias | {
        c.motorista = t.motorista
        c.origem = t.origem
        c.destino = t.destino
        c.status = CaronaFinalizada
    }
    all t: TrajetoRecorrente | #t.ocorrencias >= 2
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

assert BuscaSomenteCriadasFuturas {
    all c: caronasDisponiveis |
        c.status = CaronaCriada and c.situacaoPartida = Futura
}

assert TrajetoRecorrenteTemAoMenosDuasViagens {
    all t: TrajetoRecorrente | #t.ocorrencias >= 2
}

assert ApenasMotoristaControlaCiclo {
    all c: Carona, u: Usuario |
        (podeCancelarCarona[c, u] or podeIniciarCarona[c, u] or podeFinalizarCarona[c, u])
        implies u = c.motorista
}

check VeiculoPertenceAoMotorista for 5 but 8 Int
check CaronaFinalizadaNaoCancela for 5 but 8 Int
check BuscaSomenteCriadasFuturas for 5 but 8 Int
check TrajetoRecorrenteTemAoMenosDuasViagens for 5 but 8 Int
check ApenasMotoristaControlaCiclo for 5 but 8 Int
run { some c: Carona | c.status = CaronaEmAndamento } for 4 but 8 Int
run { some v: ValorMonetario | v.reais = 5 and v.centavos = 50 } for 4 but 8 Int
