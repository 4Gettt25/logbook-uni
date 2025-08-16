// Main JavaScript file for Logbook application

// API base URL
const API_BASE = '/api/logs';

// Utility functions
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleString();
}

function formatLogLevel(level) {
    return `<span class="badge log-level-${level}">${level}</span>`;
}

function formatStatus(status) {
    return `<span class="badge status-${status}">${status.replace('_', ' ')}</span>`;
}

function showToast(message, type = 'success') {
    const toastContainer = document.getElementById('toastContainer') || createToastContainer();
    
    const toastId = 'toast-' + Date.now();
    const toastHtml = `
        <div class="toast" id="${toastId}" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="toast-header bg-${type} text-white">
                <strong class="me-auto">
                    <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-triangle'}"></i>
                    ${type === 'success' ? 'Success' : 'Error'}
                </strong>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast"></button>
            </div>
            <div class="toast-body">
                ${message}
            </div>
        </div>
    `;
    
    toastContainer.insertAdjacentHTML('beforeend', toastHtml);
    
    const toast = new bootstrap.Toast(document.getElementById(toastId));
    toast.show();
    
    // Remove toast element after it's hidden
    document.getElementById(toastId).addEventListener('hidden.bs.toast', function() {
        this.remove();
    });
}

function createToastContainer() {
    const container = document.createElement('div');
    container.id = 'toastContainer';
    container.className = 'toast-container';
    document.body.appendChild(container);
    return container;
}

function showLoading(element) {
    const loadingHtml = `
        <div class="loading-overlay">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
        </div>
    `;
    element.style.position = 'relative';
    element.insertAdjacentHTML('beforeend', loadingHtml);
}

function hideLoading(element) {
    const loadingOverlay = element.querySelector('.loading-overlay');
    if (loadingOverlay) {
        loadingOverlay.remove();
    }
}

// API functions
async function apiRequest(url, options = {}) {
    try {
        const response = await fetch(url, {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        return await response.json();
    } catch (error) {
        console.error('API request failed:', error);
        throw error;
    }
}

async function fetchLogs(params = {}) {
    const queryString = new URLSearchParams(params).toString();
    const url = `${API_BASE}${queryString ? '?' + queryString : ''}`;
    return await apiRequest(url);
}

async function createLog(logData) {
    return await apiRequest(API_BASE, {
        method: 'POST',
        body: JSON.stringify(logData)
    });
}

async function updateLog(id, logData) {
    return await apiRequest(`${API_BASE}/${id}`, {
        method: 'PUT',
        body: JSON.stringify(logData)
    });
}

async function deleteLog(id) {
    return await apiRequest(`${API_BASE}/${id}`, {
        method: 'DELETE'
    });
}

function exportLogs(format = 'csv') {
    const url = `${API_BASE}/export?format=${format}&limit=1000`;
    window.open(url, '_blank');
}

// Common event handlers
function confirmDelete(id, callback) {
    if (confirm('Are you sure you want to delete this log entry? This action cannot be undone.')) {
        callback(id);
    }
}

// Initialize common functionality
document.addEventListener('DOMContentLoaded', function() {
    // Initialize Bootstrap tooltips
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
    
    // Initialize Bootstrap popovers
    const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    popoverTriggerList.map(function (popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });
});

// Global error handler
window.addEventListener('unhandledrejection', function(event) {
    console.error('Unhandled promise rejection:', event.reason);
    showToast('An unexpected error occurred. Please try again.', 'danger');
});
