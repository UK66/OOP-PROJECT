# Library Management System — Evaluation & Setup Guide
### GUI Desktop Application + MySQL/JDBC Backend

This document contains everything needed to set up, evaluate, and run the **Library Management System** on any Windows machine.

---

## 📦 What Has Been Packaged

The build produces three ready-to-use artifacts in the project:

| Deliverable | Location | Description |
|---|---|---|
| **Native Installer (`.exe`)** | `dist/installer/LibraryManagementSystem-1.0.0.exe` | Double-click Windows setup wizard with folder selector, desktop icon, and Start menu shortcuts. **No Java pre-installation required.** |
| **Windows MSI Package (`.msi`)** | `dist/installer/LibraryManagementSystem-1.0.0.msi` | Standard Windows Installer package. **No Java required.** |
| **Portable Standalone App** | `dist/LibraryManagementSystem/LibraryManagementSystem.exe` | Instant zero-install portable application folder with bundled JRE. |
| **Standalone Uber JAR** | `target/LibrarySystem-1.0.0.jar` | Single fat JAR containing all code, FlatLaf theme, and MySQL JDBC driver. |

---

## 🚀 Quick Setup Instructions (Evaluator Machine)

### Step 1 — Database Setup (MySQL)
1. Ensure **MySQL Server** (8.0+) is installed and running on port `3306`.
2. Open **MySQL Workbench** or run from PowerShell / Terminal:
   ```powershell
   mysql -u root -p < schema.sql
   ```
   *(Or open `schema.sql` inside MySQL Workbench and click the Execute lightning bolt).*
3. This creates `library_db` with all tables, relations, check constraints, and initial catalog seed data.

---

### Step 2 — Configure Database Credentials
You do **not** need to recompile or modify source code to configure credentials!

The application automatically checks for an external `db.properties` file located right in the application folder:
- **For the installed application:** Place or edit `db.properties` in the installation directory.
- **For portable run:** Edit `dist/LibraryManagementSystem/db.properties` (or `src/main/resources/db.properties`).

Default configuration:
```properties
db.url=jdbc:mysql://localhost:3306/library_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.username=root
db.password=your_mysql_password
```

---

### Step 3 — Launch the Application

Choose whichever method you prefer:

#### Option A: Run the Installer (Recommended)
1. Double-click **`dist/installer/LibraryManagementSystem-1.0.0.exe`**.
2. Follow the setup wizard to complete installation.
3. Launch **Library Management System** from your Desktop shortcut or Start Menu.

#### Option B: Run Portably (Zero Install)
1. Navigate to `dist/LibraryManagementSystem/`.
2. Double-click **`LibraryManagementSystem.exe`**.
3. The app starts immediately using the self-contained bundled Java runtime.

#### Option C: Run Standalone Fat JAR
If JDK 17+ is installed:
```powershell
java -jar target/LibrarySystem-1.0.0.jar
```

---

## 🛠️ Rebuilding From Source (Optional)

If you wish to recompile the project or re-run tests from scratch:

```powershell
# 1. Run all 31 unit and integration tests
.\mvnw.cmd test

# 2. Build the shaded fat JAR
.\mvnw.cmd clean package

# 3. Build standalone native app-image with jpackage
& "C:\Program Files\Java\jdk-24\bin\jpackage.exe" `
  --type app-image `
  --name "LibraryManagementSystem" `
  --input "target/package-input" `
  --main-jar "LibrarySystem-1.0.0.jar" `
  --main-class "com.library.Main" `
  --dest "dist" `
  --add-modules java.desktop,java.sql,java.naming,java.management,java.instrument,jdk.unsupported `
  --app-version "1.0.0"

# 4. Build Windows .exe installer
$wixPath = (Resolve-Path "target\wix311").Path
$env:PATH = "$wixPath;$env:PATH"
& "C:\Program Files\Java\jdk-24\bin\jpackage.exe" `
  --type exe `
  --app-image "dist\LibraryManagementSystem" `
  --dest "dist\installer" `
  --name "LibraryManagementSystem" `
  --app-version "1.0.0" `
  --win-dir-chooser --win-menu --win-shortcut
```

---

## 🔍 Troubleshooting

| Issue | Cause | Solution |
|---|---|---|
| **"Cannot connect to MySQL database"** on startup | MySQL service is stopped or port is blocked | Start the MySQL80 service (`Start-Service MySQL80` or via Windows Services) |
| **"Database authentication failed"** | MySQL password doesn't match `db.properties` | Update `db.password` in `db.properties` to match your MySQL root password |
| **"Unknown database library_db"** | `schema.sql` has not been executed | Run `schema.sql` in MySQL to initialize tables and sample data |
| **Port conflict** | MySQL is running on a port other than 3306 | Change port in `db.url` in `db.properties` |
