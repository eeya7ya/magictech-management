package com.magictech.modules.sales.service;

import com.magictech.core.auth.User;
import com.magictech.core.auth.UserRepository;
import com.magictech.core.auth.UserRole;
import com.magictech.modules.notifications.entity.NotificationPriority;
import com.magictech.modules.notifications.entity.NotificationType;
import com.magictech.modules.notifications.service.NotificationService;
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
 * Sales Order Service - WITH NOTIFICATIONS
 * Business logic for sales order management with automatic database updates and notifications
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

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private UserRepository userRepository;

    // TODO: Uncomment when StorageService is available
    // @Autowired
    // private StorageService storageService;

    // TODO: Uncomment when ProjectElementService is available
    // @Autowired
    // private ProjectElementService projectElementService;

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

        // Send notification to MASTER and SALES users
        sendOrderCreatedNotification(savedOrder);

        return savedOrder;
    }

    /**
     * Send notification when order is created
     */
    private void sendOrderCreatedNotification(SalesOrder order) {
        if (notificationService == null || userRepository == null) {
            return; // Notifications not configured
        }

        try {
            String customerName = order.getCustomer() != null
                ? order.getCustomer().getName()
                : "Unknown Customer";

            String title = "New Sales Order #" + order.getId();
            String message = String.format(
                "New order created for %s - Total: $%.2f",
                customerName,
                order.getTotalAmount()
            );

            // Notify MASTER users
            List<User> masterUsers = userRepository.findByRoleAndActiveTrue(UserRole.MASTER);
            for (User user : masterUsers) {
                notificationService.createNotification(
                    user.getId(),
                    title,
                    message,
                    NotificationType.NEW_SALES_ORDER,
                    NotificationPriority.NORMAL,
                    "SALES",
                    order.getId(),
                    "SalesOrder",
                    "/sales/orders/" + order.getId()
                );
            }

            // Notify SALES users
            List<User> salesUsers = userRepository.findByRoleAndActiveTrue(UserRole.SALES);
            for (User user : salesUsers) {
                notificationService.createNotification(
                    user.getId(),
                    title,
                    message,
                    NotificationType.NEW_SALES_ORDER,
                    NotificationPriority.NORMAL,
                    "SALES",
                    order.getId(),
                    "SalesOrder",
                    "/sales/orders/" + order.getId()
                );
            }
        } catch (Exception e) {
            // Don't fail the order creation if notification fails
            System.err.println("Failed to send order created notification: " + e.getMessage());
        }
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

        // TODO: Uncomment when ProjectElementService and StorageService are available
        // For each item:
        // 1. Create project element
        // 2. Deduct from storage
        // for (SalesOrderItem item : items) {
        //     // Create project element
        //     projectElementService.addElementToProject(
        //             order.getProjectId(),
        //             item.getStorageItemId(),
        //             item.getQuantity(),
        //             item.getUnitPrice(),
        //             "Added from Sales Order #" + orderId
        //     );
        //
        //     // Deduct from storage
        //     storageService.deductItem(item.getStorageItemId(), item.getQuantity(),
        //             "Sales Order #" + orderId + " - Project ID: " + order.getProjectId());
        // }

        order.setStatus("PUSHED_TO_PROJECT");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(pushedBy);

        return salesOrderRepository.save(order);
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