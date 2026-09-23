package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.enums.AgreementStateEnum;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.*;
import com.zhaocai.archives.dossier.mapper.DeviceTypeMapper;
import com.zhaocai.archives.dossier.service.IDeviceDetailsService;
import com.zhaocai.archives.dossier.service.IDeviceEigenvalueService;
import com.zhaocai.archives.dossier.service.IDeviceItemService;
import com.zhaocai.archives.dossier.service.IDeviceTypeService;
import com.zhaocai.archives.dossier.tree.DeviceTypeTree;
import com.zhaocai.archives.main.domain.DeviceClass;
import com.zhaocai.archives.main.service.IDeviceClassService;
import com.zhaocai.archives.utils.KeyUtils;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.PageUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备分类Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class DeviceTypeServiceImpl extends ServiceImpl<DeviceTypeMapper, DeviceType> implements IDeviceTypeService {
    @Autowired
    private DeviceTypeMapper deviceTypeMapper;

    @Resource
    private IDeviceClassService iDeviceClassService;

    @Resource
    private IDeviceItemService deviceItemService;

    @Resource
    private IDeviceEigenvalueService deviceEigenvalueService;

    @Resource
    private IDeviceDetailsService deviceDetailsService;

    /**
     * 查询设备分类
     *
     * @param id 设备分类主键
     * @return 设备分类
     */
    @Override
    public DeviceType selectDeviceTypeById(Long id) {
        DeviceType deviceType = deviceTypeMapper.selectDeviceTypeById(id);
        if (deviceType == null) {
            return null;
        }
        if (deviceType.getUpId() != null && deviceType.getUpId() != 0) {
            DeviceType deviceType1 = deviceTypeMapper.selectDeviceTypeById(deviceType.getUpId());
            deviceType.setBelongingLevel(deviceType1.getDeviceName());
        } else {
            deviceType.setBelongingLevel("顶级");
        }
        if ("N".equals(deviceType.getIsMain())) {
            deviceType.setDeviceCode(deviceType.getDeviceCode() + "-" + deviceType.getOrganCode().substring(0, 4));
        }
        return deviceType;
    }


    /**
     * 查询设备分类
     *
     * @param id 设备分类主键
     * @return 设备分类
     */
    @Override
    public DeviceType selectDeviceTypeByIdNoChange(Long id) {
        return deviceTypeMapper.selectDeviceTypeById(id);
    }

    /**
     * 查询设备分类列表
     *
     * @param deviceType 设备分类
     * @return 设备分类
     */
    @Override
    public List<DeviceType> selectDeviceTypeList(DeviceType deviceType) {
        return deviceTypeMapper.selectDeviceTypeList(deviceType);
    }

    /**
     * 新增设备分类
     *
     * @param deviceType 设备分类
     * @return 结果
     */
    @Override
    public synchronized int insertDeviceType(DeviceType deviceType) {
        if (deviceType == null) {
            throw new RuntimeException("参数不能为空");
        }
        if (StringUtils.isEmpty(deviceType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        QueryWrapper<DeviceType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("organ_code", deviceType.getOrganCode());
        queryWrapper.eq("device_code", deviceType.getDeviceCode());
        queryWrapper.eq("del_flag", "0");
        List<DeviceType> list = this.list(queryWrapper);
        if (list != null && !list.isEmpty()) {
            throw new RuntimeException("分类编码已存在,请刷新后重试");
        }
        deviceType.setState(0L);
        deviceType.setCreateTime(DateUtils.getNowDate());
        return deviceTypeMapper.insertDeviceType(deviceType);
    }

    /**
     * 修改设备分类
     *
     * @param deviceType 设备分类
     * @return 结果
     */
    @Override
    public int updateDeviceType(DeviceType deviceType) {
        if (deviceType == null) {
            throw new RuntimeException("参数不能为空");
        }
        if (StringUtils.isEmpty(deviceType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (StringUtils.isEmpty(deviceType.getDeviceCode())) {
            throw new RuntimeException("编码不能为空");
        }
        String qc = "-" + deviceType.getOrganCode().substring(0, 4);
        deviceType.setDeviceCode(deviceType.getDeviceCode().replace(qc, ""));
        QueryWrapper<DeviceType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("organ_code", deviceType.getOrganCode());
        queryWrapper.eq("device_code", deviceType.getDeviceCode());
        queryWrapper.eq("del_flag", "0");
        queryWrapper.eq("is_main", "N");
        List<DeviceType> list = this.list(queryWrapper);
        if (list != null && !list.isEmpty()) {
            for (DeviceType type : list) {
                if (!type.getId().equals(deviceType.getId())) {
                    throw new RuntimeException("材料分类编码已存在,请刷新后重试");
                }
            }
        }
        deviceType.setUpdateTime(DateUtils.getNowDate());
        return deviceTypeMapper.updateDeviceType(deviceType);
    }

    /**
     * 修改设备分类
     *
     * @param deviceType 设备分类
     * @return 结果
     */
    @Override
    public int updateDeviceTypeNoChange(DeviceType deviceType) {
        return deviceTypeMapper.updateDeviceType(deviceType);
    }

    /**
     * 批量删除设备分类
     *
     * @param ids 需要删除的设备分类主键
     * @return 结果
     */
    @Override
    public boolean deleteDeviceTypeByIds(Long[] ids) {
        if (ids == null) {
            throw new RuntimeException("参数为空");
        }
        List<DeviceType> materialTypes = deviceTypeMapper.selectBatchIds(Arrays.asList(ids));
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (DeviceType type : materialTypes) {
                if ("Y".equals(type.getIsMain())) {
                    throw new RuntimeException("主库同步数据不允许删除");
                }
                if (type.getState() != null && type.getState() != 0L) {
                    throw new RuntimeException("流程中数据不允许删除");
                }
                int i = baseMapper.getMaterialJoinNoMy(type.getId());
                if (i > 0) {
                    throw new RuntimeException("当前分类下存在数据，无法进行删除");
                }
            }
        }
        return this.removeBatchByIds(Arrays.asList(ids));
    }

    /**
     * 删除设备分类信息
     *
     * @param id 设备分类主键
     * @return 结果
     */
    @Override
    public int deleteDeviceTypeById(Long id) {
        return deviceTypeMapper.deleteDeviceTypeById(id);
    }

    @Override
    public List<DeviceType> initData(DeviceType deviceType) {
        List<DeviceType> deviceTypes = deviceTypeMapper.selectDeviceTypeList(deviceType);
        List<DeviceClass> mtrClasses = iDeviceClassService.selectDeviceClassList(null);
        Map<String, Long> map = new HashMap<>();
        Map<String, DeviceClass> mtrMap = new HashMap<>();
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            mtrClasses.forEach(mtrClass -> {
                map.put(mtrClass.getId(), KeyUtils.generateId());
                mtrMap.put(mtrClass.getId(), mtrClass);
            });
        }
        if (deviceTypes == null || deviceTypes.isEmpty()) {
            //初始化-最开始无数据情况
            if (mtrClasses != null && !mtrClasses.isEmpty()) {
                mtrClasses.forEach(mtrClass -> {
                    DeviceType type = new DeviceType();
                    type.setId(map.get(mtrClass.getId()));
                    type.setUpId(map.get(mtrClass.getParentId()));
                    type.setDeviceName(mtrClass.getDeviceClassName());
                    type.setDeviceType(mtrClass.getDeviceClassType());
                    type.setDeviceCode(mtrClass.getDeviceClassCode());
                    type.setUnit(mtrClass.getMeasureUnit());
                    type.setDeviceLevel(mtrClass.getClassLevel());
                    type.setDeviceLevelCd(mtrClass.getClassLevelCd());
                    type.setSubjectMatterCode(mtrClass.getSubjectMatterCode());
                    type.setSubjectMatterName(mtrClass.getSubjectMatterName());
                    type.setState(3L);
                    type.setIsTransaction(mtrClass.getSubjectMatter() + "");
                    type.setOrganCode(deviceType.getOrganCode());
                    type.setHostId(mtrClass.getId());
                    type.setIsMain("Y");
                    type.setCreateId(SecurityUtils.getUserId());
                    type.setCreateBy(SecurityUtils.getUsername());
                    type.setCreateTime(DateUtils.getNowDate());
                    type.setDelFlag(mtrClass.getValid() + "");
                    deviceTypes.add(type);
                });
                this.saveBatch(deviceTypes);
            }
        } else {
            //有数据的情况，对比主库数据，新增主库存在副库不存在的数据，且非本副库新增至主库数据。（关联数据是否还需要将主库数据同步到副库）
            Map<String, Long> params = new HashMap<>();
            deviceTypes.forEach(item -> {
                if ("Y".equals(item.getIsMain())) {
                    params.put(item.getHostId(), item.getId());
                }
            });
            List<DeviceType> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!params.containsKey(key)) {
                    DeviceClass mtrClass = mtrMap.get(key);
                    DeviceType type = new DeviceType();
                    type.setId(map.get(mtrClass.getId()));
                    if (params.containsKey(mtrClass.getParentId())) {
                        type.setUpId(params.get(mtrClass.getParentId()));
                    } else {
                        type.setUpId(map.get(mtrClass.getParentId()));
                    }
                    type.setDeviceName(mtrClass.getDeviceClassName());
                    type.setDeviceType(mtrClass.getDeviceClassType());
                    type.setDeviceCode(mtrClass.getDeviceClassCode());
                    type.setUnit(mtrClass.getMeasureUnit());
                    type.setDeviceLevel(mtrClass.getClassLevel());
                    type.setDeviceLevelCd(mtrClass.getClassLevelCd());
                    type.setSubjectMatterCode(mtrClass.getSubjectMatterCode());
                    type.setSubjectMatterName(mtrClass.getSubjectMatterName());
                    type.setState(3L);
                    type.setIsTransaction(mtrClass.getSubjectMatter() + "");
                    type.setOrganCode(deviceType.getOrganCode());
                    type.setHostId(mtrClass.getId());
                    type.setIsMain("Y");
                    type.setCreateId(SecurityUtils.getUserId());
                    type.setCreateBy(SecurityUtils.getUsername());
                    type.setCreateTime(DateUtils.getNowDate());
                    type.setDelFlag(mtrClass.getValid() + "");
                    deviceTypes.add(type);
                    addList.add(type);
                }
            }
            if (!addList.isEmpty()) {
                this.saveBatch(addList);
            }
        }
        return deviceTypes;
    }

    @Override
    public List<DeviceTypeTree> getDeviceTypeTree(DeviceType deviceType) {
        DeviceType typex = new DeviceType();
        typex.setOrganCode(deviceType.getOrganCode());
        typex.setDelFlag("0");
        PageUtils.clearPage();
        List<DeviceType> select = baseMapper.selectDeviceTypeList(typex);
        Map<Long, Long> typeMap = new HashMap<>();
        select.forEach(item -> {
            typeMap.put(item.getId(), item.getUpId());
        });
        Map<Long, DeviceTypeTree> map = new HashMap<>();
        Map<Long, Long> idsMap = new HashMap<>();
        if (MaterialType.ALL.equals(deviceType.getQueryType()) || MaterialType.UNTREATED.equals(deviceType.getQueryType())) {
            //查询未处理的类型数据
            DeviceType type = new DeviceType();
            type.setState(3L);
            type.setDelFlag("0");
            type.setIsMain("N");
            type.setMainId("0");
            type.setOrganCode(deviceType.getOrganCode());
            List<DeviceType> deviceTypes = this.selectDeviceTypeList(type);
            deviceTypes.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
            //查询未处理的值数据
            DeviceEigenvalue eigenvalue = new DeviceEigenvalue();
            eigenvalue.setState(3L);
            eigenvalue.setDelFlag("0");
            eigenvalue.setIsMain("N");
            eigenvalue.setMainId("0");
            eigenvalue.setOrganCode(deviceType.getOrganCode());
            List<DeviceEigenvalue> list = deviceEigenvalueService.selectDeviceEigenvalueList(eigenvalue);
            list.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
            //查询未处理的项数据
            DeviceItem deviceItem = new DeviceItem();
            deviceItem.setState(3L);
            deviceItem.setDelFlag("0");
            deviceItem.setIsMain("N");
            deviceItem.setMainId("0");
            deviceItem.setOrganCode(deviceType.getOrganCode());
            List<DeviceItem> deviceItems = deviceItemService.selectDeviceItemListNoChange(deviceItem);
            deviceItems.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
            //查询未处理的详情数据
            DeviceDetails deviceDetails = new DeviceDetails();
            deviceDetails.setState(3L);
            deviceDetails.setDelFlag("0");
            deviceDetails.setIsMain("N");
            deviceDetails.setMainId("0");
            deviceDetails.setOrganCode(deviceType.getOrganCode());
            List<DeviceDetails> deviceDetails1 = deviceDetailsService.selectDeviceDetailsListNoChange(deviceDetails);
            deviceDetails1.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
        }

        if (MaterialType.ALL.equals(deviceType.getQueryType()) || MaterialType.PROCESSED.equals(deviceType.getQueryType())) {
            List<DeviceItem> deviceItems = deviceItemService.getProcessed(deviceType.getOrganCode());
            deviceItems.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });

            List<DeviceEigenvalue> eigenvalues = deviceEigenvalueService.getProcessed(deviceType.getOrganCode());
            eigenvalues.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });

            List<DeviceDetails> details = deviceDetailsService.getProcessed(deviceType.getOrganCode());
            details.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });

            List<DeviceType> deviceTypes = baseMapper.getProcessed(deviceType.getOrganCode());
            deviceTypes.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
        }

        select.forEach(item -> {
            if (StringUtils.isEmpty(deviceType.getQueryType()) || idsMap.containsKey(item.getId())) {
                DeviceTypeTree materialTypeTree = new DeviceTypeTree();
                materialTypeTree.setId(item.getId() + "");
                materialTypeTree.setLabel(item.getDeviceName());
                materialTypeTree.setType(item.getDeviceType());
                materialTypeTree.setCode(item.getDeviceCode());
                if ("N".equals(item.getIsMain())) {
                    materialTypeTree.setCode(item.getDeviceCode() + "-" + item.getOrganCode().substring(0, 4));
                }
                materialTypeTree.setChildren(new ArrayList<>());
                materialTypeTree.setState(item.getState());
                materialTypeTree.setMainId(item.getMainId());
                materialTypeTree.setIsMain(item.getIsMain());
                materialTypeTree.setBusinessId(item.getWfBatch());
                materialTypeTree.setProcessId(item.getWfProcessId());
                map.put(item.getId(), materialTypeTree);
            }
        });

        // 构建树形结构
        List<DeviceTypeTree> list = new ArrayList<>();
        for (DeviceType type : select) {
            DeviceTypeTree treeVo = map.get(type.getId());
            if (treeVo != null) {
                if (type.getUpId() == null) {
                    // 根节点，直接添加
                    list.add(treeVo);
                } else {
                    // 非根节点，找到父节点并添加到其子节点列表中
                    DeviceTypeTree materialTypeTree = map.get(type.getUpId());
                    if (materialTypeTree != null) {
                        materialTypeTree.getChildren().add(treeVo);
                    }
                }
            }
        }
        return list;
    }

    private void xhtq(DeviceItem item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
        boolean flag = true;
        Long typeId = item.getTypeId();
        while (flag) {
            flag = false;
            idsMap.put(typeId, typeId);
            if (typeMap.containsKey(typeId)) {
                typeId = typeMap.get(typeId);
                flag = true;
            }
        }
    }

    private void xhtq(DeviceType item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
        boolean flag = true;
        Long typeId = item.getId();
        while (flag) {
            flag = false;
            idsMap.put(typeId, typeId);
            if (typeMap.containsKey(typeId)) {
                typeId = typeMap.get(typeId);
                flag = true;
            }
        }
    }

    private void xhtq(DeviceEigenvalue item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
        boolean flag = true;
        Long typeId = item.getTypeId();
        while (flag) {
            flag = false;
            idsMap.put(typeId, typeId);
            if (typeMap.containsKey(typeId)) {
                typeId = typeMap.get(typeId);
                flag = true;
            }
        }
    }

    private void xhtq(DeviceDetails item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
        boolean flag = true;
        Long typeId = item.getTypeId();
        while (flag) {
            flag = false;
            idsMap.put(typeId, typeId);
            if (typeMap.containsKey(typeId)) {
                typeId = typeMap.get(typeId);
                flag = true;
            }
        }
    }


    @Override
    public DeviceType initDeviceType(DeviceType deviceType) {
        if (StringUtils.isEmpty(deviceType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (deviceType.getId() == null) {
            throw new RuntimeException("id不能为空");
        }
        DeviceType type = this.getById(deviceType.getId());
        if (type == null) {
            throw new RuntimeException("类型不存在");
        }
        String materialCode = type.getDeviceCode();
        Integer maxCode = baseMapper.getMaxCode(materialCode, type.getId(), type.getOrganCode());
        if (maxCode != null) {
            maxCode += 1;
            //根据规则，长度大于8的流水号有3位
            if (materialCode.length() >= 8) {
                if (maxCode < 100 && maxCode >= 10) {
                    materialCode = materialCode + "0" + maxCode;
                } else if (maxCode < 10) {
                    materialCode = materialCode + "00" + maxCode;
                } else if (maxCode >= 1000) {
                    //超出规则
                    Integer reuseTowCode = baseMapper.getReuseTowCode(materialCode, type.getId(), type.getOrganCode(), 999);
                    if (reuseTowCode == null) {
                        throw new RuntimeException("编码已到最大编码限制，无法进行新增");
                    }
                    maxCode = reuseTowCode;
                    if (maxCode < 100 && maxCode >= 10) {
                        materialCode = materialCode + "0" + maxCode;
                    } else if (maxCode < 10) {
                        materialCode = materialCode + "00" + maxCode;
                    } else {
                        materialCode = materialCode + maxCode;
                    }
                } else {
                    materialCode = materialCode + maxCode;
                }
            } else {
                if (maxCode < 10) {
                    materialCode = materialCode + "0" + maxCode;
                } else if (maxCode >= 100) {
                    //超出规则
                    Integer reuseTowCode = baseMapper.getReuseTowCode(materialCode, type.getId(), type.getOrganCode(), 99);
                    if (reuseTowCode == null) {
                        throw new RuntimeException("编码已到最大编码限制，无法进行新增");
                    }
                    maxCode = reuseTowCode;
                    if (maxCode < 10) {
                        materialCode = materialCode + "0" + maxCode;
                    } else {
                        materialCode = materialCode + maxCode;
                    }
                } else {
                    materialCode = materialCode + maxCode;
                }
            }
        } else {
            if (materialCode.length() >= 8) {
                materialCode = materialCode + "001";
            }else {
                materialCode = materialCode + "01";
            }
        }
        DeviceType materialType1 = new DeviceType();
        materialType1.setId(KeyUtils.generateId());
        materialType1.setUpId(deviceType.getId());
        materialType1.setDeviceCode(materialCode);
        materialType1.setIsMain("N");
        try {
            if (!StringUtils.isEmpty(type.getDeviceLevelCd())) {
                int cj = Integer.parseInt(type.getDeviceLevelCd()) + 1;
                materialType1.setDeviceLevelCd(cj + "");
                materialType1.setDeviceLevel(this.getLevel(cj + ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        materialType1.setDeptId(SecurityUtils.getSysUser().getDeptId());
        materialType1.setCreateId(SecurityUtils.getUserId());
        materialType1.setCreateBy(SecurityUtils.getUsername());
        materialType1.setCreateTime(DateUtils.getNowDate());
        return materialType1;
    }

    private String getLevel(String level) {
        level = level.replace("1", "一")
                .replace("2", "二")
                .replace("3", "三")
                .replace("4", "四")
                .replace("5", "五")
                .replace("6", "六")
                .replace("7", "七")
                .replace("8", "八")
                .replace("9", "九");
        level = level + "级";
        return level;
    }


    @Override
    public void addTypeByMain(DeviceClass mtrClass) {
        DeviceType materialType = new DeviceType();
        materialType.setHostId(mtrClass.getParentId());
        List<DeviceType> materialTypes = baseMapper.selectDeviceTypeList(materialType);
        //已存在数据排除
        DeviceType type1 = new DeviceType();
        type1.setHostId(mtrClass.getId());
        type1.setDelFlag("0");
        List<DeviceType> materialTypes1 = baseMapper.selectDeviceTypeList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getUpId());
            });
        }
        List<DeviceType> list = new ArrayList<>();
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (DeviceType deviceType : materialTypes) {
                if (map.containsKey(deviceType.getOrganCode()) && (deviceType.getId() + "").equals(map.get(deviceType.getOrganCode()) + "")) {
                    continue;
                }
                DeviceType type = new DeviceType();
                type.setId(KeyUtils.generateId());
                type.setUpId(deviceType.getId());
                type.setDeviceName(mtrClass.getDeviceClassName());
                type.setDeviceType(mtrClass.getDeviceClassType());
                type.setDeviceCode(mtrClass.getDeviceClassCode());
                type.setUnit(mtrClass.getMeasureUnit());
                type.setDeviceLevel(mtrClass.getClassLevel());
                type.setDeviceLevelCd(mtrClass.getClassLevelCd());
                type.setSubjectMatterCode(mtrClass.getSubjectMatterCode());
                type.setSubjectMatterName(mtrClass.getSubjectMatterName());
                type.setState(3L);
                type.setIsTransaction(mtrClass.getSubjectMatter() + "");
                type.setOrganCode(deviceType.getOrganCode());
                type.setHostId(mtrClass.getId());
                type.setIsMain("Y");
                type.setCreateId(SecurityUtils.getUserId());
                type.setCreateBy(SecurityUtils.getUsername());
                type.setCreateTime(DateUtils.getNowDate());
                type.setDelFlag(mtrClass.getValid() + "");
                list.add(type);
            }
        }
        if (!list.isEmpty()) {
            this.saveBatch(list);
        }
    }

    /**
     * 主库修改同步修改副库
     *
     * @param mtrClass
     */
    @Override
    public void updateByHostId(DeviceClass mtrClass) {
        DeviceType materialType = new DeviceType();
        materialType.setHostId(mtrClass.getId());
        materialType.setDelFlag("0");
        List<DeviceType> materialTypes = baseMapper.selectDeviceTypeList(materialType);
        if (materialTypes != null && !materialTypes.isEmpty()) {
            materialTypes.forEach(type -> {
                type.setDeviceName(mtrClass.getDeviceClassName());
                type.setDeviceType(mtrClass.getDeviceClassType());
                type.setDeviceCode(mtrClass.getDeviceClassCode());
                type.setUnit(mtrClass.getMeasureUnit());
                type.setDeviceLevel(mtrClass.getClassLevel());
                type.setDeviceLevelCd(mtrClass.getClassLevelCd());
                type.setIsTransaction(mtrClass.getSubjectMatter() + "");
                type.setSubjectMatterCode(mtrClass.getSubjectMatterCode());
                type.setSubjectMatterName(mtrClass.getSubjectMatterName());
            });
        }
        this.updateBatchById(materialTypes);
    }


    @Override
    public void deleteByHostId(String[] histIds, Map<Long, Long> idsMap) {
        if (histIds != null) {
            QueryWrapper<DeviceType> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<DeviceType> materialTypes = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<DeviceType> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main","Y");
            materialTypes.addAll(this.list(qw));
            List<DeviceType> newDeviceDetails = new ArrayList<>();
            List<DeviceType> upDeviceDetails = new ArrayList<>();
            if (idsMap != null && !idsMap.isEmpty()) {
                materialTypes.forEach(item -> {
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
            this.updateBatchById(upDeviceDetails);
            List<Long> collect = newDeviceDetails.stream().map(DeviceType::getId).collect(Collectors.toList());
            this.removeBatchByIds(collect);
        }
    }

    /**
     * 审核通过
     *
     * @param variables
     */
    @Override
    public void processAuditPass(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        super.update(new LambdaUpdateWrapper<DeviceType>()
                .set(DeviceType::getState, AgreementStateEnum.APPROVE.getState())
                .eq(DeviceType::getId, businessId));

        QueryWrapper<DeviceItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type_id", Long.parseLong(businessId));
//        queryWrapper.eq("state", 1L);  非正式流程先干掉
        queryWrapper.eq("del_flag", "0");
        queryWrapper.eq("is_main", "N");
        List<DeviceItem> materialItems = deviceItemService.list(queryWrapper);
        materialItems.forEach(materialItem -> {
            materialItem.setState(3L);
        });
        deviceItemService.updateBatchById(materialItems);

        QueryWrapper<DeviceEigenvalue> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("type_id", Long.parseLong(businessId));
//        queryWrapper1.eq("state", 1L);
        queryWrapper1.eq("del_flag", "0");
        queryWrapper1.eq("is_main", "N");
        List<DeviceEigenvalue> eigenvalues = deviceEigenvalueService.list(queryWrapper1);
        eigenvalues.forEach(materialEigenvalue -> {
            materialEigenvalue.setState(3L);
        });
        deviceEigenvalueService.updateBatchById(eigenvalues);

        QueryWrapper<DeviceDetails> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.eq("type_id", Long.parseLong(businessId));
//        queryWrapper2.eq("state", 1L);
        queryWrapper2.eq("del_flag", "0");
        queryWrapper2.eq("is_main", "N");
        List<DeviceDetails> materialDetails = deviceDetailsService.list(queryWrapper2);
        materialDetails.forEach(materialDetails1 -> {
            materialDetails1.setState(3L);
        });
        deviceDetailsService.updateBatchById(materialDetails);

    }


    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @Override
    public synchronized int addToMain(DeviceType materialType) {
        if (materialType.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialType.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        DeviceType materialType1 = this.selectDeviceTypeByIdNoChange(materialType.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        DeviceClass mtrClass = iDeviceClassService.selectDeviceClassById(materialType.getHostId());
        //查询编码是否已在主库存在
        DeviceClass mtrClass1 = new DeviceClass();
        mtrClass1.setDeviceClassCode(materialType1.getDeviceCode());
        mtrClass1.setValid(0L);
        List<DeviceClass> mtrClasses = iDeviceClassService.selectDeviceClassList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        boolean pd = true;
        DeviceType materialType2 = this.selectDeviceTypeByIdNoChange(materialType1.getUpId());
        if (materialType2 != null && mtrClass.getDeviceClassCode().equals(materialType2.getDeviceCode())) {
            pd = false;
        }
        if ((mtrClasses != null && !mtrClasses.isEmpty()) || pd) {
            //已存在时 新增改为关联
            DeviceClass aClass = iDeviceClassService.initCode(mtrClass);
            aClass.setDeviceClassName(materialType1.getDeviceName());
            aClass.setMeasureUnit(materialType1.getUnit());
            aClass.setSubjectMatter(materialType1.getIsTransaction() == null ? 0 : Integer.parseInt(materialType1.getIsTransaction()));
            aClass.setSubjectMatterName(materialType1.getSubjectMatterName());
            aClass.setSubjectMatterCode(materialType1.getSubjectMatterCode());
            aClass.setSonId(materialType.getId());
            iDeviceClassService.insertDeviceClass(aClass);
            materialType1.setMainId(aClass.getId());
            materialType1.setIsMain("Y");
            return baseMapper.updateDeviceType(materialType1);
        } else {
            //不存在时 新增至主库
            materialType1.setIsMain("Y");
            materialType1.setHostId(key);
            int i = baseMapper.updateDeviceType(materialType1);
            if (i > 0) {
                DeviceClass aClass = new DeviceClass();
                aClass.setId(key);
                aClass.setParentId(materialType.getHostId());
                aClass.setDeviceClassCode(materialType1.getDeviceCode());
                if (!materialType1.getDeviceCode().contains(mtrClass.getDeviceClassCode())) {
                    DeviceClass aClass1 = iDeviceClassService.initCode(mtrClass);
                    aClass.setDeviceClassCode(aClass1.getDeviceClassCode());
                }
                aClass.setDeviceClassName(materialType1.getDeviceName());
                aClass.setMeasureUnit(materialType1.getUnit());
                aClass.setSubjectMatter(materialType1.getIsTransaction() == null ? 0 : Integer.parseInt(materialType1.getIsTransaction()));
                aClass.setSubjectMatterName(materialType1.getSubjectMatterName());
                aClass.setSubjectMatterCode(materialType1.getSubjectMatterCode());
                aClass.setSonId(materialType.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateTime(DateUtils.getNowDate());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                iDeviceClassService.insertDeviceClass(aClass);
            }
            return i;
        }
    }


    /**
     * 关联至主库
     *
     * @param materialType
     * @return
     */
    @Override
    public synchronized int associationToMain(DeviceType materialType) {
        if (materialType.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialType.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        DeviceType materialType1 = this.selectDeviceTypeByIdNoChange(materialType.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialType.getHostId());
        return baseMapper.updateDeviceType(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(DeviceType materialType) {
        if (materialType.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        DeviceType materialType1 = this.selectDeviceTypeByIdNoChange(materialType.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return baseMapper.updateDeviceType(materialType1);
    }

    @Override
    public long selectDeviceTypeListCount(DeviceType deviceType) {
        return baseMapper.selectDeviceTypeListCount(deviceType);
    }

    @Override
    public int getMaterialJoin(Long id) {
        return baseMapper.getMaterialJoin(id);
    }


}
