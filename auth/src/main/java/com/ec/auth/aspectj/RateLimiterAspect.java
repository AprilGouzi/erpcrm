package com.ec.auth.aspectj;

import com.ec.common.annotation.RateLimiter;
import com.ec.common.enums.LimitType;
import com.ec.common.exception.ServiceException;
import com.ec.common.utils.ServletUtils;
import com.ec.common.utils.StringUtils;
import com.ec.common.utils.ip.IpUtils;
import net.sf.jsqlparser.statement.select.Join;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * @author 囍崽
 * version 1.0
 *
 * 限流处理
 */
@Aspect
@Component
public class RateLimiterAspect {
    private static final Logger log = LoggerFactory.getLogger(RateLimiterAspect.class);

    private RedisTemplate<Object, Object> redisTemplate;

    private RedisScript<Long> limitScript;

    @Autowired
    public void setRedisTemplate1(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setLimitScript(RedisScript<Long> limitScript) {
        this.limitScript = limitScript;
    }

    /**
     * @ Before前置通知，在目标方法执行之前执行
     * 注意：如果在此回调方法中抛出异常，则目标方法不会再执行，会继续执行后置通知 -> 异常通知。
     * <p>@ annotation切入点表达式
     * 1. 编写自定义注解
     * 2. 在业务类要做为连接点的方法上添加自定义注解
     * 3. annotation切入点表达式</p>
     * <p>
     *    <p>总结</p>
     *     <p>
     *         execution表达式：<br>
     *         1. 根据我们所指定的方法的描述信息来匹配切入点方法，最常用<br>
     *         2. 要匹配的切入点方法的方法名不规则，或者有一些比较特殊的需求，通过execution切入点表达式描述比较繁琐<br>
     *     </p>
     *     <p>
     *         annotation切入点表达式：<br>
     *         基于注解的方式来匹配切入点方法。这种方式虽然多一步操作，我们需要自定义一个注解，
     *         但是相对来比较灵活。我们需要匹配哪个方法，就在方法上加上对应的注解就可以了
     *     </p>
     * </p>
     * @param point
     * @param rateLimiter
     */
    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter){
        String key = rateLimiter.key();
        int time = rateLimiter.time();
        int count = rateLimiter.count();
        String combineKey = getCombineKey(rateLimiter, point);
        List<Object> keys = Collections.singletonList(combineKey);
        try {
            Long number = redisTemplate.execute(limitScript, keys, count, time);
            if (StringUtils.isNull(number) || number.intValue() > count) {
                throw new ServiceException("访问过于频繁，请稍候再试");
            }
            log.info("限制请求'{}',当前请求'{}',缓存key'{}'", count, number.intValue(), key);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("服务器限流异常，请稍候再试");
        }
    }

    /**
     *
     * <body>
     *    <h2>StringBuffer</h2>
     *    <p>
     *        使用StringBuffer类时，每次都会对StringBuffer对象本身进行操作，<br/>
     *        而不是生成新的对象，对字符串进行修改推荐使用StringBuffer<br/>
     *        线程安全的
     *    </p>
     *    <p>
     *        toString()：返回此序列中数据的字符串表示形式.
     *    </p>
     * </body>
     * @param rateLimiter
     * @param point
     * @return
     */
    public String getCombineKey(RateLimiter rateLimiter, JoinPoint point) {
        StringBuffer stringBuffer = new StringBuffer(rateLimiter.key());
        if (rateLimiter.limitType() == LimitType.IP) {
            stringBuffer.append(IpUtils.getIpAddr(ServletUtils.getRequest())).append("-");
        }
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = method.getDeclaringClass();
        stringBuffer.append(targetClass.getName()).append("-").append(method.getName());
        return stringBuffer.toString();
    }
}
