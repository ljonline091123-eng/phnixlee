package com.zhaocai.business.pub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.pub.domain.Message;

import java.util.List;

/**
 * 消息中心Service接口
 * 
 * @author WH
 * @date 2024-05-24
 */
public interface IMessageService  extends IService<Message>
{
    /**
     * 查询消息中心
     * 
     * @param id 消息中心主键
     * @return 消息中心
     */
    public Message selectMessageById(String id);

    /**
     * 查询消息中心列表
     * 
     * @param message 消息中心
     * @return 消息中心集合
     */
    public List<Message> selectMessageList(Message message);

    /**
     * 新增消息中心
     * 
     * @param message 消息中心
     * @return 结果
     */
    public int insertMessage(Message message);

    /**
     * 修改消息中心
     * 
     * @param message 消息中心
     * @return 结果
     */
    public int updateMessage(Message message);

    /**
     * 批量删除消息中心
     * 
     * @param ids 需要删除的消息中心主键集合
     * @return 结果
     */
    public int deleteMessageByIds(String[] ids);

    /**
     * 删除消息中心信息
     * 
     * @param id 消息中心主键
     * @return 结果
     */
    public int deleteMessageById(String id);
}
