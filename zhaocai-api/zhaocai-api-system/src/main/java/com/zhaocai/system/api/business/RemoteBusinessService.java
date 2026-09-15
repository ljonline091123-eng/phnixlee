package com.zhaocai.system.api.business;

import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.constant.ServiceNameConstants;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.system.api.factory.RemoteBusinessFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * @author ssy
 * @date 2024/6/1 11:37
 */
@FeignClient(contextId = "remoteBusinessService", value = ServiceNameConstants.BUSINESS_SERVICE, fallbackFactory = RemoteBusinessFallbackFactory.class)
public interface RemoteBusinessService {

    /**
     * 处理招标公告已发布阶段
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/notice/handleTenderNoticeIssueStatus")
    public ResultData<Boolean> handleNoticeIssueStatus(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 处理招标公告已公示阶段
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/notice/handleTenderNoticePublicityStatus")
    public ResultData<Boolean> handleNoticePublicityStatus(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 同步第三方部门数据
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/syncPlatformBasicData/syncDept")
    public ResultData<Boolean> syncDept(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 同步第三方用户数据
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/syncPlatformBasicData/syncUser")
    public ResultData<Boolean> syncUser(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 同步第三方行政区划
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/syncPlatformBasicData/syncAreaDivision")
    public ResultData<Boolean> syncAreaDivision(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 同步第三方国家和地区档案
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/syncPlatformBasicData/syncCountry")
    public ResultData<Boolean> syncCountry(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);


    /**
     * 接收项目同步
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/project/receiptProject")
    public ResultData receiptProject(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);


    @GetMapping("/syncPlatformBasicData/syncAccount")
    public ResultData<Boolean> syncAccount(@RequestHeader(SecurityConstants.FROM_SOURCE) String inner);

    @GetMapping("/vendor/removeBlacklist")
    public ResultData<Boolean> removeBlacklist(@RequestHeader(SecurityConstants.FROM_SOURCE) String inner);

    /**
     * 刷新招标率报表数据
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/bidReport/handleBidReport")
    public ResultData<Boolean> handleBidReport(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 刷新供应商报表数据
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/vendorReport/handleVendorReport")
    public ResultData<Boolean> handleVendorReport(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 刷新问题报表数据
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/problemReport/handleProblemReport")
    public ResultData<Boolean> handleProblemReport(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
