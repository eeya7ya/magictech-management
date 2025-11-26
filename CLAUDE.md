# CLAUDE.md - MagicTech Management System

**AI Assistant Guide for Code Development**

Last Updated: 2025-11-26
Project: MagicTech Management System
Version: 1.0-SNAPSHOT

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture & Design Patterns](#architecture--design-patterns)
3. [Codebase Structure](#codebase-structure)
4. [Key Components](#key-components)
5. [Database Architecture](#database-architecture)
6. [UI Framework & Patterns](#ui-framework--patterns)
7. [Module System](#module-system)
8. [Notification & Messaging System](#notification--messaging-system)
9. [Workflow System](#workflow-system)
10. [Development Workflows](#development-workflows)
11. [Coding Conventions](#coding-conventions)
12. [Security Considerations](#security-considerations)
13. [Testing & API](#testing--api)
14. [Common Tasks](#common-tasks)

---

## Project Overview

### What is MagicTech Management System?

A **hybrid JavaFX desktop application with Spring Boot backend** for comprehensive business management, including:
- Storage/Inventory Management
- Sales Order Processing & Customer Management
- Presales (Quotations & Sizing)
- Project Management with Workflows
- Finance & Invoicing
- Quality Assurance
- Maintenance Tracking
- Real-time Notifications & Inter-module Communication
- 8-Step Project Workflow with Site Surveys

### Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 21 |
| Backend Framework | Spring Boot | 3.2.0 |
| UI Framework | JavaFX | 21.0.1 |
| Database | PostgreSQL | Latest |
| ORM | Hibernate/JPA | Spring Boot managed |
| Connection Pool | HikariCP | Spring Boot managed |
| Messaging | Redis (Lettuce) | Spring Boot managed |
| UI Library | AtlantaFX | 2.0.1 |
| UI Controls | ControlsFX | 11.2.1 |
| Icons | Ikonli (FontAwesome 5 + Material) | 12.3.1 |
| PDF Processing | Apache PDFBox + iText 7 | 2.0.29 / 7.2.5 |
| Excel Processing | Apache POI | 5.2.5 |
| Build Tool | Maven | Latest |

### Project Statistics

- **Total Java Files**: ~71
- **Lines of Code**: ~17,174
- **Package**: `com.magictech`
- **Main Class**: `com.magictech.MainApp`
- **Server Port**: 8085

---

## Architecture & Design Patterns

### Hybrid Architecture

This application uses a **unique hybrid architecture**:

```
┌─────────────────────────────────────────┐
│         JavaFX Desktop UI               │
│    (FXML + Programmatic JavaFX)         │
│                                         │
│  ┌────────────────────────────────┐   │
│  │     Spring Boot Backend        │   │
│  │  - Business Logic (Services)   │   │
│  │  - Data Access (Repositories)  │   │
│  │  - REST API (Controllers)      │   │
│  └────────────────────────────────┘   │
│                 ▼                       │
│         PostgreSQL Database             │
└─────────────────────────────────────────┘
```

**Key Insight**: Both desktop UI and REST API server run in the **same JVM process**, enabling both local desktop usage and remote API access.

### Design Patterns Used

1. **MVC (Model-View-Controller)**: JavaFX FXML (View), Controllers (Controller), Entities/Services (Model)
2. **Repository Pattern**: Spring Data JPA for data access abstraction
3. **Service Layer Pattern**: Business logic encapsulation with `@Service`
4. **Dependency Injection**: Spring's `@Autowired` throughout
5. **Factory Pattern**: `ModuleConfig.createXxxConfig()` factory methods
6. **Builder Pattern**: `ModuleConfig.Builder` for configuration
7. **ViewModel Pattern**: Separate ViewModels for JavaFX property binding
8. **Template Method Pattern**: `BaseModuleController` and `BaseStorageModuleController`
9. **Strategy Pattern**: `ModuleStorageConfig` enum for module-specific behaviors
10. **Soft Delete Pattern**: `active` boolean flag on all entities

---

## Codebase Structure

### Root Package: `com.magictech`

```
com.magictech/
├── MainApp.java                          # Application entry point
│
├── core/                                 # Framework-level reusable code
│   ├── api/                              # REST API controllers
│   │   └── AuthController.java           # Authentication API endpoints
│   │
│   ├── auth/                             # User authentication & management
│   │   ├── User.java                     # User entity
│   │   ├── UserRole.java                 # Role enumeration
│   │   ├── UserRepository.java           # User data access
│   │   └── AuthenticationService.java    # Authentication logic
│   │
│   ├── config/                           # Application configuration
│   │   ├── DatabaseConfig.java           # JPA/Hibernate configuration
│   │   ├── SecurityConfig.java           # Security config (placeholder)
│   │   ├── RedisConfig.java              # Redis pub/sub configuration
│   │   └── SchedulingConfig.java         # Scheduled tasks configuration
│   │
│   ├── messaging/                        # NEW: Real-time notification system
│   │   ├── config/
│   │   │   └── RedisConfig.java          # Redis connection & listeners
│   │   ├── constants/
│   │   │   └── NotificationConstants.java # Notification types, channels
│   │   ├── dto/
│   │   │   └── NotificationMessage.java  # Notification message DTO
│   │   ├── entity/
│   │   │   ├── Notification.java         # Notification persistence
│   │   │   ├── NotificationUserStatus.java # User read status tracking
│   │   │   └── DeviceRegistration.java   # Device tracking
│   │   ├── repository/
│   │   │   ├── NotificationRepository.java
│   │   │   ├── NotificationUserStatusRepository.java
│   │   │   └── DeviceRegistrationRepository.java
│   │   ├── service/
│   │   │   ├── NotificationService.java  # Main notification service
│   │   │   ├── NotificationListenerService.java # Redis listener
│   │   │   └── DeviceRegistrationService.java
│   │   ├── ui/
│   │   │   ├── NotificationManager.java  # UI notification management
│   │   │   └── NotificationPopup.java    # Visual notification popup
│   │   └── util/
│   │       └── NotificationSoundGenerator.java # Sound generation
│   │
│   ├── module/                           # Module framework
│   │   ├── BaseModuleController.java     # Abstract base for all modules
│   │   └── ModuleConfig.java             # Module metadata & configuration
│   │
│   └── ui/                               # UI framework components
│       ├── SceneManager.java             # Scene navigation manager
│       ├── SpringFXMLLoader.java         # Spring-aware FXML loader
│       ├── components/                   # Reusable UI components
│       │   ├── BackgroundManager.java
│       │   ├── GradientBackgroundPane.java
│       │   ├── DashboardBackgroundPane.java
│       │   ├── NotificationPopup.java    # Legacy notification popup
│       │   └── ToastNotification.java    # Toast-style notifications
│       └── controllers/                  # Core UI controllers
│           ├── LoginController.java      # Login screen
│           ├── MainDashboardController.java  # Module dashboard
│           └── UserManagementController.java # User management UI
│
└── modules/                              # Business domain modules
    ├── storage/                          # Storage module (foundation)
    │   ├── base/
    │   │   └── BaseStorageModuleController.java  # Base for storage-based modules
    │   ├── config/
    │   │   └── ModuleStorageConfig.java  # Column visibility configuration
    │   ├── entity/
    │   │   ├── StorageItem.java          # Main shared entity (with workflowStatus)
    │   │   ├── StorageColumnConfig.java  # Dynamic columns
    │   │   └── WorkflowStatus.java       # NEW: Item workflow status enum
    │   ├── model/
    │   │   └── StorageItemViewModel.java # JavaFX binding model
    │   ├── repository/
    │   │   ├── StorageItemRepository.java
    │   │   └── StorageColumnConfigRepository.java
    │   ├── service/
    │   │   ├── StorageService.java
    │   │   ├── StorageColumnConfigService.java
    │   │   ├── AnalyticsService.java     # NEW: Analytics & reporting
    │   │   ├── ExcelImportService.java
    │   │   └── ExcelExportService.java
    │   └── StorageController.java        # Storage module UI
    │
    ├── sales/                            # Sales module (SIGNIFICANTLY EXPANDED)
    │   ├── entity/
    │   │   ├── Customer.java
    │   │   ├── CustomerElement.java      # NEW: Customer-item relationships
    │   │   ├── CustomerTask.java         # NEW: Customer tasks
    │   │   ├── CustomerNote.java         # NEW: Customer notes
    │   │   ├── CustomerSchedule.java     # NEW: Customer schedules
    │   │   ├── CustomerDocument.java     # NEW: Customer documents
    │   │   ├── CustomerCostBreakdown.java # NEW: Cost analysis
    │   │   ├── SalesOrder.java
    │   │   ├── SalesOrderItem.java
    │   │   ├── SalesContract.java
    │   │   ├── ProjectWorkflow.java      # NEW: 8-step workflow tracker
    │   │   ├── WorkflowStepCompletion.java # NEW: Step-level tracking
    │   │   ├── SiteSurveyData.java       # NEW: Site survey Excel + images
    │   │   ├── SizingPricingData.java    # NEW: Presales pricing data
    │   │   ├── BankGuaranteeData.java    # NEW: Finance bank guarantee
    │   │   ├── MissingItemRequest.java   # NEW: Missing items tracking
    │   │   ├── ProjectCostData.java      # NEW: Project cost tracking
    │   │   └── ProjectCostBreakdown.java # NEW: Cost breakdown
    │   ├── model/                        # ViewModels for JavaFX
    │   │   ├── CustomerViewModel.java
    │   │   ├── CustomerElementViewModel.java
    │   │   ├── CustomerTaskViewModel.java
    │   │   ├── CustomerNoteViewModel.java
    │   │   ├── CustomerScheduleViewModel.java
    │   │   ├── SalesOrderViewModel.java
    │   │   └── SalesOrderItemViewModel.java
    │   ├── repository/                   # All repositories
    │   ├── service/
    │   │   ├── CustomerService.java
    │   │   ├── CustomerElementService.java
    │   │   ├── CustomerTaskService.java
    │   │   ├── CustomerNoteService.java
    │   │   ├── CustomerScheduleService.java
    │   │   ├── CustomerDocumentService.java
    │   │   ├── CustomerCostBreakdownService.java
    │   │   ├── SalesOrderService.java
    │   │   ├── ProjectWorkflowService.java # NEW: Workflow orchestration
    │   │   ├── WorkflowStepService.java    # NEW: Step management
    │   │   ├── WorkflowNotificationService.java
    │   │   ├── SiteSurveyExcelService.java # NEW: Excel parsing
    │   │   ├── ComprehensiveExcelExportService.java
    │   │   └── SalesExcelExportService.java
    │   ├── SalesController.java          # Main sales UI with workflow
    │   ├── SalesStorageController.java   # Sales storage view
    │   └── CustomerManagementController.java # NEW: Customer management UI
    │
    ├── presales/                         # NEW: Presales module
    │   └── PresalesController.java       # Quotations & sizing/pricing
    │
    ├── finance/                          # NEW: Finance module
    │   └── FinanceController.java        # Invoicing & bank guarantees
    │
    ├── projects/                         # Project management module
    │   ├── entity/
    │   │   ├── Project.java
    │   │   ├── ProjectElement.java       # Links StorageItem to Project
    │   │   ├── ProjectTask.java
    │   │   ├── ProjectNote.java
    │   │   ├── ProjectSchedule.java
    │   │   ├── ProjectDocument.java      # NEW: Document storage (PDFs, etc.)
    │   │   └── SiteSurveyRequest.java    # NEW: Site survey requests
    │   ├── model/                        # ViewModels
    │   │   ├── ProjectViewModel.java
    │   │   ├── ProjectElementViewModel.java
    │   │   ├── ProjectTaskViewModel.java
    │   │   └── ProjectScheduleViewModel.java
    │   ├── repository/
    │   ├── service/
    │   │   ├── ProjectService.java
    │   │   ├── ProjectElementService.java
    │   │   ├── ProjectTaskService.java
    │   │   ├── ProjectNoteService.java
    │   │   ├── ProjectScheduleService.java
    │   │   ├── ProjectDocumentService.java
    │   │   └── SiteSurveyRequestService.java
    │   ├── ProjectsController.java       # Main projects UI
    │   ├── ProjectDetailViewController.java # Detailed project view
    │   └── ProjectsStorageController.java # Storage view
    │
    ├── maintenance/                      # Maintenance module
    │   ├── MaintenanceController.java
    │   └── MaintenanceStorageController.java
    │
    └── qualityassurance/                 # NEW: QA module (formerly "pricing")
        └── QualityAssuranceController.java # QA management + analytics
```

### Resources Structure

```
src/main/resources/
├── application.properties                # Spring Boot configuration
├── fxml/                                 # FXML view files
│   ├── login.fxml
│   └── main-dashboard.fxml
├── css/                                  # Stylesheets
│   ├── styles.css
│   └── dashboard.css
└── images/                               # Image assets
```

---

## Key Components

### 1. Application Entry Point: `MainApp.java`

```java
@SpringBootApplication
public class MainApp extends javafx.application.Application {
    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        // Start Spring Boot context BEFORE JavaFX
        springContext = SpringApplication.run(MainApp.class);
    }

    @Override
    public void start(Stage primaryStage) {
        SceneManager sceneManager = springContext.getBean(SceneManager.class);
        sceneManager.setPrimaryStage(primaryStage);
        sceneManager.showLoginScene();
    }
}
```

**Initialization Flow**:
1. JavaFX launches application
2. `init()` starts Spring Boot context
3. `start()` initializes JavaFX UI with Spring beans
4. Shows login screen

### 2. SceneManager (Navigation Hub)

**Responsibilities**:
- Manages primary JavaFX Stage
- Handles scene transitions with loading overlays
- Stores current user context
- Creates and initializes module controllers
- Manages cleanup on scene changes

**Critical Pattern**: Manual dependency injection for module controllers:
```java
StorageController controller = new StorageController();
context.getAutowireCapableBeanFactory().autowireBean(controller);
controller.initialize(currentUser, config);
```

**Why?** Module controllers are **not** Spring-managed beans (created dynamically), so we manually wire dependencies.

### 3. Controller Hierarchy

```
BaseModuleController (abstract)
├── setupUI()          [abstract]
├── loadData()         [abstract]
├── refresh()          [abstract]
├── cleanup()
└── Alert helpers (showSuccess, showError, showWarning)
    ▼
BaseStorageModuleController (abstract, storage-specific)
├── All of BaseModuleController methods
├── TableView management with checkboxes
├── Search/filter functionality
├── Bulk selection operations
├── Excel import/export
├── Animated backgrounds
└── Column configuration per module
    ▼
Concrete Module Controllers
├── StorageController
├── SalesStorageController
├── MaintenanceStorageController
├── ProjectsStorageController
└── PricingStorageController
```

### 4. Service Layer Pattern

All services follow this structure:

```java
@Service
@Transactional  // All methods run in transactions
public class StorageService {

    @Autowired
    private StorageItemRepository repository;

    // CRUD operations
    public StorageItem saveItem(StorageItem item) { ... }
    public Optional<StorageItem> findById(Long id) { ... }
    public List<StorageItem> findAllActive() { ... }
    public void deleteItem(Long id) { ... }

    // Business logic
    public List<StorageItem> searchItems(String searchTerm) { ... }
    public void updateQuantity(Long id, int quantity) { ... }
    public List<StorageItem> findLowStockItems() { ... }
}
```

**Convention**: One service per entity, annotated with `@Service` and `@Transactional`.

### 5. Repository Pattern

```java
@Repository
public interface StorageItemRepository extends JpaRepository<StorageItem, Long> {

    // Derived query methods (auto-implemented by Spring Data)
    List<StorageItem> findByActiveTrue();
    List<StorageItem> findByManufactureAndActiveTrue(String manufacture);

    // Custom queries
    @Query("SELECT s FROM StorageItem s WHERE s.active = true AND " +
           "(LOWER(s.productName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.manufacture) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<StorageItem> searchItems(@Param("searchTerm") String searchTerm);
}
```

**Convention**: All repositories extend `JpaRepository<Entity, Long>`.

### 6. ViewModel Pattern

**Why?** JavaFX `TableView` requires JavaFX Properties for reactive binding, but entities use plain Java fields.

```java
public class StorageItemViewModel {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty productName = new SimpleStringProperty();
    private final IntegerProperty quantity = new SimpleIntegerProperty();

    // Constructor from entity
    public StorageItemViewModel(StorageItem item) {
        this.id.set(item.getId());
        this.productName.set(item.getProductName());
        this.quantity.set(item.getQuantity());
    }

    // Properties for JavaFX binding
    public StringProperty productNameProperty() { return productName; }
    public IntegerProperty quantityProperty() { return quantity; }

    // Convert back to entity
    public StorageItem toEntity() { ... }
}
```

---

## Database Architecture

### Configuration

**Database**: PostgreSQL
**Connection Pool**: HikariCP (10 max, 5 min idle)
**Schema Management**: `hibernate.hbm2ddl.auto=update` (auto-create/update)
**SQL Logging**: Enabled in DEBUG mode

### Entity Standard Pattern

**ALL entities follow this structure**:

```java
@Entity
@Table(name = "table_name")
public class EntityName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Business fields
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    // Standard metadata fields (EVERY entity has these)
    @Column(name = "date_added", nullable = false)
    private LocalDateTime dateAdded;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(nullable = false)
    private Boolean active = true;  // Soft delete flag

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        this.dateAdded = LocalDateTime.now();
        if (this.active == null) this.active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}
```

### Entity Relationships Map

#### **User Management**
```
User
├── id: Long (PK)
├── username: String (UNIQUE)
├── password: String (plain text - NOT PRODUCTION READY)
├── role: UserRole (enum)
├── photoPath: String
├── lastLogin: LocalDateTime
└── Standard metadata fields
```

**UserRole enum**: `MASTER`, `SALES`, `PRESALES`, `FINANCE`, `MAINTENANCE`, `PROJECTS`, `QUALITY_ASSURANCE`, `STORAGE`, `CLIENT`

#### **Storage Module** (Shared Entity)
```
StorageItem (THE core shared entity across modules)
├── id: Long (PK)
├── manufacture: String
├── productName: String (required)
├── code: String
├── serialNumber: String
├── quantity: Integer
├── price: BigDecimal
├── customFields: String (JSON)
└── Standard metadata fields

StorageColumnConfig (Dynamic column configuration)
├── id: Long (PK)
├── columnName: String (UNIQUE)
├── columnLabel: String
├── columnType: String (TEXT/NUMBER/DATE/BOOLEAN)
├── columnWidth: Integer
├── displayOrder: Integer
├── visible: Boolean
├── editable: Boolean
└── isDefault: Boolean
```

**Key Insight**: `StorageItem` is shared by ALL storage-based modules. Column visibility is controlled by `ModuleStorageConfig`, not database.

#### **Sales Module**
```
Customer
├── id: Long (PK)
├── name: String (required)
├── email: String (UNIQUE, @Email)
├── phone: String
├── address: String
├── company: String
└── Standard metadata fields
    ▼ One-to-Many
SalesOrder
├── id: Long (PK)
├── orderType: String ("PROJECT" or "CUSTOMER")
├── projectId: Long (nullable, references Project)
├── customerId: Long (@ManyToOne → Customer, nullable)
├── subtotal: BigDecimal
├── tax: BigDecimal
├── saleDiscount: BigDecimal
├── crewCost: BigDecimal
├── additionalMaterials: BigDecimal
├── totalAmount: BigDecimal (auto-calculated)
├── status: String (DRAFT/CONFIRMED/PUSHED_TO_PROJECT/CANCELLED)
└── items: List<SalesOrderItem> (@OneToMany, cascade=ALL)
    ▼
    SalesOrderItem
    ├── id: Long (PK)
    ├── salesOrderId: Long (@ManyToOne → SalesOrder)
    ├── storageItemId: Long (@ManyToOne → StorageItem)
    ├── quantity: Integer
    ├── unitPrice: BigDecimal
    └── totalPrice: BigDecimal (calculated)

SalesContract (separate entity)
├── id: Long (PK)
├── salesOrderId: Long (@ManyToOne → SalesOrder)
└── Contract-specific fields
```

**Important**: `SalesOrder.totalAmount` is auto-calculated via `@PrePersist` and `@PreUpdate` hooks.

#### **Projects Module**
```
Project (One-to-Many relationships)
├── id: Long (PK)
├── projectName: String (required)
├── projectLocation: String
├── dateOfIssue: LocalDate
├── dateOfCompletion: LocalDate
├── status: String
├── notes: Text
└── Standard metadata fields
    ▼ One-to-Many
    ├── elements: List<ProjectElement>  # Storage items linked to project
    ├── tasks: List<ProjectTask>        # Task checklist
    ├── notes: List<ProjectNote>        # Project notes/comments
    └── schedules: List<ProjectSchedule> # Timeline/schedule

ProjectElement (Junction entity)
├── id: Long (PK)
├── projectId: Long (@ManyToOne → Project)
├── storageItemId: Long (@ManyToOne(fetch=EAGER) → StorageItem)
├── quantityNeeded: Integer
├── quantityAllocated: Integer
├── status: String
└── Standard metadata fields

ProjectTask (Checklist/Task management)
├── id: Long (PK)
├── projectId: Long (@ManyToOne → Project)
├── taskTitle: String
├── taskDetails: Text
├── isCompleted: Boolean
├── priority: String
├── dueDate: LocalDateTime
├── scheduleTaskName: String (link to ProjectSchedule)
└── Standard metadata fields

ProjectNote
├── id: Long (PK)
├── projectId: Long (@ManyToOne → Project)
├── noteContent: Text
└── Standard metadata fields

ProjectSchedule
├── id: Long (PK)
├── projectId: Long (@ManyToOne → Project)
├── taskName: String
├── startDate: LocalDate
├── endDate: LocalDate
└── Standard metadata fields
```

### Soft Delete Pattern

**ALL entities use soft delete by default**:
- `active = true`: Active record
- `active = false`: Soft deleted (hidden from queries)

**Repository queries**: Always filter by `findByActiveTrue()` or add `WHERE active = true`.

**Hard delete**: Available via service methods (permanent removal from database).

---

## UI Framework & Patterns

### JavaFX + Spring Integration

#### SpringFXMLLoader Pattern

```java
@Component
public class SpringFXMLLoader {
    @Autowired
    private ApplicationContext context;

    public <T> T load(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource(fxmlPath));
        loader.setControllerFactory(context::getBean);  // KEY: Spring DI
        return loader.load();
    }
}
```

**This allows**: FXML controllers to use `@Autowired` and access Spring beans.

### FXML Structure

**Location**: `/src/main/resources/fxml/`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import com.magictech.core.ui.components.GradientBackgroundPane?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>

<StackPane xmlns:fx="http://javafx.com/fxml"
           fx:controller="com.magictech.core.ui.controllers.LoginController">

    <GradientBackgroundPane fx:id="gradientBackground"/>

    <!-- UI elements with fx:id for controller access -->
    <TextField fx:id="usernameField"/>
    <PasswordField fx:id="passwordField"/>
    <Button fx:id="loginButton" onAction="#handleLogin"/>

</StackPane>
```

**Pattern**: Controllers are Spring-managed, FXML uses `fx:controller` attribute, fields bound via `fx:id`.

### Custom UI Components

All custom components support animation cleanup:

```java
public class GradientBackgroundPane extends Region {
    private Timeline animation;

    public void stopAnimation() {
        if (animation != null) {
            animation.stop();
            animation = null;
        }
    }
}
```

**Components**:
1. `GradientBackgroundPane` - Animated gradient background
2. `DashboardBackgroundPane` - Dashboard-specific animated background
3. `BackgroundManager` - Background animation manager

### Styling

**UI Library**: AtlantaFX (modern JavaFX theme)
**Icons**: Ikonli (FontAwesome 5 + Material Design)

**CSS Files**:
- `/src/main/resources/css/styles.css` - Global styles
- `/src/main/resources/css/dashboard.css` - Dashboard-specific

### TableView Pattern

**Storage-based modules use standardized TableView pattern**:

```java
// Checkbox column for bulk selection
TableColumn<StorageItemViewModel, Boolean> selectColumn = new TableColumn<>("");
selectColumn.setCellFactory(col -> new CheckBoxTableCell<>(index -> {
    StorageItemViewModel item = tableView.getItems().get(index);
    return selectionMap.get(item);
}));

// Double-click to edit
tableView.setOnMouseClicked(event -> {
    if (event.getClickCount() == 2 && !tableView.getSelectionModel().isEmpty()) {
        handleEdit();
    }
});
```

**Features**:
- Checkbox selection with SelectAll
- Reactive selection count
- Double-click to edit
- Empty state handling
- Search/filter integration

---

## Module System

### Module Configuration

Each module is defined via `ModuleConfig` using **Factory Pattern**:

```java
public static ModuleConfig createStorageConfig() {
    return new Builder()
        .name("storage")
        .displayName("Storage Management")
        .description("Manage inventory and stock items")
        .icon("📦")
        .colorScheme("module-red")
        .allowedRoles(UserRole.MASTER, UserRole.STORAGE)
        .build();
}
```

### Storage Module Pattern

**`ModuleStorageConfig` enum** defines column visibility per module:

```java
SALES(
    Arrays.asList("id", "manufacture", "productName", "availabilityStatus", "price"),
    Arrays.asList("manufacture", "productName", "price"),  // editable columns
    true  // useAvailabilityStatus (hide actual quantities)
),
STORAGE(
    Arrays.asList("id", "manufacture", "productName", "quantity", "price"),
    Arrays.asList("manufacture", "productName", "quantity", "price"),
    false  // show actual quantity numbers
)
```

**Key Insight**: Multiple modules share the **SAME** `storage_items` table but see **different columns** and have **different permissions**:
- **Sales**: Sees availability status (✅/❌), can't see actual quantities
- **Storage**: Sees actual quantities, full edit access
- **Maintenance/Projects/Pricing**: Custom column sets

### Module Lifecycle

1. User clicks module card on dashboard
2. `SceneManager.showModule(moduleName)` called
3. Fresh controller instance created (`new ModuleController()`)
4. Spring autowires dependencies via `autowireBean()`
5. `controller.initialize(currentUser, moduleConfig)` called
6. `setupUI()` builds the UI programmatically
7. `loadData()` fetches data from database
8. User interacts with module
9. On navigation: `cleanup()` or `immediateCleanup()` called
10. Background animations stopped

---

## Notification & Messaging System

### Overview

The application features a **comprehensive real-time notification system** built on **Redis Pub/Sub** for inter-module communication. This enables seamless collaboration between different modules (Sales, Projects, Presales, Finance, QA, etc.) with instant notifications.

### Architecture

```
┌─────────────────┐         Redis          ┌─────────────────┐
│  Sales Module   │ ───► Pub/Sub Channels ◄─── │ Projects Module │
└─────────────────┘                        └─────────────────┘
        │                                           │
        ▼                                           ▼
┌──────────────────────────────────────────────────────────┐
│            NotificationService (Core)                    │
│  - Publishes notifications to Redis                     │
│  - Stores notification history in PostgreSQL            │
│  - Tracks read status per user                          │
└──────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────┐
│      NotificationListenerService (Subscribers)           │
│  - Listens on module-specific channels                  │
│  - Displays notifications via UI popups                 │
│  - Plays notification sounds                            │
└──────────────────────────────────────────────────────────┘
```

### Key Components

#### 1. **Notification Entity**

```java
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Notification metadata
    private String title;
    private String message;
    private String type;  // INFO, WARNING, SUCCESS, ERROR
    private String priority;  // LOW, MEDIUM, HIGH, URGENT

    // Module routing
    private String module;  // Target module (SALES, PROJECTS, etc.)
    private String action;  // Action type (CREATE, UPDATE, APPROVE, etc.)
    private String entityType;  // Entity type (PROJECT, ORDER, etc.)
    private Long entityId;  // Entity ID reference

    // Status tracking
    private Boolean isRead = false;
    private Boolean resolved = false;  // For approvals

    // Metadata
    private String metadata;  // JSON for additional data
    private LocalDateTime timestamp;

    // User tracking
    private String senderUsername;
    private String targetDeviceId;  // Optional device targeting
}
```

#### 2. **NotificationUserStatus Entity**

Tracks which users have seen which notifications (prevents showing same notification multiple times):

```java
@Entity
@Table(name = "notification_user_status")
public class NotificationUserStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long notificationId;
    private String username;
    private LocalDateTime seenAt;
    private Boolean dismissed = false;
}
```

#### 3. **NotificationService** (Core Service)

Main service for publishing and managing notifications:

```java
@Service
public class NotificationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    // Publish notification to Redis and save to database
    public void publishNotification(NotificationMessage message) {
        // Save to database
        Notification notification = saveNotification(message);

        // Publish to Redis channels
        String moduleChannel = message.getModule() + "_notifications";
        redisTemplate.convertAndSend(moduleChannel, message);

        // Also publish to specific action channel if needed
        if (message.getAction() != null) {
            String actionChannel = String.format("%s:%s:%s",
                message.getModule(), message.getAction(), message.getEntityType());
            redisTemplate.convertAndSend(actionChannel, message);
        }
    }

    // Built-in notification templates
    public void notifyProjectCreated(Long projectId, String projectName, String creator);
    public void notifyElementApprovalRequest(Long projectId, Long elementId);
    public void notifyConfirmationRequested(Long projectId);
    public void notifyProjectCompleted(Long projectId, String projectName);
}
```

#### 4. **Redis Channels**

The system uses multiple channel patterns:

| Channel Pattern | Purpose | Example |
|----------------|---------|---------|
| `{module}_notifications` | Module-specific notifications | `projects_notifications`, `sales_notifications` |
| `{module}:{action}:{entityType}` | Action-specific channels | `projects:APPROVE:PROJECT_ELEMENT` |
| `all_notifications` | Broadcast to all modules | Used for system-wide announcements |

#### 5. **NotificationListener Service**

Subscribes to Redis channels and displays UI notifications:

```java
@Service
public class NotificationListenerService {

    @Autowired
    private NotificationManager notificationManager;

    @MessageListener
    public void handleNotification(NotificationMessage message) {
        // Check if user should see this notification
        if (shouldDisplayForCurrentUser(message)) {
            // Display visual popup
            Platform.runLater(() -> {
                notificationManager.showNotification(
                    message.getTitle(),
                    message.getMessage(),
                    message.getType()
                );

                // Play sound if enabled
                NotificationSoundGenerator.playNotificationSound();
            });

            // Mark as seen for this user
            notificationService.markAsSeenByUser(message.getId(), currentUser);
        }
    }
}
```

#### 6. **Notification UI Components**

- **NotificationPopup**: Visual popup using ControlsFX `Notifications`
- **ToastNotification**: Lightweight toast-style notifications
- **NotificationManager**: Manages popup display, stacking, and auto-dismiss
- **NotificationSoundGenerator**: Generates simple notification beeps

### Common Notification Workflows

#### Workflow 1: Project Created (Sales → Projects)

```java
// In SalesController
projectWorkflowService.createWorkflow(project);

// NotificationService automatically publishes:
notificationService.notifyProjectCreated(
    project.getId(),
    project.getProjectName(),
    currentUser.getUsername()
);

// ProjectsController receives notification via Redis listener
// and displays popup: "New project 'ABC Corp' created by John"
```

#### Workflow 2: Approval Request (Projects → Sales)

```java
// In ProjectsController
projectElementService.requestApproval(element);

// Publishes approval notification
notificationService.notifyElementApprovalRequest(
    projectId,
    elementId
);

// SalesController receives and displays approval popup
// with Accept/Reject buttons
```

### Notification Types & Priorities

**Types** (affects visual styling):
- `INFO` - Blue, informational
- `WARNING` - Orange, warning
- `SUCCESS` - Green, success
- `ERROR` - Red, error/critical

**Priorities** (affects persistence and sound):
- `LOW` - Auto-dismiss after 5 seconds
- `MEDIUM` - Auto-dismiss after 8 seconds
- `HIGH` - Persists until dismissed
- `URGENT` - Persists + blocking modal dialog

### Missed Notifications

The system loads missed notifications on login:

```java
// On user login, load notifications missed since last session
List<Notification> missedNotifications =
    notificationService.getNotificationsSinceLastSeen(
        username,
        lastLoginTime
    );

// Display all missed notifications as stacked popups
for (Notification notification : missedNotifications) {
    notificationManager.showNotification(notification);
}
```

### Configuration

**Redis Configuration** (`RedisConfig.java`):
```java
@Configuration
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(
            new RedisStandaloneConfiguration("localhost", 6379)
        );
    }

    @Bean
    public RedisMessageListenerContainer redisContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory());
        container.setRecoveryInterval(5000L);  // Auto-reconnect every 5s
        return container;
    }
}
```

**Requirements**:
- Redis server running on localhost:6379 (default)
- Redis pub/sub enabled (default)
- No authentication required (can be configured)

---

## Workflow System

### Overview

The application implements a **comprehensive 8-step sequential workflow** for project lifecycle management, spanning multiple modules (Sales, Presales, Finance, Projects, QA). This workflow orchestrates complex business processes with approval gates, notifications, and external actions.

### The 8-Step Workflow

```
Step 1: Site Survey          (Sales uploads Excel with photos)
   ↓ → Notification to Presales
Step 2: Selection & Design   (Presales uploads sizing/pricing)
   ↓ → Notification to Finance
Step 3: Bank Guarantee       (Finance uploads bank guarantee)
   ↓ → Notification to Sales
Step 4: Missing Item Request (Sales tracks missing items)
   ↓
Step 5: Tender Acceptance    (Sales approves/rejects tender)
   ↓ → Notification to Projects
Step 6: Project Finished     (Projects completes execution)
   ↓ → Notification to QA/Storage
Step 7: After-Sales Support  (Post-sale service)
   ↓
Step 8: Completion           (Final closure)
```

### Key Workflow Entities

#### 1. **ProjectWorkflow** (Main Workflow Tracker)

```java
@Entity
@Table(name = "project_workflows")
public class ProjectWorkflow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Workflow identification
    private Long projectId;  // References Project entity
    private Long customerId;  // Optional: for customer-based workflows

    // Status tracking
    private Integer currentStep;  // 1-8
    private String status;  // IN_PROGRESS, COMPLETED, REJECTED, ON_HOLD

    // Step completion flags
    private Boolean step1Completed = false;  // Site Survey
    private Boolean step2Completed = false;  // Selection & Design
    private Boolean step3Completed = false;  // Bank Guarantee
    private Boolean step4Completed = false;  // Missing Items
    private Boolean step5Completed = false;  // Tender Acceptance
    private Boolean step6Completed = false;  // Project Finished
    private Boolean step7Completed = false;  // After-Sales
    private Boolean step8Completed = false;  // Completion

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    // Helper methods
    public boolean isStepCompleted(int stepNumber) {
        // Returns completion status for given step
    }

    public void markStepCompleted(int stepNumber) {
        // Marks step as completed and advances currentStep
    }
}
```

#### 2. **WorkflowStepCompletion** (Detailed Step Tracking)

```java
@Entity
@Table(name = "workflow_step_completions")
public class WorkflowStepCompletion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long workflowId;
    private Integer stepNumber;  // 1-8
    private String stepName;  // "Site Survey", "Selection & Design", etc.

    // Completion tracking
    private Boolean completed = false;
    private LocalDateTime completedAt;
    private String completedBy;  // Username

    // External action tracking
    private Boolean needsExternalAction = false;
    private String externalModule;  // PRESALES, FINANCE, PROJECT, QA
    private Boolean externalActionCompleted = false;
    private LocalDateTime externalActionCompletedAt;

    // Step-specific data
    private String rejectionReason;  // For Step 5 (tender rejection)
    private LocalDate expectedCompletionDate;  // For Step 6
    private Boolean delayAlarm = false;  // Warning for delays
}
```

#### 3. **Step-Specific Data Entities**

Each workflow step has associated data:

**Step 1: Site Survey**
```java
@Entity
public class SiteSurveyData {
    private Long workflowId;

    // Excel file with embedded images/photos
    @Lob
    @Column(name = "excel_file", columnDefinition = "BYTEA")
    private byte[] excelFile;

    private String filename;
    private String mimeType;
    private Long fileSize;

    // Parsed data (JSON format)
    @Column(columnDefinition = "TEXT")
    private String parsedData;

    // Survey metadata
    private String surveyDoneBy;  // SALES or PROJECT
    private String surveyUsername;
    private LocalDateTime surveyDate;

    // Approval
    private Boolean approvedBySales;
    private LocalDateTime approvalDate;
}
```

**Step 2: Selection & Design (Sizing/Pricing)**
```java
@Entity
public class SizingPricingData {
    private Long workflowId;

    // Sizing & pricing Excel sheet
    @Lob
    private byte[] sizingPricingFile;

    private String filename;

    // Uploaded by Presales
    private String uploadedBy;
    private LocalDateTime uploadedAt;

    // Status
    private Boolean sentToSales;
    private LocalDateTime sentToSalesAt;
}
```

**Step 3: Bank Guarantee**
```java
@Entity
public class BankGuaranteeData {
    private Long workflowId;

    // Bank guarantee document
    @Lob
    private byte[] guaranteeFile;

    private String filename;

    // Uploaded by Finance
    private String uploadedBy;
    private LocalDateTime uploadedAt;

    // Guarantee details
    private BigDecimal guaranteeAmount;
    private LocalDate guaranteeExpiryDate;
    private String bankName;
}
```

**Step 4: Missing Item Request**
```java
@Entity
public class MissingItemRequest {
    private Long workflowId;
    private Long projectId;

    // Missing item details
    private String itemDescription;
    private Integer quantityNeeded;
    private String reason;

    // Status
    private String status;  // PENDING, APPROVED, REJECTED, ORDERED

    // Requestor
    private String requestedBy;
    private LocalDateTime requestedAt;
}
```

**Step 6: Project Cost Data**
```java
@Entity
public class ProjectCostData {
    private Long workflowId;
    private Long projectId;

    // Cost tracking
    private BigDecimal totalCost;
    private BigDecimal laborCost;
    private BigDecimal materialCost;
    private BigDecimal overheadCost;

    // Profit analysis
    private BigDecimal revenue;
    private BigDecimal profit;
    private BigDecimal profitMargin;

    // Completion tracking
    private LocalDate expectedCompletion;
    private LocalDate actualCompletion;
}
```

### Workflow Services

#### 1. **ProjectWorkflowService** (Orchestration)

```java
@Service
@Transactional
public class ProjectWorkflowService {

    // Create new workflow
    public ProjectWorkflow createWorkflow(Project project) {
        ProjectWorkflow workflow = new ProjectWorkflow();
        workflow.setProjectId(project.getId());
        workflow.setCurrentStep(1);
        workflow.setStatus("IN_PROGRESS");

        // Create step completion records for all 8 steps
        for (int i = 1; i <= 8; i++) {
            workflowStepService.createStepCompletion(workflow.getId(), i);
        }

        return workflowRepository.save(workflow);
    }

    // Submit step data
    public void submitSiteSurvey(Long workflowId, byte[] excelFile, String filename);
    public void submitSizingPricing(Long workflowId, byte[] file);
    public void submitBankGuarantee(Long workflowId, byte[] file);

    // Advance workflow
    public void completeStep(Long workflowId, int stepNumber, String username) {
        WorkflowStepCompletion step = workflowStepService.getStep(workflowId, stepNumber);
        step.setCompleted(true);
        step.setCompletedAt(LocalDateTime.now());
        step.setCompletedBy(username);

        // Trigger next step notification
        notificationService.notifyStepCompleted(workflowId, stepNumber);
    }
}
```

#### 2. **WorkflowStepService** (Step Management)

```java
@Service
public class WorkflowStepService {

    // Get pending external actions for a module
    public List<WorkflowStepCompletion> getPendingActionsForModule(String moduleName) {
        return repository.findByExternalModuleAndExternalActionCompletedFalse(moduleName);
    }

    // Mark external action complete
    public void completeExternalAction(Long stepId, String username) {
        WorkflowStepCompletion step = repository.findById(stepId)
            .orElseThrow(() -> new RuntimeException("Step not found"));

        step.setExternalActionCompleted(true);
        step.setExternalActionCompletedAt(LocalDateTime.now());

        repository.save(step);

        // Notify original module that action is complete
        notificationService.notifyExternalActionCompleted(step);
    }
}
```

### Workflow UI Integration

**In Sales Module**:
```java
// SalesController has workflow wizard UI
private void showWorkflowDialog(Project project) {
    // Display TabPane with 8 tabs (one per step)
    // Each tab shows:
    // - Step status (✓ completed, ⏳ in progress, ○ pending)
    // - Upload button (for file-based steps)
    // - External action status
    // - "Complete Step" button
}
```

**Step Indicators**:
```
[✓] Step 1: Site Survey (Completed 2024-11-20)
[⏳] Step 2: Selection & Design (In Progress - Waiting for Presales)
[○] Step 3: Bank Guarantee (Pending)
[○] Step 4: Missing Item Request (Pending)
...
```

### Workflow Notifications

The workflow system integrates heavily with the notification system:

```java
// Example: Step 1 completion triggers notification to Presales
workflowService.completeStep(workflowId, 1, currentUser);

// This triggers:
notificationService.publishNotification(
    NotificationMessage.builder()
        .title("Site Survey Completed")
        .message("Site survey for Project X is ready for sizing")
        .type("INFO")
        .module("PRESALES")
        .action("SIZING_REQUEST")
        .entityType("WORKFLOW")
        .entityId(workflowId)
        .build()
);

// Presales module receives notification and displays:
// "Site Survey Completed - Project X needs sizing/pricing"
// [View Survey] [Upload Sizing Sheet]
```

### Approval Workflow Pattern

Some workflow steps require approval (e.g., adding project elements):

```java
// Projects module requests approval to add element
notificationService.publishNotification(
    NotificationMessage.builder()
        .title("Element Approval Request")
        .message("Project ABC wants to add Item XYZ (Qty: 10)")
        .type("WARNING")
        .priority("HIGH")
        .module("SALES")
        .action("APPROVE")
        .entityType("PROJECT_ELEMENT")
        .entityId(elementId)
        .metadata("{\"projectId\": 123, \"elementId\": 456}")
        .build()
);

// Sales module displays persistent notification with buttons:
// [✓ Approve] [✗ Reject]

// On approval:
projectElementService.approveElement(elementId);
notificationService.publishNotification(
    // Approval confirmation back to Projects
);
```

---

## Development Workflows

### Prerequisites

- **Java 21** or higher
- **Maven 3.6+**
- **PostgreSQL** database server
- **Redis** server (for real-time notifications)
- **IntelliJ IDEA** (recommended) or any Java IDE

### Build & Run

```bash
# Build project
mvn clean install

# Run application (Spring Boot)
mvn spring-boot:run

# Run with JavaFX plugin
mvn javafx:run

# Package as JAR
mvn package
java -jar target/magictech-management-1.0-SNAPSHOT.jar
```

### Database Setup

1. **Install PostgreSQL**
2. **Create database**:
   ```sql
   CREATE DATABASE magictech_db;
   ```
3. **Update credentials** in `/src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/magictech_db
   spring.datasource.username=postgres
   spring.datasource.password=your_password
   ```
4. **Run application** - Hibernate will auto-create tables

### Redis Setup

1. **Install Redis**:
   ```bash
   # On Ubuntu/Debian
   sudo apt-get install redis-server

   # On macOS
   brew install redis

   # On Windows
   # Download from https://redis.io/download
   ```

2. **Start Redis server**:
   ```bash
   # On Linux/macOS
   redis-server

   # Or as a service
   sudo systemctl start redis
   ```

3. **Verify Redis is running**:
   ```bash
   redis-cli ping
   # Should return: PONG
   ```

4. **Configuration** (optional):
   - Default: localhost:6379
   - No authentication by default
   - Update `RedisConfig.java` if using different host/port

5. **Monitor notifications** (for debugging):
   ```bash
   redis-cli
   > PUBSUB CHANNELS
   > SUBSCRIBE projects_notifications
   > SUBSCRIBE sales_notifications
   ```

**Note**: The application will start without Redis, but notifications won't work. Check logs for Redis connection errors.

### Testing API Endpoints

Use the provided test script:
```bash
chmod +x test-api.sh
./test-api.sh
```

Or manually:
```bash
# Health check
curl http://localhost:8085/api/auth/health

# Login
curl -X POST http://localhost:8085/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### Adding a New Module

**Step-by-step guide**:

1. **Create package structure**:
   ```
   com.magictech.modules.newmodule/
   ├── entity/
   ├── repository/
   ├── service/
   ├── model/          (if ViewModels needed)
   └── NewModuleController.java
   ```

2. **Create entity**:
   ```java
   @Entity
   @Table(name = "new_module_items")
   public class NewModuleItem {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;

       // Business fields

       // Standard metadata fields (copy from StorageItem)
       private LocalDateTime dateAdded;
       private LocalDateTime lastUpdated;
       private String createdBy;
       private Boolean active = true;

       @PrePersist
       protected void onCreate() { ... }

       @PreUpdate
       protected void onUpdate() { ... }
   }
   ```

3. **Create repository**:
   ```java
   @Repository
   public interface NewModuleRepository extends JpaRepository<NewModuleItem, Long> {
       List<NewModuleItem> findByActiveTrue();
   }
   ```

4. **Create service**:
   ```java
   @Service
   @Transactional
   public class NewModuleService {
       @Autowired
       private NewModuleRepository repository;

       public List<NewModuleItem> findAllActive() {
           return repository.findByActiveTrue();
       }
   }
   ```

5. **Create controller** (extend base):
   ```java
   public class NewModuleController extends BaseModuleController {
       @Autowired
       private NewModuleService service;

       @Override
       protected void setupUI() { /* Build UI */ }

       @Override
       protected void loadData() { /* Load data */ }

       @Override
       protected void refresh() { /* Reload data */ }
   }
   ```

6. **Add factory method** to `ModuleConfig`:
   ```java
   public static ModuleConfig createNewModuleConfig() {
       return new Builder()
           .name("newmodule")
           .displayName("New Module")
           .description("Description")
           .icon("🆕")
           .colorScheme("module-blue")
           .allowedRoles(UserRole.MASTER)
           .build();
   }
   ```

7. **Add to dashboard** in `MainDashboardController`:
   ```java
   createModuleCard("New Module", "🆕", "Description",
       "module-blue", () -> sceneManager.showModule("newmodule"));
   ```

8. **Add navigation** in `SceneManager.showModule()`:
   ```java
   case "newmodule" -> {
       NewModuleController controller = new NewModuleController();
       context.getAutowireCapableBeanFactory().autowireBean(controller);
       controller.initialize(currentUser, ModuleConfig.createNewModuleConfig());
       moduleRoot = (VBox) controller.getView();
   }
   ```

9. **Update DatabaseConfig** to scan new repository:
   ```java
   @EnableJpaRepositories(basePackages = {
       // ... existing packages
       "com.magictech.modules.newmodule"
   })
   ```

### Adding Storage-Based Module

**If your module should reuse `StorageItem` entity** (like Sales/Projects do):

1. **Extend** `BaseStorageModuleController` instead of `BaseModuleController`
2. **Add configuration** to `ModuleStorageConfig` enum
3. **Use** `StorageService` and `StorageItemRepository`
4. **No need** to create separate entity/repository

Example:
```java
public class NewStorageController extends BaseStorageModuleController {
    public NewStorageController() {
        super(ModuleStorageConfig.NEW_MODULE);  // Define in enum
    }
}
```

---

## Coding Conventions

### Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Entities | Singular, PascalCase | `User`, `Project`, `StorageItem` |
| Repositories | `EntityNameRepository` | `UserRepository`, `ProjectRepository` |
| Services | `EntityNameService` | `UserService`, `ProjectService` |
| Controllers (JavaFX) | `PageNameController` | `LoginController`, `MainDashboardController` |
| Controllers (Module) | `ModuleNameController` | `StorageController`, `SalesStorageController` |
| ViewModels | `EntityNameViewModel` | `StorageItemViewModel` |
| Table names | Plural, snake_case | `users`, `storage_items`, `project_tasks` |
| Column names | snake_case | `date_added`, `product_name`, `last_updated` |
| Packages | lowercase | `com.magictech.modules.sales` |

### Code Style

**Entity Metadata Fields** (include in ALL entities):
```java
@Column(name = "date_added", nullable = false)
private LocalDateTime dateAdded;

@Column(name = "last_updated")
private LocalDateTime lastUpdated;

@Column(name = "created_by", length = 100)
private String createdBy;

@Column(nullable = false)
private Boolean active = true;
```

**JPA Lifecycle Hooks** (include in ALL entities):
```java
@PrePersist
protected void onCreate() {
    this.dateAdded = LocalDateTime.now();
    if (this.active == null) this.active = true;
}

@PreUpdate
protected void onUpdate() {
    this.lastUpdated = LocalDateTime.now();
}
```

**Calculated Fields Pattern**:
```java
@PrePersist
@PreUpdate
protected void onUpdate() {
    calculateTotals();  // Auto-calculate before save
}

private void calculateTotals() {
    this.totalAmount = subtotal.add(tax).subtract(discount);
}
```

**Service Transaction Annotation**:
```java
@Service
@Transactional  // Class-level for all methods
public class MyService {
    // All methods run in transactions by default
}
```

**Repository Query Patterns**:
```java
// Derived queries
List<Entity> findByActiveTrue();
List<Entity> findByFieldAndActiveTrue(String field);

// Custom queries
@Query("SELECT e FROM Entity e WHERE e.active = true AND ...")
List<Entity> customQuery(@Param("param") String param);
```

**Controller Alert Helpers** (inherited from `BaseModuleController`):
```java
showSuccess("Operation completed successfully");
showError("An error occurred: " + ex.getMessage());
showWarning("Are you sure?");
```

**JavaFX Cleanup Pattern**:
```java
@Override
public void cleanup() {
    if (backgroundPane != null) {
        backgroundPane.stopAnimation();
        backgroundPane = null;
    }
    // Clear references to prevent memory leaks
    tableView = null;
    selectionMap.clear();
}
```

### File Organization

**Controller structure**:
```java
public class MyController extends BaseModuleController {
    // 1. Dependencies (autowired)
    @Autowired
    private MyService service;

    // 2. UI components
    private TableView<MyViewModel> tableView;
    private TextField searchField;

    // 3. Data
    private ObservableList<MyViewModel> items;

    // 4. Lifecycle methods (in order)
    @Override
    protected void setupUI() { ... }

    @Override
    protected void loadData() { ... }

    @Override
    protected void refresh() { ... }

    @Override
    public void cleanup() { ... }

    // 5. Event handlers
    private void handleAdd() { ... }
    private void handleEdit() { ... }
    private void handleDelete() { ... }

    // 6. Helper methods
    private MyViewModel createViewModel(MyEntity entity) { ... }
}
```

### Error Handling

**Services**:
```java
public void deleteItem(Long id) {
    StorageItem item = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("Item not found: " + id));
    repository.delete(item);
}
```

**Controllers**:
```java
try {
    service.deleteItem(id);
    showSuccess("Item deleted successfully");
    refresh();
} catch (Exception ex) {
    showError("Failed to delete item: " + ex.getMessage());
}
```

**REST APIs**:
```java
try {
    User user = userService.authenticate(username, password);
    return ResponseEntity.ok(Map.of("success", true, "user", user));
} catch (Exception ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", ex.getMessage()));
}
```

---

## Security Considerations

### Current State: BASIC (NOT PRODUCTION-READY)

**Authentication**:
- Plain text password comparison
- Spring Security autoconfiguration **DISABLED**

**Authorization**:
- Role-based via `UserRole` enum
- Checked at UI level (module access)
- **MASTER** role has full access

### CRITICAL TODO for Production

1. **Enable Password Hashing**:
   ```java
   @Autowired
   private BCryptPasswordEncoder passwordEncoder;

   user.setPassword(passwordEncoder.encode(plainPassword));

   // Verification
   passwordEncoder.matches(plainPassword, user.getPassword());
   ```

2. **Enable Spring Security**:
   - Remove exclusion from `application.properties`
   - Configure `SecurityConfig`
   - Add method-level security (`@PreAuthorize`)

3. **Add JWT for REST API**:
   - Token generation on login
   - Token validation filter
   - Refresh token mechanism

4. **Add CSRF Protection**:
   - Enable Spring Security CSRF
   - Add tokens to forms

5. **Implement Session Management**:
   - Session timeout
   - Concurrent session control
   - Logout functionality

6. **Add Audit Logging**:
   - Log all authentication attempts
   - Log data modifications
   - Track user actions

7. **Input Validation**:
   - Use `@Valid` and Bean Validation
   - Sanitize user inputs
   - Prevent SQL injection (already handled by JPA)

8. **Restrict CORS**:
   ```java
   @CrossOrigin(origins = "https://trusted-domain.com")
   ```

---

## Testing & API

### REST API Endpoints

**Base URL**: `http://localhost:8085/api`

#### Authentication API (`/api/auth`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/health` | Health check | No |
| POST | `/login` | Authenticate user | No |
| GET | `/users` | Get all active users | No* |
| GET | `/users/{id}` | Get user by ID | No* |
| GET | `/users/role/{role}` | Get users by role | No* |
| POST | `/users` | Create new user | MASTER* |
| PUT | `/users/{id}/password` | Update password | No* |
| DELETE | `/users/{id}` | Deactivate user | MASTER* |
| PUT | `/users/{id}/activate` | Activate user | MASTER* |
| GET | `/stats` | Get statistics | No* |

*No authentication currently enforced - INSECURE

**Example Requests**:

```bash
# Login
curl -X POST http://localhost:8085/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Get all users
curl http://localhost:8085/api/auth/users

# Create user (MASTER only)
curl -X POST http://localhost:8085/api/auth/users \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","password":"pass123","role":"SALES"}'
```

### Test Script

Run comprehensive API tests:
```bash
./test-api.sh
```

Tests include:
- Health check
- Login (success/failure)
- User CRUD operations
- Role-based queries
- Statistics

### Default Users

**Default admin account**:
- Username: `admin`
- Password: `admin123`
- Role: `MASTER`

**Note**: Created automatically on first run if no users exist.

---

## Common Tasks

### Task 1: Add a New Field to Existing Entity

```java
// 1. Add field to entity
@Column(name = "new_field")
private String newField;

// 2. Add getter/setter
public String getNewField() { return newField; }
public void setNewField(String newField) { this.newField = newField; }

// 3. Update ViewModel (if exists)
private final StringProperty newField = new SimpleStringProperty();

// 4. Update TableView column (in controller)
TableColumn<MyViewModel, String> newColumn = new TableColumn<>("New Field");
newColumn.setCellValueFactory(data -> data.getValue().newFieldProperty());

// 5. Restart app - Hibernate auto-updates schema
```

### Task 2: Add Custom Query to Repository

```java
@Repository
public interface MyRepository extends JpaRepository<MyEntity, Long> {

    // Derived query (auto-implemented)
    List<MyEntity> findByNameAndActiveTrue(String name);

    // Custom JPQL query
    @Query("SELECT e FROM MyEntity e WHERE e.active = true AND e.field = :value")
    List<MyEntity> findByCustomCriteria(@Param("value") String value);

    // Native SQL query
    @Query(value = "SELECT * FROM my_table WHERE custom_condition", nativeQuery = true)
    List<MyEntity> findByNativeQuery();
}
```

### Task 3: Add Excel Export to Module

```java
// In controller
@Autowired
private ExcelExportService excelExportService;

private void handleExport() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
    );
    File file = fileChooser.showSaveDialog(getView().getScene().getWindow());

    if (file != null) {
        try {
            List<StorageItem> items = service.findAllActive();
            excelExportService.exportToExcel(items, file.getAbsolutePath());
            showSuccess("Exported successfully");
        } catch (Exception ex) {
            showError("Export failed: " + ex.getMessage());
        }
    }
}
```

### Task 4: Add REST Endpoint

```java
@RestController
@RequestMapping("/api/mymodule")
@CrossOrigin(origins = "*")  // Restrict in production
public class MyModuleController {

    @Autowired
    private MyService service;

    @GetMapping
    public ResponseEntity<List<MyEntity>> getAll() {
        return ResponseEntity.ok(service.findAllActive());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody MyEntity entity) {
        try {
            MyEntity saved = service.save(entity);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
        }
    }
}
```

### Task 5: Add Validation

```java
// In entity
@Column(nullable = false, length = 100)
@NotBlank(message = "Name is required")
@Size(min = 3, max = 100, message = "Name must be 3-100 characters")
private String name;

@Email(message = "Invalid email format")
private String email;

// In controller (REST)
@PostMapping
public ResponseEntity<?> create(@Valid @RequestBody MyEntity entity) {
    // @Valid triggers validation, throws MethodArgumentNotValidException
    return ResponseEntity.ok(service.save(entity));
}
```

### Task 6: Add Relationship Between Entities

```java
// One-to-Many example

// Parent entity
@Entity
public class Parent {
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Child> children = new ArrayList<>();

    // Helper methods
    public void addChild(Child child) {
        children.add(child);
        child.setParent(this);
    }
}

// Child entity
@Entity
public class Child {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Parent parent;
}
```

---

## Key Files Reference

| Purpose | File Path |
|---------|-----------|
| Application entry | `/src/main/java/com/magictech/MainApp.java` |
| Spring configuration | `/src/main/resources/application.properties` |
| Database config | `/src/main/java/com/magictech/core/config/DatabaseConfig.java` |
| Scene management | `/src/main/java/com/magictech/core/ui/SceneManager.java` |
| Spring-FXML bridge | `/src/main/java/com/magictech/core/ui/SpringFXMLLoader.java` |
| Auth service | `/src/main/java/com/magictech/core/auth/AuthenticationService.java` |
| Auth API | `/src/main/java/com/magictech/core/api/AuthController.java` |
| Module base | `/src/main/java/com/magictech/core/module/BaseModuleController.java` |
| Storage base | `/src/main/java/com/magictech/modules/storage/base/BaseStorageModuleController.java` |
| Storage entity | `/src/main/java/com/magictech/modules/storage/entity/StorageItem.java` |
| Login view | `/src/main/resources/fxml/login.fxml` |
| Dashboard view | `/src/main/resources/fxml/main-dashboard.fxml` |
| Global CSS | `/src/main/resources/css/styles.css` |
| Maven config | `/pom.xml` |
| Git ignore | `/.gitignore` |
| API test script | `/test-api.sh` |

---

## Best Practices for AI Assistants

### When Adding Features

1. **Always follow existing patterns** - Don't introduce new patterns unless necessary
2. **Use base classes** - Extend `BaseModuleController` or `BaseStorageModuleController`
3. **Include metadata fields** - All entities must have `dateAdded`, `lastUpdated`, `createdBy`, `active`
4. **Use soft delete** - Add `active` flag, query with `findByActiveTrue()`
5. **Add lifecycle hooks** - Include `@PrePersist` and `@PreUpdate`
6. **Clean up resources** - Override `cleanup()` and stop animations
7. **Handle errors gracefully** - Use try-catch and show user-friendly messages
8. **Update DatabaseConfig** - Add new repository packages to scanning
9. **Follow naming conventions** - See [Naming Conventions](#naming-conventions)
10. **Test both UI and API** - Ensure desktop and REST endpoints work

### When Refactoring

1. **Don't break existing patterns** - Module system depends on consistent structure
2. **Update all affected files** - Config, services, controllers, views
3. **Test thoroughly** - UI navigation, data loading, CRUD operations
4. **Check SceneManager** - Ensure module navigation still works
5. **Verify Spring wiring** - Check `@Autowired` dependencies resolve

### When Debugging

1. **Check application logs** - SQL queries logged in DEBUG mode
2. **Verify database state** - Check if data actually saved
3. **Check Spring context** - Ensure beans are registered
4. **Test REST API separately** - Use curl or Postman
5. **Verify FXML paths** - Resource loading issues are common
6. **Check JavaFX thread** - UI updates must be on FX Application Thread

### Security Reminders

1. **Never commit passwords** - Use environment variables in production
2. **Sanitize inputs** - Validate and escape user inputs
3. **Use parameterized queries** - JPA handles this, but be careful with native queries
4. **Implement authentication** - Current state is INSECURE
5. **Restrict CORS** - Don't use `origins = "*"` in production

### Working with Notifications

1. **Always use NotificationService** - Don't directly publish to Redis
2. **Choose correct notification type** - INFO, WARNING, SUCCESS, ERROR affect styling
3. **Set appropriate priority** - HIGH/URGENT for approval requests
4. **Target specific modules** - Use `module` field to route correctly
5. **Include metadata** - Add JSON metadata for entity IDs and context
6. **Handle missed notifications** - System automatically loads on login
7. **Test notification flow** - Ensure receiving module displays correctly
8. **Use built-in templates** - `notifyProjectCreated()`, `notifyElementApprovalRequest()`, etc.

### Working with Workflows

1. **Always create workflows for projects** - Call `projectWorkflowService.createWorkflow()`
2. **Complete steps sequentially** - Steps must be completed in order (1→8)
3. **Upload files for file-based steps** - Steps 1, 2, 3 require Excel/document uploads
4. **Trigger notifications on step completion** - Notify next module when step completes
5. **Track external actions** - Mark when external module completes their action
6. **Handle approval gates** - Step 5 (Tender Acceptance) requires explicit approval
7. **Update workflow status** - Mark as COMPLETED, REJECTED, or ON_HOLD as appropriate
8. **Link step data to workflow** - Use `workflowId` foreign key in step-specific entities

### Redis Requirements

1. **Ensure Redis is running** - localhost:6379 by default
2. **Check Redis connection** - Application logs show connection status
3. **Monitor Redis channels** - Use `redis-cli PUBSUB CHANNELS` to debug
4. **Handle Redis downtime gracefully** - Auto-reconnect configured with 5s interval
5. **Don't rely solely on Redis** - Notifications also persisted to PostgreSQL

---

## Conclusion

This codebase demonstrates a well-structured enterprise application with:
- Clear separation of concerns (core vs modules)
- Consistent patterns (entities, services, repositories, controllers)
- Hybrid architecture (desktop UI + REST API)
- Modular design (easy to add new modules)
- Room for scalability

**Key Strengths**:
- Reusable base controllers reduce code duplication
- Shared `StorageItem` entity with column-level permissions
- Spring Boot + JavaFX integration is seamless
- Soft delete pattern preserves data integrity
- Real-time inter-module communication via Redis Pub/Sub
- Comprehensive 8-step workflow system with approval gates
- Modular notification system with persistence and missed notification handling
- Excel-based site survey with image embedding support
- Role-based module access with column-level security

**Areas for Improvement**:
- Security implementation (authentication/authorization - passwords in plain text)
- Unit and integration tests
- API documentation (Swagger/OpenAPI)
- Error handling standardization
- Logging framework (SLF4J/Logback)
- Redis authentication and encryption for production
- Workflow step validation and rollback mechanisms

---

**Document Maintained By**: AI Assistant (Claude)
**Last Updated**: 2025-11-26
**Version**: 2.0

**Major Changes in v2.0**:
- Added Presales, Finance, and Quality Assurance modules
- Documented comprehensive Notification & Messaging System (Redis Pub/Sub)
- Documented 8-Step Workflow System for project lifecycle management
- Expanded Sales module with customer management and workflow entities
- Added site survey functionality with Excel parsing and image support
- Updated entity relationships to reflect new workflow and notification architecture
- Added approval workflow patterns and inter-module communication

For questions or clarifications, refer to the source code and existing patterns.
