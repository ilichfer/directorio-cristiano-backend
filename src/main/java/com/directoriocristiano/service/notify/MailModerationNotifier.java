package com.directoriocristiano.service.notify;

import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.BusinessChangeRequest;
import com.directoriocristiano.model.enums.ChangeRequestType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Envía los avisos por correo. Un fallo de correo no deshace la decisión de moderación. */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class MailModerationNotifier implements ModerationNotifier {

    private final JavaMailSender mailSender;
    private final LogModerationNotifier logNotifier;
    private final String from;

    public MailModerationNotifier(JavaMailSender mailSender,
                                  LogModerationNotifier logNotifier,
                                  @Value("${app.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.logNotifier = logNotifier;
        this.from = from;
    }

    @Override
    public void approved(BusinessChangeRequest request) {
        logNotifier.approved(request);
        Business business = request.getBusiness();
        String text = request.getType() == ChangeRequestType.new_business
                ? "Tu negocio \"%s\" ya está publicado en el Directorio Cristiano.".formatted(business.getName())
                : "Los cambios de \"%s\" ya están publicados.".formatted(business.getName());
        send(business, "Aprobado: " + business.getName(), text);
    }

    @Override
    public void rejected(BusinessChangeRequest request) {
        logNotifier.rejected(request);
        Business business = request.getBusiness();
        String text = "Tu solicitud para \"%s\" no fue aprobada.%n%nMotivo: %s%n%n"
                .formatted(business.getName(), request.getRejectionReason())
                + "Puedes corregirla desde \"Mi negocio\" y volver a enviarla.";
        send(business, "Revisa tu solicitud: " + business.getName(), text);
    }

    @Override
    public void suspended(Business business) {
        logNotifier.suspended(business);
        String text = "Tu negocio \"%s\" fue suspendido y no se muestra en el directorio.%n%nMotivo: %s"
                .formatted(business.getName(), business.getSuspensionReason());
        send(business, "Negocio suspendido: " + business.getName(), text);
    }

    private void send(Business business, String subject, String text) {
        if (business.getOwner() == null) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (!from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(business.getOwner().getEmail());
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("No se pudo enviar el aviso a {}: {}", business.getOwner().getEmail(), e.getMessage());
        }
    }
}
