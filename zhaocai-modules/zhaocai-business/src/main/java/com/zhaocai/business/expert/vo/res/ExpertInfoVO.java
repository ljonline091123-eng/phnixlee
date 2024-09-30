package com.zhaocai.business.expert.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author ssy
 * @date 2024/5/27 15:01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ExpertInfoVO", description = "专家详情信息VO")
public class ExpertInfoVO extends AdviceObject {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value =  "专家姓名")
    private String expertName;

    @ApiModelProperty(value =  "专家手机号码")
    private String expertPhone;

    @ApiModelProperty(value =  "所属组织机构")
    private String belongOrganization;

    @ApiModelProperty(value =  "工作部门")
    private String department;

    @ApiModelProperty(value =  "学历")
    private Integer educationDegree;

    @ApiModelProperty(value =  "专业")
    private String major;

    @ApiModelProperty(value =  "业态")
    private Integer businessType;

    @ApiModelProperty(value =  "专家类别")
    private Integer expertType;

    @ApiModelProperty(value =  "专家状态(0账号审批中|1启用|2禁用)")
    private Integer expertState;

    @ApiModelProperty(value =  "对应用户id")
    private Long userId;

    @ApiModelProperty(value =  "学历（文本）")
    @DictCache(dictBizEnum= DictBizEnum.EDUCATION_DEGREE,filedName = "educationDegree")
    private String educationDegreeText;

    @ApiModelProperty(value =  "业态（文本）")
    @DictCache(dictBizEnum= DictBizEnum.EXPERT_BUSINESS_TYPE,filedName = "businessType")
    private String businessTypeText;

    @ApiModelProperty(value =  "专家类别（文本）")
    @DictCache(dictBizEnum= DictBizEnum.EXPERT_TYPE,filedName = "expertType")
    private String expertTypeText;


    @ApiModelProperty(value =  "执业资格证")
    private Integer registeredCertificate;

    @ApiModelProperty(value =  "执业资格证（文本）")
    @DictCache(dictBizEnum= DictBizEnum.REGISTERED_CERTIFICATE,filedName = "registeredCertificate")
    private String registeredCertificateText;

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

    @ApiModelProperty(value =  "技术职称（文本）")
    @DictCache(dictBizEnum= DictBizEnum.TECHNICAL_TITLES,filedName = "technicalTitles")
    private String technicalTitlesText;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "技术职称获取时间")
    private Date technicalTitlesDate;

    @ApiModelProperty(value =  "现工作单位或者部门")
    private String presentUnitDept;

    @ApiModelProperty(value =  "邮箱")
    private String email;

    @ApiModelProperty(value =  "相关专业工作简历")
    private String professionResume;

    @ApiModelProperty(value =  "工作简历附件")
    private List<AttachmentVO> resumeAttachList;

}
