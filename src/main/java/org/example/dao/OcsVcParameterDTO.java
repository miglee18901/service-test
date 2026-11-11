package org.example.dao;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotBlank;

@Getter @Setter
public class OcsVcParameterDTO {
    @NotBlank
    private String name;
    private String valueString;

    public OcsVcParameterDTO vClone() {
        OcsVcParameterDTO clone = new OcsVcParameterDTO();
        clone.setName(name);
        clone.setValueString(valueString);
        return clone;
    }
}
