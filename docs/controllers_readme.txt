Controller Layer — MadiBets
===========================

Web layer: one @RestController per domain area.
Framework: Spring Boot 3.3.0 (spring-boot-starter-web).
All controllers annotated with @RestController, @RequestMapping, @CrossOrigin.

Controllers stay thin: parse the request, call a Service or DAO, return JSON.

Current Controllers (10):
  * AuthController.java       /api/auth/*         — Register + Login
  * UserController.java       /api/users/*        — Profile CRUD, avatar, password, delete requests
  * BetController.java        /api/bets/*         — Full bet lifecycle (propose, approve, wager, grade, delete)
  * FriendshipController.java /api/friends/*      — Friend requests, accept/reject, remove
  * LeaderboardController.java /api/leaderboard/* — Bet history, rankings, stats, monthly allowance
  * GroupController.java      /api/groups/*        — Create, list, join, virtual groups
  * TaskController.java       /api/tasks*          — List tasks by creator
  * QueryController.java      /api/queries*        — Submit, list, resolve support queries
  * AdminController.java      /api/admin/*         — Dashboard stats
  * LecturerController.java   /api/lecturers/*     — Lecturer dashboard stats
