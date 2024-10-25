package com.zhaocai.business.expert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.expert.domain.Expert;
import com.zhaocai.business.expert.vo.req.ExpertVO;
import com.zhaocai.business.expert.vo.req.query.ExpertQueryVO;
import com.zhaocai.business.expert.vo.res.ExpertInfoVO;
import com.zhaocai.business.expert.vo.res.ExpertListVO;
import com.zhaocai.business.expert.vo.res.TPIExpertInfoVO;
import com.zhaocai.business.process.service.IProcessBusinessBaseService;
import com.zhaocai.common.core.bean.PageResult;

import java.util.List;

/**
 * 专家Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface IExpertService  extends IService<Expert> , IProcessBusinessBaseService {

    /**
     * 获取第三方专家信息
     */
    List<TPIExpertInfoVO> getTPIExpertInfo();

    /**
     * 分页查询专家列表
     */
    PageResult<ExpertListVO> page(ExpertQueryVO queryDTO);

    /**
     * 查询专家
     *
     * @param id 专家主键
     * @return 专家
     */
    public ExpertInfoVO getInfo(Long id);

    /**
     * 新增专家
     *
     * @param expertVO 专家信息
     * @return 结果
     */
    public boolean add(ExpertVO expertVO);

    /**
     * 批量删除专家
     *
     * @param ids 需要删除的专家主键集合
     * @return 结果
     */
    public boolean delete(List<Long> ids);

    /**
     * 修改启用状态
     *
     * @param id
     * @param state
     * @return
     */
    boolean updateStatus(Long id, Integer state);

}
