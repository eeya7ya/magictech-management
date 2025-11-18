# Integration Guide - Complete Implementation Steps

## Overview
This guide provides step-by-step instructions and code snippets to complete the Sales Module refactoring and all remaining features.

---

## Part 1: Sales Module Integration

### Step 1: Add Dependencies to SalesStorageController

**File**: `src/main/java/com/magictech/modules/sales/SalesStorageController.java`

**Add these autowired services** (around line 48):
```java
@Autowired private NotificationService notificationService;
@Autowired private ProjectCostBreakdownService costBreakdownService;
```

### Step 2: Remove "Pricing & Orders" Tab

**Location**: `createProjectDetailModal()` method (around line 280-310)

**Find this code**:
```java
Tab ordersTab = new Tab("💰 Pricing & Orders");
ordersTab.setStyle(TAB_STYLE);
ordersTab.setContent(createProjectOrdersTabRedesigned(project));
```

**DELETE** the entire ordersTab creation and addition to tabPane.

**Result**: Only 2 tabs remain (Contract PDF and Project Elements)

### Step 3: Update Project Elements Tab with Cost Breakdown

**Location**: `createProjectElementsTab()` method (around line 330)

**Replace the entire method** with this updated version:

```java
private VBox createProjectElementsTab(Project project) {
    VBox tab = new VBox(20);
    tab.setPadding(new Insets(30));
    tab.setStyle("-fx-background-color: transparent;");

    // Header
    Label header = new Label("📦 Project Elements");
    header.setFont(Font.font("System", FontWeight.BOLD, 18));
    header.setTextFill(Color.WHITE);

    // Add Element Button
    Button addElementBtn = new Button("+ Add Element to Project");
    addElementBtn.setStyle(
        "-fx-background-color: #7c3aed; " +  // Purple
        "-fx-text-fill: white; " +
        "-fx-font-weight: bold; " +
        "-fx-padding: 10 20; " +
        "-fx-background-radius: 5; " +
        "-fx-cursor: hand;"
    );
    addElementBtn.setOnAction(e -> handleAddElementToProject(project, tab));

    HBox headerBox = new HBox(20);
    headerBox.setAlignment(Pos.CENTER_LEFT);
    headerBox.getChildren().addAll(header, addElementBtn);

    // Elements FlowPane
    FlowPane elementsPane = new FlowPane(15, 15);
    elementsPane.setPadding(new Insets(10));

    // Load existing elements
    List<ProjectElement> elements = elementService.getElementsByProject(project.getId());
    for (ProjectElement element : elements) {
        VBox elementCard = createElementCard(element, project, tab);
        elementsPane.getChildren().add(elementCard);
    }

    ScrollPane scrollPane = new ScrollPane(elementsPane);
    scrollPane.setFitToWidth(true);
    scrollPane.setStyle("-fx-background-color: transparent;");
    VBox.setVgrow(scrollPane, Priority.ALWAYS);

    // **NEW: Cost Breakdown Section**
    CostBreakdownPanel breakdownPanel = new CostBreakdownPanel();

    // Calculate elements subtotal
    BigDecimal subtotal = calculateElementsSubtotal(elements);
    breakdownPanel.setElementsSubtotal(subtotal);

    // Load existing breakdown if exists
    Optional<ProjectCostBreakdown> existing = costBreakdownService.getBreakdownByProject(project.getId());
    existing.ifPresent(breakdownPanel::loadBreakdown);

    // Set save callback
    breakdownPanel.setOnSave(breakdown -> {
        try {
            breakdown.setProjectId(project.getId());
            costBreakdownService.saveBreakdown(breakdown, currentUser.getUsername());
            showSuccess("Cost breakdown saved successfully!");
        } catch (Exception ex) {
            showError("Failed to save breakdown: " + ex.getMessage());
        }
    });

    tab.getChildren().addAll(headerBox, scrollPane, breakdownPanel);
    return tab;
}

private BigDecimal calculateElementsSubtotal(List<ProjectElement> elements) {
    BigDecimal subtotal = BigDecimal.ZERO;
    for (ProjectElement element : elements) {
        if (element.getStorageItem() != null && element.getStorageItem().getPrice() != null) {
            BigDecimal price = element.getStorageItem().getPrice();
            BigDecimal quantity = new BigDecimal(element.getQuantityNeeded());
            subtotal = subtotal.add(price.multiply(quantity));
        }
    }
    return subtotal;
}
```

