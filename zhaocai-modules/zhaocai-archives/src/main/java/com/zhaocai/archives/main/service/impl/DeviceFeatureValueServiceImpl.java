package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.IDeviceEigenvalueService;
import com.zhaocai.archives.main.domain.DeviceFeatureValue;
import com.zhaocai.archives.main.domain.MtrFeatureValue;
import com.zhaocai.archives.main.mapper.DeviceFeatureValueMapper;
import com.zhaocai.archives.main.service.IDeviceFeatureValueService;
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
 * 设备特征值主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class DeviceFeatureValueServiceImpl extends ServiceImpl<DeviceFeatureValueMapper, DeviceFeatureValue> implements IDeviceFeatureValueService {
    @Autowired
    private DeviceFeatureValueMapper deviceFeatureValueMapper;

    @Resource
    private IDeviceEigenvalueService iDeviceEigenvalueService;

    /**
     * 查询设备特征值主
     *
     * @param id 设备特征值主主键
     * @return 设备特征值主
     */
    @Override
    public DeviceFeatureValue selectDeviceFeatureValueById(String id) {
        return deviceFeatureValueMapper.selectDeviceFeatureValueById(id);
    }

    /**
     * 查询设备特征值主列表
     *
     * @param deviceFeatureValue 设备特征值主
     * @return 设备特征值主
     */
    @Override
    public List<DeviceFeatureValue> selectDeviceFeatureValueList(DeviceFeatureValue deviceFeatureValue) {
        if (deviceFeatureValue == null) {
            deviceFeatureValue = new DeviceFeatureValue();
        }
        deviceFeatureValue.setValid(0L);
        return deviceFeatureValueMapper.selectDeviceFeatureValueList(deviceFeatureValue);
    }

    /**
     * 新增设备特征值主
     *
     * @param deviceFeatureValue 设备特征值主
     * @return 结果
     */
    @Override
    public synchronized int insertDeviceFeatureValue(DeviceFeatureValue deviceFeatureValue) {
        if (deviceFeatureValue == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(deviceFeatureValue.getDeviceFeatureId())) {
            throw new RuntimeException("特征项id不能为空");
        }
        DeviceFeatureValue mtrFeatureValue1 = new DeviceFeatureValue();
        mtrFeatureValue1.setFeatureValueCode(deviceFeatureValue.getFeatureValueCode());
        mtrFeatureValue1.setValid(0L);
        mtrFeatureValue1.setDeviceFeatureId(deviceFeatureValue.getDeviceFeatureId());
        List<DeviceFeatureValue> mtrFeatureValues = baseMapper.selectDeviceFeatureValueList(mtrFeatureValue1);
        if (mtrFeatureValues != null && !mtrFeatureValues.isEmpty()) {
            throw new RuntimeException("当前特征值编号已存在");
        }
        if (deviceFeatureValue.getId() == null) {
            deviceFeatureValue.setId(KeyUtils.generateId() + "");
            deviceFeatureValue.setCreateTime(DateUtils.getNowDate());
            deviceFeatureValue.setCreateId(SecurityUtils.getUserId() + "");
            deviceFeatureValue.setCreateBy(SecurityUtils.getUsername());
            deviceFeatureValue.setValid(0L);
        }
        int i = deviceFeatureValueMapper.insertDeviceFeatureValue(deviceFeatureValue);
        if (i > 0) {
            iDeviceEigenvalueService.addTypeByMain(deviceFeatureValue);
        }
        return i;
    }

    /**
     * 修改设备特征值主
     *
     * @param deviceFeatureValue 设备特征值主
     * @return 结果
     */
    @Override
    public int updateDeviceFeatureValue(DeviceFeatureValue deviceFeatureValue) {
        if (deviceFeatureValue == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(deviceFeatureValue.getDeviceFeatureId())) {
            throw new RuntimeException("特征项id不能为空");
        }
        DeviceFeatureValue mtrFeature1 = new DeviceFeatureValue();
        mtrFeature1.setFeatureValueCode(deviceFeatureValue.getFeatureValueCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setDeviceFeatureId(deviceFeatureValue.getDeviceFeatureId());
        List<DeviceFeatureValue> mtrFeatures = baseMapper.selectDeviceFeatureValueList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            for (DeviceFeatureValue mtrFeature2 : mtrFeatures) {
                if (!deviceFeatureValue.getId().equals(mtrFeature2.getId())) {
                    throw new RuntimeException("当前编号已存在");
                }
            }
        }
        deviceFeatureValue.setUpdateTime(DateUtils.getNowDate());
        deviceFeatureValue.setUpdateBy(SecurityUtils.getUsername());
        deviceFeatureValue.setIsTb("2");
        int i = deviceFeatureValueMapper.updateDeviceFeatureValue(deviceFeatureValue);
        if (i > 0) {
            iDeviceEigenvalueService.updateByHostId(deviceFeatureValue);
        }
        return i;
    }

    /**
     * 批量删除设备特征值主
     *
     * @param ids 需要删除的设备特征值主主键
     * @return 结果
     */
    @Override
    public boolean deleteDeviceFeatureValueByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<DeviceFeatureValue> mtrFeatures = this.listByIds(Arrays.asList(ids));
        Map<Long, Long> map = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(item -> {
                item.setValid(System.currentTimeMillis() / 1000L);
                item.setIsTb("3");
                if (item.getSonId() != null) {
                    map.put(item.getSonId(), item.getSonId());
                }
            });
        }
        boolean b = this.updateBatchById(mtrFeatures);
        if (b) {
            iDeviceEigenvalueService.deleteByHostId(ids,map);
        }
        return b;
    }

    /**
     * 删除设备特征值主信息
     *
     * @param id 设备特征值主主键
     * @return 结果
     */
    @Override
    public int deleteDeviceFeatureValueById(String id) {
        return deviceFeatureValueMapper.deleteDeviceFeatureValueById(id);
    }

    @Override
    public long selectDeviceFeatureValueListCount(DeviceFeatureValue deviceFeatureValue) {
        return baseMapper.selectDeviceFeatureValueListCount(deviceFeatureValue);
    }
}
