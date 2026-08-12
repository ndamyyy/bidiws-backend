package com.bidiws.security;

/**
 * Levee par StompAuthChannelInterceptor quand une frame CONNECT ne porte
 * pas de JWT valide, ou qu'un SUBSCRIBE cible une ressource hors du
 * rattachement de l'utilisateur. Spring la convertit en frame STOMP ERROR
 * renvoyee au client.
 */
public class StompAuthenticationException extends RuntimeException {

    public StompAuthenticationException(String message) {
        super(message);
    }
}
