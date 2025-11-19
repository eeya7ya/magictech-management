# COMPREHENSIVE PROJECT MODULE ANALYSIS

## 1. ENTITY CLASSES STRUCTURE

### 1.1 Project (Root Entity)
**File**: `/modules/projects/entity/Project.java`
**Database Table**: `projects`

**Core Fields**:
- `id` (Long, PK): Auto-generated identity
- `projectName` (String, NOT NULL): Name of the project (max 200 chars)
- `projectLocation` (String): Physical location (max 300 chars)
- `dateOfIssue` (LocalDate): Project issue/start date
- `dateOfCompletion` (LocalDate): Expected completion date
- `status` (String): Current status - "Planning", "In Progress", "Completed", "On Hold"
- `notes` (TEXT): Long-form notes/description

**Metadata Fields**:
- `dateAdded` (LocalDateTime, NOT NULL): Creation timestamp
- `lastUpdated` (LocalDateTime): Last modification timestamp
- `createdBy` (String): User who created the project
- `active` (Boolean, NOT NULL, default=true): Soft delete flag

**Lifecycle**:
- `@PrePersist`: Sets dateAdded and default status="Planning", active=true
- `@PreUpdate`: Updates lastUpdated timestamp

---

### 1.2 ProjectElement (Junction Entity)
**File**: `/modules/projects/entity/ProjectElement.java`
**Database Table**: `project_elements`
**Purpose**: Links storage items to projects (allocation tracking)

**Key Relationships**:
- `project` (@ManyToOne, LAZY): References Project
- `storageItem` (@ManyToOne, **EAGER**): ⚠️ CRITICAL: Set to EAGER to prevent LazyInitializationException

**Allocation Fields**:
- `quantityNeeded` (Integer, default=0): Required quantity for project
- `quantityAllocated` (Integer, default=0): Currently allocated quantity
- `status` (String): "Pending", "Allocated", "In Use", "Returned"
- `notes` (TEXT): Allocation-specific notes

**Tracking Fields**:
- `addedDate` (LocalDateTime, NOT NULL): When item was added to project
- `allocatedDate` (LocalDateTime): When item was allocated
- `addedBy` (String): User who added the element
- `active` (Boolean, default=true): Soft delete flag

---

### 1.3 ProjectTask (Checklist Entity)
**File**: `/modules/projects/entity/ProjectTask.java`
**Database Table**: `project_tasks`
**Purpose**: Project task checklist and milestone tracking

**Relationship**:
- `project` (@ManyToOne, LAZY): References Project

**Task Fields**:
- `taskTitle` (String, NOT NULL, max 300): Task title/name
- `taskDetails` (TEXT): Detailed description
- `priority` (String): "Low", "Medium", "High", "Critical"
- `isCompleted` (Boolean, default=false): Completion status
- `scheduleTaskName` (String): ✅ NEW FIELD - Links to ProjectSchedule task name

**Timing Fields**:
- `createdAt` (LocalDateTime, NOT NULL): When task was created
- `dueDate` (LocalDateTime): Task deadline
- `completedAt` (LocalDateTime): When task was marked complete
- `completedBy` (String): User who completed the task
- `createdBy` (String): User who created the task
- `active` (Boolean, default=true): Soft delete flag

**Lifecycle**:
- `@PrePersist`: Sets createdAt, defaults isCompleted=false
- `@PreUpdate`: Auto-sets completedAt when isCompleted becomes true

---

### 1.4 ProjectNote (Documentation Entity)
**File**: `/modules/projects/entity/ProjectNote.java`
**Database Table**: `project_notes`
**Purpose**: Important notes, warnings, and critical information

**Relationship**:
- `project` (@ManyToOne, LAZY): References Project

**Content Fields**:
- `noteTitle` (String, max 200): Note title
- `importantDescription` (TEXT): Main content/description
- `noteType` (String): "General", "Critical", "Warning", "Info"

**Metadata**:
- `createdAt` (LocalDateTime, NOT NULL): Creation time
- `lastUpdated` (LocalDateTime): Last modification time
- `createdBy` (String): Author
- `active` (Boolean, default=true): Soft delete flag

---

