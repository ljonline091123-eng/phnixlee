package com.zhaocai.archives.main.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.IMaterialTypeService;
import com.zhaocai.archives.dossier.tree.MaterialTypeTree;
import com.zhaocai.archives.main.domain.*;
import com.zhaocai.archives.main.mapper.MtrClassMapper;
import com.zhaocai.archives.main.service.IMtrArchivesService;
import com.zhaocai.archives.main.service.IMtrClassService;
import com.zhaocai.archives.main.service.IMtrFeatureService;
import com.zhaocai.archives.main.service.IMtrFeatureValueService;
import com.zhaocai.archives.utils.KeyUtils;
import com.zhaocai.common.core.exception.ServiceException;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.PageUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 材料分类主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class MtrClassServiceImpl extends ServiceImpl<MtrClassMapper, MtrClass> implements IMtrClassService {
    @Autowired
    private MtrClassMapper mtrClassMapper;

    @Resource
    private IMaterialTypeService materialTypeService;
    @Autowired
    private IMtrFeatureService iMtrFeatureService;
    @Autowired
    private IMtrFeatureValueService iMtrFeatureValueService;
    @Autowired
    private IMtrArchivesService iMtrArchivesService;


    /**
     * 查询材料分类主
     *
     * @param id 材料分类主主键
     * @return 材料分类主
     */
    @Override
    public MtrClass selectMtrClassById(String id) {
        MtrClass aClass = mtrClassMapper.selectMtrClassById(id);
        if (aClass.getParentId() != null && !"0".equals(aClass.getParentId())) {
            MtrClass aClass1 = mtrClassMapper.selectMtrClassById(aClass.getParentId());
            aClass.setBelongingLevel(aClass1.getMtrClassName());
        } else {
            aClass.setBelongingLevel("顶级");
        }
        return aClass;
    }

    /**
     * 查询材料分类主列表
     *
     * @param mtrClass 材料分类主
     * @return 材料分类主
     */
    @Override
    public List<MtrClass> selectMtrClassList(MtrClass mtrClass) {
        if (mtrClass == null) {
            mtrClass = new MtrClass();
        }
        mtrClass.setValid(0L);
        return mtrClassMapper.selectMtrClassList(mtrClass);
    }

    /**
     * 查询材料分类主列表
     *
     * @param mtrClass 材料分类主
     * @return 材料分类主
     */
    @Override
    public List<MtrClass> selectMtrClassListNoChange(MtrClass mtrClass) {
        return mtrClassMapper.selectMtrClassList(mtrClass);
    }

    /**
     * 新增材料分类主
     *
     * @param mtrClass 材料分类主
     * @return 结果
     */
    @Override
    public synchronized int insertMtrClass(MtrClass mtrClass) {
        if (mtrClass.getId() == null) {
            throw new RuntimeException("请先初始化");
        }
        if (mtrClass.getParentId() == null) {
            throw new RuntimeException("父级不能为空");
        }
        //判断code是否已存在
        MtrClass mtrClass1 = new MtrClass();
        mtrClass1.setValid(0L);
        mtrClass1.setMtrClassCode(mtrClass.getMtrClassCode());
        List<MtrClass> mtrClasses = this.selectMtrClassList(mtrClass1);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            throw new RuntimeException("编码已存在");
        }
        mtrClass.setCreateId(SecurityUtils.getUserId() + "");
        mtrClass.setCreateBy(SecurityUtils.getUsername());
        mtrClass.setCreateTime(DateUtils.getNowDate());
        mtrClass.setValid(0L);
        int i = mtrClassMapper.insertMtrClass(mtrClass);
        if (i > 0) {
            materialTypeService.addMaterialTypeByMain(mtrClass);
        }
        return i;
    }

    /**
     * 修改材料分类主
     *
     * @param mtrClass 材料分类主
     * @return 结果
     */
    @Override
    public synchronized int updateMtrClass(MtrClass mtrClass) {
        //判断code是否已存在
        MtrClass mtrClass1 = new MtrClass();
        mtrClass1.setValid(0L);
        mtrClass1.setMtrClassCode(mtrClass.getMtrClassCode());
        List<MtrClass> mtrClasses = this.selectMtrClassList(mtrClass1);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            for (MtrClass mtrClass2 : mtrClasses) {
                if (!mtrClass.getId().equals(mtrClass2.getId())) {
                    throw new RuntimeException("编码已存在");
                }
            }
        }
        mtrClass.setUpdateTime(DateUtils.getNowDate());
        mtrClass.setIsTb("2");
        int i = mtrClassMapper.updateMtrClass(mtrClass);
        if (i > 0) {
            materialTypeService.updateByHostId(mtrClass);
        }
        return i;
    }

    /**
     * 批量删除材料分类主
     *
     * @param ids 需要删除的材料分类主主键
     * @return 结果
     */
    @Override
    public Boolean deleteMtrClassByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<MtrClass> mtrFeatures = this.listByIds(Arrays.asList(ids));
        Map<Long, Long> map = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(item -> {
                int i = baseMapper.getMaterialJoinNoMy(item.getId());
                if (i > 0) {
                    throw new RuntimeException("当前分类下存在数据，无法进行删除");
                }
                item.setValid(System.currentTimeMillis() / 1000L);
                item.setIsTb("3");
                if (item.getSonId() != null) {
                    map.put(item.getSonId(), item.getSonId());
                }
            });
        }
        boolean b = this.updateBatchById(mtrFeatures);
        if (b) {
            materialTypeService.deleteByHostId(ids, map);
        }
        return b;
    }

    /**
     * 删除材料分类主信息
     *
     * @param id 材料分类主主键
     * @return 结果
     */
    @Override
    public int deleteMtrClassById(String id) {
        return mtrClassMapper.deleteMtrClassById(id);
    }

    @Override
    public MtrClass initCode(MtrClass mtrClass) {
        if (mtrClass.getId() == null) {
            throw new RuntimeException("id不能为空");
        }
        String materialCode = "";
        MtrClass type = new MtrClass();
        if ("0".equals(mtrClass.getId())) {
            String[] strs = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
            MtrClass aClassx = new MtrClass();
            aClassx.setParentId("0");
            List<MtrClass> mtrClasses = baseMapper.selectMtrClassList(aClassx);
            Map<String, String> map = new HashMap<>();
            for (MtrClass mtrClass1 : mtrClasses) {
                String s = retainEnglishLetters(mtrClass1.getMtrClassCode());
                map.put(s, s);
            }
            for (String s : strs) {
                if (StringUtils.isEmpty(map.get(s))) {
                    materialCode = s + "1";
                    break;
                }
            }
        } else {
            type = baseMapper.selectMtrClassById(mtrClass.getId());
            materialCode = type.getMtrClassCode();
            Integer maxCode = baseMapper.getMaxCode(materialCode, type.getId());
            if (maxCode != null) {
                maxCode += 1;
                //根据规则，长度大于8的流水号有3位
                if (materialCode.length() >= 8) {
                    if (maxCode < 100 && maxCode >= 10) {
                        materialCode = materialCode + "0" + maxCode;
                    } else if (maxCode < 10) {
                        materialCode = materialCode + "00" + maxCode;
                    } else {
                        materialCode = materialCode + maxCode;
                    }
                } else {
                    if (maxCode < 10) {
                        materialCode = materialCode + "0" + maxCode;
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
        }
        MtrClass aClass = new MtrClass();
        aClass.setId(KeyUtils.generateId() + "");
        aClass.setParentId(mtrClass.getId());
        aClass.setMtrClassCode(materialCode);
        try {
            if (!StringUtils.isEmpty(type.getClassLevelCd())) {
                int cj = Integer.parseInt(type.getClassLevelCd()) + 1;
                aClass.setClassLevelCd(cj + "");
                aClass.setClassLevel(this.getLevel(cj + ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        aClass.setCreateId(SecurityUtils.getUserId() + "");
        aClass.setCreateBy(SecurityUtils.getUsername());
        aClass.setCreateTime(DateUtils.getNowDate());
        return aClass;
    }

    /**
     * 保留字符串中的英文字符
     *
     * @param input 输入字符串
     * @return 只包含英文字符的字符串
     */
    public static String retainEnglishLetters(String input) {
        if (input == null || input.isEmpty()) {
            return input; // 处理空字符串或 null 的情况
        }
        return input.replaceAll("[^a-zA-Z]", ""); // 保留 a-z 和 A-Z 范围内的字符
    }


    @Override
    public List<MaterialTypeTree> getMtrClassTree(MtrClass materialType) {
        MtrClass mtrClass = new MtrClass();
        mtrClass.setValid(0L);
        PageUtils.clearPage();
        List<MtrClass> select = baseMapper.selectMtrClassList(mtrClass);
        Map<String, MaterialTypeTree> map = new HashMap<>();
        select.forEach(item -> {
            MaterialTypeTree materialTypeTree = new MaterialTypeTree();
            materialTypeTree.setId(item.getId() + "");
            materialTypeTree.setLabel(item.getMtrClassName());
            materialTypeTree.setType(item.getMtrClassType());
            materialTypeTree.setCode(item.getMtrClassCode());
            materialTypeTree.setChildren(new ArrayList<>());
            map.put(item.getId(), materialTypeTree);
        });

        // 构建树形结构
        List<MaterialTypeTree> list = new ArrayList<>();
        for (MtrClass type : select) {
            MaterialTypeTree treeVo = map.get(type.getId());
            if (type.getParentId() == null || type.getParentId().equals("0")) {
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

    @Override
    public List<MaterialTypeTree> getMtrClassTreeTwo(MtrClass materialType) {
        MtrClass mtrClass = new MtrClass();
        mtrClass.setValid(0L);
        PageUtils.clearPage();
        List<MtrClass> select = baseMapper.selectMtrClassList(mtrClass);
        Map<String, MaterialTypeTree> map = new HashMap<>();
        select.forEach(item -> {
            MaterialTypeTree materialTypeTree = new MaterialTypeTree();
            materialTypeTree.setId(item.getId() + "");
            materialTypeTree.setLabel(item.getMtrClassName());
            materialTypeTree.setType(item.getMtrClassType());
            materialTypeTree.setCode(item.getMtrClassCode());
            materialTypeTree.setChildren(new ArrayList<>());
            map.put(item.getId(), materialTypeTree);
        });

        // 构建树形结构
        List<MaterialTypeTree> list = new ArrayList<>();
        for (MtrClass type : select) {
            MaterialTypeTree treeVo = map.get(type.getId());
            if (type.getParentId() == null || type.getParentId().equals("0")) {
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
        //具体档案加入树
        MtrArchives detail = new MtrArchives();
        detail.setValid(0L);
        List<MtrArchives> detailList = iMtrArchivesService.selectMtrArchivesList(detail);
        if(CollectionUtil.isNotEmpty(detailList)){
            detailList.stream().forEach(item->{
                MaterialTypeTree materialTypeTree = map.get(item.getMtrClassId());
                if (materialTypeTree != null) {
                    MaterialTypeTree tree = new MaterialTypeTree();
                    tree.setId(item.getId() + "");
                    tree.setLabel(item.getMtrName());
                    tree.setType(item.getMtrClassName());
                    tree.setCode(item.getMtrCode());
                    tree.setChildren(new ArrayList<>());
                    tree.setState(item.getValid());
                    materialTypeTree.getChildren().add(tree);
                }
            });
        }
        return list;
    }

    @Override
    public long selectMtrClassListCount(MtrClass mtrClass) {
        return baseMapper.selectMtrClassListCount(mtrClass);
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
    public String importData(List<MtrClassExcelData> userList, Boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(userList) || userList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }
        MtrClass mtrClass = new MtrClass();
        mtrClass.setValid(0L);
        List<MtrClass> mtrClasses = mtrClassMapper.selectMtrClassList(mtrClass);
        Map<String, MtrClass> map = new HashMap<>();
        mtrClasses.forEach(item -> {
            map.put(item.getMtrClassCode(), item);
        });
        MtrFeature mtrFeature = new MtrFeature();
        mtrFeature.setValid(0L);
        Map<String, MtrFeature> featureMap = new HashMap<>();
        List<MtrFeature> mtrFeatures = iMtrFeatureService.selectMtrFeatureList(mtrFeature);
        mtrFeatures.forEach(item -> {
            featureMap.put(item.getMtrClassId() + "|" + item.getFeatureCode(), item);
        });
        MtrFeatureValue mtrFeatureValue = new MtrFeatureValue();
        mtrFeatureValue.setValid(0L);
        Map<String, MtrFeatureValue> valMap = new HashMap<>();
        List<MtrFeatureValue> mtrFeatureValues = iMtrFeatureValueService.selectMtrFeatureValueList(mtrFeatureValue);
        mtrFeatureValues.forEach(item -> {
            valMap.put(item.getMtrFeatureId() + "|" + item.getFeatureValueCode(), item);
        });
        MtrArchives mtrArchives1 = new MtrArchives();
        mtrArchives1.setValid(0L);
        Map<String, MtrArchives> mtrArchivesMap = new HashMap<>();
        List<MtrArchives> mtrArchives = iMtrArchivesService.selectMtrArchivesList(mtrArchives1);
        mtrArchives.forEach(item -> {
            mtrArchivesMap.put(item.getMtrClassId() + "|" + item.getMtrCode(), item);
        });
        List<MtrClass> addMtrClasses = new ArrayList<>();
        List<MtrFeature> mtrFeatureList = new ArrayList<>();
        List<MtrFeatureValue> mtrFeatureValueList = new ArrayList<>();
        List<MtrArchives> mtrArchivesList = new ArrayList<>();
        int errIndex = 1;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (MtrClassExcelData data : userList) {
            errIndex++;
            //判空
            if (StringUtils.isEmpty(data.getParentCode())) {
                failureMsg.append("<br/>第" + errIndex + "行，父级材料分类编码为空");
            }
            if (StringUtils.isEmpty(data.getMtrClassCode())) {
                failureMsg.append("<br/>第" + errIndex + "行，材料分类编码为空");
            }
            if (StringUtils.isEmpty(data.getMtrClassName())) {
                failureMsg.append("<br/>第" + errIndex + "行，材料分类名称为空");
            }
            if (!StringUtils.isEmpty(failureMsg.toString())) {
                continue;
            }
            if ("0".equals(data.getParentCode())) {
                if (map.containsKey(data.getMtrClassCode())) {
                    String id = map.get(data.getMtrClassCode()).getId();
                    String name = map.get(data.getMtrClassCode()).getMtrClassName();
                    //判断是否有特征值、特征项、详细档案
                    if (!StringUtils.isEmpty(data.getFeatureCode()) || !StringUtils.isEmpty(data.getFeatureValueCode()) || !StringUtils.isEmpty(data.getMtrCode())) {
                        if (!StringUtils.isEmpty(data.getFeatureCode())) {
                            if (featureMap.containsKey(id + "|" + data.getFeatureCode())) {
                                if (!StringUtils.isEmpty(data.getFeatureValueCode())) {
                                    String id1 = featureMap.get(id + "|" + data.getFeatureCode()).getId();
                                    if (valMap.containsKey(id1 + "|" + data.getFeatureValueCode())) {
                                        failureMsg.append("<br/>第" + errIndex + "行，特征值已存在");
                                    } else {
                                        MtrFeatureValue mtrFeatureValue1 = new MtrFeatureValue();
                                        mtrFeatureValue1.setId(KeyUtils.generateId() + "");
                                        mtrFeatureValue1.setMtrFeatureId(id1);
                                        mtrFeatureValue1.setFeatureValueName(data.getFeatureValueCode());
                                        mtrFeatureValue1.setFeatureValueCode(data.getFeatureValueCode());
                                        mtrFeatureValue1.setCreateId(SecurityUtils.getUserId() + "");
                                        mtrFeatureValue1.setCreateBy(SecurityUtils.getUsername());
                                        mtrFeatureValue1.setCreateTime(DateUtils.getNowDate());
                                        mtrFeatureValueList.add(mtrFeatureValue1);
                                        valMap.put(id1 + "|" + data.getFeatureValueCode(), mtrFeatureValue1);
                                    }
                                } else {
                                    failureMsg.append("<br/>第" + errIndex + "行，特征项已存在");
                                }
                            } else {
                                MtrFeature mtrFeature1 = new MtrFeature();
                                mtrFeature1.setId(KeyUtils.generateId() + "");
                                mtrFeature1.setMtrClassId(id);
                                mtrFeature1.setFeatureCode(data.getFeatureCode());
                                mtrFeature1.setFeatureName(data.getFeatureName());
                                mtrFeature1.setCreateId(SecurityUtils.getUserId() + "");
                                mtrFeature1.setCreateBy(SecurityUtils.getUsername());
                                mtrFeature1.setCreateTime(DateUtils.getNowDate());
                                mtrFeatureList.add(mtrFeature1);
                                featureMap.put(id + "|" + data.getFeatureCode(), mtrFeature1);
                                if (!StringUtils.isEmpty(data.getFeatureValueCode())) {
                                    MtrFeatureValue mtrFeatureValue1 = new MtrFeatureValue();
                                    mtrFeatureValue1.setId(KeyUtils.generateId() + "");
                                    mtrFeatureValue1.setMtrFeatureId(mtrFeature1.getId());
                                    mtrFeatureValue1.setFeatureValueName(data.getFeatureValueCode());
                                    mtrFeatureValue1.setFeatureValueCode(data.getFeatureValueCode());
                                    mtrFeatureValue1.setCreateId(SecurityUtils.getUserId() + "");
                                    mtrFeatureValue1.setCreateBy(SecurityUtils.getUsername());
                                    mtrFeatureValue1.setCreateTime(DateUtils.getNowDate());
                                    mtrFeatureValueList.add(mtrFeatureValue1);
                                    valMap.put(mtrFeature1.getId() + "|" + data.getFeatureValueCode(), mtrFeatureValue1);
                                }
                            }
                        }
                        if (!StringUtils.isEmpty(data.getMtrCode())) {
                            if (mtrArchivesMap.containsKey(id + "|" + data.getMtrCode())) {
                                failureMsg.append("<br/>第" + errIndex + "行，具体档案已存在");
                            } else {
                                MtrArchives mtrArchives2 = new MtrArchives();
                                mtrArchives2.setId(KeyUtils.generateId() + "");
                                mtrArchives2.setMtrClassId(id);
                                mtrArchives2.setMtrCode(data.getMtrCode());
                                mtrArchives2.setMtrName(data.getMtrName());
                                mtrArchives2.setMtrClassName(name);
                                mtrArchives2.setMeasureUnit(data.getUnit());
                                mtrArchives2.setSpecs(data.getSpecs());
                                mtrArchives2.setCreateId(SecurityUtils.getUserId() + "");
                                mtrArchives2.setCreateBy(SecurityUtils.getUsername());
                                mtrArchives2.setCreateTime(DateUtils.getNowDate());
                                mtrArchivesList.add(mtrArchives2);
                                mtrArchivesMap.put(id + "|" + data.getMtrCode(), mtrArchives2);
                            }
                        }
                    } else {
                        failureMsg.append("<br/>第" + errIndex + "行，材料分类已存在");
                    }
                } else {
                    MtrClass mtrClass1 = new MtrClass();
                    mtrClass1.setValid(0L);
                    mtrClass1.setId(KeyUtils.generateId() + "");
                    mtrClass1.setParentId(data.getParentCode());
                    mtrClass1.setMtrClassCode(data.getMtrClassCode());
                    mtrClass1.setMtrClassName(data.getMtrClassName());
                    mtrClass1.setMeasureUnit(data.getMeasureUnit());
                    mtrClass1.setCreateId(SecurityUtils.getUserId() + "");
                    mtrClass1.setCreateBy(SecurityUtils.getUsername());
                    mtrClass1.setCreateTime(DateUtils.getNowDate());
                    addMtrClasses.add(mtrClass1);
                    map.put(mtrClass1.getMtrClassCode(), mtrClass1);
                }
            } else {
                if (map.containsKey(data.getMtrClassCode())) {
                    String id = map.get(data.getMtrClassCode()).getId();
                    String name = map.get(data.getMtrClassCode()).getMtrClassName();
                    //判断是否有特征值、特征项、详细档案
                    if (!StringUtils.isEmpty(data.getFeatureCode()) || !StringUtils.isEmpty(data.getFeatureValueCode()) || !StringUtils.isEmpty(data.getMtrCode())) {
                        if (!StringUtils.isEmpty(data.getFeatureCode())) {
                            if (featureMap.containsKey(id + "|" + data.getFeatureCode())) {
                                if (!StringUtils.isEmpty(data.getFeatureValueCode())) {
                                    String id1 = featureMap.get(id + "|" + data.getFeatureCode()).getId();
                                    if (valMap.containsKey(id1 + "|" + data.getFeatureValueCode())) {
                                        failureMsg.append("<br/>第" + errIndex + "行，特征值已存在");
                                    } else {
                                        MtrFeatureValue mtrFeatureValue1 = new MtrFeatureValue();
                                        mtrFeatureValue1.setId(KeyUtils.generateId() + "");
                                        mtrFeatureValue1.setMtrFeatureId(id1);
                                        mtrFeatureValue1.setFeatureValueName(data.getFeatureValueCode());
                                        mtrFeatureValue1.setFeatureValueCode(data.getFeatureValueCode());
                                        mtrFeatureValue1.setCreateId(SecurityUtils.getUserId() + "");
                                        mtrFeatureValue1.setCreateBy(SecurityUtils.getUsername());
                                        mtrFeatureValue1.setCreateTime(DateUtils.getNowDate());
                                        mtrFeatureValueList.add(mtrFeatureValue1);
                                        valMap.put(id1 + "|" + data.getFeatureValueCode(), mtrFeatureValue1);
                                    }
                                } else {
                                    failureMsg.append("<br/>第" + errIndex + "行，特征项已存在");
                                }
                            } else {
                                MtrFeature mtrFeature1 = new MtrFeature();
                                mtrFeature1.setId(KeyUtils.generateId() + "");
                                mtrFeature1.setMtrClassId(id);
                                mtrFeature1.setFeatureCode(data.getFeatureCode());
                                mtrFeature1.setFeatureName(data.getFeatureName());
                                mtrFeature1.setCreateId(SecurityUtils.getUserId() + "");
                                mtrFeature1.setCreateBy(SecurityUtils.getUsername());
                                mtrFeature1.setCreateTime(DateUtils.getNowDate());
                                mtrFeatureList.add(mtrFeature1);
                                featureMap.put(id + "|" + data.getFeatureCode(), mtrFeature1);

                                if (!StringUtils.isEmpty(data.getFeatureValueCode())) {
                                    MtrFeatureValue mtrFeatureValue1 = new MtrFeatureValue();
                                    mtrFeatureValue1.setId(KeyUtils.generateId() + "");
                                    mtrFeatureValue1.setMtrFeatureId(mtrFeature1.getId());
                                    mtrFeatureValue1.setFeatureValueName(data.getFeatureValueCode());
                                    mtrFeatureValue1.setFeatureValueCode(data.getFeatureValueCode());
                                    mtrFeatureValue1.setCreateId(SecurityUtils.getUserId() + "");
                                    mtrFeatureValue1.setCreateBy(SecurityUtils.getUsername());
                                    mtrFeatureValue1.setCreateTime(DateUtils.getNowDate());
                                    mtrFeatureValueList.add(mtrFeatureValue1);
                                    valMap.put(mtrFeature1.getId() + "|" + data.getFeatureValueCode(), mtrFeatureValue1);
                                }
                            }
                        }
                        if (!StringUtils.isEmpty(data.getMtrCode())) {
                            if (mtrArchivesMap.containsKey(id + "|" + data.getMtrCode())) {
                                failureMsg.append("<br/>第" + errIndex + "行，具体档案已存在");
                            } else {
                                MtrArchives mtrArchives2 = new MtrArchives();
                                mtrArchives2.setId(KeyUtils.generateId() + "");
                                mtrArchives2.setMtrClassId(id);
                                mtrArchives2.setMtrCode(data.getMtrCode());
                                mtrArchives2.setMtrName(data.getMtrName());
                                mtrArchives2.setMtrClassName(name);
                                mtrArchives2.setMeasureUnit(data.getUnit());
                                mtrArchives2.setSpecs(data.getSpecs());
                                mtrArchives2.setCreateId(SecurityUtils.getUserId() + "");
                                mtrArchives2.setCreateBy(SecurityUtils.getUsername());
                                mtrArchives2.setCreateTime(DateUtils.getNowDate());
                                mtrArchivesList.add(mtrArchives2);
                                mtrArchivesMap.put(id + "|" + data.getMtrCode(), mtrArchives2);
                            }
                        }
                    } else {
                        failureMsg.append("<br/>第" + errIndex + "行，材料分类已存在");
                    }
                } else {
                    MtrClass mtrClass1 = new MtrClass();
                    mtrClass1.setValid(0L);
                    mtrClass1.setId(KeyUtils.generateId() + "");
                    mtrClass1.setParentId(map.get(data.getParentCode()).getId());
                    mtrClass1.setMtrClassCode(data.getMtrClassCode());
                    mtrClass1.setMtrClassName(data.getMtrClassName());
                    mtrClass1.setMeasureUnit(data.getMeasureUnit());
                    mtrClass1.setCreateId(SecurityUtils.getUserId() + "");
                    mtrClass1.setCreateBy(SecurityUtils.getUsername());
                    mtrClass1.setCreateTime(DateUtils.getNowDate());
                    addMtrClasses.add(mtrClass1);
                    map.put(mtrClass1.getMtrClassCode(), mtrClass1);
                    if (!StringUtils.isEmpty(data.getFeatureCode())) {
                        MtrFeature mtrFeature1 = new MtrFeature();
                        mtrFeature1.setId(KeyUtils.generateId() + "");
                        mtrFeature1.setMtrClassId(mtrClass1.getId());
                        mtrFeature1.setFeatureCode(data.getFeatureCode());
                        mtrFeature1.setFeatureName(data.getFeatureName());
                        mtrFeature1.setCreateId(SecurityUtils.getUserId() + "");
                        mtrFeature1.setCreateBy(SecurityUtils.getUsername());
                        mtrFeature1.setCreateTime(DateUtils.getNowDate());
                        mtrFeatureList.add(mtrFeature1);
                        featureMap.put(mtrClass1.getId() + "|" + data.getFeatureCode(), mtrFeature1);
                        if (!StringUtils.isEmpty(data.getFeatureValueCode())) {
                            MtrFeatureValue mtrFeatureValue1 = new MtrFeatureValue();
                            mtrFeatureValue1.setId(KeyUtils.generateId() + "");
                            mtrFeatureValue1.setMtrFeatureId(mtrFeature1.getId());
                            mtrFeatureValue1.setFeatureValueName(data.getFeatureValueCode());
                            mtrFeatureValue1.setFeatureValueCode(data.getFeatureValueCode());
                            mtrFeatureValue1.setCreateId(SecurityUtils.getUserId() + "");
                            mtrFeatureValue1.setCreateBy(SecurityUtils.getUsername());
                            mtrFeatureValue1.setCreateTime(DateUtils.getNowDate());
                            mtrFeatureValueList.add(mtrFeatureValue1);
                            valMap.put(mtrFeature1.getId() + "|" + data.getFeatureValueCode(), mtrFeatureValue1);
                        }
                    }
                    if (!StringUtils.isEmpty(data.getMtrCode())) {
                        MtrArchives mtrArchives2 = new MtrArchives();
                        mtrArchives2.setId(KeyUtils.generateId() + "");
                        mtrArchives2.setMtrClassId(mtrClass1.getId());
                        mtrArchives2.setMtrCode(data.getMtrCode());
                        mtrArchives2.setMtrName(data.getMtrName());
                        mtrArchives2.setMtrClassName(data.getMtrClassName());
                        mtrArchives2.setMeasureUnit(data.getUnit());
                        mtrArchives2.setSpecs(data.getSpecs());
                        mtrArchives2.setCreateId(SecurityUtils.getUserId() + "");
                        mtrArchives2.setCreateBy(SecurityUtils.getUsername());
                        mtrArchives2.setCreateTime(DateUtils.getNowDate());
                        mtrArchivesList.add(mtrArchives2);
                        mtrArchivesMap.put(mtrClass1.getId() + "|" + data.getMtrCode(), mtrArchives2);
                    }
                }
            }
        }
        if (StringUtils.isEmpty(failureMsg.toString())) {
            if (!addMtrClasses.isEmpty()) {
                for (MtrClass mtrClass1 : addMtrClasses) {
                    mtrClassMapper.insertMtrClass(mtrClass1);
                }
            }
            if (!mtrFeatureList.isEmpty()) {
                for (MtrFeature mtrClass1 : mtrFeatureList) {
                    iMtrFeatureService.insertMtrFeature(mtrClass1);
                }
            }
            if (!mtrFeatureValueList.isEmpty()) {
                for (MtrFeatureValue mtrClass1 : mtrFeatureValueList) {
                    iMtrFeatureValueService.insertMtrFeatureValue(mtrClass1);
                }
            }
            if (!mtrArchivesList.isEmpty()) {
                for (MtrArchives mtrClass1 : mtrArchivesList) {
                    iMtrArchivesService.insertMtrArchives(mtrClass1);
                }
            }
        } else {
            failureMsg.insert(0, "很抱歉，导入失败！错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        successMsg.insert(0, "恭喜您，数据已全部导入成功！");
        return successMsg.toString();
    }

}
