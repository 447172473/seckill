package com.summer.seckill.serivce;

import com.summer.seckill.SeckillStatus;
import com.summer.seckill.task.MultiThreadingCreateOrder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class SeckillOrderServiceImpl implements ISeckillOrderService{

    private final MultiThreadingCreateOrder multiThreadingCreateOrder;

    private final RedisTemplate redisTemplate;

    SeckillOrderServiceImpl(MultiThreadingCreateOrder multiThreadingCreateOrder, RedisTemplate redisTemplate){
        this.multiThreadingCreateOrder = multiThreadingCreateOrder;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Boolean add(Long id, String time, String username) {

        //递增，判断是否排队
        Long userQueueCount = redisTemplate.boundHashOps("UserQueueCount"+id).increment(username, 1);
        if(userQueueCount>1){
            //100：表示有重复抢单
            throw new RuntimeException("100");
        }

        //获取队列中的商品id
        Long goodsCount = (Long) redisTemplate.opsForValue().get("SeckillGoodsCount"+id);
        if(goodsCount == null || goodsCount < 0){
            //清理排队信息
            redisTemplate.boundHashOps("UserQueueCount").increment(username, -1);
            throw  new RuntimeException("101");
        }

        //排队信息封装
        SeckillStatus seckillStatus = new SeckillStatus(username, new Date(),1, id,time);

        //将秒杀抢单信息存入到Redis中,这里采用List方式存储,List本身是一个队列
        redisTemplate.boundListOps("SeckillOrderQueue").leftPush(seckillStatus);

        //将抢单状态存入到Redis中
        redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);

        multiThreadingCreateOrder.createOrder();
        return null;
    }
}
