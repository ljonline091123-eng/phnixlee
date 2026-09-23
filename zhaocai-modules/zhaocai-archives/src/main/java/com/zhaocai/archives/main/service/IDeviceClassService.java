package com.zhaocai.archives.main.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.tree.DeviceTypeTree;
import com.zhaocai.archives.main.domain.DeviceClass;
import com.zhaocai.archives.main.domain.DeviceClassExcelData;

import java.util.List;

/**
 * 设备分类主Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IDeviceClassService extends IService<DeviceClass> {
    /**
     * 查询设备分类主
     *
     * @param id 设备分类主主键
     * @return 设备分类主
     */
    public DeviceClass selectDeviceClassById(String id);

    /**
     * 查询设备分类主列表
     *
     * @param deviceClass 设备分类主
     * @return 设备分类主集合
     */
    public List<DeviceClass> selectDeviceClassList(DeviceClass deviceClass);

    /**
     * 新增设备分类主
     *
     * @param deviceClass 设备分类主
     * @return 结果
     */
    public int insertDeviceClass(DeviceClass deviceClass);

    /**
     * 修改设备分类主
     *
     * @param deviceClass 设备分类主
     * @return 结果
     */
    public int updateDeviceClass(DeviceClass deviceClass);

    /**
     * 批量删除设备分类主
     *
     * @param ids 需要删除的设备分类主主键集合
     * @return 结果
     */
    public boolean deleteDeviceClassByIds(String[] ids);

    /**
     * 删除设备分类主信息
     *
     * @param id 设备分类主主键
     * @return 结果
     */
    public int deleteDeviceClassById(String id);

    List<DeviceTypeTree> getDeviceClassTree();

    DeviceClass initCode(DeviceClass deviceClass);

    long selectDeviceClassListCount(DeviceClass deviceClass);

    String importData(List<DeviceClassExcelData> userList, boolean updateSupport, String operName);

    List<DeviceTypeTree> getDeviceClassTreeTwo();
}
