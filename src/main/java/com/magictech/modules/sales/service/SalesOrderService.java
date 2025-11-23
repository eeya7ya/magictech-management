package com.magictech.modules.sales.service;

import com.magictech.core.messaging.service.NotificationService;
import com.magictech.modules.sales.entity.SalesOrder;
import com.magictech.modules.sales.entity.SalesOrderItem;
import com.magictech.modules.sales.repository.SalesOrderRepository;
import com.magictech.modules.sales.repository.SalesOrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Sales Order Service
 * Business logic for sales order management with automatic database updates
 *
 * NOTE: This service requires StorageService and ProjectElementService
 * Uncomment the @Autowired sections below when those services are available
 */
@Service
@Transactional
public class SalesOrderService {

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderItemRepository salesOrderItemRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.magictech.modules.storage.service.StorageService storageService;

    @Autowired
    private com.magictech.modules.projects.service.ProjectElementService projectElementService;

    @Autowired
    private com.magictech.modules.projects.service.ProjectService projectService;

    /**
     * Create a new sales order
     */
    public SalesOrder createSalesOrder(SalesOrder salesOrder) {
        salesOrder.setCreatedAt(LocalDateTime.now());
        salesOrder.setUpdatedAt(LocalDateTime.now());
        salesOrder.setActive(true);
        salesOrder.setStatus("DRAFT");
        salesOrder.calculateTotals(); // Calculate all totals

        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);

