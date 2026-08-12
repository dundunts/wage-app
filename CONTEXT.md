# Wage App

Wage App accounts for work shifts and employee pay independently for each work location.

## Language

**Company**:
A work location for which shift sessions and payroll are accounted independently.
_Avoid_: Legal entity, restaurant

**Employee**:
A person whose work and pay are accounted by one or more companies. An employee does not need to be a user.
_Avoid_: User, account

**User**:
An authenticated identity that may be linked to an employee. A user acts in the companies associated with that employee.
_Avoid_: Employee

**Position**:
An employee's operational designation, such as manager or waiter. It does not by itself grant system permissions.
_Avoid_: Role, permission

**Shift Session**:
The accounting session for one company team's work shift. Employees may join or leave the working team at different times during the session.
_Avoid_: Employee shift, shift

**Shift Session Start**:
The initial time boundary of a Shift Session. It starts the first Shift Interval but is not a Checkpoint.
_Avoid_: Zero checkpoint

**Shift Interval**:
The part of a Shift Session between its Shift Session Start and first Checkpoint, or between two consecutive Checkpoints. It is accounted to the team listed by the Checkpoint that ends it.

**Checkpoint**:
A boundary that ends a Shift Interval and records its team together with the Company's accumulated Revenue and Restaurant Tips at that time. A Checkpoint is recorded whenever the team changes; the start of a Shift Session is not itself a Checkpoint. Staff may call it a report, but "report" is too ambiguous for the domain model.
_Avoid_: Report, event

**Regular Checkpoint**:
Any Checkpoint that is followed by another Shift Interval in the same Shift Session.

**Final Checkpoint**:
The last Checkpoint in a Shift Session. It uses the same allocation rules as a Regular Checkpoint.

**Revenue**:
The Company's accumulated restaurant revenue at a point in a Shift Session.

**Restaurant Tips**:
The Company's accumulated tips and service-payment pool at a point in a Shift Session.
_Avoid_: Employee tips

**Employee Tips**:
The part of Restaurant Tips allocated to one Employee.
_Avoid_: Restaurant tips

**Shift Result Draft**:
A provisional calculation of employee payments for a shift session that has not yet been confirmed.
_Avoid_: Shift result, report

**Shift Result**:
The confirmed set of employee payments for one company's shift session.
_Avoid_: Report, payroll

**Recalculation**:
Reopening a closed Shift Session to change its Checkpoints and calculate its Shift Result again.

**Manual Override**:
A Shift Result whose Payments are entered directly instead of being calculated from Checkpoints.

**Percent from Revenue**:
The established name for the monetary amount allocated to an employee from company revenue after applying the company's percentage and the shift allocation rules. Despite the name, the value is money, not a percentage rate.
_Avoid_: Percentage rate

**Revenue Percentage**:
The percentage of Revenue allocated across the working team as Percent from Revenue.

**Payment**:
An employee's Percent from Revenue and Employee Tips recorded in a Shift Result. Monetary amounts are whole Russian rubles.
_Avoid_: Payroll, salary

**Payroll**:
Employee payments aggregated over a period for one company.
_Avoid_: Payment, shift result

**Payroll Period**:
The inclusive date range covered by a Payroll. Standard periods are the first or second half of a calendar month; a custom range may also be used.
_Avoid_: Salary period

**Salary**:
The general staff-facing name for employee pay. Use a more precise term such as Payment or Payroll in the domain model.
