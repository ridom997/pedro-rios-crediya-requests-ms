package co.com.pedrorido.model.external;

import java.time.Instant;

public record RequestStatusChangeMessage(
        String solicitudId, String estadoAnterior, String estadoNuevo,
        String usuarioId, Instant when) {}