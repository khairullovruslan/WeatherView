package org.tomato.weather.service.authServices;

import jakarta.servlet.http.Cookie;
import org.hibernate.exception.ConstraintViolationException;
import org.tomato.weather.dao.SessionRepository;
import org.tomato.weather.entity.Session;
import org.tomato.weather.entity.User;
import org.tomato.weather.exception.authException.SessionDuplicateException;
import org.tomato.weather.exception.authException.SessionNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class CookieAndSessionService {
    private final SessionRepository sessionRepository = SessionRepository.getInstance();
    public final static int hoursLiveSession = 15;
    private final static CookieAndSessionService INSTANCE = new CookieAndSessionService();
    private CookieAndSessionService(){}

    public static CookieAndSessionService getInstance() {
        return INSTANCE;
    }


    public Cookie createCookieAndRegisterNewSession(User user){
        LocalDateTime dateTimeWithHours = LocalDate.now().atStartOfDay()
                .plusHours(hoursLiveSession);
        Session session = Session
                .builder()
                .id(String.valueOf(UUID.randomUUID()))
                .user(user)
                .expiresAt(dateTimeWithHours)
                .build();
        try {
            Session session1 = sessionRepository.save(session);
            Cookie cookie = new Cookie("SESSION_ID", session1.getId());
            cookie.setMaxAge(60 * 60 * 5);
            return cookie;
        }
        catch (ConstraintViolationException e){
            throw new SessionDuplicateException();
        }

    }

    public Cookie findAndUpdateSessionByUserId(User user)  {
        Optional<Session> session = sessionRepository.findUpdateByUser(user);
        if (session.isPresent()){
            return new Cookie("SESSION_ID", session.get().getId());
        }
        throw new SessionNotFoundException();


    }
    public String findCookie(Cookie[] cookies) {
        for (Cookie cookie : cookies) {
            if ("SESSION_ID".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;

    }
    public boolean sessionIsValidate(Session session){
        return !LocalDateTime.now().isAfter(session.getExpiresAt());
    }

    public User findUserByCookie(Cookie[] cookies) {
        for (Cookie cookie : cookies) {
            if ("SESSION_ID".equals(cookie.getName())) {
                Optional<Session> session = sessionRepository.findById(cookie.getValue());
                if (session.isPresent()){
                    if (sessionIsValidate(session.get())){
                        return session.get().getUser();
                    }
                    sessionRepository.cleanSession();
                }
                throw new SessionNotFoundException();
            }
        }
        throw new SessionNotFoundException();

    }

}
