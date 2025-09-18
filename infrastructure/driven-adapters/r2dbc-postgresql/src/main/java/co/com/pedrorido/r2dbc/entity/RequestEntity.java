package co.com.pedrorido.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Table("solicitud")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RequestEntity {
    @Id
    @Column("id_solicitud")
    private UUID id;

    @Column("monto")
    private BigDecimal amount;

    @Column("plazo")
    private Integer term;

    @Column("email")
    private String email;

    @Column("id_estado")
    private Long statusId;

    @Column("id_tipo_prestamo")
    private Long typeLoanId;

    @Column("deuda_mensual")
    private BigDecimal monthlyDebt;
}
