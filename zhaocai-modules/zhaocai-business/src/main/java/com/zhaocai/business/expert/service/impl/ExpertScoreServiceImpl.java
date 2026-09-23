package com.zhaocai.business.expert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.vo.req.EvalVendorCountVO;
import com.zhaocai.business.expert.domain.ExpertScore;
import com.zhaocai.business.expert.mapper.ExpertScoreMapper;
import com.zhaocai.business.expert.service.IExpertScoreService;
import org.springframework.stereotype.Service;

/**
 * 专家评分Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
public class ExpertScoreServiceImpl extends ServiceImpl<ExpertScoreMapper,ExpertScore> implements IExpertScoreService {

    @Override
    public Integer getNotEvalVendorCount(EvalVendorCountVO queryDTO) {
        return baseMapper.findNotEvalVendorCount(queryDTO);
    }

}
