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
    public static String reservaAceita(Carona carona) {
        return """
            # Reserva aceita

            Olá!

            Sua solicitação de reserva para a carona com destino a **%s** foi **aceita** pelo motorista.

            Acesse o **UNIcar** para visualizar os detalhes da carona.

            Desejamos uma excelente viagem!

            Atenciosamente,

            **Equipe UNIcar**
            """.formatted(carona.getDestinoDescricao());
    }

    public static String reservaRecusada(Carona carona) {
        return """
            # Reserva recusada

            Olá!

            Informamos que sua solicitação de reserva para a carona com destino a **%s** não pôde ser aceita pelo motorista.

            Você pode acessar o **UNIcar** para buscar outras caronas disponíveis.

            Atenciosamente,

            **Equipe UNIcar**
            """.formatted(carona.getDestinoDescricao());
    }

    public static String passageiroRemovido(Carona carona) {
        return """
            # Remoção da carona

            Olá!

            Informamos que você foi removido da lista de passageiros da carona com destino a **%s**.

            Caso tenha dúvidas, entre em contato com o motorista pelo **UNIcar**.

            Atenciosamente,

            **Equipe UNIcar**
            """.formatted(carona.getDestinoDescricao());
    }

    public static String reservaCanceladaPeloPassageiro(Usuario passageiro, Carona carona) {
        return """
            # Reserva cancelada

            Olá!

            O passageiro **%s** cancelou a reserva da sua carona com destino a **%s**.

            Acesse o **UNIcar** para visualizar as informações atualizadas da carona.

            Atenciosamente,

            **Equipe UNIcar**
            """.formatted(
                passageiro.getNome(),
                carona.getDestinoDescricao()
        );
    }

    public static String reservaCanceladaPeloMotorista(Carona carona) {
        return """
            # Reserva cancelada

            Olá!

            Informamos que o motorista cancelou sua participação na carona com destino a **%s**.

            Você pode acessar o **UNIcar** para buscar outras caronas disponíveis.

            Atenciosamente,

            **Equipe UNIcar**
            """.formatted(carona.getDestinoDescricao());
    }

    public static String passageiroRemovidoPeloMotorista(Carona carona) {
        return """
            # Remoção da carona

            Olá!

            Informamos que o motorista removeu sua participação na carona com destino a **%s**.

            Caso tenha dúvidas, entre em contato com o motorista pelo **UNIcar**.

            Você também pode acessar o sistema para buscar outras caronas disponíveis.

            Atenciosamente,

            **Equipe UNIcar**
            """.formatted(carona.getDestinoDescricao());
    }

    public static String caronaCancelada(Carona carona) {
        return """
            # Carona cancelada

            Olá!

            Informamos que a carona com destino a **%s** foi cancelada pelo motorista.

            Caso ainda precise realizar esse trajeto, acesse o **UNIcar** para encontrar outras caronas disponíveis.

            Atenciosamente,

            **Equipe UNIcar**
            """.formatted(carona.getDestinoDescricao());
    }

    public static String caronaFinalizada(Carona carona) {
        return """
            # Carona finalizada

            Olá!

            A carona com destino a **%s** foi finalizada com sucesso.

            Esperamos que sua experiência tenha sido positiva.

            Atenciosamente,

            **Equipe UNIcar**
            """.formatted(carona.getDestinoDescricao());
    }

    public static String solicitarAvaliacaoPassageiro(Carona carona) {
        return """
            # Avalie sua viagem

            Olá!

            Sua viagem com destino a **%s** foi concluída.

            Sua opinião é muito importante para a comunidade do **UNIcar**. Acesse o sistema e avalie sua experiência com o motorista.

            Atenciosamente,

            **Equipe UNIcar**
            """.formatted(carona.getDestinoDescricao());
    }

    public static String solicitarAvaliacaoMotorista() {
        return """
            # Avalie seus passageiros

            Olá!

            Sua carona foi concluída com sucesso.

            Acesse o **UNIcar** e compartilhe sua experiência avaliando os passageiros que participaram da viagem.

            Sua avaliação contribui para tornar a comunidade mais segura e confiável.

            Atenciosamente,

            **Equipe UNIcar**
            """;
    }

    public static String caronaIniciada(Carona carona) {
        return """
            # Carona iniciada

            Olá!

            Informamos que o motorista iniciou a carona com destino a **%s**.

            Desejamos uma viagem tranquila e segura.

            Atenciosamente,

            **Equipe UNIcar**
            """.formatted(carona.getDestinoDescricao());
    }

}