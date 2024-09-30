package com.zhaocai.business.bidding.service.impl;

import com.zhaocai.business.common.exception.ParamValidateException;

import java.util.LinkedList;

/**
 * @author ssy
 * @date 2024/6/30 14:33
 */
public class TenderFlowService {

    public Integer nextFlow(LinkedList<Integer> statusEnumList, Integer status){
        int index = statusEnumList.indexOf(status);
        if (index == -1){
            throw new ParamValidateException("未查询到当前流程数据");
        }
        if (index + 1 >= statusEnumList.size()){
            throw new ParamValidateException("未查询到下一流程数据");
        }

        return statusEnumList.get(index + 1);
    }

}
