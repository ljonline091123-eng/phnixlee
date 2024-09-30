package com.zhaocai.business.manager.http.dto.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyListRequestDTO {

    private static final String PARENT_PROJECT_CODE = "parentProjectCode";
    private static final String RESPONSIBILITY_DEPT_ID = "responsibilityDeptld";
    private static final String COMPANY_ID = "companyld";
    private static final String GROUP_ID = "groupld";


    private String propertyKey;

    private String propertyValue;

    public static void addPropertyToList(List<PropertyListRequestDTO> propertyList,
                                         String propertyName, String propertyValue) {
        PropertyListRequestDTO dto = new PropertyListRequestDTO(propertyName, propertyValue);
        propertyList.add(dto);
    }
}
