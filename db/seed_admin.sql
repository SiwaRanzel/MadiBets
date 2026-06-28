-- SQL Seed Script to create the initial Admin account
-- The password is "12345" (hashed using BCrypt because the Spring Boot application expects BCrypt hashes)

INSERT INTO User (name, surname, email, password, userType) 
VALUES ('System', 'Admin', 'admin@mandela.ac.za', '$2a$10$zDCw63WppOURRN5/s0LAReSBFNPW9yXHF4SphSH3IzPY1hRalzWi2', 'ADMIN');

SET @adminId = LAST_INSERT_ID();

INSERT INTO Admin (userID) 
VALUES (@adminId);

-- Initialize the admin wallet/account (balance 0.00)
INSERT INTO Account (userID, balance) 
VALUES (@adminId, 0.00);
