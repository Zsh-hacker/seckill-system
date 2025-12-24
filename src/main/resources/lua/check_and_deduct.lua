-- 检查并扣减库存（原子操作）
local stockKey = KEYS[1]
local limitKey = KEYS[2]
local userId = ARGV[1]
local activityId = ARGV[2]
local quantity = tonumber(ARGV[3])
local limitPerUser = tonumber(ARGV[4])

-- 1. 检查用户是否已购买
local userPurchaseKey = limitKey .. ":" .. userId .. ":" .. activityId
local purchased = redis.call('get', userPurchaseKey)
if purchased and tonumber(purchased) >= limitPerUser then
    return -1  -- 超过限购数量
end

-- 2. 检查库存
local currentStock = redis.call('get', stockKey)
if not currentStock then
    return -2  -- 库存键不存在
end

currentStock = tonumber(currentStock)
if currentStock < quantity then
    return -3  -- 库存不足
end

-- 3. 原子操作：扣减库存 + 记录用户购买
redis.call('decrby', stockKey, quantity)
if not purchased then
    redis.call('set', userPurchaseKey, quantity)
else
    redis.call('incrby', userPurchaseKey, quantity)
end

-- 设置过期时间（24小时）
redis.call('expire', userPurchaseKey, 86400)

return currentStock - quantity  -- 返回新库存