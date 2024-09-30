package com.zhaocai.business.manager.http.dto.res;


import lombok.Data;


/**
 * 流程服务-流程操作日志列表返回
 */
@Data
public class BpmListProcessLogResponseDTO {

        /**
         * 任务key
         */
      private String taskKey;
      /**
       * 任务名称
       */
      private String taskName;
      /**
       * 前一个处理人id
       */
      private String preHandlerId;
      /**
       * 前一个处理人名称
       */
      private String preHandlerName;
      /**
       * 当前操作人id
       */
      private String handlerId;
      /**
       * 当前操作人名称
       */
      private String handlerName;
      /**
       * 操作code
       */
      private String operateCode;
      /**
       * 操作名称
       */
      private String operateName;
      /**
       * 是否已处理
       */
      private Boolean handled;
      /**
       * 操作备注
       */
      private String operateRemark;
      /**
       * 操作批语
       */
      private String operateComment;
      /**
       * 开始时间
       */
      private String startTime;
      /**
       * 结束时间
       */
      private String endTime;

}
