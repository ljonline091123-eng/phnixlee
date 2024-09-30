package com.zhaocai.common.core.config;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * @author ssy
 * @date 2024/8/14 14:17
 */
public interface MyBaseMapper <T> extends BaseMapper<T> {

    /**
     * 批量插入
     * */
    int insertBatchSomeColumn(@Param("list") List<T> batchList);

    /**
     * 通过ID批量更新数据
     *
     * @param entityList 实体列表
     * @return 影响行数
     */
    int updateBatchColumnById(@Param("list") Collection<T> entityList, @Param("ew") Wrapper<T> updateWrapper);

}
