# Library Management System — Implementation Plan (v2)
### GUI Desktop App + MySQL/JDBC Backend — Built in Google Antigravity

---

## 1. What Changed From v1

| | v1 (original) | v2 (updated requirement) |
|---|---|---|
| **Interface** | Console menu | **GUI desktop app** (Swing or JavaFX) |
| **Data storage** | File serialization / CSV | **MySQL via JDBC** |
| **Deliverable** | Source project | **Installable executable** (native installer, runs standalone on evaluator's PC) |
| **Scope** | Web excluded implicitly | **Web explicitly excluded** — pure desktop |
| **Estimated time** | 5–6 days | **8–9 days** (see Section 6) |

The core OOP design (Book/Member/Transaction/Person, encapsulation/inheritance/polymorphism/abstraction) stays the same — you're swapping the persistence and presentation layers, not the domain model.

---

## 2. Key Decisions to Make Before Coding

### 2.1 GUI Framework: Swing vs JavaFX
| | Swing | JavaFX |
|---|---|---|
| Bundled with JDK | Yes, zero extra setup | No — needs separate JavaFX SDK modules |
| Packaging into installer | Simpler with `jpackage` | Needs extra `--add-modules` flags, slightly more setup |
| Look & feel | Dated by default, but `FlatLaf` library gives it a modern look cheaply | Modern out of the box, CSS-stylable |
| Learning curve for an academic timeline | Lower | Slightly higher |

**Recommendation:** Swing + the `FlatLaf` look-and-feel library (one dependency, drop-in modern theme). It keeps packaging simple, which matters a lot once you need a working installer for the demo. If you're already comfortable with JavaFX or want a more modern UI as a differentiator, it's still very doable — just budget an extra half-day for packaging.

### 2.2 Database Design (MySQL)
Three core tables mapping directly to your model classes:
```sql
CREATE TABLE books (
  book_id INT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(150) NOT NULL,
  author VARCHAR(100) NOT NULL,
  isbn VARCHAR(20) UNIQUE,
  category VARCHAR(50),
  total_copies INT NOT NULL,
  available_copies INT NOT NULL
);

CREATE TABLE members (
  member_id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(100) UNIQUE,
  contact VARCHAR(20),
  membership_date DATE
);

CREATE TABLE transactions (
  transaction_id INT PRIMARY KEY AUTO_INCREMENT,
  book_id INT NOT NULL,
  member_id INT NOT NULL,
  issue_date DATE,
  due_date DATE,
  return_date DATE,
  fine_amount DECIMAL(6,2) DEFAULT 0,
  status ENUM('ISSUED','RETURNED','OVERDUE'),
  FOREIGN KEY (book_id) REFERENCES books(book_id),
  FOREIGN KEY (member_id) REFERENCES members(member_id)
);
```
Ship this as a `schema.sql` file the evaluator can run to set up the DB before your demo (or have your app auto-create tables on first launch — nice touch, covered in Day 2).

### 2.3 Package Structure
```
com.library
 ├── model/        (Book, Member, Transaction, Person)
 ├── dao/           (BookDAO, MemberDAO, TransactionDAO, DBConnection)
 ├── service/       (BookService, MemberService, TransactionService)
 ├── ui/             (MainFrame, BookPanel, MemberPanel, IssueReturnPanel, dialogs)
 ├── util/           (Validators, exceptions, DateUtils)
 └── Main.java
```
The DAO layer is new — it isolates all JDBC/SQL code so your service layer never touches `Connection`/`ResultSet` directly. This is also good OOP practice to point to in your report (separation of concerns / abstraction via DAO interfaces).

---

## 3. Day-by-Day Plan

### **Day 1 — Design**
- Finalize class diagram + ER diagram (Section 2.2) + package structure.
- Sketch GUI wireframes: main window with tabs/side-nav for Books / Members / Issue-Return / Reports.
- Decide Swing vs JavaFX (Section 2.1) and commit.
- Set up MySQL locally (or confirm it's available on the demo PC) and create the schema.

### **Day 2 — DB Connection Layer + Core Models**
- Implement `Book`, `Member`, `Transaction`, `Person` model classes (encapsulation, inheritance).
- Implement `DBConnection` (JDBC connection manager, reading DB URL/user/pass from a config file — never hardcode credentials in source you'll show in your report).
- Add `mysql-connector-j` as a dependency (Maven/Gradle).
- Smoke-test: connect to MySQL and run a trivial query from Java.

### **Day 3 — DAO Layer**
- `BookDAO`, `MemberDAO`, `TransactionDAO`: CRUD methods using `PreparedStatement` (never string-concatenated SQL — avoids SQL injection and it's a fair question at evaluation).
- Test each DAO method independently (a throwaway `main()` or a small JUnit test) before touching the GUI.

**Output:** working data layer, verified against the actual database, no UI yet.

### **Day 4 — GUI Shell + Book Management Screen**
- Build `MainFrame` with navigation (tabs or a side panel).
- Build the Book Management panel: table view (`JTable`) of books, add/edit/delete forms, search box.
- Wire it to `BookService` → `BookDAO`.

### **Day 5 — Member Management + Issue/Return Screens**
- Member Management panel: register/edit/remove member, view a member's borrowing history.
- Issue/Return panel: issue a book to a member (dropdown/search selectors, availability check), return a book (auto fine calculation if overdue).

**Output:** all three core screens functional end-to-end against MySQL.

### **Day 6 — Business Logic Polish + Validation**
- Input validation across all forms (empty fields, invalid ISBN/email format, issuing a book with zero copies, etc.) with clear error dialogs — not silent failures or raw stack traces.
- Fine calculation rules finalized and tested.
- Reports/summary view if your syllabus expects one (e.g. list of overdue books, most-borrowed books).

### **Day 7 — Testing & Error Handling**
- Test all flows against the live database: normal path + edge cases (duplicate ISBN, member with outstanding fine trying to borrow again, DB temporarily unreachable).
- Wrap all JDBC calls in proper try-catch with user-facing error messages (this matters a lot for a live demo — a raw `SQLException` stack trace on screen looks bad).
- Fix bugs surfaced.

### **Day 8 — Packaging: Build the Installable Executable**
This is the step v1 didn't need, so it's worth detailing:
1. **Build a fat/uber JAR** — bundle your compiled classes + the MySQL JDBC driver into one runnable JAR (Maven Shade Plugin or Gradle Shadow Plugin).
2. **Use `jpackage`** (bundled with JDK 14+) to wrap that JAR into a native installer:
   - Windows → `.exe` or `.msi`
   - macOS → `.dmg` or `.pkg`
   - Linux → `.deb` or `.rpm`
   `jpackage` can bundle a minimal Java runtime with it, so the evaluator's PC doesn't need Java pre-installed — just double-click and install.
3. **Test the installer on a clean environment** (or at least a different user account) before demo day — packaging issues are the classic "worked on my machine" trap.
4. Document DB setup as a short `SETUP.md`: "run schema.sql, update `db.properties` with your MySQL credentials, then install/launch the app."

**Output:** a double-click installer + a one-page setup doc for the MySQL side.

### **Day 9 — Documentation, Report & Demo Rehearsal**
- Finalize the project report (design diagrams, screenshots, OOP concept mapping, future scope).
- Rehearse the demo exactly as you'll present it: install fresh → open app → add/search/issue/return a book live → show it hitting MySQL (e.g. open MySQL Workbench side-by-side to show the row actually changing).
- Prepare answers for likely questions: why DAO pattern, how PreparedStatement prevents injection, where each OOP pillar shows up in the code.

---

## 4. Antigravity-Specific Workflow for This Scope

- **Manager View** is well suited to isolated, mechanical tasks: generating DAO boilerplate for each table once you've defined one as a template, generating GUI form layouts from a wireframe description, writing JUnit tests for DAO methods.
- **Editor View** is where you should stay hands-on: JDBC connection handling and exception strategy, the issue/return business rules, and anything touching the packaging/`jpackage` config — these are exactly the parts that break demos when copy-pasted without understanding.
- Since Antigravity has a built-in browser for verification, that's most useful for you on the **web-adjacent tooling** (e.g. checking Maven Shade / jpackage documentation), not for the app itself — your deliverable is explicitly desktop-only.
- Commit after each day's milestone. Packaging in particular is easy to break silently; having a clean rollback point before Day 8 is worth it.

---

## 5. Things to Confirm Before You Start
- Is MySQL guaranteed to be available on the demo/evaluation PC, or do you need to bundle/install MySQL itself as part of setup? (If the latter, that's extra setup-doc work, not code work — worth clarifying with your instructor.)
- Swing vs JavaFX — lock this in Day 1; switching mid-project costs real time.
- Any specific report/analytics screens your syllabus requires beyond core CRUD + issue/return.

---

## 6. Timeline Summary

| Scope | Estimated Time |
|---|---|
| **Full scope** (GUI + MySQL/JDBC + native installer, as now required) | **8–9 days** |
| **With full-day focus rather than partial days** | **6–7 days** |
| **Minimum viable** (trim reports/extra polish, keep core CRUD + issue/return + installer) | **6 days** |

The jump from v1's 5–6 days is mostly the DAO/JDBC layer, building an actual GUI instead of a console menu, and the packaging step (Day 8) — that last one is easy to underestimate but is non-negotiable now that the deliverable is "install on your PC," not "run from source."
