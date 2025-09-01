package co.com.pedrorido.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class GeneralResponseDTO<T> {
    private boolean success;
    private String message;
    private Map<String, T> data;
}

