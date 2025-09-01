package co.com.pedrorido.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("tipo_prestamo")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class LoanTypeEntity {
    @Id
    @Column("id_tipo_prestamo")
    private Long id;

    @Column("nombre")
    private String name;

    @Column("monto_minimo")
    private BigDecimal minimumAmount;

    @Column("monto_maximo")
    private BigDecimal maximumAmount;

    @Column("tasa_interes")
    private Double interestRate;

    @Column("validacion_automatica")
    private Boolean automaticValidation;
}
