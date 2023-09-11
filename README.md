# seckill

## 多线程下单
采用多线程下单，但多线程中又需要保证用户抢单的公平性，也就是先抢先下单。<br />
用户进入秒杀抢单，如果用户复合抢单资格，只需要记录用户抢单数据，存入队列，多线程从队列中进
行消费即可，存入队列采用左压，多线程下单采用右取的方式。

### 线程池
```java
AsyncPoolConfig.class 线程池配置类 

```

1. 创建秒杀信息队列
```java
//排队信息封装
SeckillStatus seckillStatus = new SeckillStatus(username, new Date(),1, id,time);

//将秒杀抢单信息存入到Redis中,这里采用List方式存储,List本身是一个队列
redisTemplate.boundListOps("SeckillOrderQueue").leftPush(seckillStatus);
```
2. 开启多线程下单
```java
multiThreadingCreateOrder.createOrder();
```

3. 取出Redis中队列信息开始创建订单
```java
SeckillStatus seckillStauts = (SeckillStatus) redisTemplate.boundListOps("SeckillOrderQueue").rightPop();
```

## 防止秒杀重复排队
一旦排队，设置一个自增值，让该值的初始值为1，每次进入抢单的时候，对它进行递
增，如果值>1，则表明已经排队,不允许重复排队,如果重复排队，则对外抛出异常，并抛出异常信息100表示已经正
在排队。<br />
SeckillOrderServiceImpl的add方法，新增递增值判断是否排队中，代码如下
```java
//递增，判断是否排队
Long userQueueCount = redisTemplate.boundHashOps("UserQueueCount"+id).increment(username, 1);
if(userQueueCount>1){
    //100：表示有重复抢单
    throw new RuntimeException("100");
}
```
## 并发超卖问题解决
超卖问题，这里是指多人抢购同一商品的时候，多人同时判断是否有库存，如果只剩一个，则都会判断有库存，此时
会导致超卖现象产生，也就是一个商品下了多个订单的现象。

Redis进行先将数据查询出来，在内存中修改，然后存入到Redis，在并发场景，会出现数据错乱问题，为了控制数量准确，我们单独将商品数量整一个自增键，自增键是线程安全的，所以不担
心并发场景的问题。
```java
 //库存削减
            Long surplusCount = redisTemplate.boundHashOps("SeckillGoodsCount").increment(goods.getId(), -1);
```
