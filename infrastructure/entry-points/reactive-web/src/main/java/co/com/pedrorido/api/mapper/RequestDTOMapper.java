package co.com.pedrorido.api.mapper;

import co.com.pedrorido.api.dto.RequestResponseDTO;
import co.com.pedrorido.model.requestdomain.RequestDomain;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RequestDTOMapper {
    default RequestResponseDTO toDto(RequestDomain r) {
        if (r == null) throw new IllegalArgumentException("RequestDomain is null");
        return RequestResponseDTO.builder()
                .id(r.getId())
                .amount(r.getAmount())
                .term(r.getTerm())
                .email(r.getEmail())
                .statusId(r.getStatusId())
                .typeLoanId(r.getTypeLoanId())
                .build();
    }
}
