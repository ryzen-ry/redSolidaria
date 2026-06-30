package com.redsolidaria.enjambre.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespuestasTestDTO {
    private Map<Long, String> respuestas; // Map of question_id -> chosen option ("a", "b", "c", "d")
}
