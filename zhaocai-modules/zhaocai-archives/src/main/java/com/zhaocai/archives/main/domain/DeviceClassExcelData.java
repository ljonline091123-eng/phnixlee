package com.zhaocai.archives.main.domain;

import com.zhaocai.common.core.annotation.Excel;
import lombok.Data;

import java.io.Serializable;

/**
 * 设备分类主对象 device_class
 *
 * @author lzq
 * @date 2025-01-06
 */
@Data
public class DeviceClassExcelData implements Serializable {
    private static final long serialVersionUID = 1L;

    @Excel(name = "序号")
    private String xh;

    /**
     * 父节点
     */
    @Excel(name = "*父级设备分类编码")
    private String parentCode;

    /**
     * 设备分类编码
     */
    @Excel(name = "*设备分类编码")
    private String deviceClassCode;

    /**
     * 设备分类名称
     */
    @Excel(name = "*设备分类名称")
    private String deviceClassName;


    /**
     * 计量单位
     */
    @Excel(name = "计量单位")
    private String measureUnit;

    /**
     * 特征项编号
     */
    @Excel(name = "特征项编号")
    private String featureCode;

    /**
     * 特征项名称
     */
    @Excel(name = "特征项名称")
    private String featureName;


    /**
     * 特征值编号
     */
    @Excel(name = "特征值编号")
    private String featureValueCode;

    /**
     * 特征值名称
     */
    @Excel(name = "特征值名称")
    private String featureValueName;


    /**
     * 设备编号
     */
    @Excel(name = "设备编号")
    private String deviceCode;

    /**
     * 设备名称
     */
    @Excel(name = "设备名称")
    private String deviceName;


    /**
     * 计量单位
     */
    @Excel(name = "单位")
    private String unit;

}
