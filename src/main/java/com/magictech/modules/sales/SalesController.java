package com.magictech.modules.sales;

import com.magictech.modules.sales.entity.Customer;
import com.magictech.modules.sales.entity.SalesOrder;
import com.magictech.modules.sales.entity.SalesOrderItem;
import com.magictech.modules.sales.entity.SalesContract;
import com.magictech.modules.sales.service.CustomerService;
import com.magictech.modules.sales.service.SalesOrderService;
import com.magictech.modules.sales.service.SalesContractService;
import com.magictech.modules.sales.model.CustomerViewModel;
import com.magictech.modules.sales.model.SalesOrderViewModel;
import com.magictech.modules.sales.model.SalesOrderItemViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sales Controller - FIXED VERSION
 * Main controller for Sales Module UI
 * Handles: Dashboard, Customers, Projects, Orders, Contracts
 */
@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "*")
public class SalesController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private SalesContractService salesContractService;

    // Note: You may need to inject ProjectService if you want to list projects
    // @Autowired
    // private ProjectService projectService;

    // ============================================
    // DASHBOARD ENDPOINTS
    // ============================================

    /**
     * Get dashboard data (Projects + Customers lists)
     * Returns both lists for the main sales dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        // Get all customers
        List<Customer> customers = customerService.getAllCustomers();
        List<CustomerViewModel> customerViewModels = customers.stream()
                .map(this::convertToCustomerViewModel)
                .collect(Collectors.toList());

        // Get all projects - You'll need to implement this based on your ProjectService
        // List<Object> projects = projectService.getAllProjects();
        // For now, we'll just return an empty list

        // Get statistics
        long totalCustomers = customerService.getTotalCustomerCount();
        long totalOrders = salesOrderService.getTotalOrderCount();

        dashboard.put("customers", customerViewModels);
        // dashboard.put("projects", projects);
        dashboard.put("totalCustomers", totalCustomers);
        dashboard.put("totalOrders", totalOrders);

        return ResponseEntity.ok(dashboard);
    }

    // ============================================
    // CUSTOMER ENDPOINTS
    // ============================================

    /**
     * Get all customers
     */
    @GetMapping("/customers")
    public ResponseEntity<List<CustomerViewModel>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        List<CustomerViewModel> viewModels = customers.stream()
                .map(this::convertToCustomerViewModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(viewModels);
    }

    /**
     * Get customer by ID
     */
    @GetMapping("/customers/{id}")
    public ResponseEntity<CustomerViewModel> getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id)
                .map(customer -> ResponseEntity.ok(convertToCustomerViewModel(customer)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new customer (Fast selling - no project needed)
     */
    @PostMapping("/customers")
    public ResponseEntity<CustomerViewModel> createCustomer(@RequestBody Customer customer) {
        Customer createdCustomer = customerService.createCustomer(customer);
        return ResponseEntity.ok(convertToCustomerViewModel(createdCustomer));
    }

    /**
     * Update customer
     */
    @PutMapping("/customers/{id}")
    public ResponseEntity<CustomerViewModel> updateCustomer(
            @PathVariable Long id,
            @RequestBody Customer customer) {
        Customer updatedCustomer = customerService.updateCustomer(id, customer);
        return ResponseEntity.ok(convertToCustomerViewModel(updatedCustomer));
    }

    /**
     * Delete customer
     */
    @DeleteMapping("/customers/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Search customers by name
     */
    @GetMapping("/customers/search")
    public ResponseEntity<List<CustomerViewModel>> searchCustomers(@RequestParam String name) {
        List<Customer> customers = customerService.searchCustomersByName(name);
        List<CustomerViewModel> viewModels = customers.stream()
                .map(this::convertToCustomerViewModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(viewModels);
    }

    // ============================================
    // SALES ORDER ENDPOINTS
    // ============================================

    /**
     * Get all sales orders
     */
    @GetMapping("/orders")
    public ResponseEntity<List<SalesOrderViewModel>> getAllOrders() {
        List<SalesOrder> orders = salesOrderService.getAllSalesOrders();
        List<SalesOrderViewModel> viewModels = orders.stream()
                .map(this::convertToOrderViewModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(viewModels);
    }

    /**
     * Get order by ID with all details
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<SalesOrderViewModel> getOrderById(@PathVariable Long id) {
        return salesOrderService.getSalesOrderById(id)
                .map(order -> {
                    SalesOrderViewModel viewModel = convertToOrderViewModel(order);
                    // Get order items
                    List<SalesOrderItem> items = salesOrderService.getOrderItems(id);
                    List<SalesOrderItemViewModel> itemViewModels = items.stream()
                            .map(this::convertToItemViewModel)
                            .collect(Collectors.toList());
                    viewModel.setItems(itemViewModels);
                    viewModel.setTotalItems(itemViewModels.size());
                    return ResponseEntity.ok(viewModel);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new sales order for PROJECT
     */
    @PostMapping("/orders/project/{projectId}")
    public ResponseEntity<SalesOrderViewModel> createProjectOrder(
            @PathVariable Long projectId,
            @RequestBody SalesOrder order) {
        order.setOrderType("PROJECT");
        order.setProjectId(projectId);
        SalesOrder createdOrder = salesOrderService.createSalesOrder(order);
        return ResponseEntity.ok(convertToOrderViewModel(createdOrder));
    }

    /**
     * Create new sales order for CUSTOMER
     */
    @PostMapping("/orders/customer/{customerId}")
    public ResponseEntity<SalesOrderViewModel> createCustomerOrder(
            @PathVariable Long customerId,
            @RequestBody SalesOrder order) {
        order.setOrderType("CUSTOMER");
        Customer customer = customerService.getCustomerById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        order.setCustomer(customer);
        SalesOrder createdOrder = salesOrderService.createSalesOrder(order);
        return ResponseEntity.ok(convertToOrderViewModel(createdOrder));
    }

    /**
     * Update sales order (tax, discount, crew, materials, notes)
     */
    @PutMapping("/orders/{id}")
    public ResponseEntity<SalesOrderViewModel> updateOrder(
            @PathVariable Long id,
            @RequestBody SalesOrder order) {
        SalesOrder updatedOrder = salesOrderService.updateSalesOrder(id, order);
        return ResponseEntity.ok(convertToOrderViewModel(updatedOrder));
    }

    /**
     * Add item to sales order
     * Automatically checks and reserves storage
     */
    @PostMapping("/orders/{orderId}/items")
    public ResponseEntity<SalesOrderItemViewModel> addItemToOrder(
            @PathVariable Long orderId,
            @RequestBody SalesOrderItem item) {
        SalesOrderItem addedItem = salesOrderService.addItemToOrder(orderId, item);
        return ResponseEntity.ok(convertToItemViewModel(addedItem));
    }

    /**
     * Remove item from sales order
     * Automatically releases storage reservation
     */
    @DeleteMapping("/orders/items/{itemId}")
    public ResponseEntity<Void> removeItemFromOrder(@PathVariable Long itemId) {
        salesOrderService.removeItemFromOrder(itemId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get all items for an order
     */
    @GetMapping("/orders/{orderId}/items")
    public ResponseEntity<List<SalesOrderItemViewModel>> getOrderItems(@PathVariable Long orderId) {
        List<SalesOrderItem> items = salesOrderService.getOrderItems(orderId);
        List<SalesOrderItemViewModel> viewModels = items.stream()
                .map(this::convertToItemViewModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(viewModels);
    }

    /**
     * Confirm customer order
     * Automatically deducts from storage
     */
    @PostMapping("/orders/{orderId}/confirm")
    public ResponseEntity<SalesOrderViewModel> confirmOrder(
            @PathVariable Long orderId,
            @RequestParam String confirmedBy) {
        SalesOrder confirmedOrder = salesOrderService.confirmOrder(orderId, confirmedBy);
        return ResponseEntity.ok(convertToOrderViewModel(confirmedOrder));
    }

    /**
     * Push project order to project table
     * Automatically:
     * - Creates project elements
     * - Deducts from storage
     * - Updates Project module
     */
    @PostMapping("/orders/{orderId}/push-to-project")
    public ResponseEntity<SalesOrderViewModel> pushToProjectTable(
            @PathVariable Long orderId,
            @RequestParam String pushedBy) {
        SalesOrder pushedOrder = salesOrderService.pushToProjectTable(orderId, pushedBy);
        return ResponseEntity.ok(convertToOrderViewModel(pushedOrder));
    }

    /**
     * Cancel sales order
     * Releases all storage reservations
     */
    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam String cancelledBy) {
        salesOrderService.cancelOrder(orderId, cancelledBy);
        return ResponseEntity.ok().build();
    }

    /**
     * Get orders by customer
     */
    @GetMapping("/orders/customer/{customerId}")
    public ResponseEntity<List<SalesOrderViewModel>> getOrdersByCustomer(@PathVariable Long customerId) {
        List<SalesOrder> orders = salesOrderService.getOrdersByCustomer(customerId);
        List<SalesOrderViewModel> viewModels = orders.stream()
                .map(this::convertToOrderViewModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(viewModels);
    }

    /**
     * Get orders by project
     */
    @GetMapping("/orders/project/{projectId}")
    public ResponseEntity<List<SalesOrderViewModel>> getOrdersByProject(@PathVariable Long projectId) {
        List<SalesOrder> orders = salesOrderService.getOrdersByProject(projectId);
        List<SalesOrderViewModel> viewModels = orders.stream()
                .map(this::convertToOrderViewModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(viewModels);
    }

    // ============================================
    // CONTRACT ENDPOINTS (Tab 1: User Requirements)
    // ============================================

    /**
     * Get contract for sales order
     */
    @GetMapping("/orders/{orderId}/contract")
    public ResponseEntity<SalesContract> getOrderContract(@PathVariable Long orderId) {
        return salesContractService.getContractBySalesOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create contract for sales order
     */
    @PostMapping("/orders/{orderId}/contract")
    public ResponseEntity<SalesContract> createContract(
            @PathVariable Long orderId,
            @RequestBody SalesContract contract) {
        SalesOrder order = salesOrderService.getSalesOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        contract.setSalesOrder(order);
        SalesContract createdContract = salesContractService.createContract(contract);
        return ResponseEntity.ok(createdContract);
    }

    /**
     * Update contract
     */
    @PutMapping("/contracts/{id}")
    public ResponseEntity<SalesContract> updateContract(
            @PathVariable Long id,
            @RequestBody SalesContract contract) {
        SalesContract updatedContract = salesContractService.updateContract(id, contract);
        return ResponseEntity.ok(updatedContract);
    }

    /**
     * Delete contract
     */
    @DeleteMapping("/contracts/{id}")
    public ResponseEntity<Void> deleteContract(@PathVariable Long id) {
        salesContractService.deleteContract(id);
        return ResponseEntity.ok().build();
    }

    // ============================================
    // HELPER METHODS - Convert Entities to ViewModels
    // ============================================

    private CustomerViewModel convertToCustomerViewModel(Customer customer) {
        CustomerViewModel vm = new CustomerViewModel();
        vm.setId(customer.getId());
        vm.setName(customer.getName());
        vm.setEmail(customer.getEmail());
        vm.setPhone(customer.getPhone());
        vm.setAddress(customer.getAddress());
        vm.setCompany(customer.getCompany());
        vm.setNotes(customer.getNotes());
        vm.setCreatedAt(customer.getCreatedAt());
        vm.setCreatedBy(customer.getCreatedBy());
        vm.setUpdatedAt(customer.getUpdatedAt());
        vm.setUpdatedBy(customer.getUpdatedBy());

        // Get customer statistics
        List<SalesOrder> customerOrders = salesOrderService.getOrdersByCustomer(customer.getId());
        vm.setTotalOrders((long) customerOrders.size());

        // Calculate total revenue using BigDecimal
        BigDecimal totalRevenue = customerOrders.stream()
                .map(SalesOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vm.setTotalRevenue(totalRevenue);

        return vm;
    }

    private SalesOrderViewModel convertToOrderViewModel(SalesOrder order) {
        SalesOrderViewModel vm = new SalesOrderViewModel();
        vm.setId(order.getId());
        vm.setOrderType(order.getOrderType());
        vm.setProjectId(order.getProjectId());

        if (order.getCustomer() != null) {
            vm.setCustomerId(order.getCustomer().getId());
            vm.setCustomerName(order.getCustomer().getName());
        }

        vm.setSubtotal(order.getSubtotal());
        vm.setTax(order.getTax());
        vm.setSaleDiscount(order.getSaleDiscount());
        vm.setCrewCost(order.getCrewCost());
        vm.setAdditionalMaterials(order.getAdditionalMaterials());
        vm.setTotalAmount(order.getTotalAmount());
        vm.setStatus(order.getStatus());
        vm.setNotes(order.getNotes());
        vm.setCreatedAt(order.getCreatedAt());
        vm.setCreatedBy(order.getCreatedBy());
        vm.setUpdatedAt(order.getUpdatedAt());
        vm.setUpdatedBy(order.getUpdatedBy());

        return vm;
    }

    private SalesOrderItemViewModel convertToItemViewModel(SalesOrderItem item) {
        SalesOrderItemViewModel vm = new SalesOrderItemViewModel();
        vm.setId(item.getId());
        vm.setSalesOrderId(item.getSalesOrder().getId());
        vm.setStorageItemId(item.getStorageItemId());
        vm.setQuantity(item.getQuantity());
        vm.setUnitPrice(item.getUnitPrice());
        vm.setTotalPrice(item.getTotalPrice());
        vm.setNotes(item.getNotes());
        vm.setCreatedAt(item.getCreatedAt());
        vm.setUpdatedAt(item.getUpdatedAt());

        // You can add storage item details from StorageService if needed
        // StorageItem storageItem = storageService.getById(item.getStorageItemId());
        // vm.setStorageItemName(storageItem.getName());

        return vm;
    }
}