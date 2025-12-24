-- 扣减库存Lua脚本
local key = KEYS[1]
local quantity = tonumber(ARGV[1])

-- 获取当前库存
local current = redis.call('get', key)
if not current then
    return -1  -- 键不存在
end

current = tonumber(current)
if current < quantity then
    return -2  -- 库存不足
end

-- 扣减库存
local newStock = current - quantity
redis.call('set', key, newStock)
return newStock  -- 返回新库存