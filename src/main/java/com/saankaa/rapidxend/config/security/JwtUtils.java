package com.saankaa.rapidxend.config.security;

import com.saankaa.rapidxend.model.Device;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Calendar;
import java.util.Date;

@Service
public class JwtUtils implements IJwtUtils {


    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);


    public boolean validate(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException ignored) {
        }
        return false;
    }


    public String generateToken(Device device) {
        Date expiration = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(expiration);
        c.add(Calendar.DATE, 7); // 7 Days
        expiration = c.getTime();

        return Jwts.builder()
                .setSubject(device.getId())
                .setExpiration(expiration)
                .signWith(key)
                .compact();
    }

    public String getUserId(String token) {

        Jws<Claims> jws;

        try {
            jws = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return jws.getBody().getSubject();
        } catch (Exception e) {
            return null;
        }
    }


}
