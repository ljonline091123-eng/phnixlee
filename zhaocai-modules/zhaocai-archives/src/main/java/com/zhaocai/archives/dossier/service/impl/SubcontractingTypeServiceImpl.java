package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.enums.AgreementStateEnum;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.*;
import com.zhaocai.archives.dossier.mapper.SubcontractingTypeMapper;
import com.zhaocai.archives.dossier.service.ISubcontractingDetailsService;
import com.zhaocai.archives.dossier.service.ISubcontractingEigenvalueService;
import com.zhaocai.archives.dossier.service.ISubcontractingItemService;
import com.zhaocai.archives.dossier.service.ISubcontractingTypeService;
import com.zhaocai.archives.dossier.tree.SubcontractingTypeTree;
import com.zhaocai.archives.main.domain.MajorSubcontractingClass;
import com.zhaocai.archives.main.service.IMajorSubcontractingClassService;
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
 * 专业分包分类Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class SubcontractingTypeServiceImpl extends ServiceImpl<SubcontractingTypeMapper, SubcontractingType> implements ISubcontractingTypeService {
    @Autowired
    private SubcontractingTypeMapper subcontractingTypeMapper;

    @Resource
    private IMajorSubcontractingClassService iMajorSubcontractingClassService;

    @Resource
    private ISubcontractingItemService subcontractingItemService;

    @Resource
    private ISubcontractingEigenvalueService subcontractingEigenvalueService;

    @Resource
    private ISubcontractingDetailsService subcontractingDetailsService;

    /**
     * 查询专业分包分类
     *
     * @param id 专业分包分类主键
     * @return 专业分包分类
     */
    @Override
    public SubcontractingType selectSubcontractingTypeById(Long id) {
        SubcontractingType subcontractingType = subcontractingTypeMapper.selectSubcontractingTypeById(id);
        if (subcontractingType == null) {
            return null;
        }
        if (subcontractingType.getUpId() != null && subcontractingType.getUpId() != 0) {
            SubcontractingType deviceType1 = subcontractingTypeMapper.selectSubcontractingTypeById(subcontractingType.getUpId());
            subcontractingType.setBelongingLevel(deviceType1.getSubcontractingName());
        } else {
            subcontractingType.setBelongingLevel("顶级");
        }
        if ("N".equals(subcontractingType.getIsMain())) {
            subcontractingType.setSubcontractingCode(subcontractingType.getSubcontractingCode() + "-" + subcontractingType.getOrganCode().substring(0, 4));
        }
        return subcontractingType;
    }


    /**
     * 查询专业分包分类
     *
     * @param id 专业分包分类主键
     * @return 专业分包分类
     */
    @Override
    public SubcontractingType selectSubcontractingTypeByIdNoChange(Long id) {
        return subcontractingTypeMapper.selectSubcontractingTypeById(id);
    }

    /**
     * 查询专业分包分类列表
     *
     * @param subcontractingType 专业分包分类
     * @return 专业分包分类
     */
    @Override
    public List<SubcontractingType> selectSubcontractingTypeList(SubcontractingType subcontractingType) {
        return subcontractingTypeMapper.selectSubcontractingTypeList(subcontractingType);
    }

    /**
     * 新增专业分包分类
     *
     * @param subcontractingType 专业分包分类
     * @return 结果
     */
    @Override
    public synchronized int insertSubcontractingType(SubcontractingType subcontractingType) {
        if (subcontractingType == null) {
            throw new RuntimeException("参数不能为空");
        }
        if (StringUtils.isEmpty(subcontractingType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        QueryWrapper<SubcontractingType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("organ_code", subcontractingType.getOrganCode());
        queryWrapper.eq("subcontracting_code", subcontractingType.getSubcontractingCode());
        queryWrapper.eq("del_flag", "0");
        List<SubcontractingType> list = this.list(queryWrapper);
        if (list != null && !list.isEmpty()) {
            throw new RuntimeException("分类编码已存在,请刷新后重试");
        }
        subcontractingType.setState(0L);
        subcontractingType.setCreateTime(DateUtils.getNowDate());
        return subcontractingTypeMapper.insertSubcontractingType(subcontractingType);
    }

    /**
     * 修改专业分包分类
     *
     * @param subcontractingType 专业分包分类
     * @return 结果
     */
    @Override
    public int updateSubcontractingType(SubcontractingType subcontractingType) {
        if (subcontractingType == null) {
            throw new RuntimeException("参数不能为空");
        }
        if (StringUtils.isEmpty(subcontractingType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        String qc = "-" + subcontractingType.getOrganCode().substring(0, 4);
        subcontractingType.setSubcontractingCode(subcontractingType.getSubcontractingCode().replace(qc, ""));
        QueryWrapper<SubcontractingType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("organ_code", subcontractingType.getOrganCode());
        queryWrapper.eq("subcontracting_code", subcontractingType.getSubcontractingCode());
        queryWrapper.eq("del_flag", "0");
        queryWrapper.eq("is_main", "N");
        List<SubcontractingType> list = this.list(queryWrapper);
        if (list != null && !list.isEmpty()) {
            for (SubcontractingType type : list) {
                if (!type.getId().equals(subcontractingType.getId())) {
                    throw new RuntimeException("分类编码已存在,请刷新后重试");
                }
            }
        }
        subcontractingType.setUpdateTime(DateUtils.getNowDate());
        return subcontractingTypeMapper.updateSubcontractingType(subcontractingType);
    }

    /**
     * 修改专业分包分类
     *
     * @param subcontractingType 专业分包分类
     * @return 结果
     */
    @Override
    public int updateSubcontractingTypeNoChange(SubcontractingType subcontractingType) {
        return subcontractingTypeMapper.updateSubcontractingType(subcontractingType);
    }

    /**
     * 批量删除专业分包分类
     *
     * @param ids 需要删除的专业分包分类主键
     * @return 结果
     */
    @Override
    public boolean deleteSubcontractingTypeByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new RuntimeException("参数不能为空");
        }
        List<SubcontractingType> materialTypes = subcontractingTypeMapper.selectBatchIds(Arrays.asList(ids));
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (SubcontractingType type : materialTypes) {
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
     * 删除专业分包分类信息
     *
     * @param id 专业分包分类主键
     * @return 结果
     */
    @Override
    public int deleteSubcontractingTypeById(Long id) {
        return subcontractingTypeMapper.deleteSubcontractingTypeById(id);
    }

    @Override
    public List<SubcontractingTypeTree> getSubcontractingTypeTree(SubcontractingType subcontractingType) {
        SubcontractingType typex = new SubcontractingType();
        typex.setOrganCode(subcontractingType.getOrganCode());
        typex.setDelFlag("0");
        PageUtils.clearPage();
        List<SubcontractingType> select = baseMapper.selectSubcontractingTypeList(typex);
        Map<Long, Long> typeMap = new HashMap<>();
        select.forEach(item -> {
            typeMap.put(item.getId(), item.getUpId());
        });
        Map<Long, Long> idsMap = new HashMap<>();
        Map<Long, SubcontractingTypeTree> map = new HashMap<>();
        if (MaterialType.ALL.equals(subcontractingType.getQueryType()) || MaterialType.UNTREATED.equals(subcontractingType.getQueryType())) {
            //查询未处理的类型数据
            SubcontractingType type = new SubcontractingType();
            type.setState(3L);
            type.setDelFlag("0");
            type.setIsMain("N");
            type.setMainId("0");
            type.setOrganCode(subcontractingType.getOrganCode());
            List<SubcontractingType> materialTypes = this.selectSubcontractingTypeList(type);
            materialTypes.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
            //查询未处理的值数据
            SubcontractingEigenvalue eigenvalue = new SubcontractingEigenvalue();
            eigenvalue.setState(3L);
            eigenvalue.setDelFlag("0");
            eigenvalue.setIsMain("N");
            eigenvalue.setMainId("0");
            eigenvalue.setOrganCode(subcontractingType.getOrganCode());
            List<SubcontractingEigenvalue> list = subcontractingEigenvalueService.selectSubcontractingEigenvalueList(eigenvalue);
            list.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
            //查询未处理的项数据
            SubcontractingItem materialItem = new SubcontractingItem();
            materialItem.setState(3L);
            materialItem.setDelFlag("0");
            materialItem.setIsMain("N");
            materialItem.setMainId("0");
            materialItem.setOrganCode(subcontractingType.getOrganCode());
            List<SubcontractingItem> materialItems = subcontractingItemService.selectSubcontractingItemListNoChange(materialItem);
            materialItems.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
            //查询未处理的详情数据
            SubcontractingDetails materialDetails = new SubcontractingDetails();
            materialDetails.setState(3L);
            materialDetails.setDelFlag("0");
            materialDetails.setIsMain("N");
            materialDetails.setMainId("0");
            materialDetails.setOrganCode(subcontractingType.getOrganCode());
            List<SubcontractingDetails> materialDetails1 = subcontractingDetailsService.selectSubcontractingDetailsListNoChange(materialDetails);
            materialDetails1.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
        }

        if (MaterialType.ALL.equals(subcontractingType.getQueryType()) || MaterialType.PROCESSED.equals(subcontractingType.getQueryType())) {

            List<SubcontractingItem> materialItems = subcontractingItemService.getProcessed(subcontractingType.getOrganCode());
            materialItems.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });

            List<SubcontractingEigenvalue> eigenvalues = subcontractingEigenvalueService.getProcessed(subcontractingType.getOrganCode());
            eigenvalues.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });

            List<SubcontractingDetails> details = subcontractingDetailsService.getProcessed(subcontractingType.getOrganCode());
            details.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });

            List<SubcontractingType> materialTypes = baseMapper.getProcessed(subcontractingType.getOrganCode());
            materialTypes.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
        }

        select.forEach(item -> {
            if (StringUtils.isEmpty(subcontractingType.getQueryType()) || idsMap.containsKey(item.getId())) {
                SubcontractingTypeTree materialTypeTree = new SubcontractingTypeTree();
                materialTypeTree.setId(item.getId() + "");
                materialTypeTree.setLabel(item.getSubcontractingName());
                materialTypeTree.setType(item.getSubcontractingType());
                materialTypeTree.setCode(item.getSubcontractingCode());
                if ("N".equals(item.getIsMain())) {
                    materialTypeTree.setCode(item.getSubcontractingCode() + "-" + item.getOrganCode().substring(0, 4));
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
        List<SubcontractingTypeTree> list = new ArrayList<>();
        for (SubcontractingType type : select) {
            SubcontractingTypeTree treeVo = map.get(type.getId());
            if (treeVo != null) {
                if (type.getUpId() == null) {
                    // 根节点，直接添加
                    list.add(treeVo);
                } else {
                    // 非根节点，找到父节点并添加到其子节点列表中
                    SubcontractingTypeTree materialTypeTree = map.get(type.getUpId());
                    if (materialTypeTree != null) {
                        materialTypeTree.getChildren().add(treeVo);
                    }
                }
            }
        }
        return list;
    }

    private void xhtq(SubcontractingItem item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
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

    private void xhtq(SubcontractingType item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
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

    private void xhtq(SubcontractingEigenvalue item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
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

    private void xhtq(SubcontractingDetails item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
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
    public List<SubcontractingType> initData(SubcontractingType subcontractingType) {
        List<SubcontractingType> materialTypes = subcontractingTypeMapper.selectSubcontractingTypeList(subcontractingType);
        List<MajorSubcontractingClass> mtrClasses = iMajorSubcontractingClassService.selectMajorSubcontractingClassList(null);
        Map<String, Long> map = new HashMap<>();
        Map<String, MajorSubcontractingClass> mtrMap = new HashMap<>();
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
                    SubcontractingType type = new SubcontractingType();
                    type.setId(map.get(mtrClass.getId()));
                    type.setUpId(map.get(mtrClass.getParentId()));
                    type.setSubcontractingType(mtrClass.getMajorSubcontractingClassType());
                    type.setSubcontractingCode(mtrClass.getMajorSubcontractingClassCode());
                    type.setSubcontractingName(mtrClass.getMajorSubcontractingClassName());
                    type.setUnit(mtrClass.getMeasureUnit());
                    type.setIsTransaction(mtrClass.getSubjectMatter() + "");
                    type.setOrganCode(subcontractingType.getOrganCode());
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
            List<SubcontractingType> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!params.containsKey(key)) {
                    MajorSubcontractingClass mtrClass = mtrMap.get(key);
                    SubcontractingType type = new SubcontractingType();
                    type.setId(map.get(mtrClass.getId()));
                    if (params.containsKey(mtrClass.getParentId())) {
                        type.setUpId(params.get(mtrClass.getParentId()));
                    } else {
                        type.setUpId(map.get(mtrClass.getParentId()));
                    }
                    type.setSubcontractingType(mtrClass.getMajorSubcontractingClassType());
                    type.setSubcontractingCode(mtrClass.getMajorSubcontractingClassCode());
                    type.setSubcontractingName(mtrClass.getMajorSubcontractingClassName());
                    type.setUnit(mtrClass.getMeasureUnit());
                    type.setIsTransaction(mtrClass.getSubjectMatter() + "");
                    type.setOrganCode(subcontractingType.getOrganCode());
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
    public SubcontractingType initSubcontractingType(SubcontractingType subcontractingType) {
        if (StringUtils.isEmpty(subcontractingType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (subcontractingType.getId() == null) {
            throw new RuntimeException("id不能为空");
        }
        SubcontractingType type = this.getById(subcontractingType.getId());
        if (type == null) {
            throw new RuntimeException("类型不存在");
        }
        String materialCode = type.getSubcontractingCode();
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
            } else {
                materialCode = materialCode + "01";
            }
        }
        SubcontractingType materialType1 = new SubcontractingType();
        materialType1.setId(KeyUtils.generateId());
        materialType1.setUpId(subcontractingType.getId());
        materialType1.setSubcontractingCode(materialCode);
        materialType1.setState(0L);
        materialType1.setIsMain("N");
//        materialType1.setDeptId(SecurityUtils.getSysUser().getDeptId());
        materialType1.setCreateId(SecurityUtils.getUserId());
        materialType1.setCreateBy(SecurityUtils.getUsername());
        materialType1.setCreateTime(DateUtils.getNowDate());
        return materialType1;
    }


    @Override
    public void addTypeByMain(MajorSubcontractingClass mtrClass) {
        SubcontractingType materialType = new SubcontractingType();
        materialType.setHostId(mtrClass.getParentId());
        List<SubcontractingType> materialTypes = baseMapper.selectSubcontractingTypeList(materialType);
        //已存在数据排除
        SubcontractingType type1 = new SubcontractingType();
        type1.setHostId(mtrClass.getId());
        type1.setDelFlag("0");
        List<SubcontractingType> materialTypes1 = baseMapper.selectSubcontractingTypeList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getUpId());
            });
        }
        List<SubcontractingType> list = new ArrayList<>();
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (SubcontractingType labourType : materialTypes) {
                if (map.containsKey(labourType.getOrganCode()) && (labourType.getId() + "").equals(map.get(labourType.getOrganCode()) + "")) {
                    continue;
                }
                SubcontractingType type = new SubcontractingType();
                type.setId(KeyUtils.generateId());
                type.setUpId(labourType.getId());
                type.setSubcontractingType(mtrClass.getMajorSubcontractingClassType());
                type.setSubcontractingCode(mtrClass.getMajorSubcontractingClassCode());
                type.setSubcontractingName(mtrClass.getMajorSubcontractingClassName());
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
    public void updateByHostId(MajorSubcontractingClass mtrClass) {
        SubcontractingType materialType = new SubcontractingType();
        materialType.setHostId(mtrClass.getId());
        materialType.setDelFlag("0");
        List<SubcontractingType> materialTypes = baseMapper.selectSubcontractingTypeList(materialType);
        if (materialTypes != null && !materialTypes.isEmpty()) {
            materialTypes.forEach(type -> {
                type.setSubcontractingType(mtrClass.getMajorSubcontractingClassType());
                type.setSubcontractingCode(mtrClass.getMajorSubcontractingClassCode());
                type.setSubcontractingName(mtrClass.getMajorSubcontractingClassName());
                type.setUnit(mtrClass.getMeasureUnit());
                type.setIsTransaction(mtrClass.getSubjectMatter() + "");
            });
        }
        this.updateBatchById(materialTypes);
    }


    @Override
    public void deleteByHostId(String[] histIds, Map<Long, Long> idsMap) {
        if (histIds != null) {
            QueryWrapper<SubcontractingType> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<SubcontractingType> materialTypes = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<SubcontractingType> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main", "Y");
            materialTypes.addAll(this.list(qw));
            List<SubcontractingType> newDeviceDetails = new ArrayList<>();
            List<SubcontractingType> upDeviceDetails = new ArrayList<>();
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
            List<Long> collect = newDeviceDetails.stream().map(SubcontractingType::getId).collect(Collectors.toList());
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
        super.update(new LambdaUpdateWrapper<SubcontractingType>()
                .set(SubcontractingType::getState, AgreementStateEnum.APPROVE.getState())
                .eq(SubcontractingType::getId, businessId));

        QueryWrapper<SubcontractingItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type_id", Long.parseLong(businessId));
//        queryWrapper.eq("state", 1L);  非正式流程先干掉
        queryWrapper.eq("del_flag", "0");
        queryWrapper.eq("is_main", "N");
        List<SubcontractingItem> materialItems = subcontractingItemService.list(queryWrapper);
        materialItems.forEach(materialItem -> {
            materialItem.setState(3L);
        });
        subcontractingItemService.updateBatchById(materialItems);

        QueryWrapper<SubcontractingEigenvalue> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("type_id", Long.parseLong(businessId));
//        queryWrapper1.eq("state", 1L);
        queryWrapper1.eq("del_flag", "0");
        queryWrapper1.eq("is_main", "N");
        List<SubcontractingEigenvalue> eigenvalues = subcontractingEigenvalueService.list(queryWrapper1);
        eigenvalues.forEach(materialEigenvalue -> {
            materialEigenvalue.setState(3L);
        });
        subcontractingEigenvalueService.updateBatchById(eigenvalues);

        QueryWrapper<SubcontractingDetails> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.eq("type_id", Long.parseLong(businessId));
//        queryWrapper2.eq("state", 1L);
        queryWrapper2.eq("del_flag", "0");
        queryWrapper2.eq("is_main", "N");
        List<SubcontractingDetails> materialDetails = subcontractingDetailsService.list(queryWrapper2);
        materialDetails.forEach(materialDetails1 -> {
            materialDetails1.setState(3L);
        });
        subcontractingDetailsService.updateBatchById(materialDetails);

    }


    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @Override
    public synchronized int addToMain(SubcontractingType materialType) {
        if (materialType.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialType.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        SubcontractingType materialType1 = this.selectSubcontractingTypeByIdNoChange(materialType.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        MajorSubcontractingClass mtrClass = iMajorSubcontractingClassService.selectMajorSubcontractingClassById(materialType.getHostId());
        //查询编码是否已在主库存在
        MajorSubcontractingClass mtrClass1 = new MajorSubcontractingClass();
        mtrClass1.setMajorSubcontractingClassCode(materialType1.getSubcontractingCode());
        mtrClass1.setValid(0L);
        List<MajorSubcontractingClass> mtrClasses = iMajorSubcontractingClassService.selectMajorSubcontractingClassList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        boolean pd = true;
        SubcontractingType materialType2 = this.selectSubcontractingTypeByIdNoChange(materialType1.getUpId());
        if (materialType2 != null && mtrClass.getMajorSubcontractingClassCode().equals(materialType2.getSubcontractingCode())) {
            pd = false;
        }
        if ((mtrClasses != null && !mtrClasses.isEmpty()) || pd) {
            //已存在时 新增改为关联
            MajorSubcontractingClass aClass = iMajorSubcontractingClassService.initCode(mtrClass);
            aClass.setMajorSubcontractingClassName(materialType1.getSubcontractingName());
            aClass.setMeasureUnit(materialType1.getUnit());
            aClass.setSubjectMatter(materialType1.getIsTransaction() == null ? 0 : Integer.parseInt(materialType1.getIsTransaction()));
            aClass.setSonId(materialType.getId());
            iMajorSubcontractingClassService.insertMajorSubcontractingClass(aClass);
            materialType1.setMainId(aClass.getId());
            materialType1.setIsMain("Y");
            return baseMapper.updateSubcontractingType(materialType1);
        } else {
            //不存在时 新增至主库
            materialType1.setIsMain("Y");
            materialType1.setHostId(key);
            int i = baseMapper.updateSubcontractingType(materialType1);
            if (i > 0) {
                MajorSubcontractingClass aClass = new MajorSubcontractingClass();
                aClass.setId(key);
                aClass.setParentId(materialType.getHostId());
                aClass.setMajorSubcontractingClassCode(materialType1.getSubcontractingCode());
                if (!materialType1.getSubcontractingCode().contains(mtrClass.getMajorSubcontractingClassCode())) {
                    MajorSubcontractingClass aClass1 = iMajorSubcontractingClassService.initCode(mtrClass);
                    aClass.setMajorSubcontractingClassCode(aClass1.getMajorSubcontractingClassCode());
                }
                aClass.setMajorSubcontractingClassName(materialType1.getSubcontractingName());
                aClass.setMeasureUnit(materialType1.getUnit());
                aClass.setSubjectMatter(materialType1.getIsTransaction() == null ? 0 : Integer.parseInt(materialType1.getIsTransaction()));
                aClass.setSonId(materialType.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateTime(DateUtils.getNowDate());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                iMajorSubcontractingClassService.insertMajorSubcontractingClass(aClass);
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
    public synchronized int associationToMain(SubcontractingType materialType) {
        if (materialType.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialType.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        SubcontractingType materialType1 = this.selectSubcontractingTypeByIdNoChange(materialType.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialType.getHostId());
        return baseMapper.updateSubcontractingType(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(SubcontractingType materialType) {
        if (materialType.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        SubcontractingType materialType1 = this.selectSubcontractingTypeByIdNoChange(materialType.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return baseMapper.updateSubcontractingType(materialType1);
    }

    @Override
    public long selectSubcontractingTypeListCount(SubcontractingType subcontractingType) {
        return baseMapper.selectSubcontractingTypeListCount(subcontractingType);
    }

    @Override
    public int getMaterialJoin(Long id) {
        return baseMapper.getMaterialJoin(id);
    }


}
