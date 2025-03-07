package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.DeviceDetails;
import com.zhaocai.archives.dossier.domain.DeviceEigenvalue;
import com.zhaocai.archives.dossier.domain.DeviceItem;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.mapper.DeviceEigenvalueMapper;
import com.zhaocai.archives.dossier.service.IDeviceEigenvalueService;
import com.zhaocai.archives.dossier.service.IDeviceItemService;
import com.zhaocai.archives.dossier.service.IDeviceTypeService;
import com.zhaocai.archives.main.domain.DeviceFeature;
import com.zhaocai.archives.main.domain.DeviceFeatureValue;
import com.zhaocai.archives.main.service.IDeviceFeatureService;
import com.zhaocai.archives.main.service.IDeviceFeatureValueService;
import com.zhaocai.archives.utils.KeyUtils;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备特征值Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class DeviceEigenvalueServiceImpl extends ServiceImpl<DeviceEigenvalueMapper, DeviceEigenvalue> implements IDeviceEigenvalueService {
    @Autowired
    private DeviceEigenvalueMapper deviceEigenvalueMapper;

    @Resource
    private IDeviceFeatureValueService iDeviceFeatureValueService;

    @Resource
    private IDeviceFeatureService iDeviceFeatureService;

    @Resource
    private IDeviceItemService iDeviceItemService;

    @Resource
    private IDeviceTypeService iDeviceTypeService;

    /**
     * 查询设备特征值
     *
     * @param id 设备特征值主键
     * @return 设备特征值
     */
    @Override
    public DeviceEigenvalue selectDeviceEigenvalueById(Long id) {
        DeviceEigenvalue eigenvalue = deviceEigenvalueMapper.selectDeviceEigenvalueById(id);
//        if ("N".equals(eigenvalue.getIsMain())) {
//            String thStr = eigenvalue.getOrganCode().substring(0, 4);
//            String xStr = iDeviceItemService.selectDeviceItemById(eigenvalue.getItemId()).getItemCode().replace(thStr,"");
//            String typeStr = iDeviceTypeService.selectDeviceTypeById(eigenvalue.getTypeId()).getDeviceCode().replace(thStr, "");
//            eigenvalue.setEigenvalueCode(typeStr+xStr+eigenvalue.getEigenvalueCode() + "-" + thStr);
//        }
        return eigenvalue;
    }


    /**
     * 查询设备特征值列表
     *
     * @param deviceEigenvalue 设备特征值
     * @return 设备特征值
     */
    @Override
    public List<DeviceEigenvalue> selectDeviceEigenvalueListNoChange(DeviceEigenvalue deviceEigenvalue) {
        return deviceEigenvalueMapper.selectDeviceEigenvalueList(deviceEigenvalue);
    }


    /**
     * 查询设备特征值列表
     *
     * @param deviceEigenvalue 设备特征值
     * @return 设备特征值
     */
    @Override
    public List<DeviceEigenvalue> selectDeviceEigenvalueList(DeviceEigenvalue deviceEigenvalue) {
        if (deviceEigenvalue == null) {
            deviceEigenvalue = new DeviceEigenvalue();
        }
        deviceEigenvalue.setDelFlag("0");
        List<DeviceEigenvalue> deviceEigenvalues = deviceEigenvalueMapper.selectDeviceEigenvalueList(deviceEigenvalue);
        if (deviceEigenvalues != null && !deviceEigenvalues.isEmpty()) {
            String thStr = deviceEigenvalues.get(0).getOrganCode().substring(0, 4);
            DeviceItem item = iDeviceItemService.selectDeviceItemById(deviceEigenvalues.get(0).getItemId());
            String xStr = item.getItemCode().replace("-" + thStr, "");
            String typeStr = iDeviceTypeService.selectDeviceTypeById(item.getTypeId()).getDeviceCode().replace("-" + thStr, "");
            for (DeviceEigenvalue eigenvalue : deviceEigenvalues) {
                if ("N".equals(eigenvalue.getIsMain())) {
                    eigenvalue.setEigenvalueCode(typeStr + xStr + eigenvalue.getEigenvalueCode() + "-" + thStr);
                }
            }
        }
        Map<Long, Long> idsMap = new HashMap<>();
        if (MaterialType.ALL.equals(deviceEigenvalue.getQueryType()) || MaterialType.UNTREATED.equals(deviceEigenvalue.getQueryType())) {
            DeviceEigenvalue eigenvalue = new DeviceEigenvalue();
            eigenvalue.setState(3L);
            eigenvalue.setDelFlag("0");
            eigenvalue.setIsMain("N");
            eigenvalue.setMainId("0");
            eigenvalue.setTypeId(deviceEigenvalue.getTypeId());
            eigenvalue.setOrganCode(deviceEigenvalue.getOrganCode());
            List<DeviceEigenvalue> list = baseMapper.selectDeviceEigenvalueList(eigenvalue);
            list.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (MaterialType.ALL.equals(deviceEigenvalue.getQueryType()) || MaterialType.PROCESSED.equals(deviceEigenvalue.getQueryType())) {
            List<DeviceEigenvalue> eigenvalues1 = baseMapper.getProcessed(deviceEigenvalue.getOrganCode());
            eigenvalues1.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (StringUtils.isEmpty(deviceEigenvalue.getQueryType())) {
            return deviceEigenvalues;
        } else if (deviceEigenvalues != null) {
            deviceEigenvalues.removeIf(item -> !idsMap.containsKey(item.getId()));
        }
        return deviceEigenvalues;
    }

    /**
     * 新增设备特征值
     *
     * @param deviceEigenvalue 设备特征值
     * @return 结果
     */
    @Override
    public synchronized int insertDeviceEigenvalue(DeviceEigenvalue deviceEigenvalue) {
        if (deviceEigenvalue != null) {
            if (deviceEigenvalue.getItemId() == null) {
                throw new RuntimeException("未获取到特征项id");
            }
            if (StringUtils.isEmpty(deviceEigenvalue.getOrganCode())) {
                throw new RuntimeException("未获取到机构代码");
            }
            DeviceEigenvalue materialItem1 = new DeviceEigenvalue();
            materialItem1.setItemId(deviceEigenvalue.getItemId());
            materialItem1.setDelFlag("0");
            materialItem1.setOrganCode(deviceEigenvalue.getOrganCode());
            materialItem1.setEigenvalueCode(deviceEigenvalue.getEigenvalueCode());
            materialItem1.setIsMain("N");
            List<DeviceEigenvalue> materialItems = baseMapper.selectDeviceEigenvalueList(materialItem1);
            if (materialItems != null && !materialItems.isEmpty()) {
                throw new RuntimeException("编码已存在");
            }
            if (deviceEigenvalue.getId() == null) {
                deviceEigenvalue.setId(KeyUtils.generateId());
                deviceEigenvalue.setState(0L);
                deviceEigenvalue.setCreateId(SecurityUtils.getUserId());
                deviceEigenvalue.setCreateBy(SecurityUtils.getUsername());
                deviceEigenvalue.setIsMain("N");
                deviceEigenvalue.setCreateTime(DateUtils.getNowDate());
            }
        } else {
            throw new RuntimeException("未获取到特征值信息");
        }
        return deviceEigenvalueMapper.insertDeviceEigenvalue(deviceEigenvalue);
    }

    /**
     * 修改设备特征值
     *
     * @param deviceEigenvalue 设备特征值
     * @return 结果
     */
    @Override
    public int updateDeviceEigenvalue(DeviceEigenvalue deviceEigenvalue) {
        if (deviceEigenvalue == null) {
            throw new RuntimeException("未获取到特征值信息");
        }
        if (deviceEigenvalue.getItemId() == null) {
            throw new RuntimeException("未获取到特征项id");
        }
        if (StringUtils.isEmpty(deviceEigenvalue.getOrganCode())) {
            throw new RuntimeException("未获取到机构代码");
        }
//        String qc = "-"+deviceEigenvalue.getOrganCode().substring(0, 4);
//        deviceEigenvalue.setEigenvalueCode(deviceEigenvalue.getEigenvalueCode().replace(qc, ""));
        DeviceEigenvalue materialItem1 = new DeviceEigenvalue();
        materialItem1.setTypeId(deviceEigenvalue.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(deviceEigenvalue.getOrganCode());
        materialItem1.setEigenvalueCode(deviceEigenvalue.getEigenvalueCode());
        materialItem1.setIsMain("N");
        List<DeviceEigenvalue> materialItems = baseMapper.selectDeviceEigenvalueList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            for (DeviceEigenvalue type : materialItems) {
                if (!type.getId().equals(deviceEigenvalue.getId())) {
                    throw new RuntimeException("编码已存在,请刷新后重试");
                }
            }
        }
        deviceEigenvalue.setUpdateId(SecurityUtils.getUserId());
        deviceEigenvalue.setUpdateBy(SecurityUtils.getUsername());
        deviceEigenvalue.setUpdateTime(DateUtils.getNowDate());
        return deviceEigenvalueMapper.updateDeviceEigenvalue(deviceEigenvalue);
    }

    /**
     * 批量删除设备特征值
     *
     * @param ids 需要删除的设备特征值主键
     * @return 结果
     */
    @Override
    public boolean deleteDeviceEigenvalueByIds(Long[] ids) {
        if (ids == null) {
            throw new RuntimeException("参数为空");
        }
        List<DeviceEigenvalue> eigenvalues = deviceEigenvalueMapper.selectBatchIds(Arrays.asList(ids));
        for (DeviceEigenvalue eigenvalue : eigenvalues) {
            if (eigenvalue.getIsMain().equals("Y")) {
                throw new RuntimeException("主库同步数据不允许删除");
            }
            if (eigenvalue.getState() != 0L) {
                throw new RuntimeException("流程中数据不允许删除");
            }
            eigenvalue.setDelFlag("1");
        }
        return this.removeBatchByIds(Arrays.asList(ids));
    }

    /**
     * 删除设备特征值信息
     *
     * @param id 设备特征值主键
     * @return 结果
     */
    @Override
    public int deleteDeviceEigenvalueById(Long id) {
        return deviceEigenvalueMapper.deleteDeviceEigenvalueById(id);
    }

    @Override
    public List<DeviceEigenvalue> initData(DeviceEigenvalue deviceEigenvalue) {
        List<DeviceEigenvalue> eigenvalues = deviceEigenvalueMapper.selectDeviceEigenvalueList(deviceEigenvalue);
        List<DeviceFeatureValue> mtrFeatureValues = iDeviceFeatureValueService.selectDeviceFeatureValueList(null);
        DeviceItem item1 = new DeviceItem();
        item1.setOrganCode(deviceEigenvalue.getOrganCode());
        item1.setIsMain("Y");
        List<DeviceItem> materialItems = iDeviceItemService.selectDeviceItemList(item1);
        if (materialItems == null || materialItems.isEmpty()) {
            throw new RuntimeException("请先配置特征项");
        }
        Map<String, DeviceItem> params = new HashMap<>();
        materialItems.forEach(item -> {
            if ("Y".equals(item.getIsMain())) {
                params.put(item.getHostId(), item);
            }
        });
        Map<String, DeviceFeatureValue> mtrMap = new HashMap<>();
        if (mtrFeatureValues != null && !mtrFeatureValues.isEmpty()) {
            mtrFeatureValues.forEach(mtrFeatureValue -> {
                mtrMap.put(mtrFeatureValue.getId(), mtrFeatureValue);
            });
        }
        if (eigenvalues.isEmpty()) {
            //初始化-最开始无数据情况
            if (mtrFeatureValues != null && !mtrFeatureValues.isEmpty()) {
                mtrFeatureValues.forEach(mtrFeatureValue -> {
                    DeviceEigenvalue bean = new DeviceEigenvalue();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeatureValue.getDeviceFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getDeviceFeatureId()).getTypeId());
                    bean.setItemId(params.get(mtrFeatureValue.getDeviceFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getDeviceFeatureId()).getId());
                    bean.setEigenvalueName(mtrFeatureValue.getFeatureValueName());
                    bean.setEigenvalueCode(mtrFeatureValue.getFeatureValueCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(deviceEigenvalue.getOrganCode());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
                    bean.setHostId(mtrFeatureValue.getId());
                    eigenvalues.add(bean);
                });
                this.saveBatch(eigenvalues);
            }
        } else {
            //有数据的情况，对比主库数据，新增主库存在副库不存在的数据，且非本副库新增至主库数据。（关联数据是否还需要将主库数据同步到副库）
            Map<String, Long> map = new HashMap<>();
            eigenvalues.forEach(item -> {
                if ("Y".equals(item.getIsMain())) {
                    map.put(item.getHostId(), item.getId());
                }
            });
            List<DeviceEigenvalue> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!map.containsKey(key)) {
                    DeviceFeatureValue mtrFeatureValue = mtrMap.get(key);
                    DeviceEigenvalue bean = new DeviceEigenvalue();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeatureValue.getDeviceFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getDeviceFeatureId()).getTypeId());
                    bean.setItemId(params.get(mtrFeatureValue.getDeviceFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getDeviceFeatureId()).getId());
                    bean.setEigenvalueName(mtrFeatureValue.getFeatureValueName());
                    bean.setEigenvalueCode(mtrFeatureValue.getFeatureValueCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(deviceEigenvalue.getOrganCode());
                    bean.setHostId(mtrFeatureValue.getId());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
                    eigenvalues.add(bean);
                    addList.add(bean);
                }
            }
            if (!addList.isEmpty()) {
                this.saveBatch(addList);
            }
        }
        return eigenvalues;
    }

    @Override
    public void addTypeByMain(DeviceFeatureValue mtrFeatureValue) {
        DeviceItem materialItem = new DeviceItem();
        materialItem.setHostId(mtrFeatureValue.getDeviceFeatureId());
        List<DeviceItem> materialItems = iDeviceItemService.selectDeviceItemList(materialItem);
        //已存在数据排除
        DeviceEigenvalue type1 = new DeviceEigenvalue();
        type1.setHostId(mtrFeatureValue.getId());
        type1.setDelFlag("0");
        List<DeviceEigenvalue> materialTypes1 = baseMapper.selectDeviceEigenvalueList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getItemId());
            });
        }
        List<DeviceEigenvalue> eigenvalues = new ArrayList<>();
        if (materialItems != null && !materialItems.isEmpty()) {
            for (DeviceItem item : materialItems) {
                if (map.containsKey(item.getOrganCode()) && (item.getId() + "").equals(map.get(item.getOrganCode()) + "")) {
                    continue;
                }
                DeviceEigenvalue bean = new DeviceEigenvalue();
                bean.setId(KeyUtils.generateId());
                bean.setTypeId(item.getTypeId());
                bean.setItemId(item.getId());
                bean.setEigenvalueName(mtrFeatureValue.getFeatureValueName());
                bean.setEigenvalueCode(mtrFeatureValue.getFeatureValueCode());
                bean.setCreateId(SecurityUtils.getUserId());
                bean.setIsMain("Y");
                bean.setState(3L);
                bean.setOrganCode(item.getOrganCode());
                bean.setCreateBy(SecurityUtils.getUsername());
                bean.setCreateTime(DateUtils.getNowDate());
                bean.setHostId(mtrFeatureValue.getId());
                eigenvalues.add(bean);
            }
        }
        if (!eigenvalues.isEmpty()) {
            this.saveBatch(eigenvalues);
        }
    }


    @Override
    public void updateByHostId(DeviceFeatureValue mtrFeatureValue) {
        DeviceEigenvalue eigenvalue = new DeviceEigenvalue();
        eigenvalue.setHostId(mtrFeatureValue.getId());
        eigenvalue.setDelFlag("0");
        List<DeviceEigenvalue> eigenvalues = baseMapper.selectDeviceEigenvalueList(eigenvalue);
        if (eigenvalues != null && !eigenvalues.isEmpty()) {
            eigenvalues.forEach(bean -> {
                bean.setEigenvalueName(mtrFeatureValue.getFeatureValueName());
                bean.setEigenvalueCode(mtrFeatureValue.getFeatureValueCode());
            });
        }
        this.updateBatchById(eigenvalues);
    }

    @Override
    public void deleteByHostId(String[] histIds, Map<Long, Long> idsMap) {
        if (histIds != null) {
            QueryWrapper<DeviceEigenvalue> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<DeviceEigenvalue> eigenvalues = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<DeviceEigenvalue> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main","Y");
            eigenvalues.addAll(this.list(qw));
            List<DeviceEigenvalue> newDeviceDetails = new ArrayList<>();
            List<DeviceEigenvalue> upDeviceDetails = new ArrayList<>();
            if (idsMap != null && !idsMap.isEmpty()) {
                eigenvalues.forEach(item -> {
                    if (idsMap.containsKey(item.getId())) {
                        item.setHostId("0");
                        item.setIsMain("N");
                        item.setMainId("0");
                        upDeviceDetails.add(item);
                    } else {
                        newDeviceDetails.add(item);
                    }
                });
            }
            List<Long> collect = newDeviceDetails.stream().map(DeviceEigenvalue::getId).collect(Collectors.toList());
            this.removeBatchByIds(collect);
            this.updateBatchById(upDeviceDetails);
        }
    }

    /**
     * 新增至主库
     *
     * @param materialEigenvalue
     * @return
     */
    @Override
    public synchronized int addToMain(DeviceEigenvalue materialEigenvalue) {
        if (materialEigenvalue.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialEigenvalue.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        DeviceEigenvalue materialType1 = this.selectDeviceEigenvalueById(materialEigenvalue.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        DeviceFeature mtrClass = iDeviceFeatureService.selectDeviceFeatureById(materialEigenvalue.getHostId());
        //查询编码是否已在主库存在
        DeviceFeatureValue mtrClass1 = new DeviceFeatureValue();
        mtrClass1.setDeviceFeatureId(materialEigenvalue.getHostId());
        mtrClass1.setValid(0L);
        mtrClass1.setFeatureValueCode(materialType1.getEigenvalueCode());
        if (!StringUtils.isEmpty(materialEigenvalue.getEigenvalueCode())) {
            mtrClass1.setFeatureValueCode(materialEigenvalue.getEigenvalueCode());
        }
        List<DeviceFeatureValue> mtrClasses = iDeviceFeatureValueService.selectDeviceFeatureValueList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            return -1;
        } else {
            if (!StringUtils.isEmpty(materialEigenvalue.getEigenvalueCode())) {
                DeviceFeatureValue aClass = new DeviceFeatureValue();
                aClass.setId(key);
                aClass.setDeviceFeatureId(mtrClass.getId());
                aClass.setFeatureValueCode(materialEigenvalue.getEigenvalueCode());
                aClass.setFeatureValueName(materialType1.getEigenvalueName());
                aClass.setSonId(materialType1.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateTime(DateUtils.getNowDate());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                iDeviceFeatureValueService.insertDeviceFeatureValue(aClass);
                materialType1.setMainId(aClass.getId());
                materialType1.setIsMain("Y");
                return baseMapper.updateDeviceEigenvalue(materialType1);
            } else {
                //不存在时 新增至主库
                materialType1.setIsMain("Y");
                materialType1.setHostId(key);
                int i = baseMapper.updateDeviceEigenvalue(materialType1);
                if (i > 0) {
                    DeviceFeatureValue aClass = new DeviceFeatureValue();
                    aClass.setId(key);
                    aClass.setDeviceFeatureId(mtrClass.getId());
                    aClass.setFeatureValueCode(materialType1.getEigenvalueCode());
                    aClass.setFeatureValueName(materialType1.getEigenvalueName());
                    aClass.setSonId(materialType1.getId());
                    aClass.setCreateBy(SecurityUtils.getUsername());
                    aClass.setCreateTime(DateUtils.getNowDate());
                    aClass.setCreateId(SecurityUtils.getUserId() + "");
                    iDeviceFeatureValueService.insertDeviceFeatureValue(aClass);
                }
                return i;
            }
        }
    }


    /**
     * 关联至主库
     *
     * @param materialEigenvalue
     * @return
     */
    @Override
    public synchronized int associationToMain(DeviceEigenvalue materialEigenvalue) {
        if (materialEigenvalue.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialEigenvalue.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        DeviceEigenvalue materialType1 = this.selectDeviceEigenvalueById(materialEigenvalue.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialEigenvalue.getHostId());
        return baseMapper.updateDeviceEigenvalue(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialEigenvalue
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(DeviceEigenvalue materialEigenvalue) {
        if (materialEigenvalue.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        DeviceEigenvalue materialType1 = this.selectDeviceEigenvalueById(materialEigenvalue.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return baseMapper.updateDeviceEigenvalue(materialType1);
    }

    @Override
    public long selectDeviceEigenvalueListCount(DeviceEigenvalue deviceEigenvalue) {
        return baseMapper.selectDeviceEigenvalueListCount(deviceEigenvalue);
    }

    @Override
    public List<DeviceEigenvalue> getProcessed(String organCode) {
        return baseMapper.getProcessed(organCode);
    }


}