### 1.5 ProjectSchedule (Timeline/Planning Entity)
**File**: `/modules/projects/entity/ProjectSchedule.java`
**Database Table**: `project_schedules`
**Purpose**: Project timeline, phases, and milestones with progress tracking

**Relationship**:
- `project` (@ManyToOne, LAZY): References Project

**Schedule Fields**:
- `taskName` (String, NOT NULL, max 200): Phase/milestone name
- `startDate` (LocalDate): Start date
- `endDate` (LocalDate): End date
- `status` (String): "Pending", "In Progress", "Completed", "Delayed"
- `progress` (Integer, default=0): Completion percentage (0-100)
- `description` (TEXT): Phase description
- `assignedTo` (String): Person/team assigned

**Metadata**:
- `createdAt` (LocalDateTime, NOT NULL): Creation time
- `updatedAt` (LocalDateTime): Last update time
- `createdBy` (String): Creator
- `active` (Boolean, default=true): Soft delete flag

**Lifecycle**:
- `@PrePersist`: Sets createdAt, defaults progress=0, status="Pending"
- `@PreUpdate`: Sets updatedAt

---

### 1.6 ProjectDocument (File Storage Entity)
**File**: `/modules/projects/entity/ProjectDocument.java`
**Database Table**: `project_documents`
**Purpose**: Attach PDFs, contracts, reports, photos to projects

**Relationship**:
- `project` (@ManyToOne, LAZY): References Project

**Document Fields**:
- `documentName` (String, NOT NULL, max 255): File name
- `documentType` (String, max 50): "PDF", "DOCX", "XLSX", "IMAGE", etc.
- `filePath` (String, NOT NULL, max 500): Server file path
- `fileSize` (Long): Bytes
- `description` (String, max 500): Brief description
- `category` (String, max 100): "CONTRACT", "REPORT", "PLAN", "INVOICE", "PHOTO", "OTHER"

**Access Tracking**:
- `uploadedBy` (String): Uploader name
- `dateUploaded` (LocalDateTime, NOT NULL): Upload timestamp
- `lastAccessed` (LocalDateTime): Last access time
- `active` (Boolean, default=true): Soft delete flag

**Utility Method**:
- `getFileSizeFormatted()`: Returns human-readable size (B, KB, MB, GB)

---

## 2. UI ARCHITECTURE - ProjectsStorageController

**File**: `/modules/projects/ProjectsStorageController.java`
**Extends**: `BaseModuleController`
**Pattern**: Dual-screen UI (Project Selection → Project Workspace)

### 2.1 Initialization Flow

```
setupUI()
├── Create DashboardBackgroundPane (animated background)
├── Create mainContainer (StackPane)
│   ├── projectSelectionScreen (VBox)
│   └── projectWorkspaceScreen (BorderPane) [hidden initially]
├── Create loadingIndicator (ProgressIndicator)
└── Set up scene with transparent styling
```

### 2.2 PROJECT SELECTION SCREEN

**Components**:
- **Header**: Icon (📁), Title, Subtitle with instructions
- **Projects Card**: 
  - ListView with custom ListCell rendering
  - Shows all active projects
  - **Custom cell styling**:
    - Selected: Purple background + bright purple border
    - Normal: Dark background + dim border
    - Hover: Blue highlight (non-selected only)
- **Buttons**: 
  - "Open Project" → Opens workspace
  - "← Back to Dashboard" → Navigates away

**Cell Rendering** (`ProjectListView.setCellFactory()`):
```
Each Project Cell displays:
├── Project name with icon (📋)
├── Location with icon (📍)
└── Status with dynamic color coding:
    ├── Planning → #a855f7 (purple)
    ├── In Progress → #3b82f6 (blue)
    ├── Completed → #22c55e (green)
    └── Default → #9ca3af (gray)
```

**Data Loading**:
- Async Task to load projects from ProjectService.getAllProjects()
- Uses ListView.getItems().setAll() for population

---

### 2.3 PROJECT WORKSPACE SCREEN

**Structure**: `BorderPane`
```
TOP: Project Header
├── Back button ("← Back to Projects")
├── Project title label (large, white)
└── Status badge (color-coded)

CENTER: TabPane with 3 tabs
├── 📅 Schedule Tab (Blue: #3b82f6)
├── ✅ Tasks Tab (Green: #22c55e)
└── 📦 Elements Tab (Orange: #fb923c)
```

