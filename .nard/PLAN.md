ATTENDANCE SYSTEM
FLOW

1. START
  AttendanceSystem launches Main form

2. MAIN FORM
  Input:
    Student ID

  Button:
    Time In/Out

  Display:
    Student information
    Attendance result

  Table:
    Recent attendance logs

  Buttons:
    Register Student
    Admin


3. TIME IN/OUT
  Find student using student_number

  If student is not found:
    Show Invalid Student ID

  If student is not active:
    Show Student is not active

  Check schedule for current day

  If student has no schedule today:
    Show Student is not scheduled today

  Find todays attendance log for the student

  If no attendance log exists:
    Record TIME IN
      date = today
      time_in = current time
      time_out = null

  If time_in exists and time_out is null:
    Record TIME OUT
      time_out = current time

  If time_in and time_out already exist:
    Confirm, then overwrite time_out with current time

  After processing:
    Show result
    Clear Student ID
    Refresh attendance log table


4. REGISTER STUDENT
  Open RegisterForm from Main

  Enter:
    Student information
    Student schedule

  Save student

  Return to Main


5. ADMIN
  Open AdminForm from Main

  Manage students:
    Activate student
    Deactivate student

  View attendance logs

  Return to Main


6. RETURN
  Clear Student ID field
  Ready for next student

---

## DATABASE MODEL

STUDENT
  id - INT, PK, AI
  first_name - VARCHAR, NOT NULL
  last_name - VARCHAR, NOT NULL
  email - VARCHAR, NULL
  status - CHAR(1), NOT NULL
    a = active
    d = deactivated
    Default = a


SCHEDULE
  id - INT, PK, AI
  student_id - INT, FK to student.id, NOT NULL
  days - VARCHAR
    Store day codes: M, T, W, Th, F, Sa, Su
  time_in - (TIME or VARCHAR HH:MM)
  time_out - (TIME or VARCHAR HH:MM)


ATTENDANCE
  id - INT, PK, AI
  student_id - INT, FK to student.id, NOT NULL
  date - DATE, NOT NULL
  time_in - TIME, NULL
  time_out - TIME, NULL
  status - VARCHAR, NULL
    - values: PRESENT, LATE (after schedule valdiation)

---

## REGISTRATION IMPLEMENTATION PLAN (DONE)

Scope: the **Register Student** feature only. Simple, no over-engineering.

### Decisions
- Student identifier = `STUDENT.id` (auto_increment). No `student_number`.
- `STUDENT.email VARCHAR(100) NULL` exists for the form's email field.
- Day codes built from checkboxes: `M, T, W, Th, F, Sa, Su`.
- Time dropdowns populated hourly, `06:00`–`20:00`.
- One `SCHEDULE` row per student.

### Components
- `src/attendancesystem/StudentDAO.java`
  - `registerStudent(firstName, lastName, email, days, timeIn, timeOut)`
  - Inserts `STUDENT` (status `'a'`) + `SCHEDULE` in one transaction; returns the generated id; rolls back on error.
- `src/attendancesystem/DbConnection.java`
  - `getConnection()` reads `global.properties` (`db.host/port/name/driver/user/password`).
- `src/attendancesystem/RegisterForm.java`
  - Constructor populates combos, attaches listeners, sets `DISPOSE_ON_CLOSE`.
  - Helpers: `buildDays()`, `resetForm()`, `registerStudentActionPerformed()`, `resetFormActionPerformed()`.
  - Generated code (`initComponents`, `//GEN` blocks) and `.form` files untouched.
- `src/attendancesystem/Main.java`
  - REGISTER STUDENT button opens `RegisterForm`.

### Validation
- first name and last name required;
- at least one day selected;
- time-in earlier than time-out.

### Verification
- Compiled with JDK 8 using `lib/jcalendar-1.4.jar` + `lib/mysql-connector-j-9.2.0.jar`.
- Test registration produced a `STUDENT` row `(Juan, Dela Cruz, a, juan@example.com)` and a `SCHEDULE` row `(student_id='1', days='M,W,F', 08:00, 17:00)`, then removed.

### Notes / out of scope
- JDBC driver: `com.mysql.cj.jdbc.Driver` (bundled 9.2.0). The NetBeans `MySQLDriver` 5.1.23 cannot authenticate to MySQL 8 (`caching_sha2_password`).
- `Admin` and `EditUser` wiring not started.

---

## MAIN FORM TIME IN/OUT IMPLEMENTATION PLAN (DONE)

Scope: the **Main** form search + time-in/out + today's-log table. Simple, no over-engineering.

### Decisions
- `ATTENDANCE.time_out` altered to `TIME NULL` so "still timed in" = `NULL`.
- Punch validation: **full** — active status + scheduled today + `LATE`.
- Already timed out: **confirm dialog, then overwrite** `time_out`.
- Today's-log table shows 5 columns: `Student ID, Name, Time In, Time Out, Status`.
- `ATTENDANCE.status` values: `PRESENT` / `LATE` (LATE when punch time is after `SCHEDULE.time_in`).

