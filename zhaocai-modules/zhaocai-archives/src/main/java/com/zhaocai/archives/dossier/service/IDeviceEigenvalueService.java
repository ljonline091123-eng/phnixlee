package com.zhaocai.archives.dossier.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.domain.DeviceEigenvalue;
import com.zhaocai.archives.main.domain.DeviceFeatureValue;

import java.util.List;
import java.util.Map;

/**
 * 设备特征值Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IDeviceEigenvalueService extends IService<DeviceEigenvalue> {
    /**
     * 查询设备特征值
     *
     * @param id 设备特征值主键
     * @return 设备特征值
     */
    public DeviceEigenvalue selectDeviceEigenvalueById(Long id);

    public List<DeviceEigenvalue> selectDeviceEigenvalueListNoChange(DeviceEigenvalue deviceEigenvalue);

    /**
     * 查询设备特征值列表
     *
     * @param deviceEigenvalue 设备特征值
     * @return 设备特征值集合
     */
    public List<DeviceEigenvalue> selectDeviceEigenvalueList(DeviceEigenvalue deviceEigenvalue);

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
     * 批量删除设备特征值
     *
     * @param ids 需要删除的设备特征值主键集合
     * @return 结果
     */
    public boolean deleteDeviceEigenvalueByIds(Long[] ids);

    /**
     * 删除设备特征值信息
     *
     * @param id 设备特征值主键
     * @return 结果
     */
    public int deleteDeviceEigenvalueById(Long id);

    List<DeviceEigenvalue> initData(DeviceEigenvalue deviceEigenvalue);

    void addTypeByMain(DeviceFeatureValue mtrClass);

    void updateByHostId(DeviceFeatureValue mtrClass);


    void deleteByHostId(String[] histIds, Map<Long, Long> idsMap);


    int addToMain(DeviceEigenvalue deviceEigenvalue);

    int associationToMain(DeviceEigenvalue deviceEigenvalue);

    int unAssociationToMain(DeviceEigenvalue deviceEigenvalue);

    long selectDeviceEigenvalueListCount(DeviceEigenvalue deviceEigenvalue);

    List<DeviceEigenvalue> getProcessed(String organCode);
}
