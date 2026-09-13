# Library Management System — Setup Guide

## Prerequisites

| Requirement | Version |
|---|---|
| Java (JDK) | 17 or newer |
| MySQL Server | 8.x |
| MySQL Workbench | Optional but recommended |

---

## Step 1 — Set Up the Database

1. Open **MySQL Workbench** (or the MySQL CLI).
2. Run the schema script:
   ```sql
   SOURCE /path/to/LibrarySystem/schema.sql;
   ```
   Or paste the contents of `schema.sql` directly into Workbench and execute.

   This creates the `library_db` database with all tables and seed data.

---

## Step 2 — Configure Database Credentials

Edit the file:
```
LibrarySystem/src/main/resources/db.properties
```

Update these two lines with your MySQL credentials:
```properties
db.username=root
db.password=your_actual_password
```

> **Note:** The `db.url` defaults to `localhost:3306`. Change the host/port if your MySQL runs elsewhere.

---

## Step 3 — Build & Run

### Using Maven (recommended):
```bash
# From the LibrarySystem/ directory:
mvn clean package
java -jar target/LibrarySystem-1.0.0.jar
```

### Using an IDE (IntelliJ / Eclipse / VS Code):
1. Open the `LibrarySystem/` folder as a Maven project.
2. Let Maven download dependencies.
3. Run `com.library.Main`.

---

## Step 4 — Running the Installer (Day 8 deliverable)

After packaging is complete, simply run:
- **Windows:** `LibrarySystem-1.0.0.exe` or `.msi`

The installer bundles a Java runtime — no separate Java installation needed on the evaluator's PC.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `Communications link failure` | MySQL not running — start MySQL service |
| `Access denied for user` | Wrong username/password in `db.properties` |
| `Unknown database library_db` | `schema.sql` not run yet — see Step 1 |
| App window doesn't appear | Check Java version: `java --version` (needs 17+) |
