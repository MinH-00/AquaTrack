package com.aquatrack.aquarium.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class AquariumRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String fishName;

    @NotBlank
    private String boardId;
}
