package com.zhaocai.archives.dossier.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.domain.DeviceType;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.tree.DeviceTypeTree;
import com.zhaocai.archives.main.domain.DeviceClass;

import java.util.List;
import java.util.Map;

/**
 * 设备分类Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IDeviceTypeService extends IService<DeviceType> {
    /**
     * 查询设备分类
     *
     * @param id 设备分类主键
     * @return 设备分类
     */
    public DeviceType selectDeviceTypeById(Long id);

    /**
     * 查询设备分类
     *
     * @param id 设备分类主键
     * @return 设备分类
     */
    public DeviceType selectDeviceTypeByIdNoChange(Long id);

    /**
     * 查询设备分类列表
     *
     * @param deviceType 设备分类
     * @return 设备分类集合
     */
    public List<DeviceType> selectDeviceTypeList(DeviceType deviceType);

    /**
     * 新增设备分类
     *
     * @param deviceType 设备分类
     * @return 结果
     */
    public int insertDeviceType(DeviceType deviceType);

    /**
     * 修改设备分类
     *
     * @param deviceType 设备分类
     * @return 结果
     */
    public int updateDeviceType(DeviceType deviceType);

    /**
     * 批量删除设备分类
     *
     * @param ids 需要删除的设备分类主键集合
     * @return 结果
     */
    public boolean deleteDeviceTypeByIds(Long[] ids);

    /**
     * 删除设备分类信息
     *
     * @param id 设备分类主键
     * @return 结果
     */
    public int deleteDeviceTypeById(Long id);

    List<DeviceType> initData(DeviceType deviceType);

    List<DeviceTypeTree> getDeviceTypeTree(DeviceType deviceType);

    DeviceType initDeviceType(DeviceType deviceType);

    void addTypeByMain(DeviceClass mtrClass);

    void updateByHostId(DeviceClass mtrClass);

    void deleteByHostId(String[] histIds, Map<Long,Long> idsMap);

    void processAuditPass(Map<String, Object> variables);


    int addToMain(DeviceType deviceType);

    int associationToMain(DeviceType deviceType);

    int unAssociationToMain(DeviceType deviceType);


    long selectDeviceTypeListCount(DeviceType deviceType);

    int getMaterialJoin(Long id);

    int updateDeviceTypeNoChange(DeviceType materialType);
}
