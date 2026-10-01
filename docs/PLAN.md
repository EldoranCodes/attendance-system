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
    Show Already timed out for today

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

## DATABASE MODEL

STUDENT
  id - INT, PK, AI
  student_number - VARCHAR, unique, NOT NULL
    Used for time-in/out
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


ATTENDANCE_LOG
  id - INT, PK, AI
  student_id - INT, FK to student.id, NOT NULL
  date - DATE, NOT NULL
  time_in - TIME, NULL
  time_out - TIME, NULL
  status - VARCHAR, NULL
    - values: PRESENT, LATE (after schedule valdiation)

