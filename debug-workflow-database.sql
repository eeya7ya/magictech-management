-- Workflow Debugging SQL Script
-- Run this in PostgreSQL to check the database state

-- ===================================================================
-- STEP 1: Check if workflow exists for your project
-- ===================================================================

-- Replace 'dwq' with your actual project name
SELECT
    pw.id AS workflow_id,
    pw.project_id,
    p.project_name,
    pw.current_step,
    pw.status,
    pw.step1_completed,
    pw.step2_completed,
    pw.created_at,
    pw.last_updated_at
FROM project_workflows pw
JOIN projects p ON pw.project_id = p.id
WHERE p.project_name LIKE '%dwq%'  -- Change 'dwq' to your project name
ORDER BY pw.created_at DESC;

-- Expected: Should return at least one row with your workflow

-- ===================================================================
-- STEP 2: Check workflow step completions (the CRITICAL table)
-- ===================================================================

-- This shows DETAILED status of each step
SELECT
    wsc.id,
    wsc.workflow_id,
    wsc.step_number,
    wsc.step_name,
    wsc.completed,                    -- CRITICAL: Must be TRUE for Step 1
    wsc.completed_by,
    wsc.completed_at,
    wsc.needs_external_action,
    wsc.external_module,
    wsc.external_action_completed
FROM workflow_step_completions wsc
WHERE wsc.workflow_id IN (
    SELECT pw.id
    FROM project_workflows pw
    JOIN projects p ON pw.project_id = p.id
    WHERE p.project_name LIKE '%dwq%'  -- Change to your project name
)
ORDER BY wsc.step_number;

-- Expected for Step 1 to be "completed":
-- | step_number | completed | completed_by | completed_at |
-- |-------------|-----------|--------------|--------------|
-- | 1           | true      | admin        | [timestamp]  |

-- Expected for Step 2 with pending Presales request:
-- | step_number | completed | needs_external_action | external_module | external_action_completed |
-- |-------------|-----------|----------------------|-----------------|---------------------------|
-- | 2           | false     | true                 | PRESALES        | false                     |

-- ===================================================================
-- STEP 3: Check if site survey data exists
-- ===================================================================

SELECT
    ssd.id,
    ssd.workflow_id,
    ssd.project_id,
    ssd.file_name,
    ssd.file_size,
    ssd.survey_done_by,              -- Should be 'SALES' or 'PROJECT'
    ssd.survey_done_by_user,
    ssd.uploaded_at,
    ssd.active
FROM site_survey_data ssd
WHERE ssd.project_id IN (
    SELECT p.id
    FROM projects p
    WHERE p.project_name LIKE '%dwq%'  -- Change to your project name
)
ORDER BY ssd.uploaded_at DESC;

-- Expected: Should return one row with your uploaded Excel file
-- If EMPTY: Step 1 wasn't actually completed (file upload failed)

-- ===================================================================
-- STEP 4: Check sizing/pricing data (Step 2)
-- ===================================================================

SELECT
    spd.id,
    spd.workflow_id,
    spd.project_id,
    spd.file_name,
    spd.file_size,
    spd.uploaded_by,
    spd.uploaded_at,
    spd.active
FROM sizing_pricing_data spd
WHERE spd.project_id IN (
    SELECT p.id
    FROM projects p
    WHERE p.project_name LIKE '%dwq%'  -- Change to your project name
)
ORDER BY spd.uploaded_at DESC;

-- Expected: Empty if Presales hasn't uploaded yet
-- After Presales upload: Should return one row

-- ===================================================================
-- STEP 5: Get ALL pending external actions for PRESALES
-- ===================================================================

-- This query shows what Presales module should display
SELECT
    wsc.id AS step_id,
    wsc.workflow_id,
    wsc.project_id,
    p.project_name,
    wsc.step_number,
    wsc.step_name,
    wsc.created_at AS requested_at,
    wsc.external_module
FROM workflow_step_completions wsc
JOIN projects p ON wsc.project_id = p.id
WHERE wsc.external_module = 'PRESALES'
  AND wsc.needs_external_action = true
  AND wsc.external_action_completed = false
  AND wsc.active = true
ORDER BY wsc.created_at DESC;

