package com.zhaocai.archives.utils;

/**
 * @author liaozhiqin
 * @date 2025/1/7
 */
public class KeyUtils {

    private static final KeyUtils KEY_ID = new KeyUtils(1);

    // 时间戳部分占10位，允许的时间范围为约172小时
    private static final int TIMESTAMP_BITS = 10;
    private static final long EPOCH = 807638400000L; // 自定义纪元时间戳（例如：2021-01-01）

    private static final long MAX_TIMESTAMP = ~(-1L << TIMESTAMP_BITS);

    // 机器ID部分占5位，支持最多32个节点
    private static final int WORKER_ID_BITS = 5;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    // 序列号部分占17位，每毫秒最多生成131072个ID
    private static final int SEQUENCE_BITS = 17;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;


    public static long generateId() {
        return KEY_ID.nextId();
    }


    /**
     * 构造函数
     *
     * @param workerId 工作机器ID
     */
    public KeyUtils(long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        this.workerId = workerId;
    }

    /**
     * 生成下一个ID
     *
     * @return 下一个ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << (WORKER_ID_BITS + SEQUENCE_BITS))
                | (workerId << SEQUENCE_BITS)
                | sequence;
    }

    /**
     * 等待直到获取到一个新的毫秒时间戳
     *
     * @param lastTimestamp 上一次的时间戳
     * @return 新的时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳
     *
     * @return 当前时间戳
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }


}