### Components
- `src/attendancesystem/AttendanceDAO.java`
  - `findToday(studentId)` -> `Today{id, timeIn, timeOut}` or `null`.
  - `insertTimeIn(studentId, timeIn, status)` -> inserts `time_out = NULL`.
  - `updateTimeOut(attendanceId, timeOut)` -> sets/overwrites.
  - `getTodayLogs()` -> `{student_id, name, time_in, time_out, status}` joined to `STUDENT`.
- `src/attendancesystem/StudentDAO.java` (additions)
  - `findById(id)` -> `{id, first_name, last_name, status}` or `null`.
  - `getSchedule(id)` -> `{days, time_in, time_out}` or `null`.
- `src/attendancesystem/Main.java`
  - `setupTable()` -> 5-column non-editable `DefaultTableModel` (replaces generated 4-col model at runtime).
  - `loadTodayLogs()` -> refresh on startup and after each punch.
  - `timeInOut()` -> wired to TIME IN/OUT button and Student ID Enter.
  - `todayDayCode()` / `hasDay()` -> current day vs `SCHEDULE.days`.
  - Generated code (`initComponents`, `//GEN` blocks) and `.form` files untouched.
- `src/attendancesystem/RegisterForm.java`
  - BACK button -> `dispose()`; `windowClosed` reopens `Main` (`Main` hides itself on open).

### Verification
- Compiled with JDK 8 (`lib/jcalendar-1.4.jar` + `lib/mysql-connector-j-9.2.0.jar`).
- End-to-end against live DB: insert time-in (`time_out` null), time-out, overwrite, and JOIN name all OK; test rows removed.

---

# SYSTEM DESIGN (diagrams)

Status legend: `[DONE]` implemented and verified · `[TODO]` not started · `[?]` undecided.

## A. COMPONENT DIAGRAM (text)

```
┌───────────────────────────────────────────────────────────────┐
│                  package attendancesystem                     │
│                                                               │
│  PRESENTATION (Swing, NetBeans .form)                         │
│                                                               │
│  ┌──────────────────┐                                         │
│  │ AttendanceSystem │  main() ──► launches Main               │
│  │     [DONE]       │                                         │
│  └────────┬─────────┘                                         │
│           ▼                                                   │
│  ┌──────────────────┐  REGISTER   ┌──────────────┐            │
│  │      Main        │────────────►│ RegisterForm │            │
│  │     [DONE]       │             │   [DONE]     │            │
│  │                  │  ADMIN      └──────────────┘            │
│  │                  │────────────►┌──────────┐                │
│  └────────┬─────────┘             │  Admin   │                │
│           │  TIME IN/OUT          │  [TODO]  │                │
│           │                       └────┬─────┘                │
│           │                            │ EDIT STUDENT         │
│           │                            ▼                      │
│           │                       ┌──────────┐                │
│           │                       │ EditUser │                │
│           │                       │  [TODO]  │                │
│           │                       └──────────┘                │
│           ▼                                                   │
│  DATA ACCESS (DAO)                                            │
│  ┌──────────────┐          ┌────────────────┐                 │
│  │ StudentDAO   │          │ AttendanceDAO  │                 │
│  │  [DONE]      │          │    [DONE]      │                 │
│  └──────┬───────┘          └───────┬────────┘                 │
│         └───────────┬──────────────┘                          │
│                     ▼                                         │
│            ┌─────────────────┐  reads global.properties        │
│            │  DbConnection   │  db.host/port/name/driver/      │
│            │    [DONE]       │  user/password                  │
│            └────────┬────────┘                                │
└─────────────────────│─────────────────────────────────────────┘
                      ▼
              ┌────────────────┐
              │    MySQL 8     │
              │   STUDENT      │
              │   SCHEDULE     │
              │   ATTENDANCE   │
              └────────────────┘
```

## B. USE CASE DIAGRAM (text)

```
   ACTOR                        ATTENDANCE SYSTEM (in scope)
 ─────────                    ┌──────────────────────────────────┐
                              │                                  │
 FRONT DESK / USER ──────────►│ (UC1) Time In / Time Out         │
                   ──────────►│ (UC2) View Today's Attendance Log│
                   ──────────►│ (UC3) Register Student           │
                              │                                  │
                              │ --- OUT OF SCOPE (abolished) --- │
                              │ (UC4) Search Student             │
                              │ (UC5) Edit Student               │
                              │ (UC6) Activate / Deactivate      │
                              │ (UC7) View Attendance by Date    │
                              │ (UC8) Admin Login  [?]           │
                              └──────────────────────────────────┘
```

Notes
- No student actor: students never log in; front-desk staff operate the kiosk.
- In scope: UC1–UC3 (all `[DONE]`).
- `Admin` / `EditUser` / reports / activate-deactivate are **abolished** (out of scope).

