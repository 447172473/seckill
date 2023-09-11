package com.summer.seckill.task;


import com.summer.seckill.SeckillGoods;
import com.summer.seckill.SeckillOrder;
import com.summer.seckill.SeckillStatus;
import com.summer.seckill.common.IdUtils;
import com.summer.seckill.dao.SeckillGoodsDao;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MultiThreadingCreateOrder {

    private final RedisTemplate redisTemplate;

    private final SeckillGoodsDao seckillGoodsDao;

    public MultiThreadingCreateOrder(RedisTemplate redisTemplate, SeckillGoodsDao seckillGoodsDao){
        this.redisTemplate = redisTemplate;
        this.seckillGoodsDao = seckillGoodsDao;
    }


    /***
     * 多线程下单操作
     */
    @Async("getAsyncExecutor")
    public void createOrder() {
        try {
            System.out.println("----准备@Async执行----");

            SeckillStatus seckillStauts = (SeckillStatus) redisTemplate.boundListOps("SeckillOrderQueue").rightPop();
            if(seckillStauts==null){
                System.out.println("----redis key SeckillOrderQueue null----");
                return;
            }

            //查询商品详情
            SeckillGoods goods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_"+seckillStauts.getTime()).get(seckillStauts.getGoodsId());
            if(goods==null || goods.getStockCount()<=0){
                //清理排队信息
                clearQueue(seckillStauts);
                System.out.println("----redis key SeckillGoods_ null----");
                return;
            }



            //创建订单
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setId(IdUtils.getId());
            seckillOrder.setSeckillId(seckillStauts.getGoodsId());
            seckillOrder.setMoney(goods.getCostPrice());
            seckillOrder.setUserId(seckillStauts.getUsername());
            seckillOrder.setSellerId(goods.getSellerId());
            seckillOrder.setCreateTime(new Date());
            seckillOrder.setStatus("0");
            redisTemplate.boundHashOps("SeckillOrder").put(seckillStauts.getUsername(),seckillOrder);

            //库存削减
            Long surplusCount = redisTemplate.boundHashOps("SeckillGoodsCount").increment(goods.getId(), -1);
            goods.setStockCount(surplusCount.intValue());
            //商品库存=0->将数据同步到MySQL，并清理Redis缓存
            if(surplusCount<=0){
                //并且将商品数据同步到MySQL中
                seckillGoodsDao.updateById(goods);
                //清理Redis缓存
                redisTemplate.boundHashOps("SeckillGoods_"+seckillStauts.getTime()).delete(seckillStauts.getOrderId());
            }else{
                //将数据同步到Redis
                redisTemplate.boundHashOps("SeckillGoods_"+seckillStauts.getTime()).put(seckillStauts.getOrderId(),goods);
            }

            //变更抢单状态
            seckillStauts.setOrderId(seckillOrder.getId());
            seckillStauts.setMoney(seckillOrder.getMoney().floatValue());
            seckillStauts.setStatus(2); //抢单成功，待支付
            redisTemplate.boundHashOps("UserQueueStatus").put(seckillStauts.getUsername(),seckillStauts);

            System.out.println("----正在执行----");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /***
     * 清理用户排队信息
     * @param seckillStatus
     */
    public void clearQueue(SeckillStatus seckillStatus){
        //清理排队标示
        redisTemplate.boundHashOps("UserQueueCount").delete(seckillStatus.getUsername());
        //清理抢单标示
        redisTemplate.boundHashOps("UserQueueStatus").delete(seckillStatus.getUsername());
        updateUserQueueStatus(seckillStatus);
    }

    private void updateUserQueueStatus(SeckillStatus seckillStauts){
        seckillStauts.setStatus(4); //抢单成功，待支付
        redisTemplate.boundHashOps("UserQueueStatus").put(seckillStauts.getUsername(),seckillStauts);

    }

}
