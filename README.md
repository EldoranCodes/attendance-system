# Attendance System

A simple desktop attendance system made with **Java (Swing)** and **MySQL**.

## What it can do

- Register a student with their name, email, and weekly schedule
- Time in / time out using a Student ID
- Show today's attendance log in a table
- Marks students as PRESENT or LATE

## How to run

1. Start MySQL 8 (make sure it's running, e.g. `sudo systemctl start mysql` or MySQL in Docker)
2. Import the database into MySQL:

   Option A - using the mysql command line:
   ```
   mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS AttendanceSystem"
   mysql -u root -p AttendanceSystem < AttendanceSystem.sql
   ```

   Option B - using phpMyAdmin / MySQL Workbench:
   - Open your MySQL tool
   - Create a database named `AttendanceSystem`
   - Click Import
   - Choose the `AttendanceSystem.sql` file from this project
   - Click Go / Execute

3. Check `global.properties` for your DB settings (host, port, user, password)
4. Open the project in NetBeans (JDK 8)
5. Run `AttendanceSystem.java`

## How to use

1. Click **REGISTER STUDENT** to add a student (days + time in/out = their schedule)
2. Type the student's ID in the **Student ID** box
3. Press **TIME IN / OUT** — first press = time in, second press = time out
4. The table at the bottom shows today's logs

## Project layout

```
src/attendancesystem/   Java source code
docs/                   PLAN.md and STUDENT_GUIDE.txt
lib/                    mysql-connector-j-9.2.0.jar, jcalendar-1.4.jar
global.properties       database settings
AttendanceSystem.sql    database script
build.xml               build file (Ant)
```

## Database

Three tables:

- **STUDENT** — id, first name, last name, email, status (a = active, d = deactivated)
- **SCHEDULE** — student id, days (M,W,F), time in, time out
- **ATTENDANCE** — student id, date, time in, time out, status (PRESENT / LATE)

---

## Text Diagram (how the parts connect)

```
┌─────────────────────────────────────────────────────────┐
│                    Java Application                     │
│                                                         │
│  AttendanceSystem.main()                                │
│        │                                                │
│        ▼                                                │
│  ┌──────────┐   REGISTER    ┌──────────────┐            │
│  │   Main   │──────────────►│ RegisterForm │            │
│  │  [DONE]  │               │    [DONE]    │            │
│  │          │               └──────────────┘            │
│  │ TIME IN/ │                                           │
│  │  OUT     │                                           │
│  └────┬─────┘                                           │
│       │                                                 │
│       ▼                                                 │
│  ┌───────────────────────┐  ┌────────────────────┐      │
│  │     StudentDAO        │  │    AttendanceDAO   │      │
│  │ register, findById,   │  │ findToday, insert, │      │
│  │ getSchedule           │  │ update, getLogs    │      │
│  └──────────┬────────────┘  └─────────┬──────────┘      │
│             └──────────┬──────────────┘                 │
│                        ▼                                │
│             ┌────────────────────┐                      │
│             │   DbConnection     │ reads global.        │
│             │                    │ properties           │
│             └─────────┬──────────┘                      │
└───────────────────────│─────────────────────────────────┘
                        ▼
                ┌──────────────┐
                │   MySQL 8    │
                │  Attendance  │
                │  System DB   │
                └──────────────┘
```

Layers: UI (Swing forms) → DAO (SQL queries) → DbConnection → MySQL.

---

## Use Cases

```
   ACTOR                        ATTENDANCE SYSTEM
 ─────────                    ┌───────────────────────────────┐
                              │                               │
 FRONT DESK / USER ──────────►│ 1. Register Student           │
                   ──────────►│ 2. Time In / Time Out         │
                   ──────────►│ 3. View Today's Attendance Log│
                              └───────────────────────────────┘
```

- Students never log in — front-desk staff operate the computer.

---

## Database Diagram

```
 ┌──────────────────────────┐
 │        STUDENT           │
 ├──────────────────────────┤        ┌──────────────────────────┐
 │ id         INT, PK, AI   │        │        SCHEDULE          │
 │ first_name VARCHAR(50)   │───┐    ├──────────────────────────┤
 │ last_name  VARCHAR(50)   │   │    │ id          INT, PK, AI  │
 │ email      VARCHAR(100)  │   │    │ student_id  INT          │
 │ status     CHAR(1)       │   ├───►│ days        VARCHAR      │
 │   a=active d=inactive    │   │    │   e.g. 'M,W,F'           │
 └──────────────────────────┘   │    │ time_in     TIME         │
            │                   │    │ time_out    TIME         │
            │                   │    └──────────────────────────┘
            │                   │
            │                   │    ┌──────────────────────────┐
            │                   └───►│      ATTENDANCE          │
            │                        ├──────────────────────────┤
            └───────────────────────►│ id          INT, PK, AI  │
                                     │ student_id  INT          │
                                     │ date        DATE         │
                                     │ time_in     TIME         │
                                     │ time_out    TIME (NULL=  │
                                     │               still in)  │
                                     │ status      VARCHAR      │
                                     │  PRESENT / LATE          │
                                     └──────────────────────────┘
```

- One student has one schedule row.
- One student can have many attendance rows (one per day).

---

## Flowchart

```
START
  │
  ▼
Launch Main form ──► load today's logs into table
  │
  ▼
┌─────────────────────────────────────┐
│ Which action?                       │
│  A) TIME IN/OUT   B) REGISTER       │
└─────────────────────────────────────┘
  │                        │
  ▼ A                      ▼ B
Enter Student ID           RegisterForm opens
  │                        fill name + email
  ▼                        pick days + times
empty? ──► show error        │
  ▼ ok                       ▼
not a number? ─► error      REGISTER STUDENT pressed
  ▼ ok                       │
student exists? ─► error      ├─ validation fails ─► warning
  ▼ yes                      │
active? ──────► error        ▼
  ▼ active                   INSERT student + schedule
scheduled today? ─► error       (one transaction)
  ▼ yes                       │
  ▼                           ▼
Today's record?          show success ──► BACK
  ├─ none ──► TIME IN        │
  │   (status = PRESENT      ▼
  │    or LATE)            Main form again
  ├─ timed in, not out ─► TIME OUT
  └─ already out ──► confirm ─┬─ no ─► cancel
                             └─ yes ─► overwrite TIME OUT
  │
  ▼
Refresh table ──► clear Student ID ──► ready for next student
```
