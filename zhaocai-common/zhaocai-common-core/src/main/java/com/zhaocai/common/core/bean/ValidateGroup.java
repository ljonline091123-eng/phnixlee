package com.zhaocai.common.core.bean;

import javax.validation.GroupSequence;

/**
 * @Validated字段校验分组接口
 */
public interface ValidateGroup {
    /**
     * validate 新增 分组校验
     */
    public interface AddGroup {
    }

    /**
     * validate 删除 分组校验
     */
    public interface DeleteGroup {
    }


    /**
     * validate 更新 分组校验
     */
    public interface UpdateGroup {
    }


    /**
     * validate  其他校验
     */
    public interface OtherGroup {
    }

    /**
     * validate  其他校验
     */
    public interface CustomGroup {
    }

    /**
     * validate  查询校验
     */
    public interface QueryGroup {
    }


    /**
     * validate 新增和更新 分组校验
     */
    @GroupSequence({AddGroup.class, UpdateGroup.class})
    public interface Group {

    }
}
