package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.MaterialEigenvalue;
import com.zhaocai.archives.dossier.domain.MaterialItem;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.mapper.MaterialEigenvalueMapper;
import com.zhaocai.archives.dossier.mapper.MaterialItemMapper;
import com.zhaocai.archives.dossier.service.IMaterialEigenvalueService;
import com.zhaocai.archives.dossier.service.IMaterialItemService;
import com.zhaocai.archives.dossier.service.IMaterialTypeService;
import com.zhaocai.archives.main.domain.MtrClass;
import com.zhaocai.archives.main.domain.MtrFeature;
import com.zhaocai.archives.main.service.IMtrClassService;
import com.zhaocai.archives.main.service.IMtrFeatureService;
import com.zhaocai.archives.main.service.IMtrFeatureValueService;
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
 * 材料特征项Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class MaterialItemServiceImpl extends ServiceImpl<MaterialItemMapper, MaterialItem> implements IMaterialItemService {
    @Autowired
    private MaterialItemMapper materialItemMapper;

    @Autowired
    private MaterialEigenvalueMapper materialEigenvalueMapper;

    @Resource
    private IMaterialTypeService materialTypeService;

    @Resource
    private IMtrFeatureService iMtrFeatureService;

    @Resource
    private IMtrClassService mtrClassService;

    @Resource
    private IMaterialEigenvalueService iMaterialEigenvalueService;

    @Resource
    private IMtrFeatureValueService iMtrFeatureValueService;

    /**
     * 查询材料特征项
     *
     * @param id 材料特征项主键
     * @return 材料特征项
     */
    @Override
    public MaterialItem selectMaterialItemById(Long id) {
        MaterialItem item = materialItemMapper.selectMaterialItemById(id);
//        if ("N".equals(item.getIsMain())) {
//            item.setItemCode(item.getItemCode() + "-" + item.getOrganCode().substring(0, 4));
//        }
        return item;
    }

    /**
     * 查询材料特征项列表
     *
     * @param materialItem 材料特征项
     * @return 材料特征项
     */
    @Override
    public List<MaterialItem> selectMaterialItemListNoChange(MaterialItem materialItem) {
        return materialItemMapper.selectMaterialItemList(materialItem);
    }


    /**
     * 查询材料特征项列表
     *
     * @param materialItem 材料特征项
     * @return 材料特征项
     */
    @Override
    public List<MaterialItem> selectMaterialItemList(MaterialItem materialItem) {
        if (materialItem == null) {
            materialItem = new MaterialItem();
        }
        materialItem.setDelFlag("0");
        List<MaterialItem> materialItems = materialItemMapper.selectMaterialItemList(materialItem);
        materialItems.forEach(materialItem1 -> {
            if ("N".equals(materialItem1.getIsMain())) {
                materialItem1.setItemCode(materialItem1.getItemCode() + "-" + materialItem1.getOrganCode().substring(0, 4));
            }
        });
        Map<Long, Long> idsMap = new HashMap<>();
        if (MaterialType.ALL.equals(materialItem.getQueryType()) || MaterialType.UNTREATED.equals(materialItem.getQueryType())) {
            //查询未处理的项数据
            MaterialItem materialItem1 = new MaterialItem();
            materialItem1.setState(3L);
            materialItem1.setDelFlag("0");
            materialItem1.setIsMain("N");
            materialItem1.setMainId("0");
            materialItem1.setOrganCode(materialItem.getOrganCode());
            List<MaterialItem> materialItems1 = this.selectMaterialItemListNoChange(materialItem1);
            materialItems1.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });

            MaterialEigenvalue eigenvalue = new MaterialEigenvalue();
            eigenvalue.setState(3L);
            eigenvalue.setDelFlag("0");
            eigenvalue.setIsMain("N");
            eigenvalue.setMainId("0");
            eigenvalue.setTypeId(materialItem.getTypeId());
            eigenvalue.setOrganCode(materialItem.getOrganCode());
            List<MaterialEigenvalue> list = iMaterialEigenvalueService.selectMaterialEigenvalueList(eigenvalue);
            list.forEach(item -> {
                idsMap.put(item.getItemId(), item.getItemId());
            });
        }
        if (MaterialType.ALL.equals(materialItem.getQueryType()) || MaterialType.PROCESSED.equals(materialItem.getQueryType())) {
            List<MaterialEigenvalue> eigenvalues = iMaterialEigenvalueService.getProcessed(materialItem.getOrganCode());
            eigenvalues.forEach(item -> {
                idsMap.put(item.getItemId(), item.getItemId());
            });
            List<MaterialItem> processed = baseMapper.getProcessed(materialItem.getOrganCode());
            //查询已处理的项数据
            processed.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (StringUtils.isEmpty(materialItem.getQueryType())) {
            return materialItems;
        } else {
            materialItems.removeIf(item -> !idsMap.containsKey(item.getId()));
        }
        return materialItems;
    }

    /**
     * 新增材料特征项
     *
     * @param materialItem 材料特征项
     * @return 结果
     */
    @Override
    public synchronized int insertMaterialItem(MaterialItem materialItem) {
        if (materialItem == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(materialItem.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (materialItem.getTypeId() == null) {
            throw new RuntimeException("类型不能为空");
        }
        MaterialItem materialItem1 = new MaterialItem();
        materialItem1.setTypeId(materialItem.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(materialItem.getOrganCode());
        materialItem1.setItemCode(materialItem.getItemCode());
        materialItem1.setIsMain("N");
        List<MaterialItem> materialItems = materialItemMapper.selectMaterialItemList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            throw new RuntimeException("特征编码已存在");
        }
        if (materialItem.getId() == null) {
            materialItem.setId(KeyUtils.generateId());
            materialItem.setCreateId(SecurityUtils.getUserId());
            materialItem.setCreateBy(SecurityUtils.getUsername());
            materialItem.setCreateTime(DateUtils.getNowDate());
            materialItem.setState(0L);
            materialItem.setIsMain("N");
//            materialItem.setDeptId(SecurityUtils.getSysUser().getDeptId());
        }
        materialItem.setCreateTime(DateUtils.getNowDate());
        return materialItemMapper.insertMaterialItem(materialItem);
    }

    /**
     * 修改材料特征项
     *
     * @param materialItem 材料特征项
     * @return 结果
     */
    @Override
    public int updateMaterialItem(MaterialItem materialItem) {
        if (materialItem == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(materialItem.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (materialItem.getTypeId() == null) {
            throw new RuntimeException("类型不能为空");
        }
//        String qc = "-"+materialItem.getOrganCode().substring(0, 4);
//        materialItem.setItemCode(materialItem.getItemCode().replace(qc, ""));
        MaterialItem materialItem1 = new MaterialItem();
        materialItem1.setTypeId(materialItem.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(materialItem.getOrganCode());
        materialItem1.setItemCode(materialItem.getItemCode());
        materialItem1.setIsMain("N");
        List<MaterialItem> materialItems = materialItemMapper.selectMaterialItemList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            for (MaterialItem item : materialItems) {
                if (!materialItem.getTypeId().equals(item.getTypeId())) {
                    throw new RuntimeException("特征编码已存在");
                }
            }
        }
        materialItem.setUpdateTime(DateUtils.getNowDate());
        materialItem.setUpdateId(SecurityUtils.getUserId());
        materialItem.setUpdateBy(SecurityUtils.getUsername());
        materialItem.setUpdateTime(DateUtils.getNowDate());
        return materialItemMapper.updateMaterialItem(materialItem);
    }


    /**
     * 批量删除材料特征项
     *
     * @param ids 需要删除的材料特征项主键
     * @return 结果
     */
    @Override
    public Boolean deleteMaterialItemByIds(Long[] ids) {
        if (ids == null) {
            throw new RuntimeException("参数为空");
        }
        List<MaterialItem> materialItems = materialItemMapper.selectBatchIds(Arrays.asList(ids));
        if (materialItems != null && !materialItems.isEmpty()) {
            for (MaterialItem materialItem : materialItems) {
                if ("Y".equals(materialItem.getIsMain())) {
                    throw new RuntimeException("主库数据不允许删除");
                }
                if (materialItem.getState() != 0L) {
                    throw new RuntimeException("流程中数据不允许删除");
                }
            }
            materialEigenvalueMapper.updateDelByItemId(ids);
        }
        return this.removeBatchByIds(Arrays.asList(ids));
    }

    /**
     * 删除材料特征项信息
     *
     * @param id 材料特征项主键
     * @return 结果
     */
    @Override
    public int deleteMaterialItemById(Long id) {
        return materialItemMapper.deleteMaterialItemById(id);
    }

    @Override
    public List<MaterialItem> initData(MaterialItem materialItem) {
        List<MaterialItem> materialItems = materialItemMapper.selectMaterialItemList(materialItem);
        List<MtrFeature> mtrFeatures = iMtrFeatureService.selectMtrFeatureList(null);
        MaterialType type1 = new MaterialType();
        type1.setOrganCode(materialItem.getOrganCode());
        type1.setIsMain("Y");
        List<MaterialType> materialTypes = materialTypeService.selectMaterialTypeList(type1);
        if (materialTypes == null || materialTypes.isEmpty()) {
            throw new RuntimeException("请先配置材料类型");
        }
        Map<String, Long> params = new HashMap<>();
        materialTypes.forEach(item -> {
            if ("Y".equals(item.getIsMain())) {
                params.put(item.getHostId(), item.getId());
            }
        });
        Map<String, MtrFeature> mtrMap = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(mtrFeature -> {
                mtrMap.put(mtrFeature.getId(), mtrFeature);
            });
        }
        if (materialItems == null || materialItems.isEmpty()) {
            //初始化-最开始无数据情况
            if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
                mtrFeatures.forEach(mtrFeature -> {
                    MaterialItem bean = new MaterialItem();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeature.getMtrClassId()));
                    bean.setItemName(mtrFeature.getFeatureName());
                    bean.setItemCode(mtrFeature.getFeatureCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(materialItem.getOrganCode());
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
            List<MaterialItem> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!items.containsKey(key)) {
                    MtrFeature mtrFeature = mtrMap.get(key);
                    MaterialItem bean = new MaterialItem();
                    bean.setId(KeyUtils.generateId());
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeature.getMtrClassId()));
                    bean.setItemName(mtrFeature.getFeatureName());
                    bean.setItemCode(mtrFeature.getFeatureCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(materialItem.getOrganCode());
                    bean.setHostId(mtrFeature.getId());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
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
    public void updateByHostId(MtrFeature mtrFeature) {
        MaterialItem item = new MaterialItem();
        item.setHostId(mtrFeature.getId());
        item.setDelFlag("0");
        List<MaterialItem> materialItems = baseMapper.selectMaterialItemList(item);
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
            QueryWrapper<MaterialItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<MaterialItem> materialTypes = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<MaterialItem> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main","Y");
            materialTypes.addAll(this.list(qw));
            List<MaterialItem> newDeviceDetails = new ArrayList<>();
            List<MaterialItem> upDeviceDetails = new ArrayList<>();
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
            List<Long> collect = newDeviceDetails.stream().map(MaterialItem::getId).collect(Collectors.toList());
            this.removeBatchByIds(collect);
        }
    }


    @Override
    public void addByMain(MtrFeature mtrFeature) {
        MaterialType materialType = new MaterialType();
        materialType.setHostId(mtrFeature.getMtrClassId());
        List<MaterialType> materialTypes = materialTypeService.selectMaterialTypeList(materialType);
        //已存在数据排除
        MaterialItem type1 = new MaterialItem();
        type1.setHostId(mtrFeature.getId());
        type1.setDelFlag("0");
        List<MaterialItem> materialTypes1 = baseMapper.selectMaterialItemList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getTypeId());
            });
        }
        List<MaterialItem> addList = new ArrayList<>();
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (MaterialType type : materialTypes) {
                if (map.containsKey(type.getOrganCode()) && (type.getId() + "").equals(map.get(type.getOrganCode()) + "")) {
                    continue;
                }
                MaterialItem bean = new MaterialItem();
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

    /**
     * 新增至主库
     *
     * @param materialItem
     * @return
     */
    @Override
    public synchronized int addToMain(MaterialItem materialItem) {
        if (materialItem.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialItem.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        MaterialItem materialType1 = this.selectMaterialItemById(materialItem.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        MtrClass mtrClass = mtrClassService.selectMtrClassById(materialItem.getHostId());
        //查询编码是否已在主库存在
        MtrFeature mtrClass1 = new MtrFeature();
        mtrClass1.setMtrClassId(materialItem.getHostId());
        mtrClass1.setValid(0L);
        mtrClass1.setFeatureCode(materialType1.getItemCode());
        if (!StringUtils.isEmpty(materialItem.getItemCode())) {
            mtrClass1.setFeatureCode(materialItem.getItemCode());
        }
        List<MtrFeature> mtrClasses = iMtrFeatureService.selectMtrFeatureList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            return -1;
        } else {
            if (!StringUtils.isEmpty(materialItem.getItemCode())) {
                MtrFeature aClass = new MtrFeature();
                aClass.setId(key);
                aClass.setMtrClassId(mtrClass.getId());
                aClass.setFeatureCode(materialItem.getItemCode());
                aClass.setFeatureName(materialType1.getItemName());
                aClass.setSonId(materialType1.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateTime(DateUtils.getNowDate());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                iMtrFeatureService.insertMtrFeature(aClass);
                mtrClass1.setFeatureCode(materialItem.getItemCode());
                materialType1.setMainId(aClass.getId());
                materialType1.setIsMain("Y");
                return baseMapper.updateMaterialItem(materialType1);
            } else {
                //不存在时 新增至主库
                materialType1.setIsMain("Y");
                materialType1.setHostId(key);
                int i = baseMapper.updateMaterialItem(materialType1);
                if (i > 0) {
                    MtrFeature aClass = new MtrFeature();
                    aClass.setId(key);
                    aClass.setMtrClassId(mtrClass.getId());
                    aClass.setFeatureCode(materialType1.getItemCode());
                    aClass.setFeatureName(materialType1.getItemName());
                    aClass.setSonId(materialType1.getId());
                    aClass.setCreateBy(SecurityUtils.getUsername());
                    aClass.setCreateTime(DateUtils.getNowDate());
                    aClass.setCreateId(SecurityUtils.getUserId() + "");
                    iMtrFeatureService.insertMtrFeature(aClass);
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
    public synchronized int associationToMain(MaterialItem materialItem) {
        if (materialItem.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialItem.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        MaterialItem materialType1 = this.selectMaterialItemById(materialItem.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialItem.getHostId());
        return baseMapper.updateMaterialItem(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialItem
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(MaterialItem materialItem) {
        if (materialItem.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        MaterialItem materialType1 = this.selectMaterialItemById(materialItem.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return baseMapper.updateMaterialItem(materialType1);
    }

    @Override
    public long selectMaterialItemListCount(MaterialItem materialItem) {
        return baseMapper.selectMaterialItemListCount(materialItem);
    }

    @Override
    public List<MaterialItem> getProcessed(String organCode) {
        return baseMapper.getProcessed(organCode);
    }


}
