package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.mq.MqConstants;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;
    
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private IVoucherOrderService proxy;

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        // 执行lua脚本 得到结果
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString());
        // 判断结果是否为0
        int r = result.intValue();
        if (r != 0) {
            // 返回错误
            return Result.fail(r == 1? "库存不足" : "用户已经购买过了");
        }
        // 为0 有购买资格，创建订单对象
        VoucherOrder voucherOrder = new VoucherOrder();
        // 订单 用户 代金券id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        
        // 发送消息到RocketMQ队列
        rocketMQTemplate.convertAndSend(MqConstants.VOUCHER_ORDER_TOPIC, voucherOrder);
        
        // 获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();

        // 返回订单id
        return Result.ok(orderId);
    }

    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 一人一单
        Long userId = voucherOrder.getUserId();

        // 查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        // 查询是否存在
        if (count > 0) {
            // 已经买过了
            log.error("用户已经购买过了");
            return;
        }

        // 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!success) {
            // 扣减库存失败
            log.error("库存不足");
            return;
        }

        // 创建订单
        save(voucherOrder);
        // 返回订单id
    }
}

/**
 * RocketMQ消息监听器，用于异步处理订单
 */
@Component
@Slf4j
@RocketMQMessageListener(
        topic = MqConstants.VOUCHER_ORDER_TOPIC,
        consumerGroup = "voucher-order-consumer-group"
)
class VoucherOrderListener implements RocketMQListener<VoucherOrder> {
    
    @Resource
    private IVoucherOrderService voucherOrderService;
    
    @Resource
    private RedissonClient redissonClient;

    @Override
    public void onMessage(VoucherOrder voucherOrder) {
        // 获取用户
        Long userId = voucherOrder.getUserId();
        // 创建锁对象
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 获取锁
        boolean isLock = lock.tryLock();
        // 判断是否获取到锁
        if (!isLock){
            // 获取锁失败，记录错误并返回
            log.error("一个人只允许一单");
            return;
        }
        try {
            // 调用创建订单方法
            voucherOrderService.createVoucherOrder(voucherOrder);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }
}
