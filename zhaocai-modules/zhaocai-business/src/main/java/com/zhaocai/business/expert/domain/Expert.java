package com.zhaocai.business.expert.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 专家对象 tb_expert
 *
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_expert")
public class Expert extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 专家姓名 */
    @ApiModelProperty(value =  "专家姓名")
    private String expertName;

    /** 专家手机号码 */
    @ApiModelProperty(value =  "专家手机号码")
    private String expertPhone;

    /** 所属组织机构 */
    @ApiModelProperty(value =  "所属组织机构")
    private String belongOrganization;

    /** 工作部门 */
    @ApiModelProperty(value =  "工作部门")
    private String department;

    /** 学历 */
    @ApiModelProperty(value =  "学历")
    private Integer educationDegree;

    /** 专业 */
    @ApiModelProperty(value =  "专业")
    private String major;

    /** 业态 */
    @ApiModelProperty(value =  "业态")
    private String businessType;

    /** 专家类别（1技术类 2经济类） */
    @ApiModelProperty(value =  "专家类别（1技术类 2经济类）")
    private Integer expertType;

    /** 专家状态 */
    @ApiModelProperty(value =  "专家状态(0(审批中/保存) | 1启用 | 2禁用)")
    private Integer expertState;

    /**
     * 审批状态 {@link com.zhaocai.business.common.enums.ExpertStateEnum}
     */
    @ApiModelProperty(value = "审批状态(0保存|1审批中|2审批拒绝|3审批通过)")
    private Integer state;

    /**
     * 流程实例 id
     */
    @ApiModelProperty(value = "流程实例 id")
    private String wfProcessId;

    /**
     * 流程类型
     */
    @ApiModelProperty(value = "流程类型")
    private Integer processType;

    /**
     * 审批信息,批语
     */
    @ApiModelProperty(value = "审批信息")
    private String operateComment;

    /** 对应用户id */
    @ApiModelProperty(value =  "对应用户id")
    private Long userId;

    @ApiModelProperty(value =  "执业资格证（字典类）")
    private Integer registeredCertificate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "执业资格证取得时间")
    private Date registeredCertificateDate;

    @ApiModelProperty(value =  "现从事专业工作")
    private String presentJob;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "本专业参加工作时间")
    private Date presentJobDate;

    @ApiModelProperty(value =  "技术职称（初级、中级、副高、正高、教授级高工、研究员）")
    private Integer technicalTitles;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "技术职称获取时间")
    private Date technicalTitlesDate;

    @ApiModelProperty(value =  "现工作单位或者部门")
    private String presentUnitDept;

    @ApiModelProperty(value =  "邮箱")
    private String email;

    @ApiModelProperty(value =  "相关专业工作简历")
    private String professionResume;

    @Override
    public String toString() {
        return "\nExpert{" +
                "expertName='" + expertName + '\'' +
                ", expertPhone='" + expertPhone + '\'' +
                ", belongOrganization='" + belongOrganization + '\'' +
                ", department='" + department + '\'' +
                ", educationDegree=" + educationDegree +
                ", major='" + major + '\'' +
                ", businessType=" + businessType +
                ", expertType=" + expertType +
                ", expertState=" + expertState +
                ", state=" + state +
                ", wfProcessId='" + wfProcessId + '\'' +
                ", processType=" + processType +
                ", operateComment='" + operateComment + '\'' +
                ", userId=" + userId +
                ", registeredCertificate=" + registeredCertificate +
                ", registeredCertificateDate=" + registeredCertificateDate +
                ", presentJob='" + presentJob + '\'' +
                ", presentJobDate=" + presentJobDate +
                ", technicalTitles=" + technicalTitles +
                ", technicalTitlesDate=" + technicalTitlesDate +
                ", presentUnitDept='" + presentUnitDept + '\'' +
                ", email='" + email + '\'' +
                ", professionResume='" + professionResume + '\'' +
                '}'+"\n";
    }
}
