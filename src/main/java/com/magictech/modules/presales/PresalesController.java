package com.magictech.modules.presales;

import com.magictech.modules.storage.base.BaseStorageModuleController;
import com.magictech.modules.storage.config.ModuleStorageConfig;
import org.springframework.stereotype.Component;

/**
 * Presales Module Controller
 * Handles quotations, initial customer contact, and pre-sales activities
 * Extends BaseStorageModuleController for standard storage-based operations
 */
@Component
public class PresalesController extends BaseStorageModuleController {

    /**
     * Constructor - initializes with PRESALES configuration
     */
    public PresalesController() {
        super(ModuleStorageConfig.PRESALES);
    }

    @Override
    protected String getModuleName() {
        return "Presales Module";
    }

    @Override
    protected String getModuleIcon() {
        return "📋";
    }

    @Override
    protected String getModuleColor() {
        return "#06b6d4"; // Cyan color
    }

    @Override
    protected String getModuleDescription() {
        return "Manage quotations • Initial customer contact • Pre-sales activities";
    }

    /**
     * Additional presales-specific functionality can be added here
     * Examples:
     * - Generate quotations
     * - Track customer inquiries
     * - Manage follow-ups
     * - Create preliminary pricing estimates
     */
}
