package co.com.pedrorido.model.external;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public record RequestStatusChangeMessage(
        UUID solicitudId, String estadoAnterior, String estadoNuevo,
        String usuarioId, Date when, Map<UUID, BigDecimal> debtMap, String reason) {}