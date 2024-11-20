package com.zhaocai.business.expert.vo.req.query;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author ssy
 * @date 2024/5/27 14:02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ExpertQueryVO", description = "专家列表息查询VO")
public class ExpertQueryVO extends PageRecive implements Serializable {
    private static final long serialVersionUID = -772055659721963906L;

    @ApiModelProperty(value =  "专家姓名")
    private String expertName;

    @ApiModelProperty(value =  "专家手机号码")
    private String expertPhone;

    @ApiModelProperty(value =  "所属组织机构")
    private String belongOrganization;

    @ApiModelProperty(value =  "专家类别")
    private Integer expertType;

    @ApiModelProperty(value =  "专家类别（多）")
    private String expertTypes;

    @ApiModelProperty(value =  "部门id")
    private Long deptId;

    @ApiModelProperty(value =  "部门id（多）")
    private String deptIds;

    @ApiModelProperty(value =  "部门id（数组）")
    private List<Long> deptIdList;

    @ApiModelProperty(value =  "业态")
    private String businessType;

    @ApiModelProperty(value =  "业态（多）")
    private String businessTypes;

    @ApiModelProperty(value =  "专业")
    private String major;

    @ApiModelProperty(value =  "本专业工作年限")
    private Integer workYear;

    @ApiModelProperty(value =  "工作年限比较时间")
    private Date workYearCompareDate;

    @ApiModelProperty(value =  "技术职称（初级、中级、副高、正高、教授级高工、研究员）")
    private Integer technicalTitles;

    @ApiModelProperty(value =  "执业资格证")
    private String registeredCertificate;

    @ApiModelProperty(value =  "已选择的专家数据")
    private List<Long> notIncludeExpertIdList;

    @ApiModelProperty(value =  "已选择的专家数据")
    private String notIncludeExpertIds;

    @ApiModelProperty(value = "专家随机抽取参数")
    private ExpertRandomDrawVO drawVO;

    @ApiModelProperty(value =  "招标公告状态")
    private Integer noticeStatus;

    @ApiModelProperty(value =  "专家状态(0账号审批中|1启用|2禁用)")
    private Integer expertState;

    @ApiModelProperty(value = "审批状态")
    private Integer state;

    @ApiModelProperty(value =  "对应用户id")
    private Long userId;

    @ApiModelProperty(value =  "是否剔除已有评标的专家（1是，其它否）")
    private Integer filterEvalExpert;

}