        return savedOrder;
    }

    /**
     * Update existing sales order
     */
    public SalesOrder updateSalesOrder(Long id, SalesOrder orderDetails) {
        SalesOrder order = salesOrderRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Sales order not found with id: " + id));

        order.setTax(orderDetails.getTax());
        order.setSaleDiscount(orderDetails.getSaleDiscount());
        order.setCrewCost(orderDetails.getCrewCost());
        order.setAdditionalMaterials(orderDetails.getAdditionalMaterials());
        order.setNotes(orderDetails.getNotes());
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(orderDetails.getUpdatedBy());
        order.calculateTotals(); // Recalculate totals

        return salesOrderRepository.save(order);
    }

    /**
     * Add item to sales order and automatically update storage
     */
    public SalesOrderItem addItemToOrder(Long orderId, SalesOrderItem item) {
        SalesOrder order = salesOrderRepository.findByIdAndActiveTrue(orderId)
                .orElseThrow(() -> new RuntimeException("Sales order not found with id: " + orderId));

        // TODO: Uncomment when StorageService is available
        // Check storage availability
        // boolean isAvailable = storageService.checkAvailability(item.getStorageItemId(), item.getQuantity());
        // if (!isAvailable) {
        //     throw new RuntimeException("Insufficient storage quantity for item: " + item.getStorageItemId());
        // }

        // TODO: Uncomment when StorageService is available
        // Reserve storage (soft lock)
        // storageService.reserveItem(item.getStorageItemId(), item.getQuantity());

        item.setSalesOrder(order);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        item.setActive(true);
        item.calculateTotal(); // Calculate line total

        SalesOrderItem savedItem = salesOrderItemRepository.save(item);

        // Recalculate order totals
        order.calculateTotals();
        salesOrderRepository.save(order);

        return savedItem;
    }

    /**
     * Remove item from sales order and release storage
     */
    public void removeItemFromOrder(Long itemId) {
        SalesOrderItem item = salesOrderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Sales order item not found with id: " + itemId));

        // TODO: Uncomment when StorageService is available
        // Release reserved storage
        // storageService.releaseReservation(item.getStorageItemId(), item.getQuantity());

        item.setActive(false);
        item.setUpdatedAt(LocalDateTime.now());
        salesOrderItemRepository.save(item);

        // Recalculate order totals
        SalesOrder order = item.getSalesOrder();
        order.calculateTotals();
        salesOrderRepository.save(order);
    }

    /**
     * Confirm order (for customers) - automatically updates storage
     */
    public SalesOrder confirmOrder(Long orderId, String confirmedBy) {
        SalesOrder order = salesOrderRepository.findByIdAndActiveTrue(orderId)
                .orElseThrow(() -> new RuntimeException("Sales order not found with id: " + orderId));

        if (!"CUSTOMER".equals(order.getOrderType())) {
            throw new RuntimeException("Only customer orders can be confirmed directly");
        }

        // Get all order items
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderIdAndActiveTrue(orderId);

        // TODO: Uncomment when StorageService is available
        // Deduct from storage for each item
        // for (SalesOrderItem item : items) {
        //     storageService.deductItem(item.getStorageItemId(), item.getQuantity(),
        //             "Sales Order #" + orderId + " - Customer: " + order.getCustomer().getName());
        // }

        order.setStatus("CONFIRMED");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(confirmedBy);

        return salesOrderRepository.save(order);
    }

    /**
     * Push order to project table - automatically updates project elements and storage
     */
    public SalesOrder pushToProjectTable(Long orderId, String pushedBy) {
        SalesOrder order = salesOrderRepository.findByIdAndActiveTrue(orderId)
                .orElseThrow(() -> new RuntimeException("Sales order not found with id: " + orderId));

        if (!"PROJECT".equals(order.getOrderType())) {
            throw new RuntimeException("Only project orders can be pushed to project table");
        }

        if (order.getProjectId() == null) {
            throw new RuntimeException("Order must be linked to a project");
        }

        // Get all order items
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderIdAndActiveTrue(orderId);

        // Get the project entity
        com.magictech.modules.projects.entity.Project project =
            projectService.getProjectById(order.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found: " + order.getProjectId()));

        System.out.println("📦 Pushing " + items.size() + " items to project: " + project.getProjectName());

        // For each item: Create project element (automatically deducts from storage)
        int elementCount = 0;
        for (SalesOrderItem item : items) {
            try {
                // Get the storage item
                com.magictech.modules.storage.entity.StorageItem storageItem =
                    storageService.findById(item.getStorageItemId())
                        .orElseThrow(() -> new RuntimeException("Storage item not found: " + item.getStorageItemId()));

                // Create project element
                com.magictech.modules.projects.entity.ProjectElement element =
                    new com.magictech.modules.projects.entity.ProjectElement();

                element.setProject(project);
                element.setStorageItem(storageItem);
                element.setQuantityNeeded(item.getQuantity());
                element.setQuantityAllocated(0);  // Will be set by createElementDirectly
                element.setCustomPrice(item.getUnitPrice());
                element.setStatus("PENDING");  // Will be changed to APPROVED by createElementDirectly
                element.setNotes("Added from Sales Order #" + orderId + " by " + pushedBy);
                element.setAddedBy(pushedBy);
                element.setAddedDate(LocalDateTime.now());

                // Create element directly (Sales has permission, auto-deducts from storage)
                projectElementService.createElementDirectly(element);
                elementCount++;

                System.out.println("✅ Created project element for item: " + storageItem.getProductName() +
                                 " (Qty: " + item.getQuantity() + ")");
            } catch (Exception e) {
                System.err.println("Failed to create project element for item " + item.getStorageItemId() +
                                 ": " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to push item " + item.getStorageItemId() +
                                         " to project: " + e.getMessage());
            }
        }

        System.out.println("✅ Successfully created " + elementCount + " project elements");

        order.setStatus("PUSHED_TO_PROJECT");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(pushedBy);

        SalesOrder savedOrder = salesOrderRepository.save(order);

        // Send notification to Projects module that a new project has been created
        // (excludeSender is handled in notifyProjectCreated method)
        try {
            notificationService.notifyProjectCreated(
                order.getProjectId(),
                "Project from Sales Order #" + orderId,
                pushedBy
            );
            System.out.println("✅ Published notification to Projects module (excluded sender)");
        } catch (Exception e) {
            System.err.println("Failed to send project creation notification: " + e.getMessage());
        }

        return savedOrder;
    }

    /**
     * Get sales order by ID
     */
    public Optional<SalesOrder> getSalesOrderById(Long id) {
        return salesOrderRepository.findByIdAndActiveTrue(id);
    }

    /**
     * Get all active sales orders
     */
    public List<SalesOrder> getAllSalesOrders() {
        return salesOrderRepository.findByActiveTrue();
    }

    /**
     * Get orders by type
     */
    public List<SalesOrder> getOrdersByType(String orderType) {
        return salesOrderRepository.findByOrderTypeAndActiveTrue(orderType);
    }

    /**
     * Get orders by status
     */
    public List<SalesOrder> getOrdersByStatus(String status) {
        return salesOrderRepository.findByStatusAndActiveTrue(status);
    }

    /**
     * Get orders by customer
     */
    public List<SalesOrder> getOrdersByCustomer(Long customerId) {
        return salesOrderRepository.findByCustomerIdAndActiveTrue(customerId);
    }

    /**
     * Get orders by project
     */
    public List<SalesOrder> getOrdersByProject(Long projectId) {
        return salesOrderRepository.findByProjectIdAndActiveTrue(projectId);
    }

    /**
     * Get items for a sales order
     */
    public List<SalesOrderItem> getOrderItems(Long orderId) {
        return salesOrderItemRepository.findBySalesOrderIdAndActiveTrue(orderId);
    }

    /**
     * Cancel sales order and release all reservations
     */
    public void cancelOrder(Long orderId, String cancelledBy) {
        SalesOrder order = salesOrderRepository.findByIdAndActiveTrue(orderId)
                .orElseThrow(() -> new RuntimeException("Sales order not found with id: " + orderId));

        if ("CONFIRMED".equals(order.getStatus()) || "PUSHED_TO_PROJECT".equals(order.getStatus())) {
            throw new RuntimeException("Cannot cancel confirmed or pushed orders");
        }

        // TODO: Uncomment when StorageService is available
        // Release all storage reservations
        // List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderIdAndActiveTrue(orderId);
        // for (SalesOrderItem item : items) {
        //     storageService.releaseReservation(item.getStorageItemId(), item.getQuantity());
        // }

        order.setStatus("CANCELLED");
        order.setActive(false);
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(cancelledBy);

        salesOrderRepository.save(order);
    }

    /**
     * Get total order count
     */
    public long getTotalOrderCount() {
        return salesOrderRepository.countByActiveTrue();
    }
}