# Workflow Testing Guide - Step 2 (Selection & Design)

## Prerequisites

Before testing the workflow, ensure you have:

1. ✅ Application is running (both JavaFX UI and Spring Boot backend)
2. ✅ Redis server is running on localhost:6379
3. ✅ PostgreSQL database is running
4. ✅ You're logged in as a **SALES** user (or MASTER)

---

## Step-by-Step Testing Instructions

### Part 1: Create a Project with Workflow

1. **Open Sales Module**
   - From main dashboard, click "Sales Management" (💼)

2. **Create a New Project**
   - Click "+ New Project" button
   - Enter project details:
     - Project Name: "Test Project ABC"
     - Location: "Test Location"
     - Status: "Active"
   - Click "Create Project"

3. **Open the Project**
   - Click on the newly created project in the list

4. **Navigate to Workflow Tab**
   - In the project details view, click the "🔄 Workflow Wizard" tab

5. **Verify Workflow is Created**
   - You should see "Current Step: 1 of 8" in the sidebar
   - You should see the 8-step progress indicator

---

### Part 2: Complete Step 1 (Site Survey)

**Option A: Sales Does Site Survey**
1. Open the workflow wizard (full dialog)
2. Step 1 should show: "Does Project need site survey?"
3. Click "Yes - I'll do it myself"
4. Upload an Excel file (.xlsx or .xls)
5. Step 1 should complete and advance to Step 2

**Option B: Request from Project Team**
1. Open the workflow wizard (full dialog)
2. Step 1 should show: "Does Project need site survey?"
3. Click "Yes - Request from Project Team"
4. Wait for Project team to upload (or do it yourself in Projects module)
5. Step 1 should complete and advance to Step 2

---

### Part 3: Test Step 2 (Selection & Design) - THE MAIN TEST

#### 3.1 Initial State (Before Clicking Button)

**What you should see:**
```
Step 2: Selection & Design Check

Does it need a selection and design?

[No]  [Yes - Request from Presales]
```

**Console Output:**
- Nothing yet (waiting for user action)

#### 3.2 Click "Yes - Request from Presales" Button

**Action:** Click the blue "Yes - Request from Presales" button

**Expected Console Output:**
```
🔘 'Yes - Request from Presales' button clicked
   Workflow ID: 1
   Current User: admin
   Project ID: 123
   Project Name: Test Project ABC
   Calling workflowService.requestSelectionDesignFromPresales()...
✅ Request sent successfully
   Refreshing workflow...
   Reloading UI...
✅ UI updated successfully - should now show pending state
```

**Expected UI Change:**
The dialog should immediately refresh and show:
```
⏳ Waiting for Presales Team

You requested sizing & pricing from the Presales team.
📋 The Presales team has been notified and will upload soon.
📅 Requested: Nov 26, 2025 14:30

[🔄 Check Status]

💡 This dialog will automatically show the completed state once Presales uploads the file.
```

**If Button Does Nothing:**
- Check console for error messages starting with ❌
- Check if workflowService is null
- Check if workflow.getId() is returning a valid ID
- Check database: `SELECT * FROM workflow_step_completions WHERE step_number = 2;`

#### 3.3 Verify Notification Sent to Presales

**Expected Console Output (Notification System):**
```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃  📬 NOTIFICATION RECEIVED - SHOWING POPUP             ┃
┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
┃  Title: Selection & Design Request
┃  Message: Sales person admin needs selection and design for project 'Test Project ABC'
┃  From Module: SALES
┃  Target Module: PRESALES
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

**Expected UI:**
- Notification popup should appear in top-right corner
- Title: "Selection & Design Request"
- Message: "Sales person [username] needs selection and design for project '[Project Name]'"

#### 3.4 Verify Database State

**Check workflow_step_completions table:**
```sql
SELECT
    workflow_id,
    step_number,
    needs_external_action,
    external_module,
    external_action_completed
FROM workflow_step_completions
WHERE step_number = 2;
```

**Expected Result:**
```
workflow_id | step_number | needs_external_action | external_module | external_action_completed
------------|-------------|----------------------|-----------------|-------------------------
1           | 2           | true                 | PRESALES        | false
```

---

### Part 4: Presales Responds (Upload Sizing & Pricing)

1. **Switch to Presales Module**
   - Go back to main dashboard
   - Click "Presales" module (📋)

2. **Verify Request Shows Up**
   - At the top of the Presales module, you should see:
     ```
     📋 Pending Sizing & Pricing Requests

     Project: Test Project ABC
     Status: Waiting for Sizing & Pricing
     [📥 Download Site Survey]  [📤 Upload Sizing & Pricing]
     ```

3. **Upload Sizing & Pricing File**
   - Click "📤 Upload Sizing & Pricing" button
   - Select an Excel file (.xlsx or .xls)
   - Click "Open"

**Expected Console Output:**
```
✓ Sizing & Pricing submitted successfully!
```

**Expected Notification to Sales:**
```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃  📬 NOTIFICATION RECEIVED - SHOWING POPUP             ┃
┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
┃  Title: Sizing & Pricing Completed
┃  Message: Presales uploaded sizing & pricing for 'Test Project ABC'
┃  From Module: PRESALES
┃  Target Module: SALES
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

---

### Part 5: Verify Sales Sees Completion

1. **Go Back to Sales Module**
   - Main dashboard → Sales Management

2. **Open the Same Project**
   - Click on "Test Project ABC"

3. **Check Sidebar Auto-Refresh** (⭐ NEW FEATURE)
   - The sidebar should show: **"Current Step: 3 of 8"** (auto-updated!)
   - Console should show:
     ```
     🔄 AUTO-REFRESHING PROJECT DETAILS SIDEBAR
     ```

