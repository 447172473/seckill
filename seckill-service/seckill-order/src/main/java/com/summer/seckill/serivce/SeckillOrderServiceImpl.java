package com.summer.seckill.serivce;

import com.summer.seckill.task.MultiThreadingCreateOrder;
import org.springframework.stereotype.Service;

@Service
public class SeckillOrderServiceImpl implements ISeckillOrderService{

    private final MultiThreadingCreateOrder multiThreadingCreateOrder;

    SeckillOrderServiceImpl(MultiThreadingCreateOrder multiThreadingCreateOrder){
        this.multiThreadingCreateOrder = multiThreadingCreateOrder;
    }

    @Override
    public Boolean add(Long id, String time, String username) {
        multiThreadingCreateOrder.createOrder();
        return null;
    }
}
