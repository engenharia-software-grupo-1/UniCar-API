module modules/carona

open modules/usuario
open modules/veiculo

abstract sig StatusCarona {}
one sig CaronaCriada, CaronaEmAndamento,
        CaronaFinalizada, CaronaCancelada extends StatusCarona {}

abstract sig SituacaoTemporal {}
one sig Passada, Hoje, Futura extends SituacaoTemporal {}

sig Descricao, Latitude, Longitude, Texto, Vaga {}

sig Coordenada {
    latitude: one Latitude,
    longitude: one Longitude
}

sig Ponto {
    descricao: one Descricao,
    coordenada: one Coordenada
}

sig Instante { situacao: one SituacaoTemporal }

sig ValorMonetario {
    reais: one Int,
    centavos: one Int
}

sig Carona {
    motorista: one Usuario,
    veiculo: one Veiculo,
    origem: one Ponto,
    destino: one Ponto,
    pontoEncontro: lone Texto,
    partida: one Instante,
    vagas: some Vaga,
    vagasTotais: one Int,
    valorContribuicao: lone ValorMonetario,
    observacao: lone Texto,
    status: one StatusCarona
}

fact IntegridadeCarona {
    all disj c1, c2: Coordenada |
        c1.latitude != c2.latitude or c1.longitude != c2.longitude
    all v: ValorMonetario | {
        v.reais >= 0
        v.centavos >= 0
        v.centavos <= 99
    }
    all v: Vaga | one c: Carona | v in c.vagas
    all c: Carona | {
        c.veiculo.dono = c.motorista
        c.origem.coordenada != c.destino.coordenada
        c.vagasTotais > 0
        c.vagasTotais = #c.vagas
        c.status = CaronaCriada implies
            c.partida.situacao = Futura
        c.status = CaronaEmAndamento implies
            c.partida.situacao = Hoje
        c.status = CaronaFinalizada implies
            c.partida.situacao in Hoje + Passada
    }
    all m: Usuario | lone c: Carona |
        c.motorista = m and c.status = CaronaEmAndamento
}

fun caronasDisponiveis: set Carona {
    { c: Carona |
        c.status = CaronaCriada and c.partida.situacao = Futura }
}

fun proximosStatusCarona[s: StatusCarona]: set StatusCarona {
    s = CaronaCriada => CaronaEmAndamento + CaronaCancelada
    else s = CaronaEmAndamento => CaronaFinalizada + CaronaCancelada
    else none
}

pred motoristaAutenticado[c: Carona, m: Usuario] {
    autenticado[m]
    c.motorista = m
}

pred podeCriarCarona[c: Carona, m: Usuario] {
    motoristaAutenticado[c, m]
    c.status = CaronaCriada
    c.partida.situacao = Futura
}

pred podeEditarCarona[c: Carona, m: Usuario,
                      novasVagas: Int, novaPartida: Instante] {
    motoristaAutenticado[c, m]
    c.status = CaronaCriada
    novasVagas > 0
    novaPartida.situacao = Futura
}

pred podeCancelarCarona[c: Carona, m: Usuario] {
    motoristaAutenticado[c, m]
    c.status in CaronaCriada + CaronaEmAndamento
}

pred podeIniciarCarona[c: Carona, m: Usuario] {
    motoristaAutenticado[c, m]
    c.status = CaronaCriada
}

pred podeFinalizarCarona[c: Carona, m: Usuario] {
    motoristaAutenticado[c, m]
    c.status = CaronaEmAndamento
}

pred trajetoRecorrente[m: Usuario, o, d: Coordenada] {
    #{ c: Carona |
        c.motorista = m
        and c.status = CaronaFinalizada
        and c.origem.coordenada = o
        and c.destino.coordenada = d } >= 2
}

run { some c: Carona, m: Usuario | podeCriarCarona[c, m] }
    for 4 but 8 Int
