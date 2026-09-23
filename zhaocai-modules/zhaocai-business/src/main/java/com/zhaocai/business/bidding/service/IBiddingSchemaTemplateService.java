package com.zhaocai.business.bidding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.bidding.domain.BiddingSchemaTemplate;
import com.zhaocai.business.bidding.vo.req.BiddingSchemaTemplateVO;
import com.zhaocai.business.bidding.vo.res.BiddingSchemaTemplateDetailVO;

/**
 * 采购方案评分模板关联关系Service接口
 *
 * @author WH
 * @date 2024-06-18
 */
public interface IBiddingSchemaTemplateService  extends IService<BiddingSchemaTemplate> {

    /**
     * 新增或修改采购方案评分模板关联关系
     *
     * @param biddingSchemaTemplateVO 新增或修改采购方案评分模板关联关系
     * @return 结果
     */
    boolean addOrUpdate(BiddingSchemaTemplateVO biddingSchemaTemplateVO);

    /**
     * 查询采购方案评分模板关联关系信息
     *
     * @param schemeId 方案id
     * @return 采购方案评分模板关联关系信息
     */
    BiddingSchemaTemplateDetailVO detail(Long schemeId);


}
