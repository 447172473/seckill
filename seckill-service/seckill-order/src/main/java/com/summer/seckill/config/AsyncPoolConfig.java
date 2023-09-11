package com.summer.seckill.config;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <h1>自定义异步任务线程池, 异步任务异常捕获处理器</h1>
 * */
@Slf4j
@EnableAsync    // 开启 Spring 异步任务支持
@Configuration
public class AsyncPoolConfig implements AsyncConfigurer {

    /**
     * <h2>将自定义的线程池注入到 Spring 容器中</h2>
     * */
    @Bean
    @Override
    public Executor getAsyncExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(10);       //线程池核心的线程数
        executor.setMaxPoolSize(50);        //最大的线程数
        executor.setQueueCapacity(50);      //阻塞队列的容量
        executor.setKeepAliveSeconds(60);   //线程存活时间
        executor.setThreadNamePrefix("Summer-Async-");   // 这个非常重要 每一个线程前缀 , 没有这个前缀，名字可能是任意的。

        // 等待所有任务结果候再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60); //线程任务执行时间超过了定义时间，强制销毁线程（防止线程阻塞）
        // 定义拒绝策略
        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        // 初始化线程池, 初始化 core 线程
        executor.initialize();
        return executor;
    }

    /**
     * <h2>指定系统中的异步任务在出现异常时使用到的处理器</h2>
     * */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }

    /**
     * <h2>异步任务异常捕获处理器</h2>
     * */
    @SuppressWarnings("all")
    class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(Throwable throwable, Method method,
                                            Object... objects) {

            throwable.printStackTrace();
            log.error("Async Error: [{}], Method: [{}], Param: [{}]",
                    throwable.getMessage(), method.getName(),
                    JSON.toJSONString(objects));

            // TODO 发送邮件或者是短信, 做进一步的报警处理
        }
    }
}
