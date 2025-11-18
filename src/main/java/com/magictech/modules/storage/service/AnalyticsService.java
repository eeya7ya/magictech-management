package com.magictech.modules.storage.service;

import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.entity.ProjectElement;
import com.magictech.modules.projects.service.ProjectElementService;
import com.magictech.modules.projects.service.ProjectService;
import com.magictech.modules.sales.entity.Customer;
import com.magictech.modules.sales.entity.ProjectCostBreakdown;
import com.magictech.modules.sales.entity.SalesOrder;
import com.magictech.modules.sales.service.CustomerService;
import com.magictech.modules.sales.service.ProjectCostBreakdownService;
import com.magictech.modules.sales.service.SalesOrderService;
import com.magictech.modules.storage.dto.CustomerAnalyticsDTO;
import com.magictech.modules.storage.dto.ProjectAnalyticsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Analytics Service
 * Provides business analytics for projects and customers
 */
@Service
public class AnalyticsService {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectElementService elementService;

    @Autowired
    private ProjectCostBreakdownService costBreakdownService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private SalesOrderService salesOrderService;

    /**
     * Get project analytics for all projects
     */
    public List<ProjectAnalyticsDTO> getProjectAnalytics() {
        List<Project> projects = projectService.getAllProjects();
        List<ProjectAnalyticsDTO> analytics = new ArrayList<>();

        for (Project project : projects) {
            ProjectAnalyticsDTO dto = new ProjectAnalyticsDTO();
            dto.setProjectId(project.getId());
            dto.setProjectName(project.getProjectName());
            dto.setProjectLocation(project.getProjectLocation());
            dto.setStatus(project.getStatus() != null ? project.getStatus() : "Unknown");
            dto.setDateOfIssue(project.getDateOfIssue());
            dto.setDateOfCompletion(project.getDateOfCompletion());
            dto.setCreatedBy(project.getCreatedBy());

            // Calculate duration if completed
            if (project.getDateOfIssue() != null && project.getDateOfCompletion() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                    project.getDateOfIssue(),
                    project.getDateOfCompletion()
                );
                dto.setDurationDays((int) days);
            }

            // Get elements count
            List<ProjectElement> elements = elementService.getElementsByProject(project.getId());
            dto.setElementsCount(elements.size());

            // Calculate total cost from breakdown
            try {
                Optional<ProjectCostBreakdown> breakdown = costBreakdownService.getBreakdownByProject(project.getId());
                if (breakdown.isPresent()) {
                    dto.setTotalCost(breakdown.get().getTotalAmount());
                } else {
                    // Fallback: calculate from elements
                    BigDecimal elementsTotal = BigDecimal.ZERO;
                    for (ProjectElement element : elements) {
                        if (element.getStorageItem() != null && element.getStorageItem().getPrice() != null) {
                            BigDecimal price = element.getStorageItem().getPrice();
                            BigDecimal quantity = new BigDecimal(element.getQuantityNeeded());
                            elementsTotal = elementsTotal.add(price.multiply(quantity));
                        }
                    }
                    dto.setTotalCost(elementsTotal);
                }
            } catch (Exception e) {
                dto.setTotalCost(BigDecimal.ZERO);
            }

            analytics.add(dto);
        }

        return analytics;
    }

    /**
     * Get customer analytics for all customers
     */
    public List<CustomerAnalyticsDTO> getCustomerAnalytics() {
        List<Customer> customers = customerService.getAllCustomers();
        List<CustomerAnalyticsDTO> analytics = new ArrayList<>();

        for (Customer customer : customers) {
            CustomerAnalyticsDTO dto = new CustomerAnalyticsDTO();
            dto.setCustomerId(customer.getId());
            dto.setCustomerName(customer.getName());
            dto.setEmail(customer.getEmail());
            dto.setPhone(customer.getPhone());

            // Get orders for this customer
            List<SalesOrder> orders = salesOrderService.getOrdersByCustomer(customer.getId());
            dto.setOrdersCount(orders.size());

            // Calculate total sales
            BigDecimal totalSales = BigDecimal.ZERO;
            for (SalesOrder order : orders) {
                if (order.getTotalAmount() != null) {
                    totalSales = totalSales.add(order.getTotalAmount());
                }
            }
            dto.setTotalSales(totalSales);

            // Get last order date
            if (!orders.isEmpty()) {
                dto.setLastOrderDate(orders.stream()
                    .map(SalesOrder::getDateAdded)
                    .max(java.time.LocalDateTime::compareTo)
                    .orElse(null));
            }

            // TODO: Implement most ordered product (requires item-level analysis)
            dto.setMostOrderedProduct("N/A");

            analytics.add(dto);
        }

        return analytics;
    }

    /**
     * Get overall business metrics
     */
    public BusinessMetricsDTO getBusinessMetrics() {
        BusinessMetricsDTO metrics = new BusinessMetricsDTO();

        // Project metrics
        List<Project> projects = projectService.getAllProjects();
        metrics.setTotalProjects(projects.size());
        metrics.setCompletedProjects((int) projects.stream()
            .filter(p -> "completed".equalsIgnoreCase(p.getStatus()))
            .count());
        metrics.setActiveProjects((int) projects.stream()
            .filter(p -> "in progress".equalsIgnoreCase(p.getStatus()))
            .count());

        // Customer metrics
        List<Customer> customers = customerService.getAllCustomers();
        metrics.setTotalCustomers(customers.size());

        // Sales metrics
        List<SalesOrder> orders = salesOrderService.getAllOrders();
        metrics.setTotalOrders(orders.size());

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (SalesOrder order : orders) {
            if (order.getTotalAmount() != null) {
                totalRevenue = totalRevenue.add(order.getTotalAmount());
            }
        }
        metrics.setTotalRevenue(totalRevenue);

        return metrics;
    }

    /**
     * DTO for overall business metrics
     */
    public static class BusinessMetricsDTO {
        private Integer totalProjects;
        private Integer completedProjects;
        private Integer activeProjects;
        private Integer totalCustomers;
        private Integer totalOrders;
        private BigDecimal totalRevenue;

        // Getters and Setters
        public Integer getTotalProjects() {
            return totalProjects;
        }

        public void setTotalProjects(Integer totalProjects) {
            this.totalProjects = totalProjects;
        }

        public Integer getCompletedProjects() {
            return completedProjects;
        }

        public void setCompletedProjects(Integer completedProjects) {
            this.completedProjects = completedProjects;
        }

        public Integer getActiveProjects() {
            return activeProjects;
        }

        public void setActiveProjects(Integer activeProjects) {
            this.activeProjects = activeProjects;
        }

        public Integer getTotalCustomers() {
            return totalCustomers;
        }

        public void setTotalCustomers(Integer totalCustomers) {
            this.totalCustomers = totalCustomers;
        }

        public Integer getTotalOrders() {
            return totalOrders;
        }

        public void setTotalOrders(Integer totalOrders) {
            this.totalOrders = totalOrders;
        }

        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
        }
    }
}
