package com.salted_fish.fishing.Interceptor;

import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.salted_fish.fishing.Entity.User;
import com.salted_fish.fishing.Service.UserService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestInterceptor implements HandlerInterceptor {

    private Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);

    @Autowired
    private UserService uService;

    /**
     * check if session exists,if yes,return true,if no check cookie,search user by
     * cookie and return yes
     * 
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        logger.info("start filter at " + new Date());

        String cookieValue = null;

        HttpSession session = request.getSession();

        User user = (User) session.getAttribute("userInfo");

        if (user != null) {
            logger.info("user is " + user.getUserName());
            return true;
        } else {
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (cookie.getName().equals("SESSIONV1")) {
                        cookieValue = cookie.getValue();
                        logger.info("request cookie is " + cookieValue);
                        break;
                    }
                }
            }

            if (cookieValue != null) {
                user = uService.findUserByCookieValue(cookieValue);
                if (user != null) {
                    if (new Date().before(user.getExpireTime())) {
                        session.setAttribute("userInfo", user);
                        return true;
                    } else {
                        responseWithJson(response);
                        return false;
                    }
                } else {
                    responseWithJson(response);
                    return false;
                }
            } else {
                responseWithJson(response);
                return false;
            }
        }
    }

    // response with JSON object
    public void responseWithJson(HttpServletResponse response) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("data", "");
            jsonObject.put("code", "401");
            jsonObject.put("msg", "you did not log in yet");
        } catch (JSONException e1) {
            logger.error(e1.getMessage());
        }
        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        try {
            writer = response.getWriter();
            writer.print(jsonObject);
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}