## C. FLOWCHART (step by step)

```
[START]
  │
  ▼
[F0] AttendanceSystem.main() ──► Main form
  │
  ▼
[F1] Main: loadTodayLogs() ──► AttendanceDAO.getTodayLogs()
  │        Screen: [Student ID ____] [TIME IN/OUT] [REGISTER STUDENT] [ADMIN]
  │                Today's log table (Student ID, Name, Time In, Time Out, Status)
  ▼
 ╔══════════════ which action? ══════════════╗
 ║            │            │                 ║
 ▼ A          ▼ B          ▼ C               ║
TIME IN/OUT   REGISTER     ADMIN            ║
```

### Flow A — Time In / Time Out  `[DONE]`
```
[A1] read Student ID text
      │ empty ──► "Please enter a Student ID." ──► back
      ▼
[A2] parse integer
      │ non-numeric ──► "Invalid Student ID." ──► back
      ▼
[A3] StudentDAO.findById(id)
      │ null ──► "Invalid Student ID." ──► back
      ▼
[A4] status != 'a' ──► "Student is not active." ──► back
      ▼
[A5] StudentDAO.getSchedule(id); has today's day code?
      │ no ──► "Student is not scheduled today." ──► back
      ▼
[A6] AttendanceDAO.findToday(id)
      ├─ null ──────────► status = now > sched.time_in ? LATE : PRESENT
      │                   insertTimeIn(id, now, status) ──► "Time In recorded"
      ├─ time_out = null ► updateTimeOut(row, now) ──► "Time Out recorded"
      └─ else ───────────► confirm "Already timed out. Again?"
                              │ no  ──► back
                              └ yes ──► updateTimeOut(row, now) ──► "updated"
      ▼
[A7] loadTodayLogs(); clear Student ID field; ready for next
```

### Flow B — Register Student  `[DONE]`
```
[B1] Main hides ──► RegisterForm opens
[B2] enter First Name, Last Name, Email
[B3] check days  (M T W Th F Sa Su)
[B4] pick Time In / Time Out (hourly 06:00–20:00)
[B5] press REGISTER STUDENT
      │ first/last empty  ──► warn
      │ no day selected   ──► warn
      │ timeIn >= timeOut ──► warn
      ▼
[B6] StudentDAO.registerStudent(...)  [one transaction]
        INSERT STUDENT (status 'a') ──► new id
        INSERT SCHEDULE (student_id, days, time_in, time_out)yes pelase
        COMMIT  (rollback on error)
      ▼
[B7] success ──► show generated Student ID ?         <-- see Q7
[B8] BACK ──► dispose ──► reopen Main
```

### Flow C — Admin  `[OUT OF SCOPE — abolished]`
```
[C1] Main ADMIN button ──► Admin opens   (login ? see Q1)
[C2] SEARCH ID ──► StudentDAO.findById + attendance history ──► fill fields + table
[C3] DATE FROM / DATE TO ──► filter history ──► reload table   <-- see Q6
[C4] EDIT STUDENT ──► open EditUser prefilled                  <-- see Q3/Q5
[C5] ACTIVATE / DEACTIVATE ──► update STUDENT.status 'a'/'d'    <-- see Q4
[C6] BACK ──► reopen Main
```

### Flow D — Edit Student  `[OUT OF SCOPE — abolished]`
```
[D1] prefill id, first/last name, email, schedule (days + times)
[D2] edit fields
[D3] SAVE ──► UPDATE STUDENT + UPDATE SCHEDULE
[D4] status toggle 'a' / 'd'
[D5] BACK ──► Admin
```

---

## D. DECISIONS FROM REVIEW

- **Q2 — Admin / search / edit / reports: ABOLISHED.** Learning project; keep only
  *student registration + attendance*. `Admin.java`, `EditUser.java`, reports and
  activate/deactivate are **out of scope**.
- **Q10 — Time source: MySQL `SYSDATE()`.** Replace Java-generated `HH:mm:ss` and use
  DB time for `time_in` / `time_out` / late check.
- **Q1 — Login form: PENDING / CONFLICT.** Answer said "log in form", but Admin is now
  abolished, so there is nothing to protect. Confirm whether login is dropped or kept
  (see remaining question R1).

## E. REMAINING QUESTIONS

R1. **Login** — since Admin is abolished, do we still want a login form (and if so, what
    does it guard)? Or drop it entirely and boot straight into `Main`?
R2. **Student ID after registration** — show a "Registered! ID = N" dialog so the student
    knows their auto-increment ID? Or nothing?
R3. **LATE rule** — keep "any punch after `SCHEDULE.time_in` = LATE", or add a grace
    period and/or an ABSENT status?
R4. **Schema fix** — live DB has `SCHEDULE.student_id` as `varchar(10000)` and no FK
    constraints. Change to `INT` + FK, or leave it?

