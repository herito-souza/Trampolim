package com.trampolim.api.user;
/** Minimal live session state. Never loads password hashes, documents or profile photos. */
public record SessionAccount(Long id,String email,int tokenVersion) {}
