package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
//         Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);

        // 逻辑过期时间解决缓存击穿
        Shop shop = cacheClient
                .queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if (shop == null) {
            return Result.fail("商铺不存在");
        }

        return Result.ok(shop);

    }

    private static final ExecutorService CACHE_REBUILD_POOL = Executors.newFixedThreadPool(10);

//    public Shop queryWithLogicalExpire(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        // 从redis查商铺
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 是否存在
//        if (StrUtil.isBlank(shopJson)) {
//            // 不存在直接返回
//            return null;
//        }
//        // 命中 需要判断过期时间
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        // 判断是否过期
//        if (expireTime.isAfter(LocalDateTime.now())) {
//            // 未过期 直接返回
//            return shop;
//        }
//
//        // 过期 需要缓存重建
//        // 获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        // 成功开启新新线程重建缓存or失败返回过期的信息
//        if (isLock){
//            CACHE_REBUILD_POOL.submit(() -> {
//                try {
//                    // 重建缓存
//                    this.saveShop2Redis(id, 20L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    // 释放锁
//                    unlock(lockKey);
//                }
//            });
//        }
//        // 返回
//        return shop;
//    }
//
//    public Shop queryWithMutex(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        // 从redis查商铺
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            // 存在直接返回
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//
//        // 是否命中空值
//        if (shopJson != null) {
//            return null;
//        }
//
//        // 实现缓存重建
//        // 1获取互斥锁
//        String lockKey = "lock:shop:" + id;
//        Shop shop = null;
//        try {
//            boolean isLock = tryLock(lockKey);
//            // 是否获取成功
//            // 获取失败
//            if (!isLock) {
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//            // 获取成功
//            // 不存在查数据库
//            shop = getById(id);
//            // 模拟重建的延时
//            Thread.sleep(200);
//            // 不存在返回错误
//            if (shop == null) {
//                // 空值返回redis
//                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//            // 存在写入redis
//            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            // 释放锁
//            unlock(lockKey);
//        }
//
//        // 返回
//        return shop;
//    }

//    public Shop queryWithPassThrough(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        // 从redis查商铺
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            // 存在直接返回
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//
//        // 是否命中空值
//        if (shopJson != null) {
//            return null;
//        }
//
//        // 不存在查数据库
//        Shop shop = getById(id);
//        // 不存在返回错误
//        if (shop == null) {
//            // 空值返回redis
//            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        // 存在写入redis
//        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        // 返回
//        return shop;
//    }

//    private boolean tryLock(String key) {
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(flag);
//    }
//
//    private void unlock(String key) {
//        stringRedisTemplate.delete(key);
//    }
//
//    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
//        // 查询店铺数据
//        Shop shop = getById(id);
//        Thread.sleep(200);
//        // 封装逻辑过期时间
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        // 写入redis
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
//    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("商铺id不能为空");
        }
        // 更新数据库
        updateById(shop);
        // 删除redis缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