### Step 4: Update Element Addition Dialog (Availability Check)

**Location**: `handleAddElementToProject()` method

**Replace the quantity input section** with availability checking:

```java
private void handleAddElementToProject(Project project, VBox tabContent) {
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("Add Element to Project");
    dialog.setHeaderText("Select storage item and quantity");

    // ... existing item selection code ...

    // Quantity input with availability check
    Label quantityLabel = new Label("Quantity:");
    Spinner<Integer> quantitySpinner = new Spinner<>(0, 999999, 1);
    quantitySpinner.setEditable(true);

    // **NEW: Availability indicator**
    Label availabilityLabel = new Label();
    availabilityLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

    quantitySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
        if (selectedItem[0] != null) {
            int available = selectedItem[0].getQuantity();
            if (newVal <= available) {
                availabilityLabel.setText("✅ Available");
                availabilityLabel.setTextFill(Color.web("#22c55e"));
            } else {
                availabilityLabel.setText("❌ Not Available (Only " + available + " in stock)");
                availabilityLabel.setTextFill(Color.web("#ef4444"));
            }
        }
    });

    grid.add(quantityLabel, 0, 2);
    grid.add(quantitySpinner, 1, 2);
    grid.add(availabilityLabel, 1, 3);

    // ... rest of dialog code ...

    // In the OK button handler, check availability before saving:
    int requestedQty = quantitySpinner.getValue();
    int availableQty = selectedItem[0].getQuantity();

    if (requestedQty > availableQty) {
        showError("Not enough quantity available. Only " + availableQty + " in stock.");
        return;
    }

    // Proceed with creating element...
}
```

### Step 5: Add Notification When Project Created

**Location**: Find where new projects are created (in dashboard submodule)

**Add after project creation**:

```java
// After successful project creation:
Project newProject = projectService.createProject(project);

// **NEW: Send notification to Projects module users**
notificationService.createRoleNotification(
    "PROJECTS",  // Target role
    "SALES",     // Source module
    "PROJECT_CREATED",
    "New Project Created: " + newProject.getProjectName(),
    "A new project has been created from Sales module. Project: " + newProject.getProjectName(),
    currentUser.getUsername()
);

showSuccess("Project created and notification sent!");
```

---

## Part 2: Projects Module Integration

### Step 1: Add Dependencies to ProjectsStorageController

**File**: `src/main/java/com/magictech/modules/projects/ProjectsStorageController.java`

**Add these services**:
```java
@Autowired private PendingApprovalService approvalService;
@Autowired private NotificationService notificationService;
```

### Step 2: Update Element Addition to Create Approval Request

**Location**: `handleAddElement()` method

**Replace element creation** with approval request:

```java
private void handleAddElement() {
    // ... existing item selection dialog code ...

    // Instead of directly creating element, create approval request:
    PendingApproval approval = approvalService.createProjectElementApproval(
        selectedProject.getId(),
        selectedItem[0].getId(),
        requestedQty,
        currentUser.getUsername(),
        currentUser.getId(),
        "Request to add " + selectedItem[0].getProductName() + " to project"
    );

    // Show pending element card
    VBox pendingCard = createPendingElementCard(approval);
    elementsPane.getChildren().add(pendingCard);

    showSuccess("Approval request sent to Sales team. Awaiting approval...");
}
```

### Step 3: Create Pending Element Card