-- Expected: If you clicked "Yes - Request from Presales",
-- this should return one row for your project

-- ===================================================================
-- DIAGNOSTIC: Check if Step 1 validation would pass
-- ===================================================================

-- This simulates the canStartStep() validation for Step 2
WITH step_1_status AS (
    SELECT
        workflow_id,
        completed AS step_1_completed
    FROM workflow_step_completions
    WHERE step_number = 1
      AND workflow_id IN (
          SELECT pw.id
          FROM project_workflows pw
          JOIN projects p ON pw.project_id = p.id
          WHERE p.project_name LIKE '%dwq%'
      )
)
SELECT
    workflow_id,
    step_1_completed,
    CASE
        WHEN step_1_completed = true THEN 'PASS - Can start Step 2'
        ELSE 'FAIL - Cannot start Step 2 (Step 1 not completed)'
    END AS validation_result
FROM step_1_status;

-- Expected: "PASS - Can start Step 2"
-- If "FAIL": Step 1 needs to be completed first

-- ===================================================================
-- DIAGNOSTIC: Check notifications
-- ===================================================================

-- Show recent workflow-related notifications
SELECT
    n.id,
    n.title,
    n.message,
    n.type,
    n.module AS source_module,
    n.target_module,
    n.entity_type,
    n.entity_id,
    n.timestamp,
    n.sender_username
FROM notifications n
WHERE n.entity_type IN ('PROJECT', 'WORKFLOW')
  AND n.timestamp > NOW() - INTERVAL '1 day'
ORDER BY n.timestamp DESC
LIMIT 20;

-- Expected: Should show notifications for:
-- - Site survey requests
-- - Site survey completed
-- - Sizing/pricing requests

-- ===================================================================
-- SUMMARY QUERY: One-stop check
-- ===================================================================

SELECT
    p.id AS project_id,
    p.project_name,
    pw.id AS workflow_id,
    pw.current_step,
    pw.status AS workflow_status,

    -- Step 1 status
    (SELECT completed FROM workflow_step_completions WHERE workflow_id = pw.id AND step_number = 1) AS step_1_completed,
    (SELECT completed_at FROM workflow_step_completions WHERE workflow_id = pw.id AND step_number = 1) AS step_1_completed_at,
    (SELECT EXISTS(SELECT 1 FROM site_survey_data WHERE workflow_id = pw.id AND active = true)) AS has_site_survey,

    -- Step 2 status
    (SELECT completed FROM workflow_step_completions WHERE workflow_id = pw.id AND step_number = 2) AS step_2_completed,
    (SELECT needs_external_action FROM workflow_step_completions WHERE workflow_id = pw.id AND step_number = 2) AS step_2_needs_presales,
    (SELECT external_action_completed FROM workflow_step_completions WHERE workflow_id = pw.id AND step_number = 2) AS step_2_presales_done,
    (SELECT EXISTS(SELECT 1 FROM sizing_pricing_data WHERE workflow_id = pw.id AND active = true)) AS has_sizing_pricing

FROM projects p
LEFT JOIN project_workflows pw ON p.id = pw.project_id
WHERE p.project_name LIKE '%dwq%'  -- Change to your project name
ORDER BY pw.created_at DESC
LIMIT 1;

-- This shows the complete state of your workflow in one row

-- ===================================================================
-- QUICK FIX: Manually mark Step 1 as completed (USE WITH CAUTION!)
-- ===================================================================

-- ONLY run this if Step 1 is stuck and you've verified the site survey was uploaded
-- DO NOT run this unless you understand what it does!

-- Step 1: Get your workflow_id from the queries above
-- Step 2: Replace [YOUR_WORKFLOW_ID] with the actual ID
-- Step 3: Run this:

/*
UPDATE workflow_step_completions
SET
    completed = true,
    completed_by = 'admin',  -- Change to your username
    completed_at = NOW()
WHERE workflow_id = [YOUR_WORKFLOW_ID]
  AND step_number = 1;

-- Verify it worked:
SELECT * FROM workflow_step_completions WHERE workflow_id = [YOUR_WORKFLOW_ID] AND step_number = 1;
*/

-- ===================================================================
-- END OF SCRIPT
-- ===================================================================
