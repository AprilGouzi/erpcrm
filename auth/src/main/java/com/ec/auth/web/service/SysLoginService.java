package com.ec.auth.web.service;

import com.ec.auth.manager.AsyncManager;
import com.ec.auth.manager.factory.AsyncFactory;
import com.ec.common.constant.Constants;
import com.ec.common.core.domain.entity.SysUser;
import com.ec.common.core.domain.model.LoginUser;
import com.ec.common.core.redis.RedisCache;
import com.ec.common.exception.ServiceException;
import com.ec.common.exception.user.CaptchaException;
import com.ec.common.exception.user.CaptchaExpireException;
import com.ec.common.exception.user.UserPasswordNotMatchException;
import com.ec.common.utils.DateUtils;
import com.ec.common.utils.MessageUtils;
import com.ec.common.utils.ServletUtils;
import com.ec.common.utils.ip.IpUtils;
import com.ec.sys.service.ISysConfigService;
import com.ec.sys.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * @author 囍崽
 * version 1.0
 */
@Component
public class SysLoginService {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisCache redisCache;

    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    /**
     * 登录验证
     *
     * @param tenant   租户ID
     * @param username 用户名
     * @param password 密码
     * @param code     验证码
     * @param uuid     唯一标识
     * @return 结果
     */
    public String login(String tenant, String username, String password, String code, String uuid) {
        validateCaptcha(username, code, uuid);

        //用户验证
        Authentication authentication = null;
        try {
            //该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            //在最后的 authenticate() 方法中，调用了 UserDetailsService.loadUserByUsername()
            // 并进行了密码校验，校验成功就构造一个认证过的 UsernamePasswordAuthenticationToken 对象放入 SecurityContext.

            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (Exception e) {
            if (e instanceof BadCredentialsException) {
                //用户名或者密码错误都会报Bad credentials错误，如果发生这个错误，
                // 先检查用户名和密码是否输入正确；
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(tenant, username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
                throw new UserPasswordNotMatchException();
            } else {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(tenant, username, Constants.LOGIN_FAIL, e.getMessage()));
                throw new ServiceException(e.getMessage());
            }
        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(tenant, username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
     /*   getAuthorities()，权限信息列表，默认是GrantedAuthority接口的一些实现类，通常是代表权限信息的一系列字符串。
        getCredentials()，密码信息，用户输入的密码字符串，在认证过后通常会被移除，用于保障安全。
        getDetails()，细节信息，web应用中的实现接口通常为 WebAuthenticationDetails，它记录了访问者的ip地址和sessionId的值。
        getPrincipal()，最重要的身份信息，大部分情况下返回的是UserDetails接口的实现类，也是框架中的常用接口之一。*/
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        loginUser.setTenant(tenant);
        recordLoginInfo(loginUser.getUserId());
        //生成token
        return tokenService.createToken(loginUser);
    }

    /**
     * 校验验证码
     *
     * @param username
     * @param code
     * @param uuid
     */
    public void validateCaptcha(String username, String code, String uuid) {
        String verifyKey = Constants.CAPTCHA_CODE_KEY + uuid;
        String captcha = redisCache.getCacheObject(verifyKey);
        redisCache.deleteObject(verifyKey);
        if (captcha == null) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire")));
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha)) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error")));
            throw new CaptchaException();
        }

    }

    /**
     * 记录登录信息
     *
     * @param userId
     */
    public void recordLoginInfo(Long userId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setLoginIp(IpUtils.getIpAddr(ServletUtils.getRequest()));
        sysUser.setLoginDate(DateUtils.getNowDate());
        userService.updateUserProfile(sysUser);
    }


}
