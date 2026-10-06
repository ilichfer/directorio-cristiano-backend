package com.directoriocristiano.dto;

/**
 * Decisión del moderador. {@code expectedVersion} es la versión de la solicitud que tenía a la
 * vista (FR-020); {@code reason} es obligatorio al rechazar o suspender (FR-018, FR-021).
 */
public record ModerationDecisionRequest(Integer expectedVersion, String reason) {}
