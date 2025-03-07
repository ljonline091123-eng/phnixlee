package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.domain.SubcontractingEigenvalue;
import com.zhaocai.archives.dossier.domain.SubcontractingItem;
import com.zhaocai.archives.dossier.domain.SubcontractingType;
import com.zhaocai.archives.dossier.mapper.SubcontractingEigenvalueMapper;
import com.zhaocai.archives.dossier.mapper.SubcontractingItemMapper;
import com.zhaocai.archives.dossier.service.ISubcontractingItemService;
import com.zhaocai.archives.dossier.service.ISubcontractingTypeService;
import com.zhaocai.archives.main.domain.MajorSubcontractingClass;
import com.zhaocai.archives.main.domain.MajorSubcontractingFeature;
import com.zhaocai.archives.main.service.IMajorSubcontractingClassService;
import com.zhaocai.archives.main.service.IMajorSubcontractingFeatureService;
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
 * 专业分包特征项Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class SubcontractingItemServiceImpl extends ServiceImpl<SubcontractingItemMapper, SubcontractingItem> implements ISubcontractingItemService {
    @Autowired
    private SubcontractingItemMapper subcontractingItemMapper;

    @Autowired
    private SubcontractingEigenvalueMapper subcontractingEigenvalueMapper;

    @Resource
    private IMajorSubcontractingFeatureService iMajorSubcontractingFeatureService;

    @Resource
    private ISubcontractingTypeService iSubcontractingTypeService;

    @Resource
    private IMajorSubcontractingClassService iMajorSubcontractingClassService;

    /**
     * 查询专业分包特征项
     *
     * @param id 专业分包特征项主键
     * @return 专业分包特征项
     */
    @Override
    public SubcontractingItem selectSubcontractingItemById(Long id) {
        SubcontractingItem subcontractingItem = subcontractingItemMapper.selectSubcontractingItemById(id);
//        if ("N".equals(subcontractingItem.getIsMain())) {
//            subcontractingItem.setItemCode(subcontractingItem.getItemCode() + "-" + subcontractingItem.getOrganCode().substring(0, 4));
//        }
        return subcontractingItem;
    }

    /**
     * 查询专业分包特征项列表
     *
     * @param subcontractingItem 专业分包特征项
     * @return 专业分包特征项
     */
    @Override
    public List<SubcontractingItem> selectSubcontractingItemListNoChange(SubcontractingItem subcontractingItem) {
        return subcontractingItemMapper.selectSubcontractingItemList(subcontractingItem);
    }


    /**
     * 查询专业分包特征项列表
     *
     * @param subcontractingItem 专业分包特征项
     * @return 专业分包特征项
     */
    @Override
    public List<SubcontractingItem> selectSubcontractingItemList(SubcontractingItem subcontractingItem) {
        if (subcontractingItem == null) {
            subcontractingItem = new SubcontractingItem();
        }
        subcontractingItem.setDelFlag("0");
        List<SubcontractingItem> subcontractingItems = subcontractingItemMapper.selectSubcontractingItemList(subcontractingItem);
        subcontractingItems.forEach(item -> {
            if ("N".equals(item.getIsMain())) {
                item.setItemCode(item.getItemCode() + "-" + item.getOrganCode().substring(0, 4));
            }
        });
        Map<Long, Long> idsMap = new HashMap<>();
        if (MaterialType.ALL.equals(subcontractingItem.getQueryType()) || MaterialType.UNTREATED.equals(subcontractingItem.getQueryType())) {
            //查询未处理的项数据
            SubcontractingItem materialItem1 = new SubcontractingItem();
            materialItem1.setState(3L);
            materialItem1.setDelFlag("0");
            materialItem1.setIsMain("N");
            materialItem1.setMainId("0");
            materialItem1.setOrganCode(subcontractingItem.getOrganCode());
            List<SubcontractingItem> materialItems1 = this.selectSubcontractingItemListNoChange(materialItem1);
            materialItems1.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });

            SubcontractingEigenvalue eigenvalue = new SubcontractingEigenvalue();
            eigenvalue.setState(3L);
            eigenvalue.setDelFlag("0");
            eigenvalue.setIsMain("N");
            eigenvalue.setMainId("0");
            eigenvalue.setTypeId(subcontractingItem.getTypeId());
            eigenvalue.setOrganCode(subcontractingItem.getOrganCode());
            List<SubcontractingEigenvalue> list = subcontractingEigenvalueMapper.selectSubcontractingEigenvalueList(eigenvalue);
            list.forEach(item -> {
                idsMap.put(item.getItemId(), item.getItemId());
            });
        }
        if (MaterialType.ALL.equals(subcontractingItem.getQueryType()) || MaterialType.PROCESSED.equals(subcontractingItem.getQueryType())) {
            List<SubcontractingEigenvalue> eigenvalues = subcontractingEigenvalueMapper.getProcessed(subcontractingItem.getOrganCode());
            eigenvalues.forEach(item -> {
                idsMap.put(item.getItemId(), item.getItemId());
            });
            List<SubcontractingItem> processed = baseMapper.getProcessed(subcontractingItem.getOrganCode());
            //查询已处理的项数据
            processed.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (StringUtils.isEmpty(subcontractingItem.getQueryType())) {
            return subcontractingItems;
        } else {
            subcontractingItems.removeIf(item -> !idsMap.containsKey(item.getId()));
        }
        return subcontractingItems;
    }

    /**
     * 新增专业分包特征项
     *
     * @param subcontractingItem 专业分包特征项
     * @return 结果
     */
    @Override
    public synchronized int insertSubcontractingItem(SubcontractingItem subcontractingItem) {
        if (subcontractingItem == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(subcontractingItem.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (subcontractingItem.getTypeId() == null) {
            throw new RuntimeException("类型不能为空");
        }
        SubcontractingItem materialItem1 = new SubcontractingItem();
        materialItem1.setTypeId(subcontractingItem.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(subcontractingItem.getOrganCode());
        materialItem1.setItemCode(subcontractingItem.getItemCode());
        materialItem1.setIsMain("N");
        List<SubcontractingItem> materialItems = baseMapper.selectSubcontractingItemList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            throw new RuntimeException("特征编码已存在");
        }
        if (subcontractingItem.getId() == null) {
            subcontractingItem.setId(KeyUtils.generateId());
            subcontractingItem.setCreateId(SecurityUtils.getUserId());
            subcontractingItem.setCreateBy(SecurityUtils.getUsername());
            subcontractingItem.setCreateTime(DateUtils.getNowDate());
            subcontractingItem.setState(0L);
            subcontractingItem.setIsMain("N");
//            materialItem.setDeptId(SecurityUtils.getSysUser().getDeptId());
        }
        subcontractingItem.setCreateTime(DateUtils.getNowDate());
        return subcontractingItemMapper.insertSubcontractingItem(subcontractingItem);
    }

    /**
     * 修改专业分包特征项
     *
     * @param subcontractingItem 专业分包特征项
     * @return 结果
     */
    @Override
    public int updateSubcontractingItem(SubcontractingItem subcontractingItem) {
        if (subcontractingItem == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(subcontractingItem.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (subcontractingItem.getTypeId() == null) {
            throw new RuntimeException("类型不能为空");
        }
//        String qc = "-"+subcontractingItem.getOrganCode().substring(0, 4);
//        subcontractingItem.setItemCode(subcontractingItem.getItemCode().replace(qc, ""));
        SubcontractingItem materialItem1 = new SubcontractingItem();
        materialItem1.setTypeId(subcontractingItem.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(subcontractingItem.getOrganCode());
        materialItem1.setItemCode(subcontractingItem.getItemCode());
        materialItem1.setIsMain("N");
        List<SubcontractingItem> materialItems = baseMapper.selectSubcontractingItemList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            for (SubcontractingItem item : materialItems) {
                if (!subcontractingItem.getTypeId().equals(item.getTypeId())) {
                    throw new RuntimeException("特征编码已存在");
                }
            }
        }
        subcontractingItem.setUpdateTime(DateUtils.getNowDate());
        subcontractingItem.setUpdateId(SecurityUtils.getUserId());
        subcontractingItem.setUpdateBy(SecurityUtils.getUsername());
        subcontractingItem.setUpdateTime(DateUtils.getNowDate());
        return subcontractingItemMapper.updateSubcontractingItem(subcontractingItem);
    }

    /**
     * 批量删除专业分包特征项
     *
     * @param ids 需要删除的专业分包特征项主键
     * @return 结果
     */
    @Override
    public boolean deleteSubcontractingItemByIds(Long[] ids) {
        if (ids == null) {
            throw new RuntimeException("参数为空");
        }
        List<SubcontractingItem> materialItems = subcontractingItemMapper.selectBatchIds(Arrays.asList(ids));
        if (materialItems != null && !materialItems.isEmpty()) {
            for (SubcontractingItem materialItem : materialItems) {
                if ("Y".equals(materialItem.getIsMain())) {
                    throw new RuntimeException("主库数据不允许删除");
                }
                if (materialItem.getState() != 0L) {
                    throw new RuntimeException("流程中数据不允许删除");
                }
            }
            subcontractingEigenvalueMapper.updateDelByItemId(ids);
        }
        return this.removeBatchByIds(Arrays.asList(ids));
    }

    /**
     * 删除专业分包特征项信息
     *
     * @param id 专业分包特征项主键
     * @return 结果
     */
    @Override
    public int deleteSubcontractingItemById(Long id) {
        return subcontractingItemMapper.deleteSubcontractingItemById(id);
    }

    @Override
    public List<SubcontractingItem> initData(SubcontractingItem subcontractingItem) {
        List<SubcontractingItem> materialItems = subcontractingItemMapper.selectSubcontractingItemList(subcontractingItem);
        List<MajorSubcontractingFeature> mtrFeatures = iMajorSubcontractingFeatureService.selectMajorSubcontractingFeatureList(null);
        SubcontractingType type1 = new SubcontractingType();
        type1.setOrganCode(subcontractingItem.getOrganCode());
        type1.setIsMain("Y");
        List<SubcontractingType> materialTypes = iSubcontractingTypeService.selectSubcontractingTypeList(type1);
        if (materialTypes == null || materialTypes.isEmpty()) {
            throw new RuntimeException("请先配置类型");
        }
        Map<String, Long> params = new HashMap<>();
        materialTypes.forEach(item -> {
            if ("Y".equals(item.getIsMain())) {
                params.put(item.getHostId(), item.getId());
            }
        });
        Map<String, MajorSubcontractingFeature> mtrMap = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(mtrFeature -> {
                mtrMap.put(mtrFeature.getId(), mtrFeature);
            });
        }
        if (materialItems == null || materialItems.isEmpty()) {
            //初始化-最开始无数据情况
            if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
                mtrFeatures.forEach(mtrFeature -> {
                    SubcontractingItem bean = new SubcontractingItem();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeature.getMajorSubcontractingClassId()));
                    bean.setItemName(mtrFeature.getFeatureName());
                    bean.setItemCode(mtrFeature.getFeatureCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(subcontractingItem.getOrganCode());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
                    bean.setHostId(mtrFeature.getId());
                    materialItems.add(bean);
                });
                this.saveBatch(materialItems);
            }
        } else {
            //有数据的情况，对比主库数据，新增主库存在副库不存在的数据，且非本副库新增至主库数据。（关联数据是否还需要将主库数据同步到副库）
            Map<String, Long> items = new HashMap<>();
            materialItems.forEach(item -> {
                if ("Y".equals(item.getIsMain())) {
                    items.put(item.getHostId(), item.getId());
                }
            });
            List<SubcontractingItem> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!items.containsKey(key)) {
                    MajorSubcontractingFeature mtrFeature = mtrMap.get(key);
                    SubcontractingItem bean = new SubcontractingItem();
                    bean.setId(KeyUtils.generateId());
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeature.getMajorSubcontractingClassId()));
                    bean.setItemName(mtrFeature.getFeatureName());
                    bean.setItemCode(mtrFeature.getFeatureCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(subcontractingItem.getOrganCode());
                    bean.setHostId(mtrFeature.getId());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
                    bean.setDelFlag(mtrFeature.getValid() + "");
                    materialItems.add(bean);
                    addList.add(bean);
                }
            }
            if (!addList.isEmpty()) {
                this.saveBatch(addList);
            }
        }
        return materialItems;
    }


    @Override
    public void addTypeByMain(MajorSubcontractingFeature mtrFeature) {
        SubcontractingType materialType = new SubcontractingType();
        materialType.setHostId(mtrFeature.getMajorSubcontractingClassId());
        List<SubcontractingType> materialTypes = iSubcontractingTypeService.selectSubcontractingTypeList(materialType);
        //已存在数据排除
        SubcontractingItem type1 = new SubcontractingItem();
        type1.setHostId(mtrFeature.getId());
        type1.setDelFlag("0");
        List<SubcontractingItem> materialTypes1 = baseMapper.selectSubcontractingItemList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getTypeId());
            });
        }
        List<SubcontractingItem> addList = new ArrayList<>();
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (SubcontractingType type : materialTypes) {
                if (map.containsKey(type.getOrganCode()) && (type.getId() + "").equals(map.get(type.getOrganCode()) + "")) {
                    continue;
                }
                SubcontractingItem bean = new SubcontractingItem();
                bean.setId(KeyUtils.generateId());
                bean.setTypeId(type.getId());
                bean.setItemName(mtrFeature.getFeatureName());
                bean.setItemCode(mtrFeature.getFeatureCode());
                bean.setCreateId(SecurityUtils.getUserId());
                bean.setIsMain("Y");
                bean.setState(3L);
                bean.setOrganCode(type.getOrganCode());
                bean.setCreateBy(SecurityUtils.getUsername());
                bean.setCreateTime(DateUtils.getNowDate());
                bean.setHostId(mtrFeature.getId());
                addList.add(bean);
            }
        }
        if (!addList.isEmpty()) {
            this.saveBatch(addList);
        }

    }

    @Override
    public void updateByHostId(MajorSubcontractingFeature mtrFeature) {
        SubcontractingItem item = new SubcontractingItem();
        item.setHostId(mtrFeature.getId());
        item.setDelFlag("0");
        List<SubcontractingItem> materialItems = baseMapper.selectSubcontractingItemList(item);
        if (materialItems != null && !materialItems.isEmpty()) {
            materialItems.forEach(bean -> {
                bean.setItemName(mtrFeature.getFeatureName());
                bean.setItemCode(mtrFeature.getFeatureCode());
            });
        }
        this.updateBatchById(materialItems);
    }


    @Override
    public void deleteByHostId(String[] histIds, Map<Long, Long> idsMap) {
        if (histIds != null) {
            QueryWrapper<SubcontractingItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<SubcontractingItem> materialTypes = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<SubcontractingItem> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main","Y");
            materialTypes.addAll(this.list(qw));
            List<SubcontractingItem> newDeviceDetails = new ArrayList<>();
            List<SubcontractingItem> upDeviceDetails = new ArrayList<>();
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
            List<Long> collect = newDeviceDetails.stream().map(SubcontractingItem::getId).collect(Collectors.toList());
            this.removeBatchByIds(collect);
        }
    }


    /**
     * 新增至主库
     *
     * @param materialItem
     * @return
     */
    @Override
    public synchronized int addToMain(SubcontractingItem materialItem) {
        if (materialItem.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialItem.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        SubcontractingItem materialType1 = this.selectSubcontractingItemById(materialItem.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        MajorSubcontractingClass mtrClass = iMajorSubcontractingClassService.selectMajorSubcontractingClassById(materialItem.getHostId());
        //查询编码是否已在主库存在
        MajorSubcontractingFeature mtrClass1 = new MajorSubcontractingFeature();
        mtrClass1.setMajorSubcontractingClassId(materialItem.getHostId());
        mtrClass1.setValid(0L);
        mtrClass1.setFeatureCode(materialType1.getItemCode());
        if (!StringUtils.isEmpty(materialItem.getItemCode())) {
            mtrClass1.setFeatureCode(materialItem.getItemCode());
        }
        List<MajorSubcontractingFeature> mtrClasses = iMajorSubcontractingFeatureService.selectMajorSubcontractingFeatureList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            return -1;
        } else {
            if (!StringUtils.isEmpty(materialItem.getItemCode())) {
                MajorSubcontractingFeature aClass = new MajorSubcontractingFeature();
                aClass.setId(key);
                aClass.setMajorSubcontractingClassId(mtrClass.getId());
                aClass.setFeatureCode(materialItem.getItemCode());
                aClass.setFeatureName(materialType1.getItemName());
                aClass.setSonId(materialType1.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateTime(DateUtils.getNowDate());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                iMajorSubcontractingFeatureService.insertMajorSubcontractingFeature(aClass);
                materialType1.setMainId(aClass.getId());
                materialType1.setIsMain("Y");
                return baseMapper.updateSubcontractingItem(materialType1);
            } else {
                //不存在时 新增至主库
                materialType1.setIsMain("Y");
                materialType1.setHostId(key);
                int i = baseMapper.updateSubcontractingItem(materialType1);
                if (i > 0) {
                    MajorSubcontractingFeature aClass = new MajorSubcontractingFeature();
                    aClass.setId(key);
                    aClass.setMajorSubcontractingClassId(mtrClass.getId());
                    aClass.setFeatureCode(materialType1.getItemCode());
                    aClass.setFeatureName(materialType1.getItemName());
                    aClass.setSonId(materialType1.getId());
                    aClass.setCreateBy(SecurityUtils.getUsername());
                    aClass.setCreateTime(DateUtils.getNowDate());
                    aClass.setCreateId(SecurityUtils.getUserId() + "");
                    iMajorSubcontractingFeatureService.insertMajorSubcontractingFeature(aClass);
                }
                return i;
            }
        }
    }


    /**
     * 关联至主库
     *
     * @param materialItem
     * @return
     */
    @Override
    public synchronized int associationToMain(SubcontractingItem materialItem) {
        if (materialItem.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialItem.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        SubcontractingItem materialType1 = this.selectSubcontractingItemById(materialItem.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialItem.getHostId());
        return baseMapper.updateSubcontractingItem(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialItem
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(SubcontractingItem materialItem) {
        if (materialItem.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        SubcontractingItem materialType1 = this.selectSubcontractingItemById(materialItem.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return baseMapper.updateSubcontractingItem(materialType1);
    }

    @Override
    public long selectSubcontractingItemListCount(SubcontractingItem subcontractingItem) {
        return baseMapper.selectSubcontractingItemListCount(subcontractingItem);
    }

    @Override
    public List<SubcontractingItem> getProcessed(String organCode) {
        return baseMapper.getProcessed(organCode);
    }


}