**Add new method**:

```java
private VBox createPendingElementCard(PendingApproval approval) {
    VBox card = new VBox(8);
    card.setPadding(new Insets(12));
    card.setStyle(
        "-fx-background-color: linear-gradient(to bottom, #3a2a5a, #2a2a2a); " +
        "-fx-border-color: #f59e0b; " +  // Orange border for pending
        "-fx-border-width: 2; " +
        "-fx-background-radius: 8; " +
        "-fx-border-radius: 8;"
    );
    card.setMaxWidth(250);

    Label statusLabel = new Label("⏳ PENDING APPROVAL");
    statusLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");

    // ... add approval details ...

    return card;
}
```

### Step 4: Handle Approval Responses

**Add listener for approval updates** (poll or use NotificationManager):

```java
// When approval is approved:
private void handleApprovalApproved(PendingApproval approval) {
    // Create actual element
    ProjectElement element = new ProjectElement();
    element.setProject(selectedProject);
    element.setStorageItem(storageService.findById(approval.getStorageItemId()).get());
    element.setQuantityNeeded(approval.getQuantity());
    element.setQuantityAllocated(approval.getQuantity());
    element.setStatus("Allocated");

    // Deduct from storage
    StorageItem item = element.getStorageItem();
    item.setQuantity(item.getQuantity() - approval.getQuantity());
    storageService.updateItem(item.getId(), item);

    // Save element
    elementService.createElement(element);

    // Remove pending card, add real card
    refreshElements();
}
```

---

## Part 3: Storage Module Analytics

### Add Analytics Tab to StorageController

**File**: `src/main/java/com/magictech/modules/storage/StorageController.java`

**In `createMainContent()` method**, add third tab:

```java
// Existing tabs
Tab storageTab = new Tab("📦 Storage Items");
Tab projectsTab = new Tab("📁 Projects");

// **NEW: Analytics Tab**
Tab analyticsTab = new Tab("📊 Analytics");
analyticsTab.setContent(createAnalyticsView());

tabPane.getTabs().addAll(storageTab, projectsTab, analyticsTab);
```

**Add new method**:

```java
private VBox createAnalyticsView() {
    VBox view = new VBox(20);
    view.setPadding(new Insets(30));

    // Project Workflow Section
    Label pwTitle = new Label("Project Workflow Analysis");
    pwTitle.setFont(Font.font("System", FontWeight.BOLD, 18));

    // TODO: Add charts using JavaFX Charts or external library

    // Customer Sales Section
    Label csTitle = new Label("Customer Sales Analysis");
    csTitle.setFont(Font.font("System", FontWeight.BOLD, 18));

    // TODO: Add bar charts, line charts

    view.getChildren().addAll(pwTitle, /* charts */, csTitle, /* charts */);
    return view;
}
```

---

## Part 4: Purple Theme Application

### Update Color Constants in All Controllers

**Find and replace** in all module controllers:

```java
// OLD colors:
#ef4444 (red)
#3b82f6 (blue)
#fb923c (orange)

// NEW colors:
#7c3aed (purple)
#6b21a8 (dark purple)
#a78bfa (light purple)
```

**Background**: Always `#1a1a1a` (dark black)

### Update MainDashboardController

**File**: `src/main/java/com/magictech/core/ui/controllers/MainDashboardController.java`

**In `createModuleCard()` method**:

```java
private VBox createModuleCard(String title, String icon, String description,
                               String colorClass, Runnable action) {
    VBox card = new VBox(15);
    // ... existing code ...

    // Replace color classes:
    // "module-red" → "#7c3aed"
    // "module-blue" → "#6b21a8"
    // "module-orange" → "#a78bfa"
    // etc.

    String gradient = String.format(
        "-fx-background-color: linear-gradient(to bottom right, %s, %s);",
        "#7c3aed", "#6b21a8"
    );
    card.setStyle(gradient + " -fx-background-radius: 15; ...");

    return card;
}
```

