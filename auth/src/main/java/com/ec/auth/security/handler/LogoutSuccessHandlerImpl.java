package com.ec.auth.security.handler;

import com.alibaba.fastjson.JSON;
import com.ec.auth.manager.AsyncManager;
import com.ec.auth.manager.factory.AsyncFactory;
import com.ec.auth.web.service.TokenService;
import com.ec.common.constant.Constants;
import com.ec.common.constant.HttpStatus;
import com.ec.common.core.domain.AjaxResult;
import com.ec.common.core.domain.model.LoginUser;
import com.ec.common.utils.ServletUtils;
import com.ec.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author 囍崽
 * version 1.0
 * 自定义退出处理类，返回成功
 */
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler {

    @Autowired
    private TokenService tokenService;
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser)){
            String username = loginUser.getUsername();
            //删除用户缓存记录
            tokenService.delLoginUser(loginUser.getToken());

            String tenant = request.getHeader("tenant");

            // 记录用户退出日志
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(tenant, username, Constants.LOGOUT, "退出成功"));
        }
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.error(HttpStatus.SUCCESS, "退出成功")));
    }
}
