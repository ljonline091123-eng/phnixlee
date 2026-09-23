package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.enums.AgreementStateEnum;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.*;
import com.zhaocai.archives.dossier.mapper.LabourTypeMapper;
import com.zhaocai.archives.dossier.service.ILabourDetailsService;
import com.zhaocai.archives.dossier.service.ILabourEigenvalueService;
import com.zhaocai.archives.dossier.service.ILabourItemService;
import com.zhaocai.archives.dossier.service.ILabourTypeService;
import com.zhaocai.archives.dossier.tree.LabourTypeTree;
import com.zhaocai.archives.main.domain.LaborServicesClass;
import com.zhaocai.archives.main.service.ILaborServicesClassService;
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
 * 劳务分类Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class LabourTypeServiceImpl extends ServiceImpl<LabourTypeMapper, LabourType> implements ILabourTypeService {
    @Autowired
    private LabourTypeMapper labourTypeMapper;

    @Resource
    private ILaborServicesClassService laborServicesClassService;

    @Resource
    private ILabourItemService labourItemService;

    @Resource
    private ILabourEigenvalueService labourEigenvalueService;

    @Resource
    private ILabourDetailsService labourDetailsService;


    /**
     * 查询劳务分类
     *
     * @param id 劳务分类主键
     * @return 劳务分类
     */
    @Override
    public LabourType selectLabourTypeById(Long id) {
        LabourType labourType = labourTypeMapper.selectLabourTypeById(id);
        if (labourType == null) {
            return null;
        }
        if (labourType.getUpId() != null && labourType.getUpId() != 0) {
            LabourType deviceType1 = labourTypeMapper.selectLabourTypeById(labourType.getUpId());
            labourType.setBelongingLevel(deviceType1.getLabourName());
        } else {
            labourType.setBelongingLevel("顶级");
        }
        if ("N".equals(labourType.getIsMain())) {
            labourType.setLabourCode(labourType.getLabourCode() + "-" + labourType.getOrganCode().substring(0, 4));
        }
        return labourType;
    }


    /**
     * 查询劳务分类
     *
     * @param id 劳务分类主键
     * @return 劳务分类
     */
    @Override
    public LabourType selectLabourTypeByIdNoChange(Long id) {
        return labourTypeMapper.selectLabourTypeById(id);
    }

    /**
     * 查询劳务分类列表
     *
     * @param labourType 劳务分类
     * @return 劳务分类
     */
    @Override
    public List<LabourType> selectLabourTypeList(LabourType labourType) {
        return labourTypeMapper.selectLabourTypeList(labourType);
    }

    /**
     * 新增劳务分类
     *
     * @param labourType 劳务分类
     * @return 结果
     */
    @Override
    public synchronized int insertLabourType(LabourType labourType) {
        if (labourType == null) {
            throw new RuntimeException("参数不能为空");
        }
        if (StringUtils.isEmpty(labourType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        QueryWrapper<LabourType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("organ_code", labourType.getOrganCode());
        queryWrapper.eq("labour_code", labourType.getLabourCode());
        queryWrapper.eq("del_flag", "0");
        List<LabourType> list = this.list(queryWrapper);
        if (list != null && !list.isEmpty()) {
            throw new RuntimeException("材料分类编码已存在,请刷新后重试");
        }
        labourType.setState(0L);
        labourType.setCreateTime(DateUtils.getNowDate());
        return labourTypeMapper.insertLabourType(labourType);
    }

    /**
     * 修改劳务分类
     *
     * @param labourType 劳务分类
     * @return 结果
     */
    @Override
    public int updateLabourType(LabourType labourType) {
        if (labourType == null) {
            throw new RuntimeException("参数不能为空");
        }
        if (StringUtils.isEmpty(labourType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        String qc = "-" + labourType.getOrganCode().substring(0, 4);
        labourType.setLabourCode(labourType.getLabourCode().replace(qc, ""));
        QueryWrapper<LabourType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("organ_code", labourType.getOrganCode());
        queryWrapper.eq("labour_code", labourType.getLabourCode());
        queryWrapper.eq("del_flag", "0");
        queryWrapper.eq("is_main", "N");
        List<LabourType> list = this.list(queryWrapper);
        if (list != null && !list.isEmpty()) {
            for (LabourType type : list) {
                if (!type.getId().equals(labourType.getId())) {
                    throw new RuntimeException("材料分类编码已存在,请刷新后重试");
                }
            }
        }
        labourType.setUpdateTime(DateUtils.getNowDate());
        return labourTypeMapper.updateLabourType(labourType);
    }

    /**
     * 修改劳务分类
     *
     * @param labourType 劳务分类
     * @return 结果
     */
    @Override
    public int updateLabourTypeNoChange(LabourType labourType) {
        return labourTypeMapper.updateLabourType(labourType);
    }

    /**
     * 批量删除劳务分类
     *
     * @param ids 需要删除的劳务分类主键
     * @return 结果
     */
    @Override
    public boolean deleteLabourTypeByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new RuntimeException("参数不能为空");
        }
        List<LabourType> materialTypes = labourTypeMapper.selectBatchIds(Arrays.asList(ids));
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (LabourType type : materialTypes) {
                if ("Y".equals(type.getIsMain())) {
                    throw new RuntimeException("主库同步数据不允许删除");
                }
                if (type.getState() != 0L) {
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
     * 删除劳务分类信息
     *
     * @param id 劳务分类主键
     * @return 结果
     */
    @Override
    public int deleteLabourTypeById(Long id) {
        return labourTypeMapper.deleteLabourTypeById(id);
    }

    @Override
    public List<LabourType> initData(LabourType labourType) {
        List<LabourType> materialTypes = labourTypeMapper.selectLabourTypeList(labourType);
        List<LaborServicesClass> mtrClasses = laborServicesClassService.selectLaborServicesClassList(null);
        Map<String, Long> map = new HashMap<>();
        Map<String, LaborServicesClass> mtrMap = new HashMap<>();
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            mtrClasses.forEach(mtrClass -> {
                map.put(mtrClass.getId(), KeyUtils.generateId());
                mtrMap.put(mtrClass.getId(), mtrClass);
            });
        }
        if (materialTypes == null || materialTypes.isEmpty()) {
            //初始化-最开始无数据情况
            if (mtrClasses != null && !mtrClasses.isEmpty()) {
                mtrClasses.forEach(mtrClass -> {
                    LabourType type = new LabourType();
                    type.setId(map.get(mtrClass.getId()));
                    type.setUpId(map.get(mtrClass.getParentId()));
                    type.setLabourType(mtrClass.getLaborServicesClassType());
                    type.setLabourCode(mtrClass.getLaborServicesClassCode());
                    type.setLabourName(mtrClass.getLaborServicesClassName());
                    type.setUnit(mtrClass.getMeasureUnit());
                    type.setIsTransaction(mtrClass.getSubjectMatter() + "");
                    type.setOrganCode(labourType.getOrganCode());
                    type.setHostId(mtrClass.getId());
                    type.setIsMain("Y");
                    type.setState(3L);
                    type.setCreateId(SecurityUtils.getUserId());
                    type.setCreateBy(SecurityUtils.getUsername());
                    type.setCreateTime(DateUtils.getNowDate());
                    type.setDelFlag(mtrClass.getValid() + "");
                    materialTypes.add(type);
                });
                this.saveBatch(materialTypes);
            }
        } else {
            //有数据的情况，对比主库数据，新增主库存在副库不存在的数据，且非本副库新增至主库数据。（关联数据是否还需要将主库数据同步到副库）
            Map<String, Long> params = new HashMap<>();
            materialTypes.forEach(item -> {
                if ("Y".equals(item.getIsMain())) {
                    params.put(item.getHostId(), item.getId());
                }
            });
            List<LabourType> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!params.containsKey(key)) {
                    LaborServicesClass mtrClass = mtrMap.get(key);
                    LabourType type = new LabourType();
                    type.setId(map.get(mtrClass.getId()));
                    if (params.containsKey(mtrClass.getParentId())) {
                        type.setUpId(params.get(mtrClass.getParentId()));
                    } else {
                        type.setUpId(map.get(mtrClass.getParentId()));
                    }
                    type.setLabourType(mtrClass.getLaborServicesClassType());
                    type.setLabourCode(mtrClass.getLaborServicesClassCode());
                    type.setLabourName(mtrClass.getLaborServicesClassName());
                    type.setUnit(mtrClass.getMeasureUnit());
                    type.setIsTransaction(mtrClass.getSubjectMatter() + "");
                    type.setOrganCode(labourType.getOrganCode());
                    type.setHostId(mtrClass.getId());
                    type.setIsMain("Y");
                    type.setState(3L);
                    type.setCreateId(SecurityUtils.getUserId());
                    type.setCreateBy(SecurityUtils.getUsername());
                    type.setCreateTime(DateUtils.getNowDate());
                    type.setDelFlag(mtrClass.getValid() + "");
                    materialTypes.add(type);
                    addList.add(type);
                }
            }
            if (!addList.isEmpty()) {
                this.saveBatch(addList);
            }
        }
        return materialTypes;
    }

    @Override
    public LabourType initMaterialType(LabourType labourType) {
        if (StringUtils.isEmpty(labourType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (labourType.getId() == null) {
            throw new RuntimeException("id不能为空");
        }
        LabourType type = this.getById(labourType.getId());
        if (type == null) {
            throw new RuntimeException("类型不存在");
        }
        String materialCode = type.getLabourCode();
        Integer maxCode = baseMapper.getMaxCode(materialCode, labourType.getId(), labourType.getOrganCode());
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
                    Integer reuseTowCode = baseMapper.getReuseTowCode(materialCode, labourType.getId(), labourType.getOrganCode(), 999);
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
                    Integer reuseTowCode = baseMapper.getReuseTowCode(materialCode, labourType.getId(), labourType.getOrganCode(), 99);
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
        LabourType materialType1 = new LabourType();
        materialType1.setId(KeyUtils.generateId());
        materialType1.setUpId(labourType.getId());
        materialType1.setLabourCode(materialCode);
        materialType1.setState(0L);
        materialType1.setIsMain("N");
//        materialType1.setDeptId(SecurityUtils.getSysUser().getDeptId());
        materialType1.setCreateId(SecurityUtils.getUserId());
        materialType1.setCreateBy(SecurityUtils.getUsername());
        materialType1.setCreateTime(DateUtils.getNowDate());
        return materialType1;
    }

    @Override
    public List<LabourTypeTree> getLabourTypeTree(LabourType labourType) {
        LabourType typex = new LabourType();
        typex.setOrganCode(labourType.getOrganCode());
        typex.setDelFlag("0");
        PageUtils.clearPage();
        List<LabourType> select = baseMapper.selectLabourTypeList(typex);
        Map<Long, Long> typeMap = new HashMap<>();
        select.forEach(item -> {
            typeMap.put(item.getId(), item.getUpId());
        });
        Map<Long, Long> idsMap = new HashMap<>();
        Map<Long, LabourTypeTree> map = new HashMap<>();
        if (MaterialType.ALL.equals(labourType.getQueryType()) || MaterialType.UNTREATED.equals(labourType.getQueryType())) {
            //查询未处理的类型数据
            LabourType type = new LabourType();
            type.setState(3L);
            type.setDelFlag("0");
            type.setIsMain("N");
            type.setMainId("0");
            type.setOrganCode(labourType.getOrganCode());
            List<LabourType> materialTypes = this.selectLabourTypeList(type);
            materialTypes.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
            //查询未处理的值数据
            LabourEigenvalue eigenvalue = new LabourEigenvalue();
            eigenvalue.setState(3L);
            eigenvalue.setDelFlag("0");
            eigenvalue.setIsMain("N");
            eigenvalue.setMainId("0");
            eigenvalue.setOrganCode(labourType.getOrganCode());
            List<LabourEigenvalue> list = labourEigenvalueService.selectLabourEigenvalueList(eigenvalue);
            list.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
            //查询未处理的项数据
            LabourItem materialItem = new LabourItem();
            materialItem.setState(3L);
            materialItem.setDelFlag("0");
            materialItem.setIsMain("N");
            materialItem.setMainId("0");
            materialItem.setOrganCode(labourType.getOrganCode());
            List<LabourItem> materialItems = labourItemService.selectLabourItemListNoChange(materialItem);
            materialItems.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
            //查询未处理的详情数据
            LabourDetails materialDetails = new LabourDetails();
            materialDetails.setState(3L);
            materialDetails.setDelFlag("0");
            materialDetails.setIsMain("N");
            materialDetails.setMainId("0");
            materialDetails.setOrganCode(labourType.getOrganCode());
            List<LabourDetails> materialDetails1 = labourDetailsService.selectLabourDetailsListNoChange(materialDetails);
            materialDetails1.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
        }

        if (MaterialType.ALL.equals(labourType.getQueryType()) || MaterialType.PROCESSED.equals(labourType.getQueryType())) {

            List<LabourItem> materialItems = labourItemService.getProcessed(labourType.getOrganCode());
            materialItems.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });

            List<LabourEigenvalue> eigenvalues = labourEigenvalueService.getProcessed(labourType.getOrganCode());
            eigenvalues.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });

            List<LabourDetails> details = labourDetailsService.getProcessed(labourType.getOrganCode());
            details.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });

            List<LabourType> materialTypes = baseMapper.getProcessed(labourType.getOrganCode());
            materialTypes.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
        }

        select.forEach(item -> {
            if (StringUtils.isEmpty(labourType.getQueryType()) || idsMap.containsKey(item.getId())) {
                LabourTypeTree materialTypeTree = new LabourTypeTree();
                materialTypeTree.setId(item.getId() + "");
                materialTypeTree.setLabel(item.getLabourName());
                materialTypeTree.setType(item.getLabourType());
                materialTypeTree.setCode(item.getLabourCode());
                if ("N".equals(item.getIsMain())) {
                    materialTypeTree.setCode(item.getLabourCode() + "-" + item.getOrganCode().substring(0, 4));
                }
                materialTypeTree.setChildren(new ArrayList<>());
                materialTypeTree.setMainId(item.getMainId());
                materialTypeTree.setState(item.getState());
                materialTypeTree.setIsMain(item.getIsMain());
                materialTypeTree.setBusinessId(item.getWfBatch());
                materialTypeTree.setProcessId(item.getWfProcessId());
                map.put(item.getId(), materialTypeTree);
            }
        });

        // 构建树形结构
        List<LabourTypeTree> list = new ArrayList<>();
        for (LabourType type : select) {
            LabourTypeTree treeVo = map.get(type.getId());
            if (treeVo != null) {
                if (type.getUpId() == null) {
                    // 根节点，直接添加
                    list.add(treeVo);
                } else {
                    // 非根节点，找到父节点并添加到其子节点列表中
                    LabourTypeTree materialTypeTree = map.get(type.getUpId());
                    if (materialTypeTree != null) {
                        materialTypeTree.getChildren().add(treeVo);
                    }
                }
            }
        }
        return list;
    }


    private void xhtq(LabourItem item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
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

    private void xhtq(LabourType item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
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

    private void xhtq(LabourEigenvalue item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
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

    private void xhtq(LabourDetails item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
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
    public void addTypeByMain(LaborServicesClass mtrClass) {
        LabourType materialType = new LabourType();
        materialType.setHostId(mtrClass.getParentId());
        List<LabourType> materialTypes = baseMapper.selectLabourTypeList(materialType);
        //已存在数据排除
        LabourType type1 = new LabourType();
        type1.setHostId(mtrClass.getId());
        type1.setDelFlag("0");
        List<LabourType> materialTypes1 = baseMapper.selectLabourTypeList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getUpId());
            });
        }
        List<LabourType> list = new ArrayList<>();
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (LabourType labourType : materialTypes) {
                if (map.containsKey(labourType.getOrganCode()) && (labourType.getId() + "").equals(map.get(labourType.getOrganCode()) + "")) {
                    continue;
                }
                LabourType type = new LabourType();
                type.setId(KeyUtils.generateId());
                type.setUpId(labourType.getId());
                type.setLabourType(mtrClass.getLaborServicesClassType());
                type.setLabourCode(mtrClass.getLaborServicesClassCode());
                type.setLabourName(mtrClass.getLaborServicesClassName());
                type.setUnit(mtrClass.getMeasureUnit());
                type.setIsTransaction(mtrClass.getSubjectMatter() + "");
                type.setOrganCode(labourType.getOrganCode());
                type.setHostId(mtrClass.getId());
                type.setIsMain("Y");
                type.setState(3L);
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
    public void updateByHostId(LaborServicesClass mtrClass) {
        LabourType materialType = new LabourType();
        materialType.setHostId(mtrClass.getId());
        materialType.setDelFlag("0");
        List<LabourType> materialTypes = baseMapper.selectLabourTypeList(materialType);
        if (materialTypes != null && !materialTypes.isEmpty()) {
            materialTypes.forEach(type -> {
                type.setLabourType(mtrClass.getLaborServicesClassType());
                type.setLabourCode(mtrClass.getLaborServicesClassCode());
                type.setLabourName(mtrClass.getLaborServicesClassName());
                type.setUnit(mtrClass.getMeasureUnit());
                type.setIsTransaction(mtrClass.getSubjectMatter() + "");
            });
        }
        this.updateBatchById(materialTypes);
    }


    @Override
    public void deleteByHostId(String[] histIds, Map<Long, Long> idsMap) {
        if (histIds != null) {
            QueryWrapper<LabourType> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<LabourType> materialTypes = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<LabourType> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main","Y");
            materialTypes.addAll(this.list(qw));
            List<LabourType> newDeviceDetails = new ArrayList<>();
            List<LabourType> upDeviceDetails = new ArrayList<>();
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
            List<Long> collect = newDeviceDetails.stream().map(LabourType::getId).collect(Collectors.toList());
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
        super.update(new LambdaUpdateWrapper<LabourType>()
                .set(LabourType::getState, AgreementStateEnum.APPROVE.getState())
                .eq(LabourType::getId, businessId));

        QueryWrapper<LabourItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type_id", Long.parseLong(businessId));
//        queryWrapper.eq("state", 1L);  非正式流程先干掉
        queryWrapper.eq("del_flag", "0");
        queryWrapper.eq("is_main", "N");
        List<LabourItem> materialItems = labourItemService.list(queryWrapper);
        materialItems.forEach(materialItem -> {
            materialItem.setState(3L);
        });
        labourItemService.updateBatchById(materialItems);

        QueryWrapper<LabourEigenvalue> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("type_id", Long.parseLong(businessId));
//        queryWrapper1.eq("state", 1L);
        queryWrapper1.eq("del_flag", "0");
        queryWrapper1.eq("is_main", "N");
        List<LabourEigenvalue> eigenvalues = labourEigenvalueService.list(queryWrapper1);
        eigenvalues.forEach(materialEigenvalue -> {
            materialEigenvalue.setState(3L);
        });
        labourEigenvalueService.updateBatchById(eigenvalues);

        QueryWrapper<LabourDetails> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.eq("type_id", Long.parseLong(businessId));
//        queryWrapper2.eq("state", 1L);
        queryWrapper2.eq("del_flag", "0");
        queryWrapper2.eq("is_main", "N");
        List<LabourDetails> materialDetails = labourDetailsService.list(queryWrapper2);
        materialDetails.forEach(materialDetails1 -> {
            materialDetails1.setState(3L);
        });
        labourDetailsService.updateBatchById(materialDetails);

    }


    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @Override
    public synchronized int addToMain(LabourType materialType) {
        if (materialType.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialType.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        LabourType materialType1 = this.selectLabourTypeByIdNoChange(materialType.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        LaborServicesClass mtrClass = laborServicesClassService.selectLaborServicesClassById(materialType.getHostId());
        //查询编码是否已在主库存在
        LaborServicesClass mtrClass1 = new LaborServicesClass();
        mtrClass1.setLaborServicesClassCode(materialType1.getLabourCode());
        mtrClass1.setValid(0L);
        List<LaborServicesClass> mtrClasses = laborServicesClassService.selectLaborServicesClassList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        boolean pd = true;
        LabourType materialType2 = this.selectLabourTypeByIdNoChange(materialType1.getUpId());
        if (materialType2 != null && mtrClass.getLaborServicesClassCode().equals(materialType2.getLabourCode())) {
            pd = false;
        }
        if ((mtrClasses != null && !mtrClasses.isEmpty()) || pd) {
            //已存在时 新增改为关联
            LaborServicesClass aClass = laborServicesClassService.initCode(mtrClass);
            aClass.setLaborServicesClassName(materialType1.getLabourName());
            aClass.setMeasureUnit(materialType1.getUnit());
            aClass.setSubjectMatter(materialType1.getIsTransaction() == null ? 0 : Integer.parseInt(materialType1.getIsTransaction()));
            aClass.setSonId(materialType.getId());
            laborServicesClassService.insertLaborServicesClass(aClass);
            materialType1.setMainId(aClass.getId());
            materialType1.setIsMain("Y");
            return baseMapper.updateLabourType(materialType1);
        } else {
            //不存在时 新增至主库
            materialType1.setIsMain("Y");
            materialType1.setHostId(key);
            int i = baseMapper.updateLabourType(materialType1);
            if (i > 0) {
                LaborServicesClass aClass = new LaborServicesClass();
                aClass.setId(key);
                aClass.setParentId(materialType.getHostId());
                aClass.setLaborServicesClassCode(materialType1.getLabourCode());
                if (!materialType1.getLabourCode().contains(mtrClass.getLaborServicesClassCode())) {
                    LaborServicesClass aClass1 = laborServicesClassService.initCode(mtrClass);
                    aClass.setLaborServicesClassCode(aClass1.getLaborServicesClassCode());
                }
                aClass.setLaborServicesClassName(materialType1.getLabourName());
                aClass.setMeasureUnit(materialType1.getUnit());
                aClass.setSubjectMatter(materialType1.getIsTransaction() == null ? 0 : Integer.parseInt(materialType1.getIsTransaction()));
                aClass.setSonId(materialType.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateTime(DateUtils.getNowDate());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                laborServicesClassService.insertLaborServicesClass(aClass);
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
    public synchronized int associationToMain(LabourType materialType) {
        if (materialType.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialType.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        LabourType materialType1 = this.selectLabourTypeByIdNoChange(materialType.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialType.getHostId());
        return baseMapper.updateLabourType(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(LabourType materialType) {
        if (materialType.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        LabourType materialType1 = this.selectLabourTypeByIdNoChange(materialType.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return baseMapper.updateLabourType(materialType1);
    }

    @Override
    public long selectLabourTypeListCount(LabourType labourType) {
        return baseMapper.selectLabourTypeListCount(labourType);
    }

    @Override
    public int getMaterialJoin(Long id) {
        return baseMapper.getMaterialJoin(id);
    }


}
