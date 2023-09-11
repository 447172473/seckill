package com.summer.seckill.task;


import com.summer.seckill.SeckillGoods;
import com.summer.seckill.SeckillOrder;
import com.summer.seckill.common.IdUtils;
import com.summer.seckill.dao.SeckillGoodsDao;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MultiThreadingCreateOrder {

    private final StringRedisTemplate redisTemplate;

    private final SeckillGoodsDao seckillGoodsDao;

    public MultiThreadingCreateOrder(StringRedisTemplate redisTemplate, SeckillGoodsDao seckillGoodsDao){
        this.redisTemplate = redisTemplate;
        this.seckillGoodsDao = seckillGoodsDao;
    }


    /***
     * 多线程下单操作
     */
    @Async("getAsyncExecutor")
    public void createOrder() {
        //时间区间
        String time = "20190509";
        //用户登录名
        String username = "casuser";
        //用户抢购商品
        Long id = 1L;
        //获取商品数据
        SeckillGoods goods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_"
                + time).get(id);
        //如果没有库存，则直接抛出异常
        if (goods == null || goods.getStockCount() <= 0) {
            throw new RuntimeException("已售罄!");
        }

        //如果有库存，则创建秒杀商品订单
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setId(IdUtils.getId()); //分布式部署，并发时可能会出现相同ID 需要注意
        seckillOrder.setSeckillId(id);
        seckillOrder.setMoney(goods.getCostPrice());
        seckillOrder.setUserId(username);
        seckillOrder.setSellerId(goods.getSellerId());
        seckillOrder.setCreateTime(new Date());
        seckillOrder.setStatus("0");
        //将秒杀订单存入到Redis中
        redisTemplate.boundHashOps("SeckillOrder").put(username, seckillOrder);
        //库存减少
        goods.setStockCount(goods.getStockCount() - 1);
        //判断当前商品是否还有库存
        if (goods.getStockCount() <= 0) {
            //并且将商品数据同步到MySQL中
            seckillGoodsDao.updateById(goods);
            //如果没有库存,则清空Redis缓存中该商品
            redisTemplate.boundHashOps("SeckillGoods_" + time).delete(id);
        } else {
            //如果有库存，则直数据重置到Reids中
            redisTemplate.boundHashOps("SeckillGoods_" + time).put(id, goods);
        }
        System.out.println("开始执行....");
    }
}
