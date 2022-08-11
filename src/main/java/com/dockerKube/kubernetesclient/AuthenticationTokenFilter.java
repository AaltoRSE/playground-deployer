package com.dockerKube.kubernetesclient;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;


public class AuthenticationTokenFilter extends UsernamePasswordAuthenticationFilter {

    public static Logger logger = LoggerFactory.getLogger(AuthenticationTokenFilter.class);

    @Autowired
    private Environment env;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        logger.info("enter doFilter()");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String authToken = null;
        authToken = httpRequest.getHeader("jwtToken");
        logger.info("authToken from header jwtToken: "+authToken);
        if(authToken == null ) {
            authToken = httpRequest.getHeader("Authorization");
            logger.info("authToken from header Authorization: "+authToken);
        }
        if(authToken == null) {
            authToken = request.getParameter("jwtToken");
            logger.info("authToken from parameter jwtToken: "+authToken);
        }
        Claims claims= null;
        try {
            claims = getClaimsFromToken(authToken);
            logger.info("authentication successful");
            Map mlpuser=claims.get("mlpuser", Map.class);
            UsernamePasswordAuthenticationToken authentication=new UsernamePasswordAuthenticationToken(mlpuser.get("email"), authToken, new ArrayList<>());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        }
    }

    private Claims getClaimsFromToken(String token) throws Exception {
        Claims claims = null;
        String secret = env.getProperty("jwt.auth.secret.key");
        claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        if (claims.getExpiration().before(new Date())) {
            throw new RuntimeException("jwt token expired");
        }
        logger.info("claims from token: " + claims);
        return claims;
    }

}