---

## 3. TAB ORGANIZATION & UI COMPONENTS

### 3.1 SCHEDULE TAB ("📅 Manage Schedule")

**Header Controls**:
- Title: "📅 Project Schedule"
- Buttons: [+ Add Task] [✏️ Edit] [🗑️ Delete] [🔄 Refresh]

**TableView: ProjectScheduleViewModel**
**Columns**:
1. 📋 Task Name (String, width=200)
2. 📅 Start Date (String, width=130, colored: #a5f3fc)
3. 📅 End Date (String, width=130, colored: #fca5a5)
4. 📊 Status (String, width=130)
   - **Status Cell Factory**: Color-coded backgrounds
     - Pending → Orange (#fb923c, 30% opacity)
     - In Progress → Blue (#3b82f6, 30% opacity)
     - Completed → Green (#22c55e, 30% opacity)
     - Default → Gray (#9ca3af, 30% opacity)
5. 📈 Progress (Integer, width=150)
   - **Custom Rendering**: ProgressBar + percentage label
   - Bar color: #22c55e
   - Background: rgba(255, 255, 255, 0.1)
6. 👤 Assigned To (String, width=150)

**Data Source**: ObservableList<ProjectScheduleViewModel>

**Key Features**:
- ✅ Edit schedule functionality now working
- Dynamic progress bar visualization
- Real-time status color coding

---

### 3.2 TASKS TAB ("✅ Tasks")

**Section 1: Tasks Checklist**
```
Header:
├── Title: "✅ Tasks Checklist"
└── Button: "+ Add Task"

Content Area: ScrollPane → VBox
└── Individual Task Rows (createTaskRow())
```

**Task Row Components** (`createTaskRow(ProjectTask)`):
```
HBox (task row with padding=12, spacing=15)
├── Checkbox (30×30, super-visible styling)
│   ├── When unchecked: Purple border (#8b5cf6), 3px
│   ├── When checked: Green border (#22c55e), 3px
│   ├── Background opacity changes based on state
│   └── onClick: Updates database & schedule progress
│
├── Task Info (VBox, grows horizontally)
│   ├── Task Title Label
│   │   ├── If completed: Green, strikethrough
│   │   └── If pending: White
│   └── Schedule Link Label
│       ├── If linked: "📅 Linked to: [schedule_name]"
│       └── If not: "⚠️ No schedule link"
│
├── Details (VBox)
│   ├── Priority (if set): "Priority: [LOW/MEDIUM/HIGH/CRITICAL]"
│   └── Due Date (if set): "Due: [MMM DD, YYYY]"
│
└── Delete Button (text: "🗑️ Delete", red styling)
    ├── Normal: 30% red background, red border
    └── Hover: Full red background (#ef4444)

Row Background (dynamic):
├── If completed: Green tint (rgba(34, 197, 94, 0.2))
└── If pending: Dark (rgba(15, 23, 42, 0.6))

Row Border (dynamic):
├── If completed: Green (rgba(34, 197, 94, 0.6))
└── If pending: Purple (rgba(139, 92, 246, 0.2))
```

**Task Completion Action**:
1. Update `isCompleted` flag
2. Call `taskService.toggleTaskCompletion()`
3. **IF** task has schedule link:
   - Call `updateScheduleProgress(scheduleTaskName)`
   - Calculate % completion of all linked tasks
   - Auto-update ProjectSchedule.progress
   - Auto-update ProjectSchedule.status based on %:
     - 0% → "Pending"
     - 1-99% → "In Progress"
     - 100% → "Completed"
4. Refresh UI (colors, styling, labels)

---

**Section 2: Important Notes**
```
VBox (notes section)
├── Label: "📝 Important Notes"
├── TextArea (height=150)
│   ├── Prompt: "Enter important notes for this project..."
│   ├── Style: Dark background, white text
│   ├── Borders: Purple, radius=8
│   └── Word-wrapped
└── Button: "💾 Save Notes"
    ├── Style: Purple (#8b5cf6)
    └── onClick: Call handleSaveNotes()
```

**Data Loading**:
- Load all ProjectTasks for selected project
- Load all ProjectNotes for selected project
- Render task rows in order
- Populate notes TextArea

---

### 3.3 ELEMENTS TAB ("📦 Elements")

**Header Controls**:
- Title: "📦 Project Elements"
- Buttons: [+ Add Element] [🔄 Refresh]

**Layout**: ScrollPane → FlowPane (elementsGrid)
- HGap: 20px
- VGap: 20px (default)
- Wrapping enabled

**Element Card Structure** (for each ProjectElementViewModel):
```
Each card is a VBox:
├── Header Section
│   ├── Product Name (white, bold, large)
│   └── Status badge (color-coded)
│       ├── Pending → Orange
│       ├── Allocated → Green
│       ├── In Use → Blue
│       └── Returned → Gray
│
├── Quantity Section
│   ├── Quantity Needed: X units
│   └── Quantity Allocated: Y units
│       └── Progress indicator if Y > 0
│
├── Notes Section (if present)
│   └── Display element-specific notes
│
└── Action Buttons
    ├── [✏️ Edit]
    └── [🗑️ Delete]

Card Styling:
├── Background: rgba(30, 41, 59, 0.6)
├── Border: Orange color (#fb923c, 2px)
├── Border Radius: 12px
├── Padding: 15px
└── Shadow: dropshadow (3px offset)
```

**Data Source**: ObservableList<ProjectElementViewModel>

---

## 4. VIEW MODELS FOR JAVAFX BINDING

### 4.1 ProjectViewModel
```java
Properties (JavaFX):
- id: LongProperty
- projectName: StringProperty
- projectLocation: StringProperty
- dateOfIssue: StringProperty
- dateOfCompletion: StringProperty
- status: StringProperty
- dateAdded: StringProperty

Constructor: Full initialization from entity fields
toString(): Returns summary representation
```

### 4.2 ProjectScheduleViewModel
```java
Properties (JavaFX):
- id: LongProperty
- taskName: StringProperty
- startDate: StringProperty
- endDate: StringProperty
- status: StringProperty
- progress: IntegerProperty (0-100)
- assignedTo: StringProperty

Usage: Bound directly to TableView columns
```

### 4.3 ProjectTaskViewModel
```java
Properties (JavaFX):
- id: LongProperty
- taskTitle: StringProperty
- taskDetails: StringProperty
- priority: StringProperty
- isCompleted: BooleanProperty
- scheduleTaskName: StringProperty ✅ NEW
- dueDate: LocalDateTime (non-property, for info)

Usage: Not TableView - rendered as custom HBox rows
```

### 4.4 ProjectElementViewModel
```java
Properties (JavaFX):
- id: LongProperty
- storageItemName: StringProperty
- quantityNeeded: IntegerProperty
- quantityAllocated: IntegerProperty
- status: StringProperty
- notes: StringProperty

Usage: Rendered as cards in FlowPane grid
```

---

## 5. SERVICES & DATA ACCESS LAYER

### 5.1 Service Dependencies
```java
ProjectsStorageController @Autowired:
├── ProjectService
├── ProjectScheduleService
├── ProjectTaskService
├── ProjectNoteService
├── ProjectElementService
├── StorageService (for available items)
├── PendingApprovalService
└── NotificationService
```

### 5.2 Repository Interfaces

**ProjectRepository**:
- `findByActiveTrue()`: All active projects
- `findByStatusAndActiveTrue(status)`: Filter by status
- `findByCreatedByAndActiveTrue(username)`: Filter by creator
- `searchProjects(searchTerm)`: Multi-field search
- `findByProjectNameContainingIgnoreCase(name)`: Name search
- `findByProjectLocationContainingIgnoreCase(location)`: Location search

**ProjectScheduleRepository**:
- `findByProjectIdAndActiveTrue(projectId)`: All schedules for project
- `findByProjectIdOrderByStartDateAsc(projectId)`: Sorted by start date
- `findByStatusAndActiveTrue(status)`: Filter by status
- `countByProjectIdAndActiveTrue(projectId)`: Count active schedules

**ProjectTaskRepository**:
- `findByProjectIdAndActiveTrue(projectId)`: All tasks for project
- `findByProjectIdAndIsCompletedAndActiveTrue(projectId, isCompleted)`: Filter by completion
- `countByProjectIdAndIsCompletedAndActiveTrue(projectId, isCompleted)`: Count completed/pending

**ProjectElementRepository**:
- `findByProjectIdAndActiveTrue(projectId)`: All elements for project
- `findByStorageItemIdAndActiveTrue(storageItemId)`: Find projects using storage item
- `findElementsByProjectId(projectId)`: Custom JPQL query
- `countByProjectIdAndActiveTrue(projectId)`: Count elements

---

## 6. KEY FEATURES & WORKFLOWS

### 6.1 Project Selection Workflow
```
1. Load projects list (async)
2. Display custom ListView cells with:
   - Project name, location
   - Status with color coding
   - Selection highlighting
3. User clicks project
4. Click "Open Project" button
5. Show workspace screen (animate transition)
6. Load all project data (schedules, tasks, notes, elements)
```

### 6.2 Schedule Management Workflow
```
Add Schedule Item:
1. Click "+ Add Task" in Schedule tab
2. Open dialog with form:
   - Task Name (required)
   - Start Date
   - End Date
   - Status (dropdown)
   - Progress (0-100)
   - Assigned To
3. Submit → save to DB → refresh table

Edit Schedule Item:
1. Select row in table
2. Click "✏️ Edit" button
3. Open pre-populated dialog
4. Modify fields
5. Submit → update DB → refresh

Delete Schedule Item:
1. Select row in table
2. Click "🗑️ Delete" button
3. Confirm dialog
4. Hard delete from DB → refresh
```

### 6.3 Task Management Workflow
```
Add Task:
1. Click "+ Add Task" button
2. Dialog with:
   - Task Title (required)
   - Details (textarea)
   - Priority (dropdown)
   - Due Date
   - Link to Schedule (dropdown)
3. Submit → save → refresh rows

Complete Task:
1. Click checkbox in task row
2. Automatic actions:
   - Update task.isCompleted = true
   - Call taskService.toggleTaskCompletion()
   - If linked to schedule:
     a. Calculate % of related tasks completed
     b. Update ProjectSchedule.progress
     c. Auto-update ProjectSchedule.status
   - Refresh schedule table
3. Visual feedback: row colors change, text strikethrough, checkbox highlight

Delete Task:
1. Click "🗑️ Delete" button on task row
2. Confirm dialog
3. Hard delete → reload task list

Save Notes:
1. Edit text in Important Notes TextArea
2. Click "💾 Save Notes"
3. Save/update ProjectNote records
```

### 6.4 Element Management Workflow
```
Add Element:
1. Click "+ Add Element" in Elements tab
2. Dialog with:
   - Select Storage Item (dropdown, checks availability)
   - Quantity Needed
   - Quantity Allocated
   - Status (dropdown)
   - Notes (textarea)
3. ⚠️ Availability check: Prevents allocating more than available stock
4. Submit → creates ProjectElement → refresh grid

Edit Element:
1. Click "✏️ Edit" on element card
2. Pre-populate form with current values
3. Modify fields
4. Submit → update → refresh

Delete Element:
1. Click "🗑️ Delete" on element card
2. Confirm → hard delete → refresh grid

View Element Details:
1. Hover over card → shows all information
2. Click card → may open detail view (if implemented)
```

---

## 7. STYLING & THEMING

**Color Scheme** (Black-Purple-Green):
```
Backgrounds:
- Dark base: rgba(15, 23, 42, 0.x) - Very dark gray/blue
- Card base: rgba(30, 41, 59, 0.6) - Dark blue-gray
- Semi-transparent: rgba(x, x, x, 0.2-0.8)

Primary Colors:
- Purple (Primary): #8b5cf6, #7c3aed
- Blue (Info): #3b82f6, #2563eb
- Green (Success): #22c55e, #16a34a
- Orange (Warning): #fb923c, #f97316
- Red (Danger): #ef4444, #dc2626
- Gray (Neutral): #6b7280, #4b5563

Text:
- Primary: white (#ffffff)
- Secondary: rgba(255, 255, 255, 0.7-0.9)
- Subtle: rgba(255, 255, 255, 0.4-0.6)
```

**Typography**:
- Headers: 20-32px, bold, white
- Labels: 14-16px, normal weight
- Details: 12-13px, normal weight, reduced opacity

**Borders & Spacing**:
- Border radius: 8-16px
- Border width: 1-3px (emphasized elements)
- Padding: 12-30px (varies by component)
- Spacing: 15-20px between sections

---

## 8. DATA BINDING & SYNCHRONIZATION

### 8.1 Two-Way Sync Pattern
```
UI Change (e.g., complete task)
  ↓
Update ViewData (task.isCompleted = true)
  ↓
Call Service Method (taskService.toggleTaskCompletion())
  ↓
Service updates DB via Repository
  ↓
If dependent data exists (schedule link):
  - Recalculate affected entities
  - Update related DB records
  ↓
Refresh UI from DB (loadScheduleData(), etc.)
```

### 8.2 Async Loading Pattern
```
loadScheduleData() / loadTasksData() / loadElementsData()
  ↓
Create JavaFX Task<List<ViewModel>>
  ↓
call() method:
  - Fetch from service
  - Convert entities to ViewModels
  - Return ObservableList
  ↓
setOnSucceeded():
  - Platform.runLater() for thread safety
  - Schedule table updates
  - Refresh UI
  ↓
setOnFailed():
  - Log error
  - Show error dialog
```

---

## 9. INTERACTION FEATURES

### 9.1 Visual Feedback
```
Hover Effects:
- Buttons: Color transitions on hover
- Project cells: Blue highlight (non-selected)
- Delete buttons: Red intensifies on hover

Selection Feedback:
- Project selected: Purple background + bright border
- Task completed: Green row background
- Checkbox marked: Green border + green mark

Progress Indicators:
- Loading: ProgressIndicator (visible during async)
- Schedule: ProgressBar in table
- Status badges: Color-coded (Pending/In Progress/Completed)
```

### 9.2 User Confirmations
- Delete operations: Confirmation dialogs
- Navigation away: Implicit (background cleanup)
- Error conditions: Error alerts with messages

---

## 10. INTEGRATION POINTS

### 10.1 With Storage Module
```
StorageService dependency → used to:
├── Get available storage items
├── Check stock availability before allocating
├── Display item details in element cards
└── Prevent over-allocation
```

### 10.2 With Notification System
```
NotificationService → triggers on:
├── New project created
├── Project status changed
├── Task completed
└── Schedule milestone reached
```

### 10.3 With Approval Service
```
PendingApprovalService → (if implemented):
├── May require approval for:
│   ├── Large element allocations
│   ├── Budget changes
│   └── Schedule modifications
```

---

## 11. CRITICAL FIXES IMPLEMENTED

✅ **FIX #1**: Availability check for elements before adding
✅ **FIX #2**: Highly visible checkbox marking with colors and borders
✅ **FIX #3**: Fixed text visibility in dialogs (dark backgrounds, white text)
✅ **FIX #4**: Edit Schedule functionality now working
✅ **FIX #5**: Enhanced project selection with hover effects and clear selection
✅ **FIX #6**: ProjectElement.storageItem changed from LAZY to EAGER

---

## SUMMARY TABLE

| Component | Type | Purpose | Status |
|-----------|------|---------|--------|
| Project | Entity | Root container for all project data | ✅ Complete |
| ProjectSchedule | Entity | Timeline phases with progress tracking | ✅ Complete |
| ProjectTask | Entity | Checklist items with schedule linking | ✅ Complete |
| ProjectNote | Entity | Important notes and warnings | ✅ Complete |
| ProjectElement | Entity | Storage item allocations | ✅ Complete |
| ProjectDocument | Entity | Attached files and contracts | ✅ Complete |
| ProjectsStorageController | UI Controller | Main workspace with tabs | ✅ Complete |
| ProjectDetailViewController | UI Controller | Detail view with analytics | ✅ Complete |
| ProjectsController | UI Controller | Basic welcome screen | ✅ Placeholder |
| ProjectService | Service | Project CRUD & business logic | ✅ Complete |
| Schedule/Task/Element/Note Services | Services | Entity-specific operations | ✅ Complete |
| ProjectRepository | Data Layer | Project queries | ✅ Complete |
| All child Repositories | Data Layer | Entity-specific queries | ✅ Complete |

