package com.directoriocristiano.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Correos designados como moderadores vía {@code APP_MODERATOR_EMAILS} (specs/002, research R8). */
@Component
public class ModeratorEmails {

    private final Set<String> emails;

    public ModeratorEmails(@Value("${app.moderators.emails:}") String configured) {
        this.emails = Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .map(email -> email.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean contains(String email) {
        return email != null && emails.contains(email.trim().toLowerCase(Locale.ROOT));
    }

    public Set<String> all() {
        return emails;
    }
}
