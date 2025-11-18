# Sales Module Refactor - Implementation Summary

## Phase 1: ✅ COMPLETED
- Notification system (database + services + UI)
- Approval workflow system
- Cost breakdown entities (Project & Customer)
- Scheduled tasks (cleanup & timeout)
- DatabaseConfig updates

## Phase 2: IN PROGRESS - Sales Module Refactoring

### Changes Required in SalesStorageController.java:

#### 1. Remove "Pricing & Orders" Tab
**Location**: `createProjectDetailModal()` method (around line 280-310)

**Current**: 3 tabs (Contract PDF, Pricing & Orders, Project Elements)
**New**: 2 tabs (Contract PDF, Project Elements)

**Action**: Delete the tab creation for "Pricing & Orders"

#### 2. Add Cost Breakdown to Project Elements Tab
**Location**: `createProjectElementsTab()` method (around line 330-376)

**Current Structure**:
```
- Header
- Add Element button
- FlowPane with element cards
```

**New Structure**:
```
- Header
- Add Element button
- FlowPane with element cards
- Cost Breakdown Section (NEW)
  - Elements Subtotal (auto-calculated)
  - Tax Rate % (editable)
  - Sale Offer % (editable)
  - Installation Cost (editable)
  - Licenses Cost (editable)
  - Additional Cost (editable)
  - Total Cost (auto-calculated, purple bold)
- Save Breakdown button
```

#### 3. Update Element Addition Dialog
**Location**: `handleAddElementToProject()` method

**Current**: User selects item and quantity
**New**:
- User enters desired quantity
- System checks availability
- Shows ✅ Available or ❌ Not Available
- Only allows adding if available

#### 4. Add Notification on Project Creation
**Location**: `handleAddProject()` method (in customers/projects submodule)

**Action**: After creating project, send notification to Projects module users

#### 5. Update Dependencies
Add new autowired services:
```java
@Autowired private NotificationService notificationService;
@Autowired private ProjectCostBreakdownService costBreakdownService;
```

### Purple Theme Colors:
- Primary Purple: `#7c3aed`
- Dark Purple: `#6b21a8`
- Light Purple: `#a78bfa`
- Background: Black `#1a1a1a`

## Phase 3: Projects Module Updates

### Changes Required in ProjectsStorageController.java:

#### 1. Update Element Addition to Create Approval Request
**Location**: `handleAddElement()` method

**Current**: Directly adds element to project
**New**:
```java
1. User selects item and quantity
2. Create PendingApproval with status="PENDING"
3. Send notification to SALES role
4. Element shown with "⏳ Pending Approval" status
5. Element not actually allocated until approved
```

#### 2. Handle Approval Responses
- When approved: Actually allocate element, deduct from storage
- When rejected: Remove pending element card
- When timeout: Remove pending element, notify requester

## Phase 4: Storage Module Analytics

### Add New Tab: "Analytics Dashboard"

**Tabs**:
1. **Storage Items** (existing)
2. **Projects** (existing)
3. **📊 Analytics** (NEW)

**Analytics Tab Content**:
- Project Workflow Section (pie chart + stats)
- Customer Sales Section (bar chart + line chart)
- Module Breakdown Section (custom charts)

## Phase 5: Pricing Module Analytics

### Transform to Analytics-Focused Module

**New Structure**:
1. Keep existing pricing management
2. Add new "Reports & Analytics" section with tabs:
   - Daily Transactions
   - Weekly Summary
   - Monthly Summary
   - Quarterly Reports
   - Yearly Reports
   - Custom Date Range

## Phase 6: Theme Application

### Files to Update:
1. `MainDashboardController.java` - Module cards
2. All module controllers - Headers and backgrounds
3. `styles.css` - Global purple theme
4. `dashboard.css` - Dashboard-specific styles

### Color Replacements:
- Red → Purple
- Blue → Dark Purple
- Orange → Light Purple
- Green → Keep (for success states)

## Phase 7: MainDashboard Notification Badges

### Update Required:
- Add NotificationManager integration
- Show badge count on module cards: "Projects (3)"
- Add notification bell icon in header
- Click bell to open NotificationPanel

## Implementation Priority:
1. ✅ Phase 1 - Foundation (DONE)
2. 🔄 Phase 2 - Sales Module (IN PROGRESS)
3. ⏳ Phase 3 - Projects Module
4. ⏳ Phase 4 - Storage Analytics
5. ⏳ Phase 5 - Pricing Analytics
6. ⏳ Phase 6 - Theme
7. ⏳ Phase 7 - Dashboard Badges

## Next Steps:
Continue with Sales Module refactoring by creating helper components first.
