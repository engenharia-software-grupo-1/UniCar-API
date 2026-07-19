module main

open modules/usuario
open modules/veiculo
open modules/carona
open modules/reserva
open modules/avaliacao
open modules/interesse_trajeto
open modules/comunicacao

sig FiltroBusca {
    partida: lone Instante,
    origem: lone Coordenada,
    destino: lone Coordenada,
    generoMotorista: lone Genero,
    cursoMotorista: lone Curso
}

pred caronaDisponivelPara[u: Usuario, c: Carona] {
    u.ativo = True
    c in caronasDisponiveis
    c.motorista.ativo = True
    u != c.motorista
    not bloqueioEntre[u, c.motorista]
    some vagasDisponiveis[c]
}

pred podeBuscarCarona[u: Usuario, c: Carona] {
    autenticado[u]
    caronaDisponivelPara[u, c]
}

pred atendeFiltros[c: Carona, f: FiltroBusca] {
    some f.partida implies c.partida = f.partida
    some f.origem implies c.origem.coordenada = f.origem
    some f.destino implies c.destino.coordenada = f.destino
    some f.generoMotorista implies
        c.motorista.genero = f.generoMotorista
    some f.cursoMotorista implies
        c.motorista.curso = f.cursoMotorista
}

fun resultadosBusca[u: Usuario, f: FiltroBusca]: set Carona {
    { c: Carona | podeBuscarCarona[u, c] and atendeFiltros[c, f] }
}

pred podeConsultarPerfilPublico[solicitante, alvo: Usuario] {
    autenticado[solicitante]
    alvo.ativo = True
    not bloqueioEntre[solicitante, alvo]
}

pred interesseCompativel[i: InteresseTrajeto, c: Carona] {
    caronaDisponivelPara[i.usuario, c]
    i.origem = c.origem.coordenada
    i.destino = c.destino.coordenada
}

fact InteresseCompativelGeraNotificacao {
    all i: InteresseTrajeto, c: Carona |
        interesseCompativel[i, c] implies
            one n: Notificacao | {
                n.tipo = NotifCaronaCompativel
                n.destinatario = i.usuario
                n.origemCarona = c
            }
}

pred podeExcluirVeiculo[u: Usuario, v: Veiculo] {
    autenticado[u]
    v.dono = u
    no c: Carona |
        c.veiculo = v
        and c.status in CaronaCriada + CaronaEmAndamento
}

pred podeEditarCaronaConformeLotacao[
    c: Carona, m: Usuario, novasVagas: Int, novaPartida: Instante
] {
    podeEditarCarona[c, m, novasVagas, novaPartida]
    novasVagas >= #vagasAlocadas[c]
}

fun participantesHistoricos[c: Carona]: set Usuario {
    { u: Usuario | participante[c, u] }
}

pred podeConsultarHistorico[c: Carona, u: Usuario] {
    autenticado[u]
    c.status in CaronaFinalizada + CaronaCancelada
    u in participantesHistoricos[c]
}

assert BuscaRetornaSomenteCaronasSeguras {
    all u: Usuario, f: FiltroBusca, c: resultadosBusca[u, f] | {
        c.status = CaronaCriada
        c.partida.situacao = Futura
        some vagasDisponiveis[c]
        u != c.motorista
        not bloqueioEntre[u, c.motorista]
        atendeFiltros[c, f]
    }
}

check BuscaRetornaSomenteCaronasSeguras for 6 but 8 Int

run { some u: Usuario, f: FiltroBusca |
    some resultadosBusca[u, f] } for 6 but 8 Int

pred exemploFinalizacaoComAvaliacao {
    some disj m, p: Usuario |
        some c: Carona, r: Reserva, a: Avaliacao {
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
        }
}

run exemploFinalizacaoComAvaliacao for 2 but
    8 Int,
    exactly 2 Usuario,
    exactly 1 Veiculo,
    exactly 1 Carona,
    exactly 1 Reserva,
    exactly 1 Avaliacao,
    exactly 0 InteresseTrajeto,
    exactly 2 Notificacao,
    exactly 0 Chat,
    exactly 0 Mensagem,
    exactly 0 FiltroBusca,
    exactly 1 Curso,
    exactly 0 SessaoToken,
    exactly 0 TokenRevogado,
    exactly 0 BloqueioUsuario,
    exactly 0 Cor,
    exactly 0 Texto,
    exactly 1 Nome,
    exactly 1 Placa,
    exactly 1 Modelo,
    exactly 1 Descricao,
    exactly 1 Latitude,
    exactly 2 Longitude,
    exactly 2 Coordenada,
    exactly 2 Ponto,
    exactly 1 Instante,
    exactly 1 Vaga,
    exactly 1 ValorMonetario

pred exemploAndamentoComReserva {
    some disj m, p: Usuario |
        some c: Carona, r: Reserva {
            m.ativo = True
            p.ativo = True
            not bloqueioEntre[m, p]

            c.motorista = m
            c.status = CaronaEmAndamento

            r.carona = c
            r.passageiro = p
            r.status = ReservaAceita

            no a: Avaliacao | a.carona = c
        }
}

run exemploAndamentoComReserva for 2 but
    8 Int,
    exactly 2 Usuario,
    exactly 1 Veiculo,
    exactly 1 Carona,
    exactly 1 Reserva,
    exactly 0 Avaliacao,
    exactly 0 InteresseTrajeto,
    exactly 2 Notificacao,
    exactly 0 Chat,
    exactly 0 Mensagem,
    exactly 0 FiltroBusca,
    exactly 1 Curso,
    exactly 0 SessaoToken,
    exactly 0 TokenRevogado,
    exactly 0 BloqueioUsuario,
    exactly 0 Cor,
    exactly 0 Texto,
    exactly 1 Nome,
    exactly 1 Placa,
    exactly 1 Modelo,
    exactly 1 Descricao,
    exactly 1 Latitude,
    exactly 2 Longitude,
    exactly 2 Coordenada,
    exactly 2 Ponto,
    exactly 1 Instante,
    exactly 1 Vaga,
    exactly 1 ValorMonetario
