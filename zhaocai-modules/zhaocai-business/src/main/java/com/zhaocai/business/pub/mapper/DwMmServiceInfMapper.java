package com.zhaocai.business.pub.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.pub.domain.DwMmServiceInf;

/**
 * 服务 dm073Mapper接口
 *
 * @author WH
 * @date 2024-08-29
 */
public interface DwMmServiceInfMapper extends BaseMapper<DwMmServiceInf> {

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
