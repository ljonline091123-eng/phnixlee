package com.zhaocai.business.filez.service;

import com.zhaocai.business.filez.vo.req.FileZCallBackVO;

/**
 * 联想 FileZ 回调处理接口
 *
 * @author chenming
 * @date 2024-07-04
 */
public interface IFileZCallBackService {

    /**
     * 处理文档内容操作回调
     * @param callBack
     * @param fileZTaskId
     */
    void contentUpdateHandler(FileZCallBackVO callBack, long fileZTaskId);
}
