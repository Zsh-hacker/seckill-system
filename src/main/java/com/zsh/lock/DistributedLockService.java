package com.zsh.lock;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁服务接口
 */
public interface DistributedLockService {

    /**
     * 尝试获取锁
     */
    boolean tryLock(String lockKey);

    /**
     * 尝试获取锁（带超时时间）
     */
    boolean tryLock(String lockKey, long waitTime, TimeUnit unit);

    /**
     * 尝试获取锁（带超时时间和租约时间）
     */
    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 释放锁
     */
    void  unlock(String lockKey);

    /**
     * 强制释放锁
     */
    boolean forceUnlock(String lockKey);

    /**
     * 检查所是否被持有
     */
    boolean isLocked(String lockKey);

    /**
     * 获取锁剩余时间
     */
    long getRemainTime(String lockKey, TimeUnit unit);

    /**
     * 续期锁
     */
    boolean renewLock(String lockKey, long leaseTime, TimeUnit unit);
}
