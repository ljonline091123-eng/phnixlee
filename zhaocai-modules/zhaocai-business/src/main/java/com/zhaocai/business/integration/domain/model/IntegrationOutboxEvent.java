package com.zhaocai.business.integration.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/** 外部系统可靠投递事件；业务事务先落库，再执行或重试外部调用。 */
@Getter
@Setter
@ApiModel("集成事件发件箱")
@TableName("tb_integration_outbox_event")
public class IntegrationOutboxEvent extends BaseEntity {
    public static final int PENDING = 0;
    public static final int PROCESSING = 1;
    public static final int PUBLISHED = 2;
    public static final int FAILED = 3;
    public static final int DEAD = 4;

    @ApiModelProperty("聚合类型")
    private String aggregateType;
    @ApiModelProperty("聚合标识")
    private String aggregateId;
    @ApiModelProperty("事件类型")
    private String eventType;
    @ApiModelProperty("幂等键")
    private String idempotencyKey;
    @ApiModelProperty("JSON载荷")
    private String payload;
    @ApiModelProperty("状态：0待投递，1投递中，2成功，3失败，4死信")
    private Integer status;
    @ApiModelProperty("重试次数")
    private Integer retryCount;
    @ApiModelProperty("下次重试时间")
    private Date nextRetryAt;
    @ApiModelProperty("最后错误")
    private String lastError;
    @ApiModelProperty("投递完成时间")
    private Date publishedAt;
}
