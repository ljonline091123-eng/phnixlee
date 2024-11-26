package com.zhaocai.system.api.factory;

import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.system.api.business.RemoteBusinessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @author ssy
 * @date 2024/6/1 11:37
 */
@Component
public class RemoteBusinessFallbackFactory implements FallbackFactory<RemoteBusinessService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteBusinessFallbackFactory.class);

    @Override
    public RemoteBusinessService create(Throwable throwable) {
        log.error("文件服务调用失败:{}", throwable.getMessage());
        return new RemoteBusinessService() {
            @Override
            public ResultData<Boolean> handleNoticeIssueStatus(String source) {
                return ResultData.fail("处理招标公告已发布阶段任务失败:" + throwable.getMessage());
            }

            @Override
            public ResultData<Boolean> handleNoticePublicityStatus(String source) {
                return ResultData.fail("处理招标公告已公示阶段任务失败:" + throwable.getMessage());
            }

            @Override
            public ResultData<Boolean> syncDept(String source) {
                return ResultData.fail("同步第三方部门数据任务失败:" + throwable.getMessage());
            }

            @Override
            public ResultData<Boolean> syncUser(String source) {
                return ResultData.fail("同步第三方用户数据任务失败:" + throwable.getMessage());
            }

            @Override
            public ResultData<Boolean> syncAreaDivision(String source) {
                return ResultData.fail("同步第三方行政区划失败:" + throwable.getMessage());
            }

            @Override
            public ResultData<Boolean> syncCountry(String source) {
                return ResultData.fail("同步第三方国家和地区档案失败:" + throwable.getMessage());
            }

            @Override
            public ResultData receiptProject(String source) {
                return ResultData.fail("同步第三方项目数据失败:" + throwable.getMessage());
            }

            @Override
            public ResultData syncAccount(String source) {
                return ResultData.fail("同步第三方支行数据失败:" + throwable.getMessage());
            }

        };
    }

}
