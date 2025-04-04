-- 获取库存key
local stockKey = KEYS[1]
-- 获取用户ID
local userId = ARGV[1]
-- 获取商品ID
local voucherId = ARGV[2]
-- 一人一单key
local orderKey = "seckill:order:" .. voucherId

-- 1.判断库存是否充足
local stock = redis.call('get', stockKey)
if tonumber(stock) <= 0 then
    -- 库存不足
    return 0
end

-- 2.判断用户是否下单
if redis.call('sismember', orderKey, userId) == 1 then
    -- 用户已经下过单了
    return 2
end

-- 3.扣减库存
redis.call('decr', stockKey)
-- 4.记录用户已下单
redis.call('sadd', orderKey, userId)

-- 5.下单成功
return 1 