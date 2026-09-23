package com.zhaocai.business.pub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.pub.domain.AreaDivision;
import com.zhaocai.business.pub.vo.res.AreaDivisionVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 行政区划Mapper接口
 *
 * @author WH
 * @date 2024-07-12
 */
public interface AreaDivisionMapper extends BaseMapper<AreaDivision> {

    Boolean deleteSyncAreaDivision();

    /**
     * 根据 parentCode 获取行政区划
     *
     * @param parentCode
     * @return
     */
    List<AreaDivisionVO> selectAreaDivisionListByParentCode(@Param("parentCode") String parentCode);
}
