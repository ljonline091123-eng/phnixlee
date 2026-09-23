package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.IDeviceDetailsService;
import com.zhaocai.archives.main.domain.DeviceArchives;
import com.zhaocai.archives.main.domain.DeviceClass;
import com.zhaocai.archives.main.mapper.DeviceArchivesMapper;
import com.zhaocai.archives.main.service.IDeviceArchivesService;
import com.zhaocai.archives.main.service.IDeviceClassService;
import com.zhaocai.archives.utils.KeyUtils;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备档案主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class DeviceArchivesServiceImpl extends ServiceImpl<DeviceArchivesMapper, DeviceArchives> implements IDeviceArchivesService {
    @Autowired
    private DeviceArchivesMapper deviceArchivesMapper;

    @Resource
    private IDeviceDetailsService iDeviceDetailsService;

    @Resource
    private IDeviceClassService iDeviceClassService;

    /**
     * 查询设备档案主
     *
     * @param id 设备档案主主键
     * @return 设备档案主
     */
    @Override
    public DeviceArchives selectDeviceArchivesById(String id) {
        return deviceArchivesMapper.selectDeviceArchivesById(id);
    }

    /**
     * 查询设备档案主列表
     *
     * @param deviceArchives 设备档案主
     * @return 设备档案主
     */
    @Override
    public List<DeviceArchives> selectDeviceArchivesList(DeviceArchives deviceArchives) {
        if (deviceArchives == null) {
            deviceArchives = new DeviceArchives();
        }
        deviceArchives.setValid(0L);
        List<DeviceArchives> deviceArchives1 = deviceArchivesMapper.selectDeviceArchivesList(deviceArchives);
        DeviceClass aClass = new DeviceClass();
        if (deviceArchives.getDeviceClassId() != null) {
            aClass = iDeviceClassService.selectDeviceClassById(deviceArchives.getDeviceClassId());
        }
        if (aClass != null) {
            String name = aClass.getDeviceClassName();
            deviceArchives1.forEach(item -> {
                item.setDeviceClassName(name);
            });
        }
        return deviceArchives1;
    }

    /**
     * 新增设备档案主
     *
     * @param deviceArchives 设备档案主
     * @return 结果
     */
    @Override
    public synchronized int insertDeviceArchives(DeviceArchives deviceArchives) {
        if (deviceArchives == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(deviceArchives.getDeviceClassId())) {
            throw new RuntimeException("类型id不能为空");
        }
        DeviceArchives mtrFeature1 = new DeviceArchives();
        mtrFeature1.setDeviceCode(deviceArchives.getDeviceCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setDeviceClassId(deviceArchives.getDeviceClassId());
        List<DeviceArchives> mtrFeatures = baseMapper.selectDeviceArchivesList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            throw new RuntimeException("当前编号已存在");
        }
        if (deviceArchives.getId() == null) {
            deviceArchives.setId(KeyUtils.generateId() + "");
            deviceArchives.setCreateTime(DateUtils.getNowDate());
            deviceArchives.setCreateId(SecurityUtils.getUserId() + "");
            deviceArchives.setCreateBy(SecurityUtils.getUsername());
            deviceArchives.setValid(0L);
        }
        int i = deviceArchivesMapper.insertDeviceArchives(deviceArchives);
//        if (i > 0) {
//            iDeviceDetailsService.addTypeByMain(deviceArchives);
//        }
        return i;
    }

    /**
     * 修改设备档案主
     *
     * @param deviceArchives 设备档案主
     * @return 结果
     */
    @Override
    public int updateDeviceArchives(DeviceArchives deviceArchives) {
        if (deviceArchives == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(deviceArchives.getDeviceClassId())) {
            throw new RuntimeException("类型id不能为空");
        }
        DeviceArchives mtrFeature1 = new DeviceArchives();
        mtrFeature1.setDeviceCode(deviceArchives.getDeviceCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setDeviceClassId(deviceArchives.getDeviceClassId());
        List<DeviceArchives> mtrFeatures = baseMapper.selectDeviceArchivesList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            for (DeviceArchives mtrFeature2 : mtrFeatures) {
                if (!deviceArchives.getId().equals(mtrFeature2.getId())) {
                    throw new RuntimeException("当前编号已存在");
                }
            }
        }
        deviceArchives.setUpdateTime(DateUtils.getNowDate());
        deviceArchives.setUpdateBy(SecurityUtils.getUsername());
        int i = deviceArchivesMapper.updateDeviceArchives(deviceArchives);
        if (i > 0) {
            iDeviceDetailsService.updateByHostId(deviceArchives);
        }
        return i;
    }

    /**
     * 批量删除设备档案主
     *
     * @param ids 需要删除的设备档案主主键
     * @return 结果
     */
    @Override
    public boolean deleteDeviceArchivesByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<DeviceArchives> mtrFeatures = this.listByIds(Arrays.asList(ids));
        Map<Long, Long> oldIdMaps = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(item -> {
                item.setValid(System.currentTimeMillis() / 1000L);
                if (item.getSonId() != null) {
                    oldIdMaps.put(item.getSonId(), item.getSonId());
                }
            });
        }
        boolean b = this.updateBatchById(mtrFeatures);
        if (b) {
            iDeviceDetailsService.deleteByHostId(ids, oldIdMaps);
        }
        return b;
    }

    /**
     * 删除设备档案主信息
     *
     * @param id 设备档案主主键
     * @return 结果
     */
    @Override
    public int deleteDeviceArchivesById(String id) {
        return deviceArchivesMapper.deleteDeviceArchivesById(id);
    }

    @Override
    public long selectDeviceArchivesListCount(DeviceArchives deviceArchives) {
        return baseMapper.selectDeviceArchivesListCount(deviceArchives);
    }
}
