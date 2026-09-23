package com.zhaocai.business.bidding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.bidding.domain.BiddingResult;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 投标结果信息Mapper接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface BiddingResultMapper extends BaseMapper<BiddingResult> {

    /**
     * 获取采购方案的投标供应商
     *
     * @param schemeId
     * @return
     */
    List<BiddingResult> selectBiddingVendorBySchemeId(@Param("schemeId") Long schemeId);


    @Delete("delete from tb_bidding_result where notice_id = #{noticeId} ")
    void deleteByNoticeId(Long noticeId);
}
