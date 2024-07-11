package com.ec.auth.security.filter;

import com.ec.auth.web.service.TokenService;
import com.ec.common.core.domain.model.LoginUser;
import com.ec.common.utils.SecurityUtils;
import com.ec.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.security.sasl.AuthenticationException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * token过滤器 验证token有效性
 *
 * @author ec
 */

/**
 * <h3>JWT结构</h3>
 * <p>就是令牌token，是一个String字符串，由3部分组成，中间用点隔开</p>
 * <p>
 *     <h3>令牌组成</h3>
 *     <p>
 *         1. 标头(Header)有令牌的类型和所使用的签名算法，如HMAC、SHA256、RSA；
 *         使用Base64编码组成；（Base64是一种编码，不是一种加密过程，可以被翻译成原来的样子）<br>
 *         2. 有效载荷(Payload)有效负载，包含声明；声明是有关实体（通常是用户）和其他数据的声明，
 *         不放用户敏感的信息，如密码。同样使用Base64编码<br>
 *         3. 签名(Signature)前面两部分都使用Base64进行编码，前端可以解开知道里面的信息。Signature需要使用编码后的header和payload
 * 加上我们提供的一个密钥，使用header中指定的签名算法(HS256)进行签名。签名的作用是保证JWT没有被篡改过<br>
 *     </p>
 * </p>
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    @Autowired
    private TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNull(SecurityUtils.getAuthentication())) {
            //判断tenant是否与令牌信息一致
            String tenant = request.getHeader("tenant");
            if (!loginUser.getTenant().equalsIgnoreCase(tenant)) {
                throw new AuthenticationException("令牌无效");
            }

            tokenService.verifyToken(loginUser);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        chain.doFilter(request, response);
    }
}
