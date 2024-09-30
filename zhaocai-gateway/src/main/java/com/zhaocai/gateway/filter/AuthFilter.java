package com.zhaocai.gateway.filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import com.zhaocai.common.core.constant.CacheConstants;
import com.zhaocai.common.core.constant.HttpStatus;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.constant.TokenConstants;
import com.zhaocai.common.core.utils.JwtUtils;
import com.zhaocai.common.core.utils.ServletUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.redis.service.RedisService;
import com.zhaocai.gateway.config.properties.IgnoreWhiteProperties;
import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

/**
 * 网关鉴权
 * 
 * @author ruoyi
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    // 排除过滤的 uri 地址，nacos自行添加
    @Autowired
    private IgnoreWhiteProperties ignoreWhite;

    @Autowired
    private RedisService redisService;

    @Autowired
    private Environment environment;



    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpRequest.Builder mutate = request.mutate();

        String url = request.getURI().getPath();
        // 跳过不需要验证的路径
        if (StringUtils.matches(url, ignoreWhite.getWhites()))
        {
            return checkUnderling(exchange, chain, request, url);
        }

        String token = getToken(request);
        if (StringUtils.containsAnyIgnoreCase(url, SecurityConstants.Z_FILE)) {
            String cookie = request.getHeaders().getFirst("Cookie");
            token = cookie.split("zdocs_access_token=")[1];
            mutate.header (TokenConstants.AUTHENTICATION, TokenConstants.PREFIX+token);
        }


        if (StringUtils.isEmpty(token))
        {
            return unauthorizedResponse(exchange, "令牌不能为空");
        }


        Claims claims = JwtUtils.parseToken(token);
        if (claims == null)
        {
            return unauthorizedResponse(exchange, "令牌已过期或验证不正确！");
        }
        String userkey = JwtUtils.getUserKey(claims);
        boolean islogin = redisService.hasKey(getTokenKey(userkey));
        if (!islogin)
        {
            return unauthorizedResponse(exchange, "登录状态已过期");
        }
        String userid = JwtUtils.getUserId(claims);
        String username = JwtUtils.getUserName(claims);
        if (StringUtils.isEmpty(userid) || StringUtils.isEmpty(username))
        {
            return unauthorizedResponse(exchange, "令牌验证失败");
        }

        // 设置用户信息到请求
        addHeader(mutate, SecurityConstants.USER_KEY, userkey);
        addHeader(mutate, SecurityConstants.DETAILS_USER_ID, userid);
        addHeader(mutate, SecurityConstants.DETAILS_USERNAME, username);
        // 内部请求来源参数清除
        removeHeader(mutate, SecurityConstants.FROM_SOURCE);
        return chain.filter(exchange.mutate().request(mutate.build()).build());
    }

    private Mono<Void> checkUnderling(ServerWebExchange exchange, GatewayFilterChain chain, ServerHttpRequest request, String url) {
        //底层逻辑平台接口验证token
        if (StringUtils.containsAnyIgnoreCase(url, SecurityConstants.UNDERLING_PREFIX)) {
            Boolean isCheckToken = environment.getProperty("underling.check",Boolean.class);
            log.info("isCheckToken:{}",isCheckToken);
            if (null == isCheckToken ||  !isCheckToken){
                return chain.filter(exchange);
            }
            String underlingToken = getUnderlingToken(request);
            log.info("underlingToken:{}",underlingToken);
            if (StringUtils.isEmpty(underlingToken)) {
                return unauthorizedResponse(exchange, "authorization不能为空");
            }
            String checkToken = environment.getProperty("underling.token");
            if (checkToken.equals(underlingToken)) {
                return chain.filter(exchange);
            }

            Claims claims = JwtUtils.parseToken(underlingToken);
            if (claims == null)
            {
                return unauthorizedResponse(exchange, "authorization 验证不正确！");
            }
        }

        return chain.filter(exchange);
    }


    private void addHeader(ServerHttpRequest.Builder mutate, String name, Object value)
    {
        if (value == null)
        {
            return;
        }
        String valueStr = value.toString();
        String valueEncode = ServletUtils.urlEncode(valueStr);
        mutate.header(name, valueEncode);
    }

    private void removeHeader(ServerHttpRequest.Builder mutate, String name)
    {
        mutate.headers(httpHeaders -> httpHeaders.remove(name)).build();
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String msg)
    {
        log.error("[鉴权异常处理]请求路径:{}", exchange.getRequest().getPath());
        return ServletUtils.webFluxResponseWriter(exchange.getResponse(), msg, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 获取缓存key
     */
    private String getTokenKey(String token)
    {
        return CacheConstants.LOGIN_TOKEN_KEY + token;
    }

    /**
     * 获取请求token
     */
    private String getToken(ServerHttpRequest request)
    {
        String token = request.getHeaders().getFirst(TokenConstants.AUTHENTICATION);
        // 如果前端设置了令牌前缀，则裁剪掉前缀
        if (StringUtils.isNotEmpty(token) && token.startsWith(TokenConstants.PREFIX))
        {
            token = token.replaceFirst(TokenConstants.PREFIX, StringUtils.EMPTY);
        }
        return token;
    }

    private String getUnderlingToken(ServerHttpRequest request)
    {
        String token = request.getHeaders().getFirst(TokenConstants.UNDERLING_AUTHENTICATION);
        // 如果前端设置了令牌前缀，则裁剪掉前缀
        if (StringUtils.isNotEmpty(token) && token.startsWith(TokenConstants.PREFIX))
        {
            token = token.replaceFirst(TokenConstants.PREFIX, StringUtils.EMPTY);
        }
        return token;
    }

    @Override
    public int getOrder()
    {
        return -200;
    }
}