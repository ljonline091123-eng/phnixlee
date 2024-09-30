package com.zhaocai.business.pub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.pub.domain.BusinessCode;
import org.apache.ibatis.annotations.Param;

/**
 * 系统业务编号规则Mapper接口
 *
 * @author WH
 * @date 2024-05-27
 */
public interface BusinessCodeMapper extends BaseMapper<BusinessCode> {

    /**
     * codeNumber + 1
     *
     * @param id
     * @param number
     * @param currentDate
     */
    void updateCodeNumberIncr(@Param("id") Long id, @Param("number") Long number, @Param("nowDate") String currentDate);
}
