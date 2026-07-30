package com.bloodline.madibets.service;

import com.bloodline.madibets.dao.UserDAO;
import com.bloodline.madibets.model.User;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;

/**
 * Owner: Siwapiwe (A-series). Shows the Service-layer pattern: business rules
 * live here, SQL lives in the DAO. This is the auth gate the whole team unblocks on.
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    /** A100: hash the password before it ever reaches the database (POPI). */
    public int register(User u, String plainPassword) throws SQLException {
        u.setPassword(BCrypt.hashpw(plainPassword, BCrypt.gensalt()));
        return userDAO.register(u);
    }

    /** A200: returns the User on success, or null on bad credentials. */
    public User login(String email, String plainPassword) throws SQLException {
        User u = userDAO.findByEmail(email);
        if (u != null && BCrypt.checkpw(plainPassword, u.getPassword())) {
            return u;   // controller then stores this in the session
        }
        return null;
    }
}
