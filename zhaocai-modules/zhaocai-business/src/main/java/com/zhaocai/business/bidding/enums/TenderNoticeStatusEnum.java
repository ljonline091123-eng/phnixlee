package com.zhaocai.business.bidding.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author ssy
 * @date 2024/5/27 17:02
 */
@Getter
@AllArgsConstructor
public enum TenderNoticeStatusEnum {

    /**
     * 增加了三个状态，原来逻辑是发布了招标状态就设置为1，然后供应商可以投标
     * 现在是增加了 11，12，13 发布了设置状态为11，供应商报名了设置为12(管理端就可以进入下一步招标文件了。但是要先进入报名截至状态)先选中通过的供应商列表
     * 然后在13状态下进入发布招标文件页面去填写招标文件的数据再去发布 设置状态为1 供应商就可以投标了。（和原来逻辑不冲突，只是修改了类型为公开招标的数据。）
     * Time:2024/9/26 上午11:54
     * */
    /**
     * 招标公告-招标阶段状态
     */

    ABANDON_BID(0, "废标"),
    TENDER_NOTICE(11, "发布(公告)"),
    TENDER_REGISTER(12, "发布(报名情况)"),
    TENDER_REGISTER_SAVE(13, "发布(报名截止)"),
    TENDER_ISSUE(1, "发布(招标文件)"),
    BID_OPENING(2, "开标"),
    EVALUATION_BID(3, "评标"),
    @Deprecated
    SECOND_NEG(4, "二次洽商"),
    CALI_REPORT(5, "定标"),
    WINNING_BID(6, "中标公示"),
    RESULT_RELEASE(7, "结果发布"),
    COMPLETE(8, "完成"),

    ;

    private final Integer state;

    private final String desc;

    /**
     * 根据编码查询枚举
     *
     * @param state
     * @return
     */
    public static String getValueByCode(Integer state) {
        if (Objects.isNull(state)) {
            return null;
        }
        for (TenderNoticeStatusEnum typeEnum : values()) {
            if (typeEnum.state.equals(state)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }

}
