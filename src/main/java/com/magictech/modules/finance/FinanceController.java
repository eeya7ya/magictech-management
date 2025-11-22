package com.magictech.modules.finance;

import com.magictech.modules.storage.base.BaseStorageModuleController;
import com.magictech.modules.storage.config.ModuleStorageConfig;
import org.springframework.stereotype.Component;

/**
 * Finance Module Controller
 * Handles invoicing, payments, and financial tracking
 * Extends BaseStorageModuleController for standard storage-based operations
 */
@Component
public class FinanceController extends BaseStorageModuleController {

    /**
     * Constructor - initializes with FINANCE configuration
     */
    public FinanceController() {
        super(ModuleStorageConfig.FINANCE);
    }

    @Override
    protected String getModuleName() {
        return "Finance Module";
    }

    @Override
    protected String getModuleIcon() {
        return "💰";
    }

    @Override
    protected String getModuleColor() {
        return "#eab308"; // Yellow color
    }

    @Override
    protected String getModuleDescription() {
        return "Manage invoicing • Track payments • Financial reporting";
    }

    /**
     * Additional finance-specific functionality can be added here
     * Examples:
     * - Generate invoices
     * - Track payments and receivables
     * - Financial reporting
     * - Payment status updates
     * - Integration with accounting systems
     */
}
