package com.zhaocai.business.bidding.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.BiddingMarkTemplate;
import com.zhaocai.business.bidding.domain.BiddingSchemaTemplate;
import com.zhaocai.business.bidding.mapper.BiddingSchemaTemplateMapper;
import com.zhaocai.business.bidding.service.IBiddingMarkTemplateService;
import com.zhaocai.business.bidding.service.IBiddingSchemaTemplateService;
import com.zhaocai.business.bidding.vo.req.BiddingSchemaTemplateVO;
import com.zhaocai.business.bidding.vo.res.BiddingSchemaTemplateDetailVO;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 采购方案评分模板关联关系Service业务层处理
 *
 * @author WH
 * @date 2024-06-18
 */
@Service
public class BiddingSchemaTemplateServiceImpl extends ServiceImpl<BiddingSchemaTemplateMapper,BiddingSchemaTemplate> implements IBiddingSchemaTemplateService {

    @Autowired
    private IBiddingMarkTemplateService biddingMarkTemplateService;

    @Override
    public boolean addOrUpdate(BiddingSchemaTemplateVO biddingSchemaTemplateVO) {
        BiddingSchemaTemplate schemaTemplate = BeanCopierUtil.copyBean(biddingSchemaTemplateVO, BiddingSchemaTemplate.class);
        //删除采购方案关联旧的评分模板数据
        this.remove(new LambdaQueryWrapper<BiddingSchemaTemplate>()
                .eq(BiddingSchemaTemplate::getSchemeId, schemaTemplate.getSchemeId()));
        //新增最新的方案关联的评分模板数据
        return this.save(schemaTemplate);
    }

    @Override
    public BiddingSchemaTemplateDetailVO detail(Long schemeId) {
        BiddingSchemaTemplate schemaTemplate = this.getOne(new LambdaQueryWrapper<BiddingSchemaTemplate>()
                .eq(BiddingSchemaTemplate::getSchemeId, schemeId));
        BiddingSchemaTemplateDetailVO vo = BeanCopierUtil.copyBean(schemaTemplate, BiddingSchemaTemplateDetailVO.class);
        BiddingMarkTemplate markTemplate = biddingMarkTemplateService.getById(schemaTemplate.getTemplateId());
        vo.setTemplateName(markTemplate.getName());
        return vo;
    }

}
