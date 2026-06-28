# MadiBets — The Bloodline (WRRV301)

Maven + Java 17 + MySQL 8.0 (JDBC via Connector/J). Web layer is left open
(plug in Servlets/JSP or Spring later — see `controller/README.txt`).

## Open in VS Code
1. Install the **Extension Pack for Java** (VS Code will prompt — it's in
   `.vscode/extensions.json`).
2. `File > Open Folder...` and pick this `madibets` folder.
3. Wait for the Java extension to import the Maven project (status bar bottom-left).

## First-time setup (each member, own machine)
1. Load the database in MySQL Workbench: run `db/madibets_schema.sql`.
2. Copy `db/db.properties.example` to `src/main/resources/db.properties`
   and set your local MySQL password. (This file is git-ignored — never commit it.)
3. Run the smoke test: open `Main.java`, click **Run** above `main`
   (or `mvn exec:java`). You want to see "Success. You're ready to build."

## Where things live
```
src/main/java/com/bloodline/madibets/
  config/   DatabaseConnection.java   (shared)
  model/    POJOs, one per table
  dao/      SQL only, one DAO per entity   (copy UserDAO's pattern)
  service/  business rules (auth, MadiBucks)
  controller/  web layer (add your framework here)
```

## Ownership
| Member    | Series | DAOs |
|-----------|--------|------|
| Siwapiwe  | A      | UserDAO (+ AuthService) |
| Kieran    | B      | BetDAO, AccountDAO (+ AccountService) |
| Jason     | C      | FriendshipDAO, LeaderboardDAO |
| Pieter    | D      | GroupDAO, TaskDAO, QueryDAO |

## Git
`main` stays working. Branch per use case (`feature/B100-place-bet`), PR, review, merge.

> `UserDAO`, `AuthService`, `DatabaseConnection` and `Main` are fully worked
> examples. Everything else is a stub with its use-case mapping in comments —
> copy the UserDAO pattern.