4. **Open Workflow Dialog**
   - Click "🔄 Open Full Workflow Wizard" button

5. **Navigate to Step 2**
   - If not already on Step 2, click "Next →" to get there

**Expected UI:**
```
✅ Sizing & Pricing Completed

📄 File: sizing_test_project.xlsx
👤 Uploaded by: presales_user (PRESALES team)
📅 Date: Nov 26, 2025 15:45
💾 Size: 245.67 KB

[📥 Download Sizing/Pricing File]

✅ Step 2 completed. Click 'Next →' to proceed to Step 3.
```

6. **Download the File** (Optional Test)
   - Click "📥 Download Sizing/Pricing File"
   - Choose save location
   - File should download successfully

7. **Proceed to Step 3**
   - Click "Next →" button
   - Should move to Step 3: Bank Guarantee Check

---

## Troubleshooting

### Issue: Buttons Don't Respond

**Check Console for Errors:**
- Look for messages starting with ❌
- Common errors:
  - `workflowService is null` → Spring dependency injection failed
  - `Workflow not found` → Workflow wasn't created for this project
  - `Step not found` → Workflow steps weren't initialized

**Verify Workflow Exists:**
```sql
SELECT * FROM project_workflows WHERE project_id = [YOUR_PROJECT_ID];
```

**Verify Steps Exist:**
```sql
SELECT * FROM workflow_step_completions WHERE workflow_id = [YOUR_WORKFLOW_ID];
```
(Should return 8 rows, one for each step)

### Issue: Presales Requests Don't Show Up

**Check Database:**
```sql
SELECT
    wsc.*,
    p.project_name
FROM workflow_step_completions wsc
JOIN project_workflows pw ON wsc.workflow_id = pw.id
JOIN projects p ON pw.project_id = p.id
WHERE wsc.needs_external_action = true
AND wsc.external_module = 'PRESALES'
AND wsc.external_action_completed = false;
```

**If Query Returns Empty:**
- The request wasn't properly saved
- Check if `markNeedsExternalAction()` was called

**If Query Returns Data but Presales UI is Empty:**
- Click "🔄 Refresh Requests" button in Presales module
- Check console for errors in `loadPendingRequests()`

### Issue: Notifications Don't Appear

**Check Redis Connection:**
```bash
redis-cli ping
# Should return: PONG
```

**Check Notification Service:**
```sql
SELECT * FROM notifications ORDER BY timestamp DESC LIMIT 10;
```

**Check Console:**
- Look for Redis connection errors
- Look for notification publishing confirmations

### Issue: Sidebar Doesn't Auto-Refresh

**Check Console for Auto-Refresh Logs:**
```
✅ Workflow notification listener registered for auto-refresh
🔄 AUTO-REFRESHING PROJECT DETAILS SIDEBAR
```

**If Not Showing:**
- Notification system might not be working
- Listener might not be registered
- Check if you have the project details view open (not the main list)

---

## Success Criteria

✅ All buttons respond when clicked
✅ Console shows detailed logging for each action
✅ UI updates immediately to pending state after clicking button
✅ Notification appears for Presales
✅ Request shows up in Presales module
✅ Presales can upload file successfully
✅ Notification appears for Sales when complete
✅ Sales sidebar auto-refreshes to Step 3
✅ Sales can see completed state in workflow dialog
✅ Sales can download the uploaded file

---

## Database Schema Reference

**workflow_step_completions** (Key fields for Step 2):
```
id                         | BIGINT (PK)
workflow_id                | BIGINT (FK to project_workflows)
project_id                 | BIGINT (FK to projects)
step_number                | INTEGER (should be 2)
needs_external_action      | BOOLEAN (should be true after request)
external_module            | VARCHAR (should be 'PRESALES')
external_action_completed  | BOOLEAN (false → true after upload)
```

**sizing_pricing_data** (Created after Presales uploads):
```
id                | BIGINT (PK)
workflow_id       | BIGINT (FK to project_workflows)
project_id        | BIGINT (FK to projects)
excel_file        | BYTEA (binary Excel data)
file_name         | VARCHAR
uploaded_by       | VARCHAR (presales username)
uploaded_at       | TIMESTAMP
```

---

## Quick Test Checklist

- [ ] Create project in Sales module
- [ ] Navigate to Workflow tab
- [ ] Complete Step 1 (site survey)
- [ ] See Step 2 with two buttons
- [ ] Click "Yes - Request from Presales"
- [ ] See yellow "Waiting for Presales" box appear
- [ ] See notification popup
- [ ] Check console shows "✅ Request sent successfully"
- [ ] Switch to Presales module
- [ ] See request in "Pending Sizing & Pricing Requests" list
- [ ] Click "Upload Sizing & Pricing"
- [ ] Upload Excel file
- [ ] See "✓ Sizing & Pricing submitted successfully!" message
- [ ] See notification popup for Sales
- [ ] Switch back to Sales module
- [ ] See sidebar auto-update to "Current Step: 3 of 8"
- [ ] Open workflow dialog, go to Step 2
- [ ] See blue "✅ Sizing & Pricing Completed" box
- [ ] Click download button - file downloads successfully
- [ ] Click "Next →" - moves to Step 3

---

## Contact for Issues

If buttons still don't work after following this guide:

1. Copy ALL console output (including errors)
2. Take screenshots of:
   - The workflow dialog when button is clicked
   - The Presales module showing requests (or empty)
   - The database query results
3. Report the issue with these details

Good luck testing! 🚀
