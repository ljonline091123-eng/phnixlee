package com.zhaocai.business.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import java.io.Serializable;
import java.math.BigInteger;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 第三方数据合约规划推送记录
 * </p>
 *
 * @author susiyuan
 * @since 2024-09-12
 */
@Getter
@Setter
@TableName("tb_contract_planning_push_record")
@ApiModel(value = "ContractPlanningPushRecord对象", description = "第三方数据合约规划推送记录")
public class ContractPlanningPushRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("合约规划id")
    private String contractPlanningId;

    @ApiModelProperty("项目合约编码")
    private String contractPlanningCode;

    @ApiModelProperty("推送状态（0未推送 1已推送）")
    private Integer pushStatus;

    @ApiModelProperty("推送对象")
    private String pushObj;

}
