package com.zhaocai.business.pub.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.pub.domain.DwMmAssetInf;
import org.apache.ibatis.annotations.Param;

/**
 * 资产 dm0731/材料分类Mapper接口
 *
 * @author chenming
 * @date 2024-08-28
 */
public interface DwMmAssetInfMapper extends BaseMapper<DwMmAssetInf> {

    /**
     * 删除所有数据
     * @return
     */
    @InterceptorIgnore(blockAttack = "true")
    int deleteAll();

    /**
     * 查询总数据量
     * @return
     */
    int countAllData();
}
