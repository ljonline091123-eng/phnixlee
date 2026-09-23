package com.zhaocai.business.pub.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.pub.domain.Message;
import com.zhaocai.business.pub.mapper.MessageMapper;
import com.zhaocai.business.pub.service.IMessageService;
import com.zhaocai.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息中心Service业务层处理
 * 
 * @author WH
 * @date 2024-05-24
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper,Message> implements IMessageService
        {
    @Autowired
    private MessageMapper messageMapper;

    /**
     * 查询消息中心
     * 
     * @param id 消息中心主键
     * @return 消息中心
     */
    @Override
    public Message selectMessageById(String id)
    {
        return messageMapper.selectMessageById(id);
    }

    /**
     * 查询消息中心列表
     * 
     * @param message 消息中心
     * @return 消息中心
     */
    @Override
    public List<Message> selectMessageList(Message message)
    {
        return messageMapper.selectMessageList(message);
    }

    /**
     * 新增消息中心
     * 
     * @param message 消息中心
     * @return 结果
     */
    @Override
    public int insertMessage(Message message)
    {
        message.setCreateTime(DateUtils.getNowDate());
        return messageMapper.insertMessage(message);
    }

    /**
     * 修改消息中心
     * 
     * @param message 消息中心
     * @return 结果
     */
    @Override
    public int updateMessage(Message message)
    {
        message.setUpdateTime(DateUtils.getNowDate());
        return messageMapper.updateMessage(message);
    }

    /**
     * 批量删除消息中心
     * 
     * @param ids 需要删除的消息中心主键
     * @return 结果
     */
    @Override
    public int deleteMessageByIds(String[] ids)
    {
        return messageMapper.deleteMessageByIds(ids);
    }

    /**
     * 删除消息中心信息
     * 
     * @param id 消息中心主键
     * @return 结果
     */
    @Override
    public int deleteMessageById(String id)
    {
        return messageMapper.deleteMessageById(id);
    }
}
