package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.ISubcontractingItemService;
import com.zhaocai.archives.main.domain.MajorSubcontractingClass;
import com.zhaocai.archives.main.domain.MajorSubcontractingFeature;
import com.zhaocai.archives.main.mapper.MajorSubcontractingFeatureMapper;
import com.zhaocai.archives.main.service.IMajorSubcontractingFeatureService;
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
 * 专业分包特征项主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class MajorSubcontractingFeatureServiceImpl extends ServiceImpl<MajorSubcontractingFeatureMapper, MajorSubcontractingFeature> implements IMajorSubcontractingFeatureService {
    @Autowired
    private MajorSubcontractingFeatureMapper majorSubcontractingFeatureMapper;

    @Resource
    private ISubcontractingItemService iSubcontractingItemService;

    /**
     * 查询专业分包特征项主
     *
     * @param id 专业分包特征项主主键
     * @return 专业分包特征项主
     */
    @Override
    public MajorSubcontractingFeature selectMajorSubcontractingFeatureById(String id) {
        return majorSubcontractingFeatureMapper.selectMajorSubcontractingFeatureById(id);
    }

    /**
     * 查询专业分包特征项主列表
     *
     * @param majorSubcontractingFeature 专业分包特征项主
     * @return 专业分包特征项主
     */
    @Override
    public List<MajorSubcontractingFeature> selectMajorSubcontractingFeatureList(MajorSubcontractingFeature majorSubcontractingFeature) {
        if (majorSubcontractingFeature == null) {
            majorSubcontractingFeature = new MajorSubcontractingFeature();
        }
        majorSubcontractingFeature.setValid(0L);
        return majorSubcontractingFeatureMapper.selectMajorSubcontractingFeatureList(majorSubcontractingFeature);
    }

    /**
     * 新增专业分包特征项主
     *
     * @param majorSubcontractingFeature 专业分包特征项主
     * @return 结果
     */
    @Override
    public synchronized int insertMajorSubcontractingFeature(MajorSubcontractingFeature majorSubcontractingFeature) {
        if (majorSubcontractingFeature == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(majorSubcontractingFeature.getMajorSubcontractingClassId())) {
            throw new RuntimeException("类型id不能为空");
        }
        MajorSubcontractingFeature mtrFeature1 = new MajorSubcontractingFeature();
        mtrFeature1.setFeatureCode(majorSubcontractingFeature.getFeatureCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setMajorSubcontractingClassId(majorSubcontractingFeature.getMajorSubcontractingClassId());
        List<MajorSubcontractingFeature> mtrFeatures = baseMapper.selectMajorSubcontractingFeatureList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            throw new RuntimeException("当前特征项编号已存在");
        }
        if (majorSubcontractingFeature.getId() == null) {
            majorSubcontractingFeature.setId(KeyUtils.generateId() + "");
            majorSubcontractingFeature.setCreateTime(DateUtils.getNowDate());
            majorSubcontractingFeature.setCreateId(SecurityUtils.getUserId() + "");
            majorSubcontractingFeature.setCreateBy(SecurityUtils.getUsername());
            majorSubcontractingFeature.setValid(0L);
        }
        int i = majorSubcontractingFeatureMapper.insertMajorSubcontractingFeature(majorSubcontractingFeature);
//        if (i > 0) {
//            iSubcontractingItemService.addTypeByMain(majorSubcontractingFeature);
//        }
        return i;
    }

    /**
     * 修改专业分包特征项主
     *
     * @param majorSubcontractingFeature 专业分包特征项主
     * @return 结果
     */
    @Override
    public int updateMajorSubcontractingFeature(MajorSubcontractingFeature majorSubcontractingFeature) {
        if (majorSubcontractingFeature == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(majorSubcontractingFeature.getMajorSubcontractingClassId())) {
            throw new RuntimeException("类型id不能为空");
        }
        MajorSubcontractingFeature mtrFeature1 = new MajorSubcontractingFeature();
        mtrFeature1.setFeatureCode(majorSubcontractingFeature.getFeatureCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setMajorSubcontractingClassId(majorSubcontractingFeature.getMajorSubcontractingClassId());
        List<MajorSubcontractingFeature> mtrFeatures = baseMapper.selectMajorSubcontractingFeatureList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            for (MajorSubcontractingFeature mtrFeature2 : mtrFeatures) {
                if (!majorSubcontractingFeature.getId().equals(mtrFeature2.getId())) {
                    throw new RuntimeException("当前特征项编号已存在");
                }
            }
        }
        majorSubcontractingFeature.setUpdateTime(DateUtils.getNowDate());
        majorSubcontractingFeature.setUpdateBy(SecurityUtils.getUsername());
        int i = majorSubcontractingFeatureMapper.updateMajorSubcontractingFeature(majorSubcontractingFeature);
        if (i > 0) {
            iSubcontractingItemService.updateByHostId(majorSubcontractingFeature);
        }
        return i;
    }

    /**
     * 批量删除专业分包特征项主
     *
     * @param ids 需要删除的专业分包特征项主主键
     * @return 结果
     */
    @Override
    public boolean deleteMajorSubcontractingFeatureByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<MajorSubcontractingFeature> mtrFeatures = this.listByIds(Arrays.asList(ids));
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
            iSubcontractingItemService.deleteByHostId(ids,map);
        }
        return b;
    }

    /**
     * 删除专业分包特征项主信息
     *
     * @param id 专业分包特征项主主键
     * @return 结果
     */
    @Override
    public int deleteMajorSubcontractingFeatureById(String id) {
        return majorSubcontractingFeatureMapper.deleteMajorSubcontractingFeatureById(id);
    }

    @Override
    public MajorSubcontractingFeature initCode(MajorSubcontractingClass mtrClass) {
        String maxCode = majorSubcontractingFeatureMapper.getMaxCode(mtrClass.getId());
        MajorSubcontractingFeature subcontractingFeature = new MajorSubcontractingFeature();
        subcontractingFeature.setMajorSubcontractingClassId(mtrClass.getId());
        subcontractingFeature.setFeatureCode(maxCode + 1);
        subcontractingFeature.setCreateBy(SecurityUtils.getUsername());
        subcontractingFeature.setCreateId(SecurityUtils.getUserId() + "");
        subcontractingFeature.setCreateTime(DateUtils.getNowDate());
        return subcontractingFeature;
    }

    @Override
    public long selectMajorSubcontractingFeatureListCount(MajorSubcontractingFeature majorSubcontractingFeature) {
        return baseMapper.selectMajorSubcontractingFeatureListCount(majorSubcontractingFeature);
    }
}
