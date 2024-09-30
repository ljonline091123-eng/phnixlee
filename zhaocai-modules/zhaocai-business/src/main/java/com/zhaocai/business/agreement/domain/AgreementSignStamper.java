package com.zhaocai.business.agreement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同签章签署位置对象 tb_agreement_sign_stamper
 *
 * @author chenming
 * @date 2024-09-14
 */
@Data
@TableName(value = "tb_agreement_sign_stamper")
public class AgreementSignStamper extends BaseEntity {

    /**
     * 类型（1：发起方  2：接收方）
     */
    @ApiModelProperty(value = "类型 1=：发起方,2=：接收方")
    private Integer type;

    /**
     * 模板 id
     */
    @ApiModelProperty(value = "模板 id")
    private Long templateId;

    /**
     * 签署类型
     */
    @ApiModelProperty(value = "签署类型")
    private String signType;

    /**
     * 关键词
     */
    @ApiModelProperty(value = "关键词")
    private String keyWord;

    /**
     * 签署坐标页码
     */
    @ApiModelProperty(value = "签署坐标页码")
    private Integer signPage;

    /**
     * 签署 X 轴坐标
     */
    @ApiModelProperty(value = "签署 X 轴坐标")
    private BigDecimal offsetX;

    /**
     * 签署 Y 轴坐标
     */
    @ApiModelProperty(value = "签署 Y 轴坐标")
    private BigDecimal offsetY;
}
