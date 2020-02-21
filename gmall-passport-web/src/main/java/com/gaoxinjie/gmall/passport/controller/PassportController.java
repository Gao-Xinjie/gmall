package com.gaoxinjie.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gaoxinjie.gmall.bean.UserInfo;
import com.gaoxinjie.gmall.service.UserService;
import com.gaoxinjie.gmall.util.JwtUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserService userService;

    String jwtKey = "gaoxinjie";

    @GetMapping("index.html")
    public String index(@RequestParam("originUrl") String originUrl, Model model){
        if (originUrl != null && originUrl != "") {
            model.addAttribute("originUrl",originUrl);
        }
        return "index";
    }


    @PostMapping("/login")
    @ResponseBody
    public String login(UserInfo userInfo, HttpServletRequest request){
        UserInfo info = userService.login(userInfo);
        if (info!=null){
            Map<String,Object> map = new HashMap<>();
            map.put("nickName",info.getNickName());
            map.put("userId",info.getId());
            //在获取ip地址的时候由于做了nginx反向代理，导致传递的地址都是nginx的所以需要再nginx中进行配置将真是的ip地址传递过来
//            System.out.println("nginx:"+request.getRemoteAddr());
            String ipAddr = request.getHeader("X-forwarded-for");
            String token = JwtUtil.encode(jwtKey, map, ipAddr);
            return token;
        }else {
            return "fail";
        }
    }

    @GetMapping("/verify")
    @ResponseBody
    public String verify(@RequestParam("token") String token,@RequestParam("currentIP")String currentIP){
        //验证token
        Map<String, Object> map = JwtUtil.decode(token, jwtKey, currentIP);
        if (map!=null){
            String userId = (String) map.get("userId");
            //再验证缓存
            Boolean isLogin = userService.verify(userId);
            if (isLogin){
                return "success";
            }
        }
        return "fail";
    }
}
