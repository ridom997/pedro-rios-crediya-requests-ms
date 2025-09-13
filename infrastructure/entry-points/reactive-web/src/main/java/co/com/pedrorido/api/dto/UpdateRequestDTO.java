package co.com.pedrorido.api.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UpdateRequestDTO {
    private String id;
    private Long statusId;
}
