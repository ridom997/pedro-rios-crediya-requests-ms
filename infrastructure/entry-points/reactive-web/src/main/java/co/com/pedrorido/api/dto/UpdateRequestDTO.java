package co.com.pedrorido.api.dto;

import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UpdateRequestDTO {
    private UUID id;
    private Long statusId;
}
