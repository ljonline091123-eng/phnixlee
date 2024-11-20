package com.zhaocai.business.expert.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.common.core.bean.ValidateGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author ssy
 * @date 2024/5/27 10:56
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ExpertVO", description = "专家信息VO")
public class ExpertVO implements Serializable {
    private static final long serialVersionUID = 7489388721392949260L;

    @ApiModelProperty(value =  "专家id")
    private Long id;

    @ApiModelProperty(value =  "原专家id")
    private Long expertId;

    @ApiModelProperty(value =  "专家姓名")
    @NotBlank(message = "专家姓名不能为空", groups = {ValidateGroup.AddGroup.class})
    private String expertName;

    @ApiModelProperty(value =  "专家手机号码")
    @NotBlank(message = "专家手机号码不能为空", groups = {ValidateGroup.AddGroup.class})
    private String expertPhone;

    @ApiModelProperty(value =  "所属组织机构")
//    @NotBlank(message = "所属组织机构不能为空", groups = {ValidateGroup.AddGroup.class})
    private String belongOrganization;

    @ApiModelProperty(value =  "工作部门")
//    @NotBlank(message = "工作部门不能为空", groups = {ValidateGroup.AddGroup.class})
    private String department;

    @ApiModelProperty(value =  "学历")
    private Integer educationDegree;

    @ApiModelProperty(value =  "专业")
    private String major;

    @ApiModelProperty(value =  "业态")
    private String businessType;

    @ApiModelProperty(value =  "专家类别")
    @NotNull(message = "工作部门不能为空", groups = {ValidateGroup.AddGroup.class})
    private Integer expertType;

    @ApiModelProperty(value =  "专家用户id")
    private Long userId;


    @ApiModelProperty(value =  "执业资格证")
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

    @ApiModelProperty(value = "工作简历附件")
    private List<AttachmentRequestVO> resumeAttachList;

}
