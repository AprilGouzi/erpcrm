package com.ec.auth.security.handler;

import com.alibaba.fastjson.JSON;
import com.ec.common.constant.HttpStatus;
import com.ec.common.core.domain.AjaxResult;
import com.ec.common.utils.ServletUtils;
import com.ec.common.utils.StringUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;

/**
 * @author 囍崽
 * version 1.0
 * <p>
 * 认证失败处理类 返回未授权
 */
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint, Serializable {
    @Override
    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
        int code = HttpStatus.UNAUTHORIZED;
        String msg = StringUtils.format("请求访问：{}，认证失败，无法访问系统资源", httpServletRequest.getRequestURI());
        ServletUtils.renderString(httpServletResponse, JSON.toJSONString(AjaxResult.error(code, msg)));
    }
}
