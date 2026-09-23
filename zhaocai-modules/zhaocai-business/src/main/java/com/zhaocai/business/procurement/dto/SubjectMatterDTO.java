package com.zhaocai.business.procurement.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 交易标的物
 *
 * @author chenming
 * @date 2024-08-29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectMatterDTO {

    /**
     * 交易标的物编码
     */
    private String subjectMatterCode;

    /**
     * 交易标的物名称
     */
    private String subjectMatterName;
}
