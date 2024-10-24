package com.zhaocai.business.agreement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.agreement.domain.MarketMaterialContract;
import com.zhaocai.business.agreement.vo.req.MarketMaterialContractQueryVO;
import com.zhaocai.business.agreement.vo.res.MarketMaterialContractListVO;
import org.apache.ibatis.annotations.Param;

/**
 * 易料采购合同信息Mapper接口
 *
 * @author lsn
 * @date 2024-10-22
 */
public interface MarketMaterialContractMapper extends BaseMapper<MarketMaterialContract> {

    IPage<MarketMaterialContractListVO> listMarketMaterialContract(Page mybatisPage, @Param("queryVO") MarketMaterialContractQueryVO queryVO);
}
