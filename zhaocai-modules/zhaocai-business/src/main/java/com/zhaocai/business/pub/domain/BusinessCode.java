package com.zhaocai.business.pub.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 系统业务编号规则对象 tb_business_code
 *
 * @author WH
 * @date 2024-05-27
 */
@Data
@TableName(value = "tb_business_code")
public class BusinessCode extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 业务编码
     */
    private String businessCode;

    /**
     * 业务名称
     */
    private String businessName;

    /**
     * 当前日期类型
     */
    private String nowDateType;

    /**
     * 当前日期
     */
    private String nowDate;

    /**
     * 前缀
     */
    private String prefix;

    /**
     * 编码数值
     */
    private Long codeNumber;

    /**
     * 编码长度
     */
    private Integer codeNumberLength;
}
