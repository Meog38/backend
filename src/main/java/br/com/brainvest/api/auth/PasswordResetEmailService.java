package br.com.brainvest.api.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetEmailService.class);

    private final ObjectProvider<MailSender> mailSender;
    private final boolean enabled;
    private final String from;

    public PasswordResetEmailService(
            ObjectProvider<MailSender> mailSender,
            @Value("${brainvest.mail.enabled:false}") boolean enabled,
            @Value("${brainvest.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    public boolean sendResetLink(String to, String resetLink) {
        if (!enabled || from.isBlank()) {
            return false;
        }

        MailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            logger.warn("password_reset_email_skipped reason=mail_sender_unavailable email={}", to);
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Recuperacao de senha - Do Zero ao CPA");
        message.setText("""
                Ola!

                Use o link abaixo para redefinir sua senha no Do Zero ao CPA:

                %s

                O link expira em 30 minutos. Se voce nao pediu essa recuperacao, ignore este e-mail.
                """.formatted(resetLink));
        sender.send(message);
        return true;
    }
}
