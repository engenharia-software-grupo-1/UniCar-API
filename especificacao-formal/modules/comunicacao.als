module modules/comunicacao

open modules/usuario
open modules/carona
open modules/reserva

sig Mensagem {
    remetente: one Usuario,
    lida: one Bool
}

sig Chat {
    reserva: one Reserva,
    mensagens: set Mensagem
}

sig Notificacao {
    destinatario: one Usuario,
    visualizada: one Bool,
    origemReserva: lone Reserva
}

fact IntegridadeComunicacao {
    all disj ch1, ch2: Chat | ch1.reserva != ch2.reserva
    all m: Mensagem | one ch: Chat | m in ch.mensagens
    all ch: Chat, m: ch.mensagens |
        m.remetente in ch.reserva.passageiro + ch.reserva.carona.motorista
}

fun participantes[ch: Chat]: set Usuario {
    ch.reserva.passageiro + ch.reserva.carona.motorista
}

fun chatsDe[u: Usuario]: set Chat {
    { ch: Chat | u in participantes[ch] }
}

fun notificacoesNaoVisualizadas[u: Usuario]: set Notificacao {
    { n: Notificacao | n.destinatario = u and n.visualizada = False }
}

fun mensagensDe[ch: Chat]: set Mensagem { ch.mensagens }

fun mensagensNaoLidas[ch: Chat]: set Mensagem {
    { m: ch.mensagens | m.lida = False }
}

pred podeConsultarChat[ch: Chat, u: Usuario] {
    u in participantes[ch]
}

pred podeEnviarMensagem[ch: Chat, u: Usuario] {
    u in participantes[ch]
    ch.reserva.status in ReservaPendente + ReservaAceita
    no outro: participantes[ch] - u | existeBloqueio[u, outro]
    no outro: participantes[ch] - u | existeBloqueio[outro, u]
}

pred podeMarcarChatComoLido[ch: Chat, u: Usuario] {
    podeConsultarChat[ch, u]
}

pred podeVisualizarNotificacao[n: Notificacao, u: Usuario] {
    n.destinatario = u
    n.visualizada = False
}

pred notificacaoDeReservaValida[n: Notificacao] {
    some n.origemReserva implies
        n.destinatario in n.origemReserva.passageiro + n.origemReserva.carona.motorista
}

assert IntrusoNaoEnviaMensagem {
    all ch: Chat, u: Usuario |
        podeEnviarMensagem[ch, u] implies u in participantes[ch]
}

assert IntrusoNaoConsultaChat {
    all ch: Chat, u: Usuario - participantes[ch] |
        not podeConsultarChat[ch, u]
}

assert BloqueioCortaMensagem {
    all ch: Chat, u: participantes[ch] |
        (some outro: participantes[ch] - u | bloqueioEntre[u, outro])
        implies not podeEnviarMensagem[ch, u]
}

// Indireta: toda notificação vinculada a reserva alcança uma das partes.
assert NotificacaoDeReservaSomenteParaParticipante {
    all n: Notificacao |
        notificacaoDeReservaValida[n] and some n.origemReserva implies
        n.destinatario in n.origemReserva.passageiro + n.origemReserva.carona.motorista
}

check IntrusoNaoEnviaMensagem for 5
check IntrusoNaoConsultaChat for 5
check BloqueioCortaMensagem for 5
check NotificacaoDeReservaSomenteParaParticipante for 5
run { some ch: Chat | some ch.mensagens } for 4
