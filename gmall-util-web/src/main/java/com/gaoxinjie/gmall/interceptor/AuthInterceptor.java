package com.gaoxinjie.gmall.interceptor;

import com.alibaba.fastjson.JSON;
import com.gaoxinjie.gmall.config.LoginRequire;
import com.gaoxinjie.gmall.constants.WebConst;
import com.gaoxinjie.gmall.util.CookieUtil;

import com.gaoxinjie.gmall.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = null;
        Map<String,Object> map=null;
        //newToken的情况
        token = request.getParameter("newToken");
        if (token!=null){
            //将token保存在cookie中
            CookieUtil.setCookie(request,response,"token",token, WebConst.COOKIE_MAXAGE,false);

        }else {
            //从cookie中获取
            token = CookieUtil.getCookieValue(request, "token", false);
        }

        //从token把用户信息取出来

        if(token!=null) {
            //读取token
            map = getUserMapFromToken(token);
            String nickName = (String) map.get("nickName");
            request.setAttribute("nickName", nickName);
        }

        //判断是否需要登录
        HandlerMethod handlerMethod=(HandlerMethod)handler;
        LoginRequire loginRequire = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if (loginRequire!=null){
            if (token!=null) {
                String currentIP = request.getHeader("X-forwarded-for");
                String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&currentIP=" + currentIP);
                if ("success".equals(result)){
                    String userId = (String) map.get("userId");
                    request.setAttribute("userId",userId);
                    return true;
                }else if (!loginRequire.autoRedirect()){
                    return true;
                }else { //认证失败 强行跳转
                    String  requestURL = request.getRequestURL().toString();
                    String encodeURL = URLEncoder.encode(requestURL, "UTF-8");
                    response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);
                    return false;
                }
            }else { //token = null
                if(!loginRequire.autoRedirect()){
                   return  true;
                }
                String  requestURL = request.getRequestURL().toString();
                String encodeURL = URLEncoder.encode(requestURL, "UTF-8");
                response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);
                return false;

            }
        }
        return true;
    }


    public Map<String,Object> getUserMapFromToken(String token){
        String userBase64 = StringUtils.substringBetween(token, ".");//取出jwt中间的部分，利用base64反编码
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] userBytes = base64UrlCodec.decode(userBase64);

        String userJson = new String(userBytes);
        Map userMap = JSON.parseObject(userJson, Map.class);
        return userMap;
    }
}