---

## Part 5: Notification Badges on Dashboard

### Integrate NotificationManager into MainDashboard

**File**: `src/main/java/com/magictech/core/ui/controllers/MainDashboardController.java`

**Add field**:
```java
@Autowired
private NotificationManager notificationManager;
```

**In `initialize()` method**:
```java
@Override
public void initialize(User user, ModuleConfig config) {
    this.currentUser = user;

    // Initialize notification manager
    notificationManager.initialize(user, sceneManager.getPrimaryStage());

    // Add badge update listener
    notificationManager.addBadgeUpdateListener(count -> {
        Platform.runLater(() -> updateNotificationBadge(count));
    });

    setupUI();
}
```

**Update module card titles with badge**:
```java
private void updateModuleCardBadge(String moduleName, long count) {
    // Find module card and update title
    if (count > 0) {
        titleLabel.setText(moduleName + " (" + count + ")");
    } else {
        titleLabel.setText(moduleName);
    }
}
```

**Add notification bell icon**:
```java
private HBox createNotificationBell() {
    Label bell = new Label("🔔");
    bell.setStyle("-fx-font-size: 24px; -fx-cursor: hand;");

    Label badge = new Label();
    badge.setStyle(
        "-fx-background-color: #ef4444; " +
        "-fx-text-fill: white; " +
        "-fx-background-radius: 10; " +
        "-fx-padding: 2 6 2 6; " +
        "-fx-font-size: 10px; " +
        "-fx-font-weight: bold;"
    );

    notificationManager.addBadgeUpdateListener(count -> {
        Platform.runLater(() -> {
            if (count > 0) {
                badge.setText(String.valueOf(count));
                badge.setVisible(true);
            } else {
                badge.setVisible(false);
            }
        });
    });

    StackPane stack = new StackPane(bell, badge);
    StackPane.setAlignment(badge, Pos.TOP_RIGHT);

    stack.setOnMouseClicked(e -> showNotificationPanel());

    return new HBox(stack);
}
```

---

## Part 6: Final Integration Checklist

### Before Testing:
- [ ] All dependencies autowired correctly
- [ ] DatabaseConfig updated with new packages
- [ ] Application.properties has correct database connection
- [ ] All imports resolved
- [ ] No compilation errors

### Testing Steps:
1. **Start application**: `mvn spring-boot:run`
2. **Check database**: Verify new tables created (notifications, pending_approvals, project_cost_breakdowns, customer_cost_breakdowns)
3. **Test Sales Module**:
   - Create new project → Check notification sent
   - Open project → Verify only 2 tabs (Contract PDF, Elements)
   - Add elements → Check cost breakdown appears
   - Enter cost breakdown values → Verify auto-calculation
   - Save breakdown → Check database
4. **Test Projects Module**:
   - Add element → Check approval request created
   - Check notification sent to Sales
5. **Test Approvals**:
   - Login as Sales user
   - Check notification panel shows pending approval
   - Approve request → Check element added to project
6. **Test Scheduled Tasks**:
   - Wait 2 days → Check approval auto-rejected
   - Wait 3 months → Check old notifications deleted

### Build and Deploy:
```bash
# Build
mvn clean package

# Run
java -jar target/magictech-management-1.0-SNAPSHOT.jar
```

---

## Support & Troubleshooting

### Common Issues:

**1. NotificationManager not found**
- Check `@Component` annotation exists
- Verify package scanning in Spring config

**2. Database tables not created**
- Check DatabaseConfig entity package scanning
- Verify Hibernate ddl-auto=update in properties

**3. Notifications not appearing**
- Check NotificationManager initialized in SceneManager
- Verify polling is started

**4. Cost breakdown not saving**
- Check ProjectCostBreakdownService autowired
- Verify database connection

### Need Help?
Refer to CLAUDE.md for architecture details and patterns.

---

**End of Integration Guide**
