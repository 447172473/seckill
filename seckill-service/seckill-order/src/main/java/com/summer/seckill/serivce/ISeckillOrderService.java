package com.summer.seckill.serivce;

public interface ISeckillOrderService {


    /****
     * 下单实现
     * @param id:商品ID
     * @param time:商品时区
     * @param username:用户名
     * @return
     */
    Boolean add(Long id,String time,String username);

}
