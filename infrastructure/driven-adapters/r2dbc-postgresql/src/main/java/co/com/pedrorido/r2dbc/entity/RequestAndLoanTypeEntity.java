package co.com.pedrorido.r2dbc.entity;

import lombok.*;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RequestAndLoanTypeEntity {
    @Column("id_solicitud")
    private UUID idSolicitud;

    @Column("monto")
    private BigDecimal monto;

    @Column("plazo")
    private Integer plazo;

    @Column("email")
    private String email;

    @Column("id_estado")
    private String idEstado;

    @Column("id_tipo_prestamo")
    private Long idTipoPrestamo;

    @Column("tasa_interes")
    private BigDecimal tasaInteres;
}
