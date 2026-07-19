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

abstract sig TipoNotificacao {}
one sig NotifReservaCriada, NotifReservaAceita,
        NotifReservaRecusada, NotifReservaCancelada, NotifReservaExpirada,
        NotifCaronaCancelada, NotifCaronaIniciada,
        NotifCaronaFinalizada, NotifCaronaCompativel
        extends TipoNotificacao {}

sig Notificacao {
    destinatario: one Usuario,
    tipo: one TipoNotificacao,
    visualizada: one Bool,
    origemReserva: lone Reserva,
    origemCarona: lone Carona
}

fun tiposDeReserva: set TipoNotificacao {
    NotifReservaCriada + NotifReservaAceita
    + NotifReservaRecusada + NotifReservaCancelada
    + NotifReservaExpirada
}

fun tiposDeCarona: set TipoNotificacao {
    NotifCaronaCancelada + NotifCaronaIniciada
    + NotifCaronaFinalizada + NotifCaronaCompativel
}

fun participantes[ch: Chat]: set Usuario {
    ch.reserva.passageiro + ch.reserva.carona.motorista
}

fact IntegridadeComunicacao {
    all disj ch1, ch2: Chat | ch1.reserva != ch2.reserva
    all m: Mensagem | one ch: Chat | m in ch.mensagens
    all ch: Chat, m: ch.mensagens |
        m.remetente in participantes[ch]

    all n: Notificacao | {
        (n.tipo in tiposDeReserva) iff one n.origemReserva
        (n.tipo in tiposDeCarona) iff one n.origemCarona
        some n.origemReserva implies
            n.destinatario in
                n.origemReserva.passageiro
                + n.origemReserva.carona.motorista
        n.tipo = NotifReservaRecusada implies
            n.origemReserva.status = ReservaRecusada
        n.tipo = NotifReservaCancelada implies
            n.origemReserva.status = ReservaCancelada
        n.tipo = NotifCaronaCancelada implies
            n.origemCarona.status = CaronaCancelada
    }

    all disj n1, n2: Notificacao |
        n1.destinatario != n2.destinatario
        or n1.tipo != n2.tipo
        or n1.origemReserva != n2.origemReserva
        or n1.origemCarona != n2.origemCarona

    all r: Reserva | one n: Notificacao | {
        n.tipo = NotifReservaCriada
        n.destinatario = r.carona.motorista
        n.origemReserva = r
    }
    all r: Reserva |
        r.status in ReservaAceita + ReservaConcluida implies
        one n: Notificacao | {
            n.tipo = NotifReservaAceita
            n.destinatario = r.passageiro
            n.origemReserva = r
        }
    all r: Reserva | r.status = ReservaRecusada implies
        one n: Notificacao | {
            n.tipo = NotifReservaRecusada
            n.destinatario = r.passageiro
            n.origemReserva = r
        }
    all r: Reserva | r.status = ReservaCancelada implies
        one n: Notificacao | {
            n.tipo = NotifReservaCancelada
            n.destinatario in r.passageiro + r.carona.motorista
            n.origemReserva = r
        }
    all c: Carona, r: Reserva |
        c.status = CaronaCancelada and r.carona = c implies
            one n: Notificacao | {
                n.tipo = NotifCaronaCancelada
                n.destinatario = r.passageiro
                n.origemCarona = c
            }
}

pred podeConsultarChat[ch: Chat, u: Usuario] {
    autenticado[u]
    u in participantes[ch]
}

pred podeEnviarMensagem[ch: Chat, u: Usuario] {
    podeConsultarChat[ch, u]
    ch.reserva.status in ReservaPendente + ReservaAceita
    no outro: participantes[ch] - u | bloqueioEntre[u, outro]
}

pred podeVisualizarNotificacao[n: Notificacao, u: Usuario] {
    autenticado[u]
    n.destinatario = u
    n.visualizada = False
}

run { some ch: Chat, u: Usuario |
    some ch.mensagens and podeEnviarMensagem[ch, u] } for 5 but 8 Int
