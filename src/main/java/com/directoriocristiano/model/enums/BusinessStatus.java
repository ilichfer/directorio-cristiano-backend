package com.directoriocristiano.model.enums;

/** Estado de publicación de un negocio. Solo {@code published} es visible al público. */
public enum BusinessStatus {
    draft,
    in_review,
    published,
    paused,
    suspended
}
