package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.service.ISubcontractingDetailsService;
import com.zhaocai.archives.main.domain.MajorSubcontractingArchives;
import com.zhaocai.archives.main.domain.MajorSubcontractingClass;
import com.zhaocai.archives.main.mapper.MajorSubcontractingArchivesMapper;
import com.zhaocai.archives.main.service.IMajorSubcontractingArchivesService;
import com.zhaocai.archives.main.service.IMajorSubcontractingClassService;
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
 * 专业分包档案主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class MajorSubcontractingArchivesServiceImpl extends ServiceImpl<MajorSubcontractingArchivesMapper, MajorSubcontractingArchives> implements IMajorSubcontractingArchivesService {
    @Autowired
    private MajorSubcontractingArchivesMapper majorSubcontractingArchivesMapper;

    @Resource
    private ISubcontractingDetailsService iSubcontractingDetailsService;

    @Resource
    private IMajorSubcontractingClassService iMajorSubcontractingClassService;

    /**
     * 查询专业分包档案主
     *
     * @param id 专业分包档案主主键
     * @return 专业分包档案主
     */
    @Override
    public MajorSubcontractingArchives selectMajorSubcontractingArchivesById(String id) {
        return majorSubcontractingArchivesMapper.selectMajorSubcontractingArchivesById(id);
    }

    /**
     * 查询专业分包档案主列表
     *
     * @param majorSubcontractingArchives 专业分包档案主
     * @return 专业分包档案主
     */
    @Override
    public List<MajorSubcontractingArchives> selectMajorSubcontractingArchivesList(MajorSubcontractingArchives majorSubcontractingArchives) {
        if (majorSubcontractingArchives == null) {
            majorSubcontractingArchives = new MajorSubcontractingArchives();
        }
        majorSubcontractingArchives.setValid(0L);
        List<MajorSubcontractingArchives> majorSubcontractingArchives1 = majorSubcontractingArchivesMapper.selectMajorSubcontractingArchivesList(majorSubcontractingArchives);
        MajorSubcontractingClass aClass = new MajorSubcontractingClass();
        if (majorSubcontractingArchives.getMajorSubcontractingClassId() != null) {
            aClass = iMajorSubcontractingClassService.selectMajorSubcontractingClassById(majorSubcontractingArchives.getMajorSubcontractingClassId());
        }
        if(aClass !=null){
            String name = aClass.getMajorSubcontractingClassName();
            majorSubcontractingArchives1.forEach(item -> {
                item.setMajorSubcontractingClassName(name);
            });
        }


        return majorSubcontractingArchives1;
    }

    /**
     * 新增专业分包档案主
     *
     * @param majorSubcontractingArchives 专业分包档案主
     * @return 结果
     */
    @Override
    public synchronized int insertMajorSubcontractingArchives(MajorSubcontractingArchives majorSubcontractingArchives) {
        if (majorSubcontractingArchives == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(majorSubcontractingArchives.getMajorSubcontractingClassId())) {
            throw new RuntimeException("类型id不能为空");
        }
        MajorSubcontractingArchives mtrFeature1 = new MajorSubcontractingArchives();
        mtrFeature1.setMajorSubcontractingCode(majorSubcontractingArchives.getMajorSubcontractingCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setMajorSubcontractingClassId(majorSubcontractingArchives.getMajorSubcontractingClassId());
        List<MajorSubcontractingArchives> mtrFeatures = baseMapper.selectMajorSubcontractingArchivesList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            throw new RuntimeException("当前编号已存在");
        }
        if (majorSubcontractingArchives.getId() == null) {
            majorSubcontractingArchives.setId(KeyUtils.generateId() + "");
            majorSubcontractingArchives.setCreateTime(DateUtils.getNowDate());
            majorSubcontractingArchives.setCreateId(SecurityUtils.getUserId() + "");
            majorSubcontractingArchives.setCreateBy(SecurityUtils.getUsername());
            majorSubcontractingArchives.setValid(0L);
        }
        int i = majorSubcontractingArchivesMapper.insertMajorSubcontractingArchives(majorSubcontractingArchives);
//        if (i > 0) {
//            iSubcontractingDetailsService.addTypeByMain(majorSubcontractingArchives);
//        }
        return i;
    }

    /**
     * 修改专业分包档案主
     *
     * @param majorSubcontractingArchives 专业分包档案主
     * @return 结果
     */
    @Override
    public int updateMajorSubcontractingArchives(MajorSubcontractingArchives majorSubcontractingArchives) {
        if (majorSubcontractingArchives == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(majorSubcontractingArchives.getMajorSubcontractingCode())) {
            throw new RuntimeException("类型id不能为空");
        }
        MajorSubcontractingArchives mtrFeature1 = new MajorSubcontractingArchives();
        mtrFeature1.setMajorSubcontractingCode(majorSubcontractingArchives.getMajorSubcontractingCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setMajorSubcontractingClassId(majorSubcontractingArchives.getMajorSubcontractingClassId());
        List<MajorSubcontractingArchives> mtrFeatures = baseMapper.selectMajorSubcontractingArchivesList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            for (MajorSubcontractingArchives mtrFeature2 : mtrFeatures) {
                if (!majorSubcontractingArchives.getId().equals(mtrFeature2.getId())) {
                    throw new RuntimeException("当前编号已存在");
                }
            }
        }
        majorSubcontractingArchives.setUpdateTime(DateUtils.getNowDate());
        majorSubcontractingArchives.setUpdateBy(SecurityUtils.getUsername());
        int i = majorSubcontractingArchivesMapper.updateMajorSubcontractingArchives(majorSubcontractingArchives);
        if (i > 0) {
            iSubcontractingDetailsService.updateByHostId(majorSubcontractingArchives);
        }
        return i;
    }

    /**
     * 批量删除专业分包档案主
     *
     * @param ids 需要删除的专业分包档案主主键
     * @return 结果
     */
    @Override
    public boolean deleteMajorSubcontractingArchivesByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<MajorSubcontractingArchives> mtrFeatures = this.listByIds(Arrays.asList(ids));
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
            iSubcontractingDetailsService.deleteByHostId(ids, map);
        }
        return b;
    }

    /**
     * 删除专业分包档案主信息
     *
     * @param id 专业分包档案主主键
     * @return 结果
     */
    @Override
    public int deleteMajorSubcontractingArchivesById(String id) {
        return majorSubcontractingArchivesMapper.deleteMajorSubcontractingArchivesById(id);
    }


    @Override
    public MajorSubcontractingArchives initDetails(MajorSubcontractingArchives majorSubcontractingArchives) {
        if (majorSubcontractingArchives.getMajorSubcontractingClassId() == null) {
            throw new RuntimeException("类型id不能为空");
        }
        MajorSubcontractingClass subcontractingType = iMajorSubcontractingClassService.selectMajorSubcontractingClassById(majorSubcontractingArchives.getMajorSubcontractingClassId());
        if (subcontractingType == null) {
            throw new BusinessException("请选择正确的类型");
        }
        String materialCode = subcontractingType.getMajorSubcontractingClassCode();
        Integer maxCode = baseMapper.getMaxCode(majorSubcontractingArchives.getMajorSubcontractingClassId(), subcontractingType.getMajorSubcontractingClassCode());
        if (maxCode != null) {
            maxCode += 1;
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
        MajorSubcontractingArchives materialType1 = new MajorSubcontractingArchives();
        materialType1.setId(KeyUtils.generateId() + "");
        materialType1.setMajorSubcontractingClassId(subcontractingType.getId());
        materialType1.setMajorSubcontractingCode(materialCode);
        materialType1.setCreateId(SecurityUtils.getUserId() + "");
        materialType1.setCreateBy(SecurityUtils.getUsername());
        materialType1.setCreateTime(DateUtils.getNowDate());
        return materialType1;
    }

    @Override
    public long selectMajorSubcontractingArchivesListCount(MajorSubcontractingArchives majorSubcontractingArchives) {
        return baseMapper.selectMajorSubcontractingArchivesListCount(majorSubcontractingArchives);
    }

}
