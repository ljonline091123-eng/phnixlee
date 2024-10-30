package com.zhaocai.business.bidding.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.bean.PageRecive;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 获取招标公告列表-(第三方-招标公告接口) 接收参数
 */
@Data
public class UnderlingTenderNoticeQueryVO extends PageRecive {

    /** 招标公告id */
    private Long id;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date endTime;


}
