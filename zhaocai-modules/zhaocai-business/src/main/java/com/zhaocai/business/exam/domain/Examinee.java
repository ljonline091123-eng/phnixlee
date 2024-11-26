package com.zhaocai.business.exam.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zhaocai.common.core.web.domain.BaseEntity;

/**
 * 考生管理对象 tb_examinee
 * 
 * @author xwj
 * @date 2024-11-21
 */

@Getter
@Setter
@TableName(value = "tb_examinee")
public class Examinee extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    ///** id */
    //private Long id;

    /** 工作单位 */
    @ApiModelProperty(value = "工作单位")
    private String workUnit;

    /** 考生姓名 */
    @ApiModelProperty(value = "考生姓名")
    private String examineeName;

    /** 身份证号 */
    @ApiModelProperty(value = "身份证号")
    private String identityCardId;

    /** 联系方式 */
    @ApiModelProperty(value = "联系方式")
    private String phone;

    /** 参赛证号 */
    @ApiModelProperty(value = "参赛证号")
    private String entryCardNumber;

    /** 考场 */
    @ApiModelProperty(value = "考场")
    private String examinationRoom;

    /** 座位号 */
    @ApiModelProperty(value = "座位号")
    private String seatNumber;

    /** 图片 */
    @ApiModelProperty(value = "图片")
    private String pictureUrl;

    /** 考点名称 */
    @ApiModelProperty(value = "考场名称")
    private String examPointName;


}
