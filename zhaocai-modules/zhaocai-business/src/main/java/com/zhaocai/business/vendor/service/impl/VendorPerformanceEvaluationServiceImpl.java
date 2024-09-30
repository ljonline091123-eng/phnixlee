package com.zhaocai.business.vendor.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.manager.http.service.PerformanceEvaluationService;
import com.zhaocai.business.vendor.domain.VendorPerformanceEvaluation;
import com.zhaocai.business.vendor.mapper.VendorPerformanceEvaluationMapper;
import com.zhaocai.business.vendor.service.IVendorPerformanceEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 供应商履约评价Service业务层处理
 *
 * @author WH
 * @date 2024-07-12
 */
@Service
@Slf4j
public class VendorPerformanceEvaluationServiceImpl extends ServiceImpl<VendorPerformanceEvaluationMapper, VendorPerformanceEvaluation> implements IVendorPerformanceEvaluationService {
    /**
     * 读写锁
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // 全量同步标识
    private volatile Boolean fullSyncFlag = false;

    @Autowired
    private PerformanceEvaluationService performanceEvaluationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Resource(name = "businessExecutor")
    private Executor executor;

    @Override
    public void syncVendorPerformanceEvaluationService() {
        // 获取锁状态
        boolean lockAcquired = false;
        /*
         * 重试次数，为了防止数据冲突，全量同步和增量同步需要使用互斥锁
         * 同时，为了全量同步需要尽量成功，所以需要重试多次，暂定 60 次
         */
        int retries = 1;
        log.info("[全量同步履约评价] - 开始执行全量同步履约评价任务");
        while (!lockAcquired && retries <= 30) {
            try {
                log.info("[全量同步履约评价] - 第[{}]次尝试获取写锁",retries);
                lockAcquired = lock.writeLock().tryLock(1,TimeUnit.SECONDS);
                if (lockAcquired) {
                    // 获取锁成功
                    log.info("[全量同步履约评价] - 第[{}]次尝试获取锁成功，更新同步标识、获取底层逻辑数据...",retries);
                    fullSyncFlag = true;

                    List<VendorPerformanceEvaluation> performanceEvaluations = performanceEvaluationService.listPerformanceEvaluation(null);
                    log.info("[全量同步履约评价] - 获取底层逻辑数据成功，数据量为:{}，开始更新数据库记录",performanceEvaluations.size());

                    if (CollectionUtil.isNotEmpty(performanceEvaluations)) {
                        doSyncVendorPerformanceEvaluation(null,performanceEvaluations,"全量同步履约评价");
                    }
                } else {
                    log.info("[全量同步履约评价] - 第[{}]次尝试获取锁失败，等待 2 秒后继续重试...",retries);
                    retries++;
                    TimeUnit.SECONDS.sleep(2);
                }
            } catch (InterruptedException te) {
                log.error("[全量同步履约评价] - 执行任务失败，原因：线程中断，cause by:{}",te.getMessage(),te);
                // 中断线程
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("[全量同步履约评价] - 执行任务失败，cause by:{}",e.getMessage(),e);
            } finally {
                if (lockAcquired) {
                    lock.writeLock().unlock();
                    fullSyncFlag = false;
                }
            }
        }
    }

    @Override
    public List<VendorPerformanceEvaluation> getAndSyncVendorPerformanceList(Long vendorId) {
        boolean lockAcquired = false;
        int retries = 1;
        log.info("[增量同步履约评价-{}] - 开始执行增量同步履约评价任务",vendorId);
        List<VendorPerformanceEvaluation> performanceEvaluations = null;
        while (!lockAcquired && retries <= 10) {
            try {
               if (fullSyncFlag) {
                   // 系统在进行全量同步，等待
                   log.warn("[增量同步履约评价-{}] -[] 在进行全量同步，等待 1 秒",vendorId);
                   TimeUnit.SECONDS.sleep(1);
                   continue;
               }
                // 系统不在进行全量同步，则直接获取读锁就可以了

                log.info("[增量同步履约评价-{}] - 第[{}]次尝试获取读锁",vendorId,retries);
                lockAcquired = lock.readLock().tryLock(1,TimeUnit.SECONDS);
                if (lockAcquired) {
                    // 获取锁成功
                    log.info("[增量同步履约评价-{}] - 第[{}]次尝试获取读锁，获取底层逻辑数据...",vendorId,retries);

                    // 使用同步锁，防止同一个供应商获取数据
                    synchronized (vendorId.toString()) {
                        performanceEvaluations = performanceEvaluationService.listPerformanceEvaluation(vendorId);
                        log.info("[增量同步履约评价-{}] - 获取底层逻辑数据成功，数据量为:{}",vendorId,performanceEvaluations.size());

                        if (CollectionUtil.isNotEmpty(performanceEvaluations)) {
                            List<VendorPerformanceEvaluation> finalPerformanceEvaluations = performanceEvaluations;
                            executor.execute(() -> {
                                log.info("[增量同步履约评价-{}] - 开始将数据插入数据库，数据量:{}",vendorId,finalPerformanceEvaluations.size());
                                doSyncVendorPerformanceEvaluation(vendorId, finalPerformanceEvaluations,"增量同步履约评价");
                            });
                        }
                    }
                } else {
                    log.info("[增量同步履约评价-{}] - 第[{}]次尝试获取锁失败，等待 2 秒后继续重试...",vendorId,retries);
                    retries++;
                    TimeUnit.SECONDS.sleep(2);
                }
            } catch (InterruptedException te) {
                log.error("[增量同步履约评价-{}] - 执行任务失败，原因：线程中断，cause by:{}",vendorId,te.getMessage(),te);
                // 中断线程
                Thread.currentThread().interrupt();
                throw new BusinessException("供应商履约评价正在同步中，请稍后重试");
            } catch (Exception e) {
                log.error("[增量同步履约评价-{}] - 执行任务失败，cause by:{}",vendorId,e.getMessage(),e);
                throw new BusinessException("供应商履约评价同步失败，请联系开发");
            } finally {
                if (lockAcquired) {
                    lock.readLock().unlock();
                }
            }
        }

        return performanceEvaluations;
    }


    /**
     * 更新数据库记录
     * @param vendorId
     * @param performanceEvaluations
     */
    private void doSyncVendorPerformanceEvaluation(Long vendorId, List<VendorPerformanceEvaluation> performanceEvaluations,String operate) {
        // 获取事务管理器
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);
        try {
            // 删除记录
            int deleteCount = baseMapper.deleteByVendorId(vendorId);

            // 保存
            super.saveBatch(performanceEvaluations);
            log.error("[{}] - 执行数据库操作成功，删除记录:{},新增记录:{}，提交事务",operate,deleteCount,performanceEvaluations.size());

            // 提交事务
            transactionManager.commit(status);
        } catch (Exception e) {
            log.error("[{}] - 执行数据库操作失败，开始回滚事务,cause by :{}",operate,e.getMessage(),e);

            // 回滚事务
            transactionManager.rollback(status);
        }
    }


}
