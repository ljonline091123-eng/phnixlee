package com.zhaocai.business.filez.vo.req;

import lombok.Data;


/**
 * 联想文档回调详情 vo
 *
 * @author chenming
 * @date 2024-07-22
 */
@Data
public class FileZCallBackDetailVO {

    private String taskStatus;

    private String defaultDownloadPath;

    private String contentId;

    private String filename;
}
