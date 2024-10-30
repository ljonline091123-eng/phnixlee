package com.zhaocai.business.manager.http.dto.res;

import lombok.Data;

/**
 * 查询流程分组/rest/bpm/catalog/listCatalog  返回对象
 */
@Data
public class ListCataLogDTO {

    /** 接口数据返回示例数据,二级单位的，以后可能会有三级单位数据存在
     * [
     *   {
     *     "catalogName": "湖南建工集团有限公司（总承包）",
     *     "catalogKey": "2001000000000",
     *     "systemKey": "jiantou-zhaocai"
     *   },
     *   {
     *     "catalogName": "湖南建设投资集团有限责任公司",
     *     "catalogKey": "1000000000",
     *     "systemKey": "jiantou-zhaocai"
     *   },
     *   {
     *     "catalogName": "湖南建工集团有限公司（总承包）",
     *     "catalogKey": "2001000000",
     *     "systemKey": "jiantou-zhaocai"
     *   },
     *   {
     *     "catalogName": "湖南建工集团有限公司直属一公司",
     *     "catalogKey": "2001000000001",
     *     "systemKey": "jiantou-zhaocai"
     *   }
     * ]
     * Time:2024/10/29 下午6:10
     * */

    /**
     * 分组名称
     */
    private String catalogName;

    /**
     * 分组key
     */
    private String catalogKey;

    /**
     * 系统key
     */
    private String systemKey;
}
