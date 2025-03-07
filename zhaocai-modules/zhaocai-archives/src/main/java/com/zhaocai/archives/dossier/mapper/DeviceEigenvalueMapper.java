package com.zhaocai.archives.dossier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.dossier.domain.DeviceEigenvalue;

import java.util.List;

/**
 * 设备特征值Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface DeviceEigenvalueMapper extends BaseMapper<DeviceEigenvalue> {
    /**
     * 查询设备特征值
     *
     * @param id 设备特征值主键
     * @return 设备特征值
     */
    public DeviceEigenvalue selectDeviceEigenvalueById(Long id);

    /**
     * 查询设备特征值列表
     *
     * @param deviceEigenvalue 设备特征值
     * @return 设备特征值集合
     */
    public List<DeviceEigenvalue> selectDeviceEigenvalueList(DeviceEigenvalue deviceEigenvalue);

    public long selectDeviceEigenvalueListCount(DeviceEigenvalue deviceEigenvalue);

    /**
     * 新增设备特征值
     *
     * @param deviceEigenvalue 设备特征值
     * @return 结果
     */
    public int insertDeviceEigenvalue(DeviceEigenvalue deviceEigenvalue);

    /**
     * 修改设备特征值
     *
     * @param deviceEigenvalue 设备特征值
     * @return 结果
     */
    public int updateDeviceEigenvalue(DeviceEigenvalue deviceEigenvalue);

    /**
     * 删除设备特征值
     *
     * @param id 设备特征值主键
     * @return 结果
     */
    public int deleteDeviceEigenvalueById(Long id);

    /**
     * 批量删除设备特征值
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDeviceEigenvalueByIds(Long[] ids);


    public int updateDelByItemId(Long[] ids);


    List<DeviceEigenvalue> getProcessed(String organCode);
}
