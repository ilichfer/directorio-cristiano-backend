package com.directoriocristiano.config;

import com.directoriocristiano.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Al arrancar, marca como moderadoras las cuentas existentes cuyos correos están en
 * {@code APP_MODERATOR_EMAILS}. Las que se registren después se marcan al crearse.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModeratorBootstrap implements ApplicationRunner {

    private final ModeratorEmails moderatorEmails;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String email : moderatorEmails.all()) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                if (!user.isModerator()) {
                    user.setModerator(true);
                    userRepository.save(user);
                    log.info("Cuenta {} marcada como moderadora", email);
                }
            });
        }
    }
}
