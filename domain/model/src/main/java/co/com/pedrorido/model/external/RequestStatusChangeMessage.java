package co.com.pedrorido.model.external;

import java.util.Date;
import java.util.UUID;

public record RequestStatusChangeMessage(
        UUID solicitudId, String estadoAnterior, String estadoNuevo,
        String usuarioId, Date when) {}