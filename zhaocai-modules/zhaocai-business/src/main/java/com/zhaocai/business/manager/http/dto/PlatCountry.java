package com.zhaocai.business.manager.http.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/7/14 19:51
 */
@Data
public class PlatCountry implements Serializable {

    @ApiModelProperty(value = "国家和地区档案id")
    private String id;

    @ApiModelProperty(value = "中文全称")
    private String chineseFullName;

    @ApiModelProperty(value = "中文简称")
    private String chineseAsName;

    @ApiModelProperty(value = "英文全称")
    private String englishFullName;

    @ApiModelProperty(value = "英文简称")
    private String englishAsName;

    @ApiModelProperty(value = "两字符拉丁字母代码")
    private String twoCharCode;

    @ApiModelProperty(value = "三字符拉丁字母代码")
    private String threeCharCode;

    @ApiModelProperty(value = "阿拉伯数字代码")
    private String arabicNumeralCode;


}
