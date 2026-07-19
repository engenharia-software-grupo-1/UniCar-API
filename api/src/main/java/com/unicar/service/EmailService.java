package com.unicar.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void enviarEmail(String para, String assunto, String corpo) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setTo(para);
            helper.setSubject(assunto);
            helper.setText(corpo, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("Falha ao enviar e-mail para " + para + ": " + e.getMessage());
        }
    }
}