package co.com.pedrorido.api.mapper;

import co.com.pedrorido.api.dto.CreateRequestDTO;
import co.com.pedrorido.model.utils.CreateRequestDomainDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CreateRequestDTOMapper {
    CreateRequestDomainDTO toDomain(CreateRequestDTO dto);
}