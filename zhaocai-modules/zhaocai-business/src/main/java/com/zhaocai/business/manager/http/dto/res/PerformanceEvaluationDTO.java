package com.zhaocai.business.manager.http.dto.res;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PerformanceEvaluationDTO {

    /**
     * 项目id
     */
    private String projectId;


    /**
     * 合同编号
     */
    private String conCode;

    /**
     * 合同名称
     */
    private String conName;

    /**
     * 甲方id
     */
    private String partaId;

    /**
     * 甲方名称
     */
    private String partaName;

    /**
     * 乙方id
     */
    private String partbId;

    /**
     * 乙方名称
     */
    private String partbName;

    /**
     * 优-数量
     */
    private BigDecimal excellentNum;

    /**
     * 良-数量
     */
    private BigDecimal goodNum;

    /**
     * 合格-数量
     */
    private BigDecimal qualifiedNum;

    /**
     * 差-数量
     */
    private BigDecimal badNum;
}
