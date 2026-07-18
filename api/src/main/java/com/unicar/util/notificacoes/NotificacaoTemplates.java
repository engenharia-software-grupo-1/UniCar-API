package com.unicar.util.notificacoes;

import com.unicar.domain.Carona;
import com.unicar.domain.Usuario;

public final class NotificacaoTemplates {

    private NotificacaoTemplates() {
    }

    public static String novaSolicitacaoReserva(Usuario passageiro, Carona carona) {
        return """
                # Nova solicitação de reserva

                Olá!

                Você recebeu uma **nova solicitação de reserva** para uma de suas caronas.

                ### Detalhes da solicitação

                - **Passageiro:** %s
                - **Destino da carona:** %s

                Para dar continuidade, acesse o **UNIcar** e analise a solicitação. Você poderá **aceitá-la** ou **recusá-la** conforme sua disponibilidade.

                > Enquanto a solicitação não for analisada, a reserva permanecerá pendente.

                Atenciosamente,

                **Equipe UNIcar**
                """.formatted(
                passageiro.getNome(),
                carona.getDestinoDescricao()
        );
    }

    public static String novaCaronaDisponivel(Carona carona) {
        return """
                # Nova carona disponível

                Olá!

                Uma nova carona com destino a **%s** foi cadastrada e corresponde a um trajeto pelo qual você demonstrou interesse.

                Acesse o **UNIcar** para visualizar os detalhes da carona e, se desejar, solicitar uma reserva.

                Esperamos que esta oportunidade seja útil para você.

                Atenciosamente,

                **Equipe UNIcar**
                """.formatted(
                carona.getDestinoDescricao()
        );
    }
}