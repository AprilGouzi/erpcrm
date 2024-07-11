package com.ec.auth.interceptor;

import com.ec.auth.datasource.DynamicDataSourceContextHolder;
import com.ec.auth.datasource.DynamicRoutingDataSource;
import com.ec.common.utils.DateUtils;
import com.ec.common.utils.StringUtils;
import com.ec.saas.domain.MasterTenant;
import com.ec.saas.domain.enums.TenantStatus;
import com.ec.saas.service.IMasterTenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源拦截器
 *
 * @author xxxx
 */

/**
 * <body>
 *     <p>
 *     自定义拦截器的步骤<br>
 *     第一步：自定义一个实现了Interceptor接口的类，或者继承抽象类AbstractInterceptor。<br>
 *     第二步：在配置文件中注册定义的拦截器。<br>
 *     第三步：在需要使用Action中引用上述定义的拦截器，为了方便也可以将拦截器定义为默认的拦截器，这样在不加特殊说明的情况下，所有的
 * Action都被这个拦截器拦截。<br>
 * </p>
 * <p>
 *     preHandler：方法请求处理之前被调用<br/>
 *     postHandler：方法在当前请求处理完成之后，Controller方法调用之后执行
 *     afterCompletion：方法需要在当前对应的 Interceptor 类的 postHandler 方法返回值为 true 时才会执行。顾名思义，该方法将在整个请求结束之后，
 *     也就是在 DispatcherServlet 渲染了对应的视图之后执行。此方法主要用来进行资源清理。
 * </p>
 * </body>
 */

@Component
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {

    @Autowired
    private IMasterTenantService masterTenantService;

    @Autowired
    private DynamicRoutingDataSource dynamicRoutingDataSource;

    @Value("${spring.datasource.driverClassName}")
    private String driverClassName;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String url = request.getServletPath();
        String tenant = request.getHeader("tenant");
        log.info("&&&&&&&&&&&&&&&& 租户拦截 &&&&&&&&&&&&&&&&");
        if (StringUtils.isNotBlank(tenant)) {
            if (!dynamicRoutingDataSource.existDataSource(tenant)) {
                //搜索默认数据库，去注册租户的数据源，下次进来直接session匹配数据源
                MasterTenant masterTenant = masterTenantService.selectMasterTenant(tenant);
                if (masterTenant == null) {
                    throw new RuntimeException("无此租户:" + tenant);
                } else if (TenantStatus.DISABLE.getCode().equals(masterTenant.getStatus())) {
                    throw new RuntimeException("租户[" + tenant + "]已停用");
                } else if (masterTenant.getExpirationDate() != null) {
                    if (masterTenant.getExpirationDate().before(DateUtils.getNowDate())) {
                        throw new RuntimeException("租户[" + tenant + "]已过期");
                    }
                }
                Map<String, Object> map = new HashMap<>();
                map.put("driverClassName", driverClassName);
                map.put("url", masterTenant.getUrl());
                map.put("username", masterTenant.getUsername());
                map.put("password", masterTenant.getPassword());
                dynamicRoutingDataSource.addDataSource(tenant, map);

                log.info("&&&&&&&&&&& 已设置租户:{} 连接信息: {}", tenant, masterTenant);
            } else {
                log.info("&&&&&&&&&&& 当前租户:{}", tenant);
            }
        } else {
            throw new RuntimeException("缺少租户信息");
        }
        // 为了单次请求，多次连接数据库的情况，这里设置localThread，AbstractRoutingDataSource的方法去获取设置数据源
        DynamicDataSourceContextHolder.setDataSourceKey(tenant);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
        // 请求结束删除localThread
        DynamicDataSourceContextHolder.clearDataSourceKey();
    }
}
