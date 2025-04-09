package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.ILabourTypeService;
import com.zhaocai.archives.dossier.tree.LabourTypeTree;
import com.zhaocai.archives.main.domain.LaborServicesClass;
import com.zhaocai.archives.main.mapper.LaborServicesClassMapper;
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

/**
 * 劳务分类主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class LaborServicesClassServiceImpl extends ServiceImpl<LaborServicesClassMapper, LaborServicesClass> implements ILaborServicesClassService {
    @Autowired
    private LaborServicesClassMapper laborServicesClassMapper;

    @Resource
    private ILabourTypeService iLabourTypeService;

    /**
     * 查询劳务分类主
     *
     * @param id 劳务分类主主键
     * @return 劳务分类主
     */
    @Override
    public LaborServicesClass selectLaborServicesClassById(String id) {
        LaborServicesClass aClass = laborServicesClassMapper.selectLaborServicesClassById(id);
        if (aClass.getParentId() != null && !"0".equals(aClass.getParentId())) {
            LaborServicesClass aClass1 = laborServicesClassMapper.selectLaborServicesClassById(aClass.getParentId());
            aClass.setBelongingLevel(aClass1.getLaborServicesClassName());
        } else {
            aClass.setBelongingLevel("顶级");
        }
        return aClass;
    }

    /**
     * 查询劳务分类主列表
     *
     * @param laborServicesClass 劳务分类主
     * @return 劳务分类主
     */
    @Override
    public List<LaborServicesClass> selectLaborServicesClassList(LaborServicesClass laborServicesClass) {
        if (laborServicesClass == null) {
            laborServicesClass = new LaborServicesClass();
        }
        laborServicesClass.setValid(0L);
        return laborServicesClassMapper.selectLaborServicesClassList(laborServicesClass);
    }

    /**
     * 新增劳务分类主
     *
     * @param laborServicesClass 劳务分类主
     * @return 结果
     */
    @Override
    public synchronized int insertLaborServicesClass(LaborServicesClass laborServicesClass) {
        if (laborServicesClass.getId() == null) {
            throw new RuntimeException("请先初始化");
        }
        if (laborServicesClass.getParentId() == null) {
            throw new RuntimeException("父级不能为空");
        }
        //判断code是否已存在
        LaborServicesClass mtrClass1 = new LaborServicesClass();
        mtrClass1.setValid(0L);
        mtrClass1.setLaborServicesClassCode(laborServicesClass.getLaborServicesClassCode());
        List<LaborServicesClass> mtrClasses = this.selectLaborServicesClassList(mtrClass1);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            throw new RuntimeException("编码已存在");
        }
        laborServicesClass.setCreateId(SecurityUtils.getUserId() + "");
        laborServicesClass.setCreateBy(SecurityUtils.getUsername());
        laborServicesClass.setCreateTime(DateUtils.getNowDate());
        laborServicesClass.setValid(0L);
        int i = laborServicesClassMapper.insertLaborServicesClass(laborServicesClass);
        if (i > 0) {
            iLabourTypeService.addTypeByMain(laborServicesClass);
        }
        return i;
    }

    /**
     * 修改劳务分类主
     *
     * @param laborServicesClass 劳务分类主
     * @return 结果
     */
    @Override
    public int updateLaborServicesClass(LaborServicesClass laborServicesClass) {
        //判断code是否已存在
        LaborServicesClass mtrClass1 = new LaborServicesClass();
        mtrClass1.setValid(0L);
        mtrClass1.setLaborServicesClassCode(laborServicesClass.getLaborServicesClassCode());
        List<LaborServicesClass> mtrClasses = this.selectLaborServicesClassList(mtrClass1);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            for (LaborServicesClass mtrClass2 : mtrClasses) {
                if (!laborServicesClass.getId().equals(mtrClass2.getId())) {
                    throw new RuntimeException("编码已存在");
                }
            }
        }
        laborServicesClass.setUpdateTime(DateUtils.getNowDate());
        int i = laborServicesClassMapper.updateLaborServicesClass(laborServicesClass);
        if (i > 0) {
            iLabourTypeService.updateByHostId(laborServicesClass);
        }
        return i;
    }

    /**
     * 批量删除劳务分类主
     *
     * @param ids 需要删除的劳务分类主主键
     * @return 结果
     */
    @Override
    public boolean deleteLaborServicesClassByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<LaborServicesClass> mtrFeatures = this.listByIds(Arrays.asList(ids));
        Map<Long, Long> map = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(item -> {
                int i = baseMapper.getMaterialJoinNoMy(item.getId());
                if (i > 0) {
                    throw new RuntimeException("当前分类下存在数据，无法进行删除");
                }
                item.setValid(System.currentTimeMillis() / 1000L);
                if (item.getSonId() != null) {
                    map.put(item.getSonId(), item.getSonId());
                }
            });
        }
        boolean b = this.updateBatchById(mtrFeatures);
        if (b) {
            iLabourTypeService.deleteByHostId(ids, map);
        }
        return b;
    }

    /**
     * 删除劳务分类主信息
     *
     * @param id 劳务分类主主键
     * @return 结果
     */
    @Override
    public int deleteLaborServicesClassById(String id) {
        return laborServicesClassMapper.deleteLaborServicesClassById(id);
    }


    public List<LabourTypeTree> getLaborServicesClassTree() {
        LaborServicesClass mtrClass = new LaborServicesClass();
        mtrClass.setValid(0L);
        PageUtils.clearPage();
        List<LaborServicesClass> select = baseMapper.selectLaborServicesClassList(mtrClass);
        Map<String, LabourTypeTree> map = new HashMap<>();
        select.forEach(item -> {
            LabourTypeTree materialTypeTree = new LabourTypeTree();
            materialTypeTree.setId(item.getId() + "");
            materialTypeTree.setLabel(item.getLaborServicesClassName());
            materialTypeTree.setType(item.getLaborServicesClassType());
            materialTypeTree.setCode(item.getLaborServicesClassCode());
            materialTypeTree.setChildren(new ArrayList<>());
            map.put(item.getId(), materialTypeTree);
        });

        // 构建树形结构
        List<LabourTypeTree> list = new ArrayList<>();
        for (LaborServicesClass type : select) {
            LabourTypeTree treeVo = map.get(type.getId());
            if (StringUtils.isEmpty(type.getParentId()) || "0".equals(type.getParentId())) {
                // 根节点，直接添加
                list.add(treeVo);
            } else {
                // 非根节点，找到父节点并添加到其子节点列表中
                LabourTypeTree materialTypeTree = map.get(type.getParentId());
                if (materialTypeTree != null) {
                    materialTypeTree.getChildren().add(treeVo);
                }
            }
        }
        return list;
    }


    @Override
    public LaborServicesClass initCode(LaborServicesClass laborServicesClass) {
        if (laborServicesClass.getId() == null) {
            throw new RuntimeException("id不能为空");
        }
        String materialCode = "";
        LaborServicesClass type = new LaborServicesClass();
        if ("0".equals(laborServicesClass.getId())) {
            String[] strs = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
            LaborServicesClass aClassx = new LaborServicesClass();
            aClassx.setParentId("0");
            List<LaborServicesClass> mtrClasses = baseMapper.selectLaborServicesClassList(aClassx);
            Map<String, String> map = new HashMap<>();
            for (LaborServicesClass mtrClass1 : mtrClasses) {
                String s = retainEnglishLetters(mtrClass1.getLaborServicesClassCode());
                map.put(s, s);
            }
            for (String s : strs) {
                if (StringUtils.isEmpty(map.get(s))) {
                    materialCode = s + "1";
                    break;
                }
            }
        } else {
            type = baseMapper.selectLaborServicesClassById(laborServicesClass.getId());
            materialCode = type.getLaborServicesClassCode();
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
        LaborServicesClass aClass = new LaborServicesClass();
        aClass.setId(KeyUtils.generateId() + "");
        aClass.setParentId(laborServicesClass.getId());
        aClass.setLaborServicesClassCode(materialCode);
        aClass.setCreateId(SecurityUtils.getUserId() + "");
        aClass.setCreateBy(SecurityUtils.getUsername());
        aClass.setCreateTime(DateUtils.getNowDate());
        return aClass;
    }

    @Override
    public long selectLaborServicesClassListCount(LaborServicesClass laborServicesClass) {
        return baseMapper.selectLaborServicesClassListCount(laborServicesClass);
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


}
