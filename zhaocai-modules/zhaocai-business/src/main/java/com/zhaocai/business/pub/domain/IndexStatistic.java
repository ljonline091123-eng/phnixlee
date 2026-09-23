package com.zhaocai.business.pub.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 首页统计数据对象 tb_index_statistic
 * 
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_index_statistic")
public class IndexStatistic extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 累计采购金额 */
    @ApiModelProperty(value =  "累计采购金额")
    private BigDecimal totalProcurementAmout;

    /** 合作供应商数 */
    @ApiModelProperty(value =  "合作供应商数")
    private Long totalVendor;

    /** 上线项目数 */
    @ApiModelProperty(value =  "上线项目数")
    private Long totalProject;
}
