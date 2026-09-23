package com.zhaocai.business.bidding.vo.req.query;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/5/27 9:29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TenderNoticeQueryDTO", description = "招标公告列表息查询VO")
public class TenderNoticeQueryVO extends PageRecive implements Serializable {
    private static final long serialVersionUID = -2228295662256969434L;
}
