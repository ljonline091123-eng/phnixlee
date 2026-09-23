package com.zhaocai.business.vendor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.vendor.domain.VendorCertification;
import org.apache.ibatis.annotations.Param;

/**
 * 供应商附件Mapper接口
 * 
 * @author chenming
 * @date 2024-05-29
 */
public interface VendorCertificationMapper extends BaseMapper<VendorCertification> {

    int updateByVendorId(@Param("id") Long id,@Param("uuid")  Long uuid);
}
