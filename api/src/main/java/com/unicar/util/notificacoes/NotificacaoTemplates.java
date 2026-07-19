package com.unicar.util.notificacoes;

import com.unicar.domain.Carona;
import com.unicar.domain.Usuario;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class NotificacaoTemplates {

    private NotificacaoTemplates() {
    }

    public static String novaSolicitacaoReserva(
            Usuario passageiro,
            Carona carona,
            LocalDateTime dataExpiracao
    ) {
        String prazoFormatado = dataExpiracao.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));

        return """
                <h1>Nova solicitação de reserva</h1>
                
                <p>Olá!</p>
                
                <p>Você recebeu uma <strong>nova solicitação de reserva</strong> para uma de suas caronas.</p>
                
                <h3>Detalhes da solicitação</h3>
                
                <ul>
                    <li><strong>Passageiro:</strong> %s</li>
                    <li><strong>Destino da carona:</strong> %s</li>
                </ul>
                
                <p>Para dar continuidade, acesse o <strong>UniCar</strong> e analise a solicitação. Você poderá <strong>aceitá-la</strong> ou <strong>recusá-la</strong> conforme sua disponibilidade.</p>
                
                <blockquote style="border-left: 4px solid #ccc; padding-left: 10px; color: #666; margin: 20px 0;">
                    Responda até <strong>%s</strong>. Após esse prazo, a solicitação expirará automaticamente.
                </blockquote>
                
                <p>Atenciosamente,</p>
                
                <p><strong>Equipe UniCar</strong></p>
                """.formatted(
                passageiro.getNome(),
                carona.getDestinoDescricao(),
                prazoFormatado
        );
    }

    public static String novaCaronaDisponivel(Carona carona) {
        return """
                <h1>Nova carona disponível</h1>
                
                <p>Olá!</p>
                
                <p>Uma nova carona com destino a <strong>%s</strong> foi cadastrada e corresponde a um trajeto pelo qual você demonstrou interesse.</p>
                
                <p>Acesse o <strong>UniCar</strong> para visualizar os detalhes da carona e, se desejar, solicitar uma reserva.</p>
                
                <p>Esperamos que esta oportunidade seja útil para você.</p>
                
                <p>Atenciosamente,</p>
                
                <p><strong>Equipe UniCar</strong></p>
                """.formatted(
                carona.getDestinoDescricao()
        );
    }

    public static String reservaAceita(Carona carona) {
        return """
                <h1>Reserva aceita</h1>
                
                <p>Olá!</p>
                
                <p>Sua solicitação de reserva para a carona com destino a <strong>%s</strong> foi <strong>aceita</strong> pelo motorista.</p>
                
                <p>Acesse o <strong>UniCar</strong> para visualizar os detalhes da carona.</p>
                
                <p>Desejamos uma excelente viagem!</p>
                
                <p>Atenciosamente,</p>
                
                <p><strong>Equipe UniCar</strong></p>
                """.formatted(carona.getDestinoDescricao());
    }

    public static String reservaRecusada(Carona carona) {
        return """
                <h1>Reserva recusada</h1>
                
                <p>Olá!</p>
                
                <p>Informamos que sua solicitação de reserva para a carona com destino a <strong>%s</strong> não pôde ser aceita pelo motorista.</p>
                
                <p>Você pode acessar o <strong>UniCar</strong> para buscar outras caronas disponíveis.</p>
                
                <p>Atenciosamente,</p>
                
                <p><strong>Equipe UniCar</strong></p>
                """.formatted(carona.getDestinoDescricao());
    }

    public static String reservaExpirada(Carona carona) {
        return """
                <h1>Reserva expirada</h1>

                <p>Olá!</p>

                <p>Sua solicitação de reserva para a carona com destino a <strong>%s</strong> expirou sem uma resposta do motorista.</p>

                <p>Você pode acessar o <strong>UniCar</strong> para buscar outras caronas disponíveis.</p>

                <p>Atenciosamente,</p>

                <p><strong>Equipe UniCar</strong></p>
                """.formatted(carona.getDestinoDescricao());
    }

    public static String passageiroRemovido(Carona carona) {
        return """
                <h1>Remoção da carona</h1>
                
                <p>Olá!</p>
                
                <p>Informamos que você foi removido da lista de passageiros da carona com destino a <strong>%s</strong>.</p>
                
                <p>Caso tenha dúvidas, entre em contato com o motorista pelo <strong>UniCar</strong>.</p>
                
                <p>Atenciosamente,</p>
                
                <p><strong>Equipe UniCar</strong></p>
                """.formatted(carona.getDestinoDescricao());
    }

    public static String reservaCanceladaPeloPassageiro(Usuario passageiro, Carona carona) {
        return """
                <h1>Reserva cancelada</h1>
                
                <p>Olá!</p>
                
                <p>O passageiro <strong>%s</strong> cancelou a reserva da sua carona com destino a <strong>%s</strong>.</p>
                
                <p>Acesse o <strong>UniCar</strong> para visualizar as informações atualizadas da carona.</p>
                
                <p>Atenciosamente,</p>
                
                <p><strong>Equipe UniCar</strong></p>
                """.formatted(
                passageiro.getNome(),
                carona.getDestinoDescricao()
        );
    }

    public static String reservaCanceladaPeloMotorista(Carona carona) {
        return """
                <h1>Reserva cancelada</h1>
                
                <p>Olá!</p>
                
                <p>Informamos que o motorista cancelou sua participação na carona com destino a <strong>%s</strong>.</p>
                
                <p>Você pode acessar o <strong>UniCar</strong> para buscar outras caronas disponíveis.</p>
                
                <p>Atenciosamente,</p>
                
                <p><strong>Equipe UniCar</strong></p>
                """.formatted(carona.getDestinoDescricao());
    }

    public static String passageiroRemovidoPeloMotorista(Carona carona) {
        return """
                <h1>Remoção da carona</h1>
                
                <p>Olá!</p>
                
                <p>Informamos que o motorista removeu sua participação na carona com destino a <strong>%s</strong>.</p>
                
                <p>Caso tenha dúvidas, entre em contato com o motorista pelo <strong>UniCar</strong>.</p>
                
                <p>Você também pode acessar o sistema para buscar outras caronas disponíveis.</p>
                
                <p>Atenciosamente,</p>
                
                <p><strong>Equipe UniCar</strong></p>
                """.formatted(carona.getDestinoDescricao());
    }

    public static String caronaCancelada(Carona carona) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
        String dataHoraFormatada = carona.getDataHoraPartida().format(formatter);

        return """
            <h1>Carona cancelada</h1>
            
            <p>Olá!</p>
            
            <p>Informamos que a carona agendada para o dia <strong>%s</strong> com destino a <strong>%s</strong> foi cancelada pelo motorista.</p>
            
            <p>Caso ainda precise realizar esse trajeto, acesse o <strong>UniCar</strong> para encontrar outras caronas disponíveis.</p>
            
            <p>Atenciosamente,</p>
            
            <p><strong>Equipe UniCar</strong></p>
            """.formatted(dataHoraFormatada, carona.getDestinoDescricao());
    }

    public static String caronaFinalizada(Carona carona) {
        return """
                <h1>Carona finalizada</h1>
                
                <p>Olá!</p>
                
                <p>A carona com destino a <strong>%s</strong> foi finalizada com sucesso.</p>
                
                <p>Esperamos que sua experiência tenha sido positiva.</p>
                
                <p>Atenciosamente,</p>
                
                <p><strong>Equipe UniCar</strong></p>
                """.formatted(carona.getDestinoDescricao());
    }

    public static String solicitarAvaliacaoPassageiro(Carona carona) {
        return """
                <h1>Avalie sua viagem</h1>
                
                <p>Olá!</p>
                
                <p>Sua viagem com destino a <strong>%s</strong> foi concluída.</p>
                
                <p>Sua opinião é muito importante para a comunidade do <strong>UniCar</strong>. Acesse o sistema e avalie sua experiência com o motorista.</p>
                
                <p>Atenciosamente,</p>
                
                <p><strong>Equipe UniCar</strong></p>
                """.formatted(carona.getDestinoDescricao());
    }

    public static String solicitarAvaliacaoMotorista() {
        return """
                <h1>Avalie seus passageiros</h1>
                
                <p>Olá!</p>
                
                <p>Sua carona foi concluída com sucesso.</p>
                
                <p>Acesse o <strong>UniCar</strong> e compartilhe sua experiência avaliando os passageiros que participaram da viagem.</p>
                
                <p>Sua avaliação contribui para tornar a comunidade mais segura e confiável.</p>
                
                <p>Atenciosamente,</p>
                
                <p><strong>Equipe UniCar</strong></p>
                """;
    }

    public static String caronaIniciada(Carona carona) {
        return """
                <h1>Carona iniciada</h1>
                
                <p>Olá!</p>
                
                <p>Informamos que o motorista iniciou a carona com destino a <strong>%s</strong>.</p>
                
                <p>Desejamos uma viagem tranquila e segura.</p>
                
                <p>Atenciosamente,</p>
                
                <p><strong>Equipe UniCar</strong></p>
                """.formatted(carona.getDestinoDescricao());
    }

}
