package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mq.MqConstants;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 测试控制器，用于测试RocketMQ异步下单功能
 */
@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {

    @Resource
    private RocketMQTemplate rocketMQTemplate;
    
    @Resource
    private RedisIdWorker redisIdWorker;
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    
    @Resource
    private IVoucherOrderService voucherOrderService;

    /**
     * 创建测试秒杀券
     */
    @PostMapping("/create-seckill-voucher")
    public Result createTestSeckillVoucher() {
        try {
            // 创建一个测试秒杀券
            SeckillVoucher seckillVoucher = new SeckillVoucher();
            seckillVoucher.setVoucherId(1L);
            seckillVoucher.setStock(100);
            seckillVoucher.setBeginTime(LocalDateTime.now().minusHours(1));
            seckillVoucher.setEndTime(LocalDateTime.now().plusHours(1));
            
            // 保存到数据库
            boolean success = seckillVoucherService.save(seckillVoucher);
            
            if (success) {
                // 同时在Redis中设置库存
                stringRedisTemplate.opsForValue().set("seckill:stock:" + 1L, "100");
                return Result.ok("测试秒杀券创建成功，券ID: 1, 库存: 100");
            } else {
                return Result.fail("创建失败");
            }
        } catch (Exception e) {
            log.error("创建测试秒杀券失败", e);
            return Result.fail("创建失败: " + e.getMessage());
        }
    }

    /**
     * 测试发送RocketMQ消息
     */
    @PostMapping("/send-message")
    public Result testSendMessage() {
        try {
            // 创建测试订单
            VoucherOrder testOrder = new VoucherOrder();
            testOrder.setId(redisIdWorker.nextId("order"));
            testOrder.setUserId(1L);
            testOrder.setVoucherId(1L);
            
            // 发送消息到RocketMQ
            rocketMQTemplate.convertAndSend(MqConstants.VOUCHER_ORDER_TOPIC, testOrder);
            
            log.info("测试消息已发送到RocketMQ，订单ID: {}", testOrder.getId());
            return Result.ok("消息发送成功，订单ID: " + testOrder.getId());
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return Result.fail("发送失败: " + e.getMessage());
        }
    }

    /**
     * 模拟用户登录（设置测试用户）
     */
    @PostMapping("/login-test-user")
    public Result loginTestUser(@RequestParam(defaultValue = "1") Long userId) {
        try {
            UserDTO testUser = new UserDTO();
            testUser.setId(userId);
            testUser.setNickName("测试用户" + userId);
            testUser.setIcon("test-icon.jpg");
            
            // 设置到ThreadLocal中
            UserHolder.saveUser(testUser);
            
            return Result.ok("测试用户登录成功，用户ID: " + userId);
        } catch (Exception e) {
            log.error("设置测试用户失败", e);
            return Result.fail("登录失败: " + e.getMessage());
        }
    }

    /**
     * 测试秒杀下单（模拟真实场景）
     */
    @PostMapping("/seckill/{voucherId}")
    public Result testSeckill(@PathVariable Long voucherId, @RequestParam(defaultValue = "2") Long userId) {
        try {
            // 自动设置测试用户
            UserDTO testUser = new UserDTO();
            testUser.setId(userId);
            testUser.setNickName("测试用户" + userId);
            testUser.setIcon("test-icon.jpg");
            UserHolder.saveUser(testUser);

            // 调用真实的秒杀方法
            Result result = voucherOrderService.seckillVoucher(voucherId);

            log.info("秒杀测试结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("秒杀测试失败", e);
            return Result.fail("秒杀失败: " + e.getMessage());
        }
    }

    /**
     * 查看RocketMQ连接状态
     */
    @GetMapping("/mq-status")
    public Result getMQStatus() {
        try {
            // 简单测试RocketMQ连接
            return Result.ok("RocketMQ连接正常，Topic: " + MqConstants.VOUCHER_ORDER_TOPIC);
        } catch (Exception e) {
            log.error("RocketMQ连接检查失败", e);
            return Result.fail("RocketMQ连接异常: " + e.getMessage());
        }
    }
}
