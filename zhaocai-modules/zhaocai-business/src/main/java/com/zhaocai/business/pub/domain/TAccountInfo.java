package com.zhaocai.business.pub.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 账户信息
 *
 * @author cs
 * @date 2024-11-20
 */
@Data
@TableName(value = "t_account_info")
public class TAccountInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    /** 商户ID */
    @Excel(name = "商户ID")
    private String upId;

    /** 银行账号 */
    @Excel(name = "银行账号")
    private String bankAccount;

    /** 所属银行 */
    @Excel(name = "所属银行")
    private String affiliatedBank;

    /** 支行 */
    @Excel(name = "支行")
    private String openingBranch;

    /** 银联号 */
    @Excel(name = "银联号")
    private String interbankNumber;

    /** BIPID */
    @Excel(name = "BIPID")
    private Long bipId;

    /** 账户类型 */
    @Excel(name = "账户类型")
    private Integer acountType;

    /** 币种 */
    @Excel(name = "币种")
    private Integer currency;



    /** 是否默认账户 */
    @Excel(name = "是否默认账户")
    private Integer status;

    /**
     * 创建者
     */
    @ApiModelProperty(value = "创建者")
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     *
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新者
     */
    @ApiModelProperty(value = "更新者")
    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

}
