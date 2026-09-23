package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.MaterialDetails;
import com.zhaocai.archives.dossier.domain.MaterialEigenvalue;
import com.zhaocai.archives.dossier.domain.MaterialItem;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.mapper.MaterialTypeMapper;
import com.zhaocai.archives.dossier.service.IMaterialDetailsService;
import com.zhaocai.archives.dossier.service.IMaterialEigenvalueService;
import com.zhaocai.archives.dossier.service.IMaterialItemService;
import com.zhaocai.archives.dossier.service.IMaterialTypeService;
import com.zhaocai.archives.dossier.tree.MaterialTypeTree;
import com.zhaocai.archives.main.domain.MtrClass;
import com.zhaocai.archives.main.service.IMtrArchivesService;
import com.zhaocai.archives.main.service.IMtrClassService;
import com.zhaocai.archives.main.service.IMtrFeatureService;
import com.zhaocai.archives.main.service.IMtrFeatureValueService;
import com.zhaocai.archives.utils.KeyUtils;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.PageUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 材料分类Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class MaterialTypeServiceImpl extends ServiceImpl<MaterialTypeMapper, MaterialType> implements IMaterialTypeService {
    @Autowired
    private MaterialTypeMapper materialTypeMapper;

    @Resource
    private IMtrClassService mtrClassService;

    @Resource
    private IMtrFeatureService iMtrFeatureService;

    @Resource
    private IMtrFeatureValueService iMtrFeatureValueService;

    @Resource
    private IMtrArchivesService iMtrArchivesService;

    @Resource
    private IMaterialItemService materialItemService;

    @Resource
    private IMaterialEigenvalueService iMaterialEigenvalueService;

    @Resource
    private IMaterialDetailsService iMaterialDetailsService;

    @Resource
    private RemoteSystemService remoteSystemService;

    /**
     * 查询材料分类
     *
     * @param id 材料分类主键
     * @return 材料分类
     */
    @Override
    public MaterialType selectMaterialTypeById(Long id) {
        MaterialType materialType = materialTypeMapper.selectMaterialTypeById(id);
        if (materialType == null) {
            return null;
        }
        if (materialType.getUpId() != null && materialType.getUpId() != 0) {
            MaterialType deviceType1 = materialTypeMapper.selectMaterialTypeById(materialType.getUpId());
            materialType.setBelongingLevel(deviceType1.getMaterialName());
        } else {
            materialType.setBelongingLevel("顶级");
        }
        if ("N".equals(materialType.getIsMain())) {
            materialType.setMaterialCode(materialType.getMaterialCode() + "-" + materialType.getOrganCode().substring(0, 4));
        }
        return materialType;
    }


    /**
     * 查询材料分类
     *
     * @param id 材料分类主键
     * @return 材料分类
     */
    @Override
    public MaterialType selectMaterialTypeByIdNoChange(Long id) {
        return materialTypeMapper.selectMaterialTypeById(id);
    }

    /**
     * 查询材料分类列表
     *
     * @param materialType 材料分类
     * @return 材料分类
     */
    @Override
    public List<MaterialType> selectMaterialTypeList(MaterialType materialType) {
        return materialTypeMapper.selectMaterialTypeList(materialType);
    }


    /**
     * 查询材料分类列表
     *
     * @param materialType 材料分类
     * @return 材料分类
     */
    @Override
    public List<MaterialType> initData(MaterialType materialType) {
        List<MaterialType> materialTypes = materialTypeMapper.selectMaterialTypeList(materialType);
        List<MtrClass> mtrClasses = mtrClassService.selectMtrClassList(null);
        Map<String, Long> map = new HashMap<>();
        Map<String, MtrClass> mtrMap = new HashMap<>();
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
                    MaterialType type = new MaterialType();
                    type.setId(map.get(mtrClass.getId()));
                    type.setUpId(map.get(mtrClass.getParentId()));
                    type.setMaterialType(mtrClass.getMtrClassType());
                    type.setMaterialCode(mtrClass.getMtrClassCode());
                    type.setMaterialName(mtrClass.getMtrClassName());
                    type.setMaterialLevel(mtrClass.getClassLevel());
                    type.setMaterialLevelCd(mtrClass.getClassLevelCd());
                    type.setUnit(mtrClass.getMeasureUnit());
                    type.setIsTransaction(mtrClass.getSubjectMatter() + "");
                    type.setOrganCode(materialType.getOrganCode());
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
            List<MaterialType> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!params.containsKey(key)) {
                    MtrClass mtrClass = mtrMap.get(key);
                    MaterialType type = new MaterialType();
                    type.setId(map.get(mtrClass.getId()));
                    if (params.containsKey(mtrClass.getParentId())) {
                        type.setUpId(params.get(mtrClass.getParentId()));
                    } else {
                        type.setUpId(map.get(mtrClass.getParentId()));
                    }
                    type.setMaterialType(mtrClass.getMtrClassType());
                    type.setMaterialCode(mtrClass.getMtrClassCode());
                    type.setMaterialName(mtrClass.getMtrClassName());
                    type.setMaterialLevel(mtrClass.getClassLevel());
                    type.setMaterialLevelCd(mtrClass.getClassLevelCd());
                    type.setUnit(mtrClass.getMeasureUnit());
                    type.setIsTransaction(mtrClass.getSubjectMatter() + "");
                    type.setOrganCode(materialType.getOrganCode());
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


    /**
     * 新增材料分类
     *
     * @param materialType 材料分类
     * @return 结果
     */
    @Override
    public synchronized int insertMaterialType(MaterialType materialType) {
        if (materialType == null) {
            throw new RuntimeException("参数不能为空");
        }
        if (StringUtils.isEmpty(materialType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        QueryWrapper<MaterialType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("organ_code", materialType.getOrganCode());
        queryWrapper.eq("material_code", materialType.getMaterialCode());
        queryWrapper.eq("del_flag", "0");
        List<MaterialType> list = this.list(queryWrapper);
        if (list != null && !list.isEmpty()) {
            throw new RuntimeException("材料分类编码已存在,请刷新后重试");
        }
        materialType.setState(0L);
        materialType.setCreateTime(DateUtils.getNowDate());
        return materialTypeMapper.insertMaterialType(materialType);
    }

    /**
     * 主库修改同步修改副库
     *
     * @param mtrClass
     */
    @Override
    public void updateByHostId(MtrClass mtrClass) {
        MaterialType materialType = new MaterialType();
        materialType.setHostId(mtrClass.getId());
        materialType.setDelFlag("0");
        List<MaterialType> materialTypes = baseMapper.selectMaterialTypeList(materialType);
        if (materialTypes != null && !materialTypes.isEmpty()) {
            materialTypes.forEach(type -> {
                type.setMaterialType(mtrClass.getMtrClassType());
                type.setMaterialCode(mtrClass.getMtrClassCode());
                type.setMaterialName(mtrClass.getMtrClassName());
                type.setMaterialLevel(mtrClass.getClassLevel());
                type.setMaterialLevelCd(mtrClass.getClassLevelCd());
                type.setUnit(mtrClass.getMeasureUnit());
                type.setIsTransaction(mtrClass.getSubjectMatter() + "");
            });
        }
        this.updateBatchById(materialTypes);
    }


    @Override
    public void deleteByHostId(String[] histIds, Map<Long, Long> idsMap) {
        if (histIds != null) {
            QueryWrapper<MaterialType> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<MaterialType> materialTypes = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<MaterialType> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main", "Y");
            materialTypes.addAll(this.list(qw));
            List<MaterialType> newDeviceDetails = new ArrayList<>();
            List<MaterialType> upDeviceDetails = new ArrayList<>();
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
            List<Long> collect = newDeviceDetails.stream().map(MaterialType::getId).collect(Collectors.toList());
            this.removeBatchByIds(collect);
        }
    }

    /**
     * 修改材料分类
     *
     * @param materialType 材料分类
     * @return 结果
     */
    @Override
    public int updateMaterialType(MaterialType materialType) {
        if (materialType == null) {
            throw new RuntimeException("参数不能为空");
        }
        if (StringUtils.isEmpty(materialType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        String qc = "-" + materialType.getOrganCode().substring(0, 4);
        materialType.setMaterialCode(materialType.getMaterialCode().replace(qc, ""));
        QueryWrapper<MaterialType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("organ_code", materialType.getOrganCode());
        queryWrapper.eq("material_code", materialType.getMaterialCode());
        queryWrapper.eq("del_flag", "0");
        queryWrapper.eq("is_main", "N");
        List<MaterialType> list = this.list(queryWrapper);
        if (list != null && !list.isEmpty()) {
            for (MaterialType type : list) {
                if (!type.getId().equals(materialType.getId())) {
                    throw new RuntimeException("材料分类编码已存在,请刷新后重试");
                }
            }
        }
        materialType.setUpdateTime(DateUtils.getNowDate());
        return materialTypeMapper.updateMaterialType(materialType);
    }

    /**
     * 批量删除材料分类
     *
     * @param ids 需要删除的材料分类主键
     * @return 结果
     */
    @Override
    public boolean deleteMaterialTypeByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new RuntimeException("参数不能为空");
        }
        List<MaterialType> materialTypes = materialTypeMapper.selectBatchIds(Arrays.asList(ids));
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (MaterialType type : materialTypes) {
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
     * 删除材料分类信息
     *
     * @param id 材料分类主键
     * @return 结果
     */
    @Override
    public int deleteMaterialTypeById(Long id) {
        return materialTypeMapper.deleteMaterialTypeById(id);
    }

    @Override
    public List<MaterialTypeTree> getMaterialTypeTree(MaterialType materialType) {
        MaterialType typex = new MaterialType();
        typex.setOrganCode(materialType.getOrganCode());
        typex.setDelFlag("0");
        PageUtils.clearPage();
        List<MaterialType> select = baseMapper.selectMaterialTypeList(typex);
        Map<Long, Long> typeMap = new HashMap<>();
        select.forEach(item -> {
            typeMap.put(item.getId(), item.getUpId());
        });
        Map<Long, Long> idsMap = new HashMap<>();
        Map<Long, MaterialTypeTree> map = new HashMap<>();
        if (MaterialType.ALL.equals(materialType.getQueryType()) || MaterialType.UNTREATED.equals(materialType.getQueryType())) {
            //查询未处理的类型数据
            MaterialType type = new MaterialType();
            type.setState(3L);
            type.setDelFlag("0");
            type.setIsMain("N");
            type.setMainId("0");
            type.setOrganCode(materialType.getOrganCode());
            List<MaterialType> materialTypes = this.selectMaterialTypeList(type);
            materialTypes.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
            //查询未处理的值数据
            MaterialEigenvalue eigenvalue = new MaterialEigenvalue();
            eigenvalue.setState(3L);
            eigenvalue.setDelFlag("0");
            eigenvalue.setIsMain("N");
            eigenvalue.setMainId("0");
            eigenvalue.setOrganCode(materialType.getOrganCode());
            List<MaterialEigenvalue> list = iMaterialEigenvalueService.selectMaterialEigenvalueList(eigenvalue);
            list.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
            //查询未处理的项数据
            MaterialItem materialItem = new MaterialItem();
            materialItem.setState(3L);
            materialItem.setDelFlag("0");
            materialItem.setIsMain("N");
            materialItem.setMainId("0");
            materialItem.setOrganCode(materialType.getOrganCode());
            List<MaterialItem> materialItems = materialItemService.selectMaterialItemListNoChange(materialItem);
            materialItems.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
            //查询未处理的详情数据
            MaterialDetails materialDetails = new MaterialDetails();
            materialDetails.setState(3L);
            materialDetails.setDelFlag("0");
            materialDetails.setIsMain("N");
            materialDetails.setMainId("0");
            materialDetails.setOrganCode(materialType.getOrganCode());
            List<MaterialDetails> materialDetails1 = iMaterialDetailsService.selectMaterialDetailsListNoChange(materialDetails);
            materialDetails1.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
        }

        if (MaterialType.ALL.equals(materialType.getQueryType()) || MaterialType.PROCESSED.equals(materialType.getQueryType())) {

            List<MaterialItem> materialItems = materialItemService.getProcessed(materialType.getOrganCode());
            materialItems.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });

            List<MaterialEigenvalue> eigenvalues = iMaterialEigenvalueService.getProcessed(materialType.getOrganCode());
            eigenvalues.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });

            List<MaterialDetails> details = iMaterialDetailsService.getProcessed(materialType.getOrganCode());
            details.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });

            List<MaterialType> materialTypes = baseMapper.getProcessed(materialType.getOrganCode());
            materialTypes.forEach(item -> {
                this.xhtq(item, idsMap, typeMap);
            });
        }

        select.forEach(item -> {
            if (StringUtils.isEmpty(materialType.getQueryType()) || idsMap.containsKey(item.getId())) {
                MaterialTypeTree materialTypeTree = new MaterialTypeTree();
                materialTypeTree.setId(item.getId() + "");
                materialTypeTree.setLabel(item.getMaterialName());
                materialTypeTree.setType(item.getMaterialType());
                materialTypeTree.setCode(item.getMaterialCode());
                if ("N".equals(item.getIsMain())) {
                    materialTypeTree.setCode(item.getMaterialCode() + "-" + item.getOrganCode().substring(0, 4));
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
        List<MaterialTypeTree> list = new ArrayList<>();
        for (MaterialType type : select) {
            MaterialTypeTree treeVo = map.get(type.getId());
            if (treeVo != null) {
                if (type.getUpId() == null) {
                    // 根节点，直接添加
                    list.add(treeVo);
                } else {
                    // 非根节点，找到父节点并添加到其子节点列表中
                    MaterialTypeTree materialTypeTree = map.get(type.getUpId());
                    if (materialTypeTree != null) {
                        materialTypeTree.getChildren().add(treeVo);
                    }
                }
            }
        }
        return list;
    }


    private void xhtq(MaterialItem item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
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

    private void xhtq(MaterialType item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
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

    private void xhtq(MaterialEigenvalue item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
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

    private void xhtq(MaterialDetails item, Map<Long, Long> idsMap, Map<Long, Long> typeMap) {
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
    public MaterialType initMaterialType(MaterialType materialType) {
        if (StringUtils.isEmpty(materialType.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (materialType.getId() == null) {
            throw new RuntimeException("id不能为空");
        }
        MaterialType type = this.getById(materialType.getId());
        if (type == null) {
            throw new RuntimeException("类型不存在");
        }
        String materialCode = type.getMaterialCode();
        Integer maxCode = baseMapper.getMaxCode(materialCode, materialType.getId(), materialType.getOrganCode());
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
                    Integer reuseTowCode = baseMapper.getReuseTowCode(materialCode, materialType.getId(), materialType.getOrganCode(), 999);
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
                    Integer reuseTowCode = baseMapper.getReuseTowCode(materialCode, materialType.getId(), materialType.getOrganCode(), 99);
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
        MaterialType materialType1 = new MaterialType();
        materialType1.setId(KeyUtils.generateId());
        materialType1.setUpId(materialType.getId());
        materialType1.setMaterialCode(materialCode);
        materialType1.setState(0L);
        materialType1.setIsMain("N");
        try {
            if (!StringUtils.isEmpty(type.getMaterialLevelCd())) {
                int cj = Integer.parseInt(type.getMaterialLevelCd()) + 1;
                materialType1.setMaterialLevelCd(cj + "");
                materialType1.setMaterialLevel(this.getLevel(cj + ""));
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
    public void addMaterialTypeByMain(MtrClass mtrClass) {
        MaterialType materialType = new MaterialType();
        materialType.setHostId(mtrClass.getParentId());
        List<MaterialType> materialTypes = baseMapper.selectMaterialTypeList(materialType);
        //已存在数据排除
        MaterialType type1 = new MaterialType();
        type1.setHostId(mtrClass.getId());
        type1.setDelFlag("0");
        List<MaterialType> materialTypes1 = baseMapper.selectMaterialTypeList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getUpId());
            });
        }
        List<MaterialType> list = new ArrayList<>();
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (MaterialType type : materialTypes) {
                if (map.containsKey(type.getOrganCode()) && (type.getId() + "").equals(map.get(type.getOrganCode()) + "")) {
                    continue;
                }
                MaterialType bean = new MaterialType();
                bean.setId(KeyUtils.generateId());
                bean.setUpId(type.getId());
                bean.setMaterialType(mtrClass.getMtrClassType());
                bean.setMaterialCode(mtrClass.getMtrClassCode());
                bean.setMaterialName(mtrClass.getMtrClassName());
                bean.setMaterialLevel(mtrClass.getClassLevel());
                bean.setMaterialLevelCd(mtrClass.getClassLevelCd());
                bean.setUnit(mtrClass.getMeasureUnit());
                bean.setIsTransaction(mtrClass.getSubjectMatter() + "");
                bean.setOrganCode(type.getOrganCode());
                bean.setHostId(mtrClass.getId());
                bean.setIsMain("Y");
                bean.setState(3L);
                bean.setCreateId(SecurityUtils.getUserId());
                bean.setCreateBy(SecurityUtils.getUsername());
                bean.setCreateTime(DateUtils.getNowDate());
                bean.setDelFlag(mtrClass.getValid() + "");
                list.add(bean);
            }
        }
        if (!list.isEmpty()) {
            this.saveBatch(list);
        }
    }


    @Override
    public List<MaterialTypeTree> getDeptTree() {
        SysDept dept = new SysDept();
        dept.setThridOrgLevel(1);
        List<SysDept> sysDepts = remoteSystemService.selectDeptList(dept, SecurityConstants.INNER);
        dept.setThridOrgLevel(2);
        sysDepts.addAll(remoteSystemService.selectDeptList(dept, SecurityConstants.INNER));
        Map<Long, MaterialTypeTree> map = new HashMap<>();
        sysDepts.forEach(item -> {
            MaterialTypeTree materialTypeTree = new MaterialTypeTree();
            materialTypeTree.setId(item.getDeptId() + "");
//            materialTypeTree.setLabel(item.getDeptName());
            materialTypeTree.setLabel(item.getSimpleName());
            materialTypeTree.setType(item.getThridOrgLevel() + "");
            materialTypeTree.setCode(item.getThridDeptId());
            materialTypeTree.setChildren(new ArrayList<>());
            map.put(item.getDeptId(), materialTypeTree);
        });

        // 构建树形结构
        List<MaterialTypeTree> list = new ArrayList<>();
        for (SysDept type : sysDepts) {
            MaterialTypeTree treeVo = map.get(type.getDeptId());
            if (type.getParentId() == null || "0".equals(type.getParentId() + "")) {
                // 根节点，直接添加
                list.add(treeVo);
            } else {
                // 非根节点，找到父节点并添加到其子节点列表中
                MaterialTypeTree materialTypeTree = map.get(type.getParentId());
                if (materialTypeTree != null) {
                    materialTypeTree.getChildren().add(treeVo);
                }
            }
        }
        return list;
    }

    /**
     * 新增至主库
     *
     * @param materialType
     * @return
     */
    @Override
    public synchronized int addToMain(MaterialType materialType) {
        if (materialType.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialType.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        MaterialType materialType1 = this.selectMaterialTypeByIdNoChange(materialType.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        MtrClass mtrClass = mtrClassService.selectMtrClassById(materialType.getHostId());
        //查询编码是否已在主库存在
        MtrClass mtrClass1 = new MtrClass();
        mtrClass1.setMtrClassCode(materialType1.getMaterialCode());
        mtrClass1.setValid(0L);
        List<MtrClass> mtrClasses = mtrClassService.selectMtrClassList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        boolean pd = true;
        MaterialType materialType2 = this.selectMaterialTypeByIdNoChange(materialType1.getUpId());
        if (materialType2 != null && mtrClass.getMtrClassCode().equals(materialType2.getMaterialCode())) {
            pd = false;
        }
        if ((mtrClasses != null && !mtrClasses.isEmpty()) || pd) {
            //已存在时 新增改为关联
            MtrClass aClass = mtrClassService.initCode(mtrClass);
            aClass.setMtrClassName(materialType1.getMaterialName());
            aClass.setMeasureUnit(materialType1.getUnit());
            aClass.setSubjectMatter(materialType1.getIsTransaction() == null ? 0 : Integer.parseInt(materialType1.getIsTransaction()));
            aClass.setSonId(materialType.getId());
            mtrClassService.insertMtrClass(aClass);
            materialType1.setMainId(aClass.getId());
            materialType1.setIsMain("Y");
            return materialTypeMapper.updateMaterialType(materialType1);
        } else {
            //不存在时 新增至主库
            materialType1.setIsMain("Y");
            materialType1.setHostId(key);
            int i = materialTypeMapper.updateMaterialType(materialType1);
            if (i > 0) {
                MtrClass aClass = new MtrClass();
                aClass.setId(key);
                aClass.setParentId(materialType.getHostId());
                aClass.setMtrClassCode(materialType1.getMaterialCode());
                if (!materialType1.getMaterialCode().contains(mtrClass.getMtrClassCode())) {
                    MtrClass aClass1 = mtrClassService.initCode(mtrClass);
                    aClass.setMtrClassCode(aClass1.getMtrClassCode());
                }
                aClass.setMtrClassName(materialType1.getMaterialName());
                aClass.setMeasureUnit(materialType1.getUnit());
                aClass.setSubjectMatter(materialType1.getIsTransaction() == null ? 0 : Integer.parseInt(materialType1.getIsTransaction()));
                aClass.setSonId(materialType.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateTime(DateUtils.getNowDate());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                mtrClassService.insertMtrClass(aClass);
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
    public synchronized int associationToMain(MaterialType materialType) {
        if (materialType.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialType.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        MaterialType materialType1 = this.selectMaterialTypeByIdNoChange(materialType.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialType.getHostId());
        return materialTypeMapper.updateMaterialType(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialType
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(MaterialType materialType) {
        if (materialType.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        MaterialType materialType1 = this.selectMaterialTypeByIdNoChange(materialType.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return materialTypeMapper.updateMaterialType(materialType1);
    }

    /**
     * 根据code查询判断是否为二级单位，如果是二级单位，则返回二级单位编码且可编辑，否则不可编辑。
     *
     * @param orgCode
     * @return
     */
    @Override
    public Map<String, String> getSecondaryUnit(String orgCode) {
        Map<String, String> map = new HashMap<>();
        SysDept dept = remoteSystemService.getByThridDeptId(orgCode, SecurityConstants.INNER);
        if (dept != null) {
            if (dept.getThridOrgLevel() != null && dept.getThridOrgLevel() <= 2) {
                map.put("editable", "Y");
                map.put("organCode", orgCode);
            } else {
                map.put("editable", "N");
                SysDept sysDept = remoteSystemService.getTwoLevelDeptByDeptId(dept.getDeptId(), SecurityConstants.INNER);
                map.put("organCode", sysDept.getThridDeptId());
            }
        } else {
            throw new BusinessException("未查询到组织机构信息");
        }
        return map;
    }

    @Override
    public long selectMaterialTypeListCount(MaterialType materialType) {
        return baseMapper.selectMaterialTypeListCount(materialType);
    }

    @Override
    public int getMaterialJoin(Long id) {
        return baseMapper.getMaterialJoin(id);
    }

    @Override
    public void updateMaterialTypeNoChange(MaterialType materialType) {
        baseMapper.updateMaterialType(materialType);
    }


}
