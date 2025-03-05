package com.zhaocai.business.manager.http.dto.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author ssy
 * @date 2024/8/5 16:47
 */
@Data
public class PushThirdPartyTodoTaskSonRequestDTO {

    @ApiModelProperty(value =  "消息标题")
    private String title;

    @ApiModelProperty(value =  "消息内容")
    private String content;

    @ApiModelProperty(value =  "消息到达时间")
    private String arrivalTime;

    @ApiModelProperty(value =  "业务发起时间")
    private String createTime;

    @ApiModelProperty(value =  "接收人编号")
    private Long msgToPerCode;

    @ApiModelProperty(value =  "接收人")
    private String msgToPerName;

    @ApiModelProperty(value =  "业务发起人编号")
    private Long msgFromPerCode;

    @ApiModelProperty(value =  "业务发起人")
    private String msgFromPerName;

    @ApiModelProperty(value =  "所属流程分组")
    private String flowGroup;

    @ApiModelProperty(value =  "所属流程模块")
    private String flowModule;

    @ApiModelProperty(value =  "流程名")
    private String flowName;

    @ApiModelProperty(value =  "消息类型(0 代办，1 工作通知)")
    private Integer type;

    @ApiModelProperty(value =  "公司类型(1.大汉 2.天颐 3.晟晟)")
    private Integer companyType;

    @ApiModelProperty(value =  "详情URL")
    private String detailUrl;

    @ApiModelProperty(value =  "用户自定义信息")
    private String userObj;

    @ApiModelProperty(value =  "项目简称")
    private String prjName;

}
