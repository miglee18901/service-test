package org.example.dao;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.*;

@Getter @Setter
public class OcsVcServerDTO {
    @NotNull @Positive
    private Long id;
    @NotBlank
    private String name;
    @NotBlank
    private String host;
    private Integer tps = 100;
}
