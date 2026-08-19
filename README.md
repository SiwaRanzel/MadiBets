# MadiBets — The Bloodline (WRRV301)

Maven + Java 17 + Spring Boot 3.3.0 + MySQL 8.0 (JDBC via Connector/J).
Full-stack prediction-market platform with a vanilla HTML/CSS/JS SPA frontend.

## Open in VS Code
1. Install the **Extension Pack for Java** (VS Code will prompt — it's in
   `.vscode/extensions.json`).
2. `File > Open Folder...` and pick this `MadiBets` folder.
3. Wait for the Java extension to import the Maven project (status bar bottom-left).

## First-time setup (each member, own machine)
1. Load the database in MySQL Workbench: run `db/madibets_schema.sql`.
2. Optionally run `db/seed_admin.sql` to create the default admin account
   (`admin@mandela.ac.za` / `12345`).
3. Copy `db/db.properties.example` to `src/main/resources/db.properties`
   and set your local MySQL password. (This file is git-ignored — never commit it.)
4. Run the app: open `Main.java`, click **Run** above `main`
   (or `mvn spring-boot:run`). Open `http://localhost:8081` in your browser.

## Where things live
```
src/main/java/com/bloodline/madibets/
  config/       DatabaseConnection.java         (shared JDBC helper)
  model/        14 POJOs — User, Bet, Wager, BetOutcome, Account, Friendship, Group, Task, Query, Event, etc.
  dao/          12 DAOs — SQL only, one DAO per entity
  service/      5 services — business rules (Auth, Account, Bet, Friendship, Leaderboard)
  controller/   10 REST controllers — one per domain area
src/main/resources/
  static/       SPA (index.html + app.js + style.css + images)
db/             Schema, migrations, seeds
```

## Ownership
| Member    | Series | Components |
|-----------|--------|------------|
| Siwapiwe  | A      | UserDAO, AuthService, AuthController, UserController, DeletionRequestDAO, Frontend SPA |
| Kieran    | B      | BetDAO, AccountDAO, EventDAO, BetService, AccountService, BetController |
| Jason     | C      | FriendshipDAO, LeaderboardDAO, FriendshipService, LeaderboardService, FriendshipController, LeaderboardController |
| Pieter    | D      | GroupDAO, TaskDAO, QueryDAO, GroupController, TaskController, QueryController |

## Implementation Status
All four use-case series (A–D) are **fully implemented** across backend and frontend.
See `docs/PROJECT_DOCUMENTATION.md` for full details including API reference,
database schema (16 tables), and remaining polish items.

## Git
`main` stays working. Branch per use case (`feature/B100-place-bet`), PR, review, merge.
