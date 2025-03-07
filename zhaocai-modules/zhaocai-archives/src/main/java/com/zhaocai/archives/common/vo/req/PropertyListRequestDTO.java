package com.zhaocai.archives.common.vo.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyListRequestDTO<T> {

    private static final String PARENT_PROJECT_CODE = "parentProjectCode";/* 项目部 */
    private static final String RESPONSIBILITY_DEPT_ID = "responsibilityDeptld";/* 责任单位 */
    private static final String COMPANY_ID = "companyld";/* 公司 */
    private static final String GROUP_ID = "groupld";/* 集团 */
    private static final String CONTRACT_TYPE = "contractType";/* 合同类型 */
    private static final String CONTRACT_MONEY = "contractMoney";/* 合同金额 */


    private String propertyKey;

    private T propertyValue;

    /**
     * 如果存在则更新
     */
    public static <T> void addPropertyToList(List<PropertyListRequestDTO<T>> propertyList,String propertyName,T propertyValue) {
        // 查找是否有匹配的 propertyKey
        Optional<PropertyListRequestDTO<T>> existingProperty = propertyList.stream()
                .filter(dto -> dto.getPropertyKey().equals(propertyName))
                .findFirst();

        // 如果存在则更新，否则添加新的项
        if (existingProperty.isPresent()) {
            existingProperty.get().setPropertyValue(propertyValue);
        } else {
            propertyList.add(new PropertyListRequestDTO<>(propertyName, propertyValue));
        }
    }

    /**
     * 重复插入
     */
    public static <T> void addPropertyList(List<PropertyListRequestDTO<T>> propertyList, String propertyName, T propertyValue) {
        PropertyListRequestDTO<T> dto = new PropertyListRequestDTO<>(propertyName, propertyValue);
        propertyList.add(dto);
    }
}

