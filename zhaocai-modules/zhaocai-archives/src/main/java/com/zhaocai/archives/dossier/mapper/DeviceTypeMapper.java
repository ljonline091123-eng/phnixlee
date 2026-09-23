package com.zhaocai.archives.dossier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.dossier.domain.DeviceType;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备分类Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface DeviceTypeMapper extends BaseMapper<DeviceType> {
    /**
     * 查询设备分类
     *
     * @param id 设备分类主键
     * @return 设备分类
     */
    public DeviceType selectDeviceTypeById(Long id);

    /**
     * 查询设备分类列表
     *
     * @param deviceType 设备分类
     * @return 设备分类集合
     */
    public List<DeviceType> selectDeviceTypeList(DeviceType deviceType);

    public long selectDeviceTypeListCount(DeviceType deviceType);

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
     * 删除设备分类
     *
     * @param id 设备分类主键
     * @return 结果
     */
    public int deleteDeviceTypeById(Long id);

    /**
     * 批量删除设备分类
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDeviceTypeByIds(Long[] ids);


    Integer getMaxCode(@Param("upCode") String upCode, @Param("upId") Long upId, @Param("organCode") String organCode);

    Integer getReuseTowCode(@Param("upCode") String upCode, @Param("upId") Long upId, @Param("organCode") String organCode, @Param("pdz") Integer pdz);

    int getMaterialJoinNoMy(Long id);

    int getMaterialJoin(Long id);


    List<DeviceType> getProcessed(String organCode);
}
