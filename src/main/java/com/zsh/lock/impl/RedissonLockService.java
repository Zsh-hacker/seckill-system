package com.zsh.lock.impl;

import com.zsh.lock.DistributedLockService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedissonLockService implements DistributedLockService {

    private final RedissonClient redissonClient;

    // 本地缓存锁引用，避免重复创建
    private final ConcurrentHashMap<String, RLock> lockCache = new ConcurrentHashMap<>();

    // 默认锁配置
    private static final long DEFAULT_WAIT_TIME = 3;
    private static final long DEFAULT_LEASE_TIME = 30;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    @Override
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, DEFAULT_TIME_UNIT);
    }

    @Override
    public boolean tryLock(String lockKey, long waitTime, TimeUnit unit) {
        return tryLock(lockKey, waitTime, DEFAULT_LEASE_TIME, unit);
    }

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, unit);
            if (acquired) {
                log.debug("Lock acquired: {}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Lock interrupted: {}", lockKey, e);
            return false;
        } catch (Exception e) {
            log.error("Failed to acquire lock: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        RLock lock = lockCache.get(lockKey);
        if (lock != null && lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
                log.debug("Lock released: {}", lockKey);

                // 清理本地缓存
                if (!lock.isLocked()) {
                    lockCache.remove(lockKey);
                }
            } catch (IllegalMonitorStateException e) {
                log.warn("Lock not held by current thread: {}", lockKey);
            } catch (Exception e) {
                log.error("Failed to release lock: {}", lockKey, e);
            }
        }
    }

    @Override
    public boolean forceUnlock(String lockKey) {
        RLock lock = lockCache.get(lockKey);
        if (lock != null) {
            try {
                lock.forceUnlock();
                lockCache.remove(lockKey);
                log.debug("Lock force released: {}", lockKey);
                return true;
            } catch (Exception e) {
                log.error("Failed to force unlock: {}", lockKey, e);
            }
        }
        return false;
    }

    @Override
    public boolean isLocked(String lockKey) {
        RLock lock = lockCache.get(lockKey);
        return lock != null && lock.isLocked();
    }

    @Override
    public long getRemainTime(String lockKey, TimeUnit unit) {
        RLock lock = lockCache.get(lockKey);
        if (lock != null && lock.isLocked()) {
            try {
                long remainTime = lock.remainTimeToLive();
                return unit.convert(remainTime, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.error("Failed to get lock remain time: {}", lockKey, e);
            }
        }
        return -1;
    }

    @Override
    public boolean renewLock(String lockKey, long leaseTime, TimeUnit unit) {
        RLock lock = lockCache.get(lockKey);
        if (lock != null && lock.isHeldByCurrentThread()) {
            try {
                // Redisson的锁续期方式：先解锁再重新加锁，或者使用看门狗机制
                // 这里使用重新加锁的方式
                long currentThreadId = Thread.currentThread().getId();

                // 检查锁是否还是当前线程持有
                if (lock.isHeldByCurrentThread()) {
                    // 重新设置过期时间（通过异步方式）
                    // 注意：Redisson内部有看门狗机制自动续期，这里我们只是手动触发一次
                    lock.expire(unit.toMillis(leaseTime), TimeUnit.MILLISECONDS);
                    log.debug("Lock renewed: {}, new lease time: {} {}",
                            lockKey, leaseTime, unit);
                    return true;
                }
            } catch (Exception e) {
                log.error("Failed to renew lock: {}", lockKey, e);
            }
        }
        return false;
    }

    /**
     * 获取锁实例（缓存优化）
     */
    private RLock getLock(String lockKey) {
        return lockCache.computeIfAbsent(lockKey, key -> {
            // 创建公平锁，避免饥饿问题
            String fullKey = "seckill:lock:" + key;
            return redissonClient.getFairLock(fullKey);
        });
    }

    /**
     * 清理过期的锁引用
     */
    public void cleanup() {
        lockCache.entrySet().removeIf(entry -> {
            RLock lock = entry.getValue();
            return lock != null && !lock.isLocked();
        });
        log.debug("Lock cache cleaned, remaining: {}", lockCache.size());
    }

    /**
     * 创建可重入锁（针对不同业务场景）
     */
    public RLock getReentrantLock(String businessKey) {
        String lockKey = "reentrant:lock:" + businessKey;
        return lockCache.computeIfAbsent(lockKey,
                key -> redissonClient.getLock(key));
    }

    /**
     * 创建读写锁
     */
    public RLock getReadLock(String businessKey) {
        String lockKey = "readwrite:lock:" + businessKey;
        return redissonClient.getReadWriteLock(lockKey).readLock();
    }

    public RLock getWriteLock(String businessKey) {
        String lockKey = "readwrite:lock:" + businessKey;
        return redissonClient.getReadWriteLock(lockKey).writeLock();
    }

    /**
     * 创建联锁（多个锁同时获取）
     */
    public RLock getMultiLock(String... lockKeys) {
        RLock[] locks = new RLock[lockKeys.length];
        for (int i = 0; i < lockKeys.length; i++) {
            locks[i] = getLock(lockKeys[i]);
        }
        return redissonClient.getMultiLock(locks);
    }

    /**
     * 创建红锁（RedLock算法）
     */
    public RLock getRedLock(String lockKey) {
        // 需要多个独立的Redis实例，这里简化实现
        log.warn("RedLock requires multiple Redis instances, using fair lock instead");
        return getLock(lockKey);
    }

    /**
     * 获取锁统计信息
     */
    public LockStats getLockStats() {
        LockStats stats = new LockStats();
        stats.setTotalLocks(lockCache.size());

        int lockedCount = 0;
        for (RLock lock : lockCache.values()) {
            if (lock.isLocked()) {
                lockedCount++;
            }
        }
        stats.setActiveLocks(lockedCount);

        return stats;
    }

    @Data
    public static class LockStats {
        private int totalLocks;
        private int activeLocks;

        public double getActiveRate() {
            return totalLocks > 0 ? (double) activeLocks / totalLocks : 0;
        }
    }
}