package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.service.ILabourDetailsService;
import com.zhaocai.archives.main.domain.LaborServicesArchives;
import com.zhaocai.archives.main.domain.LaborServicesClass;
import com.zhaocai.archives.main.mapper.LaborServicesArchivesMapper;
import com.zhaocai.archives.main.service.ILaborServicesArchivesService;
import com.zhaocai.archives.main.service.ILaborServicesClassService;
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
 * 劳务档案主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class LaborServicesArchivesServiceImpl extends ServiceImpl<LaborServicesArchivesMapper, LaborServicesArchives> implements ILaborServicesArchivesService {
    @Autowired
    private LaborServicesArchivesMapper laborServicesArchivesMapper;

    @Resource
    private ILabourDetailsService iLabourDetailsService;

    @Resource
    private ILaborServicesClassService laborServicesClassService;

    /**
     * 查询劳务档案主
     *
     * @param id 劳务档案主主键
     * @return 劳务档案主
     */
    @Override
    public LaborServicesArchives selectLaborServicesArchivesById(String id) {
        return laborServicesArchivesMapper.selectLaborServicesArchivesById(id);
    }

    /**
     * 查询劳务档案主列表
     *
     * @param laborServicesArchives 劳务档案主
     * @return 劳务档案主
     */
    @Override
    public List<LaborServicesArchives> selectLaborServicesArchivesList(LaborServicesArchives laborServicesArchives) {
        if (laborServicesArchives == null) {
            laborServicesArchives = new LaborServicesArchives();
        }
        laborServicesArchives.setValid(0L);
        List<LaborServicesArchives> laborServicesArchives1 = laborServicesArchivesMapper.selectLaborServicesArchivesList(laborServicesArchives);
        LaborServicesClass aClass = new LaborServicesClass();
        if (laborServicesArchives.getLaborServicesClassId() != null) {
            aClass = laborServicesClassService.selectLaborServicesClassById(laborServicesArchives.getLaborServicesClassId());
        }
        if(aClass !=null) {
            String name = aClass.getLaborServicesClassName();
            laborServicesArchives1.forEach(laborServicesArchives1Item -> {
                laborServicesArchives1Item.setLaborServicesClassName(name);
            });
        }
        return laborServicesArchives1;
    }

    /**
     * 新增劳务档案主
     *
     * @param laborServicesArchives 劳务档案主
     * @return 结果
     */
    @Override
    public synchronized int insertLaborServicesArchives(LaborServicesArchives laborServicesArchives) {
        if (laborServicesArchives == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(laborServicesArchives.getLaborServicesClassId())) {
            throw new RuntimeException("类型id不能为空");
        }
        LaborServicesArchives mtrFeature1 = new LaborServicesArchives();
        mtrFeature1.setLaborServicesCode(laborServicesArchives.getLaborServicesCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setLaborServicesClassId(laborServicesArchives.getLaborServicesClassId());
        List<LaborServicesArchives> mtrFeatures = baseMapper.selectLaborServicesArchivesList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            throw new RuntimeException("当前编号已存在");
        }
        if (laborServicesArchives.getId() == null) {
            laborServicesArchives.setId(KeyUtils.generateId() + "");
            laborServicesArchives.setCreateTime(DateUtils.getNowDate());
            laborServicesArchives.setCreateId(SecurityUtils.getUserId() + "");
            laborServicesArchives.setCreateBy(SecurityUtils.getUsername());
            laborServicesArchives.setValid(0L);
        }
        int i = laborServicesArchivesMapper.insertLaborServicesArchives(laborServicesArchives);
        if (i > 0) {
            iLabourDetailsService.addTypeByMain(laborServicesArchives);
        }
        return i;
    }

    /**
     * 修改劳务档案主
     *
     * @param laborServicesArchives 劳务档案主
     * @return 结果
     */
    @Override
    public int updateLaborServicesArchives(LaborServicesArchives laborServicesArchives) {
        if (laborServicesArchives == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(laborServicesArchives.getLaborServicesClassId())) {
            throw new RuntimeException("类型id不能为空");
        }
        LaborServicesArchives mtrFeature1 = new LaborServicesArchives();
        mtrFeature1.setLaborServicesCode(laborServicesArchives.getLaborServicesCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setLaborServicesClassId(laborServicesArchives.getLaborServicesClassId());
        List<LaborServicesArchives> mtrFeatures = baseMapper.selectLaborServicesArchivesList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            for (LaborServicesArchives mtrFeature2 : mtrFeatures) {
                if (!laborServicesArchives.getId().equals(mtrFeature2.getId())) {
                    throw new RuntimeException("当前编号已存在");
                }
            }
        }
        laborServicesArchives.setUpdateTime(DateUtils.getNowDate());
        laborServicesArchives.setUpdateBy(SecurityUtils.getUsername());
        int i = laborServicesArchivesMapper.updateLaborServicesArchives(laborServicesArchives);
        if (i > 0) {
            iLabourDetailsService.updateByHostId(laborServicesArchives);
        }
        return i;
    }

    /**
     * 批量删除劳务档案主
     *
     * @param ids 需要删除的劳务档案主主键
     * @return 结果
     */
    @Override
    public boolean deleteLaborServicesArchivesByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<LaborServicesArchives> mtrFeatures = this.listByIds(Arrays.asList(ids));
        Map<Long, Long> map = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(item -> {
                item.setValid(System.currentTimeMillis() / 1000L);
                if (item.getSonId() != null) {
                    map.put(item.getSonId(), item.getSonId());
                }
            });
        }
        boolean b = this.updateBatchById(mtrFeatures);
        if (b) {
            iLabourDetailsService.deleteByHostId(ids, map);
        }
        return b;
    }

    /**
     * 删除劳务档案主信息
     *
     * @param id 劳务档案主主键
     * @return 结果
     */
    @Override
    public int deleteLaborServicesArchivesById(String id) {
        return laborServicesArchivesMapper.deleteLaborServicesArchivesById(id);
    }


    @Override
    public LaborServicesArchives initDetails(LaborServicesArchives laborServicesArchives) {
        if (laborServicesArchives.getLaborServicesClassId() == null) {
            throw new RuntimeException("类型id不能为空");
        }
        LaborServicesClass subcontractingType = laborServicesClassService.selectLaborServicesClassById(laborServicesArchives.getLaborServicesClassId());
        if (subcontractingType == null) {
            throw new BusinessException("请选择正确的类型");
        }
        String materialCode = subcontractingType.getLaborServicesClassCode();
        Integer maxCode = baseMapper.getMaxCode(laborServicesArchives.getLaborServicesClassId(), subcontractingType.getLaborServicesClassCode());
        if (maxCode != null) {
            maxCode += 1;
            //根据规则，长度大于8的流水号有3位
            if (maxCode < 100 && maxCode >= 10) {
                materialCode = materialCode + "0" + maxCode;
            } else if (maxCode < 10) {
                materialCode = materialCode + "00" + maxCode;
            } else {
                materialCode = materialCode + maxCode;
            }
        } else {
            materialCode = materialCode + "001";
        }
        LaborServicesArchives materialType1 = new LaborServicesArchives();
        materialType1.setId(KeyUtils.generateId() + "");
        materialType1.setLaborServicesClassId(subcontractingType.getId());
        materialType1.setLaborServicesCode(materialCode);
        materialType1.setCreateId(SecurityUtils.getUserId() + "");
        materialType1.setCreateBy(SecurityUtils.getUsername());
        materialType1.setCreateTime(DateUtils.getNowDate());
        return materialType1;
    }

    @Override
    public long selectLaborServicesArchivesListCount(LaborServicesArchives laborServicesArchives) {
        return baseMapper.selectLaborServicesArchivesListCount(laborServicesArchives);
    }

}
