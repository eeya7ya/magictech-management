# MagicTech Management System - User Roles Guide

**Version:** 1.0
**Last Updated:** 2025-11-19

---

## Table of Contents

1. [Overview](#overview)
2. [Role Hierarchy](#role-hierarchy)
3. [Role Definitions](#role-definitions)
4. [Module Access Matrix](#module-access-matrix)
5. [Default User Accounts](#default-user-accounts)
6. [Creating New Users](#creating-new-users)
7. [Role-Specific Workflows](#role-specific-workflows)
8. [Security & Best Practices](#security--best-practices)

---

## Overview

The MagicTech Management System implements a **role-based access control (RBAC)** system to ensure users only have access to the modules and features necessary for their job functions. This document describes each role, their permissions, and how to manage user accounts.

### Available Roles

| Role | Code | Purpose |
|------|------|---------|
| Master Admin | `MASTER` | Full system access and administrative privileges |
| Sales Department | `SALES` | Sales orders, customers, and quotations |
| Maintenance Team | `MAINTENANCE` | Equipment maintenance tracking |
| Projects Team | `PROJECTS` | Project management and execution |
| Pricing Department | `PRICING` | Pricing management and cost analysis |
| Storage/Inventory | `STORAGE` | Inventory management and stock control |
| Client (Read-Only) | `CLIENT` | Limited read-only access |

---

## Role Hierarchy

```
MASTER (Full Access)
│
├─ STORAGE (Inventory Management)
├─ SALES (Sales & Customers)
├─ PROJECTS (Project Management)
├─ MAINTENANCE (Equipment Maintenance)
├─ PRICING (Pricing & Costing)
└─ CLIENT (Read-Only Access)
```

**Key Principle:** Only `MASTER` role has administrative privileges. All other roles have module-specific access with controlled permissions.

---

## Role Definitions

### 1. MASTER (Master Administrator)

**Full Name:** Master Administrator
**Code:** `MASTER`
**Color Theme:** Red

**Capabilities:**
- ✅ **Full system access** to all modules
- ✅ **User management** - create, edit, deactivate users
- ✅ **Module configuration** - change settings across all modules
- ✅ **Data oversight** - view and modify all data
- ✅ **System administration** - manage notifications, approvals, system settings
- ✅ **Export capabilities** - export data from any module
- ✅ **Role assignment** - assign roles to other users

**Module Access:**
- Storage Management ✅
- Sales Module ✅
- Projects Module ✅
- Maintenance Module ✅
- Pricing Module ✅
- User Management ✅
- System Settings ✅

**Use Cases:**
- System administrators
- IT managers
- Business owners
- Department heads requiring oversight

**Restrictions:**
- ⚠️ Should be assigned sparingly for security reasons
- ⚠️ Recommended maximum: 2-3 MASTER users per organization

---

### 2. STORAGE (Storage/Inventory Manager)

**Full Name:** Storage and Inventory Management
**Code:** `STORAGE`
**Color Theme:** Blue

**Capabilities:**
- ✅ **Full inventory visibility** - see actual quantity numbers
- ✅ **Stock management** - add, edit, delete inventory items
- ✅ **Quantity updates** - modify stock quantities
- ✅ **Excel import/export** - bulk inventory operations
- ✅ **Analysis dashboard** - view project and customer details (READ-ONLY)
- ✅ **Low stock alerts** - receive inventory notifications
- ✅ **Storage location management**

**Module Access:**
- Storage Management ✅ (FULL ACCESS)
- Analysis Dashboard ✅ (READ-ONLY: View project/customer details)

**Cannot Access:**
- Sales Module ❌
- Projects Module ❌
- Maintenance Module ❌
- Pricing Module ❌
- User Management ❌

**Key Differences from Other Roles:**
- **ONLY** role that sees **actual quantity numbers**
- Other roles (Sales, Projects, Maintenance) only see ✅ Available / ❌ Unavailable status
- Can update quantities that affect availability for other modules

**Use Cases:**
- Warehouse managers
- Inventory controllers
- Stock keepers
- Logistics coordinators

**Typical Workflow:**
1. Receive goods → Update quantities
2. Monitor stock levels → Set reorder points
3. Process allocation requests from Sales/Projects
4. Generate inventory reports
5. Review project/customer inventory usage (read-only analysis)

---

### 3. SALES (Sales Department)

**Full Name:** Sales Department
**Code:** `SALES`
**Color Theme:** Green

**Capabilities:**
- ✅ **Customer management** - add, edit customers
- ✅ **Sales orders** - create and manage orders
- ✅ **Quotation generation** - export detailed quotations with pricing
- ✅ **Cost breakdowns** - manage project and customer costs
- ✅ **Contract management** - handle sales contracts
- ✅ **PDF storage** - upload/download contracts and documents
- ✅ **Availability checking** - see ✅/❌ availability status (NOT quantities)
- ✅ **Pricing visibility** - view and edit product prices

**Module Access:**
- Sales Module ✅ (FULL ACCESS)
  - Customers Submodule ✅
  - Projects Submodule ✅
  - Orders Management ✅
  - Contracts ✅

**Cannot Access:**
- Storage Module ❌
- Projects Module (execution) ❌
- Maintenance Module ❌
- Pricing Module ❌
- User Management ❌

**Important Restrictions:**
- ⚠️ **Cannot see actual inventory quantities** - only availability status (✅ Available / ❌ Unavailable)
- ⚠️ Cannot modify inventory quantities
- ⚠️ Cannot execute projects (only create sales orders for projects)

**Use Cases:**
- Sales representatives
- Account managers
- Sales administrators
- Customer service representatives

**Typical Workflow:**
1. Create customer record
2. Check item availability (sees ✅/❌ status)
3. Create sales order with pricing
4. Generate quotation (Excel export with all details)
5. Upload signed contracts (PDF storage)
6. Track order status

---

### 4. PROJECTS (Projects Team)

**Full Name:** Projects Execution Team
**Code:** `PROJECTS`
**Color Theme:** Purple

**Capabilities:**
- ✅ **Project management** - create, edit, track projects
- ✅ **Task management** - create task checklists
- ✅ **Schedule management** - plan project timelines
- ✅ **Project notes** - documentation and comments
- ✅ **Element allocation** - add storage items to projects
- ✅ **Availability checking** - see ✅/❌ availability (NOT quantities)
- ✅ **PDF storage** - project documents, plans, reports
- ✅ **Excel export** - project details for reporting

**Module Access:**
- Projects Module ✅ (FULL ACCESS)
  - Project List ✅
  - Project Details ✅
  - Tasks & Checklists ✅
  - Schedules ✅
  - Project Elements ✅
  - Notes ✅

**Cannot Access:**
- Storage Module ❌
- Sales Module ❌
- Maintenance Module ❌
- Pricing Module ❌
- User Management ❌

**Important Restrictions:**
- ⚠️ **Cannot see actual inventory quantities** - only ✅/❌ availability
- ⚠️ Cannot modify inventory
- ⚠️ Cannot create sales orders

**Use Cases:**
- Project managers
- Site supervisors
- Installation teams
- Project coordinators

**Typical Workflow:**
1. Receive project from Sales
2. Create project in system
3. Add required elements (checks availability ✅/❌)
4. Create task checklist
5. Set project schedule
6. Track progress and completion
7. Upload project documentation (PDFs)
8. Export project report (Excel)

---

### 5. MAINTENANCE (Maintenance Team)

**Full Name:** Maintenance Department
**Code:** `MAINTENANCE`
**Color Theme:** Orange

**Capabilities:**
- ✅ **Maintenance tracking** - log maintenance activities
- ✅ **Equipment management** - track equipment status
- ✅ **Availability checking** - see ✅/❌ status (NOT quantities)
- ✅ **Service records** - maintenance history
- ✅ **PDF storage** - maintenance reports and documentation

**Module Access:**
- Maintenance Module ✅ (FULL ACCESS)

**Cannot Access:**
- Storage Module ❌
- Sales Module ❌
- Projects Module ❌
- Pricing Module ❌
- User Management ❌

**Important Restrictions:**
- ⚠️ **Cannot see actual inventory quantities** - only ✅/❌ availability
- ⚠️ Cannot modify inventory
- ⚠️ Limited to maintenance-specific operations

**Use Cases:**
- Maintenance technicians
- Service engineers
- Equipment managers
- Facility maintenance staff

**Typical Workflow:**
1. Receive maintenance request
2. Check equipment/parts availability (✅/❌)
3. Log maintenance activity
4. Update equipment status
5. Upload maintenance reports (PDFs)
6. Track service history

---

### 6. PRICING (Pricing Department)

**Full Name:** Pricing Management
**Code:** `PRICING`
**Color Theme:** Teal

**Capabilities:**
- ✅ **Price management** - set and update product prices
- ✅ **Cost analysis** - pricing strategies and margin calculations
- ✅ **Availability checking** - see ✅/❌ status (NOT quantities)
- ✅ **Price exports** - generate pricing sheets

**Module Access:**
- Pricing Module ✅ (FULL ACCESS)

**Cannot Access:**
- Storage Module ❌
- Sales Module ❌
- Projects Module ❌
- Maintenance Module ❌
- User Management ❌

**Important Restrictions:**
- ⚠️ **Cannot see actual inventory quantities** - only ✅/❌ availability
- ⚠️ Cannot modify inventory
- ⚠️ Cannot create sales orders or projects

**Use Cases:**
- Pricing analysts
- Cost accountants
- Financial analysts
- Pricing managers

**Typical Workflow:**
1. Review market prices
2. Update product pricing
3. Analyze cost margins
4. Generate pricing reports
5. Coordinate with Sales on pricing strategies

---

### 7. CLIENT (Read-Only Client Access)

**Full Name:** Client/Customer Access
**Code:** `CLIENT`
**Color Theme:** Gray

**Capabilities:**
- ✅ **Read-only access** - view assigned data
- ✅ **Project status viewing** - track project progress
- ✅ **Document viewing** - access shared documents
- ⚠️ **Very limited** - mostly for external stakeholders

**Module Access:**
- Limited read-only access as configured by MASTER

**Cannot Access:**
- Storage Module ❌
- Sales Module ❌
- Projects Module (full) ❌
- Maintenance Module ❌
- Pricing Module ❌
- User Management ❌

**Use Cases:**
- External clients
- Stakeholders
- Auditors (read-only)
- External consultants

---

## Module Access Matrix

| Module | MASTER | STORAGE | SALES | PROJECTS | MAINTENANCE | PRICING | CLIENT |
|--------|--------|---------|-------|----------|-------------|---------|--------|
| **Storage Management** | ✅ Full | ✅ Full | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Sales Module** | ✅ Full | ❌ | ✅ Full | ❌ | ❌ | ❌ | ❌ |
| **Projects Module** | ✅ Full | ❌ | ❌ | ✅ Full | ❌ | ❌ | Limited |
| **Maintenance Module** | ✅ Full | ❌ | ❌ | ❌ | ✅ Full | ❌ | ❌ |
| **Pricing Module** | ✅ Full | ❌ | ❌ | ❌ | ❌ | ✅ Full | ❌ |
| **User Management** | ✅ Only | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Analysis Dashboard** | ✅ Full | ✅ Read | ❌ | ❌ | ❌ | ❌ | ❌ |

### Quantity Visibility Matrix

| Module | Can See Actual Quantities? | What They See |
|--------|---------------------------|---------------|
| **MASTER** | ✅ YES | Actual numbers (e.g., "25 units") |
| **STORAGE** | ✅ YES | Actual numbers (e.g., "25 units") |
| **SALES** | ❌ NO | ✅ Available / ❌ Unavailable |
| **PROJECTS** | ❌ NO | ✅ Available / ❌ Unavailable |
| **MAINTENANCE** | ❌ NO | ✅ Available / ❌ Unavailable |
| **PRICING** | ❌ NO | ✅ Available / ❌ Unavailable |
| **CLIENT** | ❌ NO | ✅ Available / ❌ Unavailable |

---

## Default User Accounts

The system comes pre-configured with the following default accounts for testing:

| Username | Password | Role | Purpose |
|----------|----------|------|---------|
| `admin` | `admin123` | MASTER | Primary administrator |
| `manager` | `manager123` | MASTER | Secondary administrator |
| `john` | `sales123` | SALES | Sales representative |
| `mike` | `main123` | MAINTENANCE | Maintenance technician |
| `sara` | `proj123` | PROJECTS | Project manager |
| `emma` | `price123` | PRICING | Pricing analyst |
| `david` | `store123` | STORAGE | Warehouse manager |

⚠️ **SECURITY WARNING:** Change these default passwords immediately in a production environment!

---

## Creating New Users

### Via UI (Recommended)

1. **Login as MASTER** user
2. Click **User Management** button (👥 icon in dashboard header)
3. Click **➕ Add New User**
4. Fill in the form:
   - **Username:** Unique username (3-50 characters)
   - **Password:** Secure password (minimum 6 characters)
   - **Role:** Select appropriate role from dropdown
5. Click **OK** to create
6. User can immediately login with provided credentials

### Via REST API

```bash
curl -X POST http://localhost:8085/api/auth/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "securepassword123",
    "role": "SALES"
  }'
```

### User Creation Guidelines

✅ **DO:**
- Use descriptive usernames (e.g., john.smith, warehouse.manager)
- Assign the **least privileged role** necessary
- Use strong passwords (8+ characters, mixed case, numbers, symbols)
- Document user assignments and responsibilities
- Deactivate users when they leave the organization (soft delete)

❌ **DON'T:**
- Create unnecessary MASTER accounts
- Use generic usernames (user1, test, admin2)
- Share accounts between multiple people
- Use weak passwords
- Hard delete users (use deactivate instead)

---

## Role-Specific Workflows

### STORAGE Workflow: Receiving New Inventory

```
1. Login as STORAGE user
2. Navigate to Storage Management module
3. Click "➕ Add Item" or import Excel
4. Enter item details:
   - Manufacture
   - Product Name
   - Code & Serial Number
   - Quantity (actual numbers)
   - Price
5. Save item
6. Quantity is now tracked and affects availability for other roles
```

### SALES Workflow: Creating Customer Order

```
1. Login as SALES user
2. Navigate to Sales Module → Customers
3. Click "➕ Add Customer"
4. Fill customer details and save
5. Create new Sales Order for customer
6. Add items to order (system shows ✅/❌ availability)
7. Add pricing, discounts, crew costs
8. Generate quotation (Excel export with all details)
9. Upload signed contract (PDF)
10. Mark order as CONFIRMED
```

### PROJECTS Workflow: Managing Project

```
1. Login as PROJECTS user
2. Navigate to Projects Module
3. Click "➕ New Project"
4. Fill project details (name, location, dates)
5. Add project elements (system checks ✅/❌ availability)
6. Create task checklist
7. Set project schedule with milestones
8. Add project notes as needed
9. Upload project documents (PDFs)
10. Track completion and export report
```

---

## Security & Best Practices

### Password Security

⚠️ **CURRENT STATUS:** Passwords are stored in **PLAIN TEXT** (NOT PRODUCTION READY)

**For Production Deployment:**
1. Enable BCrypt password hashing
2. Implement password complexity requirements
3. Add password expiration policies
4. Implement account lockout after failed attempts

### Access Control Best Practices

1. **Principle of Least Privilege**
   - Assign the minimum role necessary
   - Regularly review user permissions

2. **Account Management**
   - Deactivate users immediately when they leave
   - Regular audit of active accounts
   - Review MASTER users quarterly

3. **Audit Trail**
   - All user actions are logged with timestamps
   - Review `created_by` and `last_updated` fields
   - Monitor user login times

4. **Session Management**
   - Users should logout when finished
   - Sessions expire after inactivity
   - No concurrent sessions from same account

### Data Segregation

| Data Type | Who Can See | Who Can Modify |
|-----------|-------------|----------------|
| Inventory Quantities | MASTER, STORAGE | MASTER, STORAGE |
| Prices | MASTER, STORAGE, SALES, PRICING | MASTER, STORAGE, SALES, PRICING |
| Customer Data | MASTER, SALES | MASTER, SALES |
| Project Data | MASTER, PROJECTS | MASTER, PROJECTS |
| User Accounts | MASTER only | MASTER only |

---

## Troubleshooting

### User Cannot Login

**Symptoms:** User created but cannot login
**Possible Causes:**
1. User account is inactive
2. Password mismatch
3. Username case sensitivity issue

**Solutions:**
1. Check user status in User Management (should show ✅ Active)
2. Reset password using "✏️ Edit" button
3. Verify username is correct (case-insensitive)
4. Check application logs for authentication errors

### User Cannot See Expected Module

**Symptoms:** User logs in but module is missing
**Possible Causes:**
1. Wrong role assigned
2. Module not enabled for that role

**Solutions:**
1. Verify role assignment in User Management
2. Check Module Access Matrix (above)
3. Re-login after role change
4. Contact MASTER user to verify permissions

### Quantity vs Availability Confusion

**Symptoms:** Sales/Projects users complaining they can't see quantities
**Explanation:** This is **BY DESIGN**

- **STORAGE** users: See actual quantities (e.g., "25 units")
- **All other users**: See availability status (✅ Available / ❌ Unavailable)

**Rationale:**
- Prevents information leakage
- Simplifies decision making for non-inventory roles
- Reduces training requirements

---

## Role Assignment Decision Tree

```
START: What is the user's primary job function?

├─ System Administration / IT / Owner?
│  └─ Assign: MASTER
│
├─ Warehouse / Inventory / Logistics?
│  └─ Assign: STORAGE
│
├─ Sales / Customer Relations / Account Management?
│  └─ Assign: SALES
│
├─ Project Execution / Installation / Site Work?
│  └─ Assign: PROJECTS
│
├─ Equipment Maintenance / Service?
│  └─ Assign: MAINTENANCE
│
├─ Pricing Strategy / Cost Analysis?
│  └─ Assign: PRICING
│
└─ External Stakeholder / Read-Only?
   └─ Assign: CLIENT
```

---

## Support & Questions

For questions about user roles and permissions:

1. **Technical Issues:** Check application logs
2. **Access Requests:** Contact your MASTER administrator
3. **Role Changes:** Submit request to MASTER user
4. **Password Resets:** MASTER users can reset passwords in User Management

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-11-19 | Initial user roles documentation |

---

**Document Maintained By:** MagicTech Development Team
**Classification:** Internal Use Only
**Review Frequency:** Quarterly

