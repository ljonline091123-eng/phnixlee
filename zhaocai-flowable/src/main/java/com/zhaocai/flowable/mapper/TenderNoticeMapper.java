package com.zhaocai.flowable.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * @author ssy
 * @date 2024/6/23 14:17
 */
public interface TenderNoticeMapper {

    void updateTenderNoticeStatus(@Param("tenderNoticeId") Long tenderNoticeId, @Param("noticeStatus") Integer noticeStatus);

}
