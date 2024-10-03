-- 参数列表
-- 1.优惠券id
local voucherId = ARGV[1]
-- 2.用户id
local userId = ARGV[2]

-- 有关数据key
local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

-- 判断库存是否充足
if (tonumber(redis.call('get', stockKey)) <= 0) then
    return 1
end

-- 判断用户是否下单 sismember
if (redis.call('sismember', orderKey, userId) == 1) then
    -- 重复下单
    return 2
end

-- 减库存
redis.call('incrby', stockKey, -1)

-- 下单
redis.call('sadd', orderKey, userId)
return 0
