package com.zhaocai.common.signature.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 签章签署位置
 *
 * @author chenming
 * @date 2024-09-14
 */
@Data
public class SignatureStamper {
    /**
     * 发起方、接收方
     */
    private Integer type;

    /**
     * 签署类型
     */
    private String signType;

    /**
     * 关键词
     */
    private String keyWord;

    /**
     * 签署坐标页码
     */
    private Integer signPage;

    /**
     * 签署 X 轴坐标
     */
    private BigDecimal offsetX;

    /**
     * 签署 Y 轴坐标
     */
    private BigDecimal offsetY;

    /**
     * 平台对应的签订类型
     */
    private String platformSignType;
}
