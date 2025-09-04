// Main JavaScript file for Logbook application

// API base URL
const API_BASE = '/api/logs';

// Utility functions
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleString();
}

function formatLogLevel(level) {
    if (!level) return `<span class="badge bg-secondary">-</span>`;
    const str = String(level);
    const http = httpStatusInfo(str);
    if (http) {
        return `<span class="badge http-badge http-${http.category}">${http.code} ${http.phrase}</span>`;
    }
    return `<span class="badge log-level-${str}">${str}</span>`;
}

function httpStatusInfo(levelStr) {
    if (!/^\d{3}$/.test(levelStr)) return null;
    const code = parseInt(levelStr, 10);
    if (code < 100 || code > 599) return null;
    const phrases = {
        100:'Continue',101:'Switching Protocols',102:'Processing',103:'Early Hints',
        200:'OK',201:'Created',202:'Accepted',203:'Non-Authoritative Information',204:'No Content',205:'Reset Content',206:'Partial Content',207:'Multi-Status',208:'Already Reported',226:'IM Used',
        300:'Multiple Choices',301:'Moved Permanently',302:'Found',303:'See Other',304:'Not Modified',305:'Use Proxy',307:'Temporary Redirect',308:'Permanent Redirect',
        400:'Bad Request',401:'Unauthorized',402:'Payment Required',403:'Forbidden',404:'Not Found',405:'Method Not Allowed',406:'Not Acceptable',407:'Proxy Authentication Required',408:'Request Timeout',409:'Conflict',410:'Gone',411:'Length Required',412:'Precondition Failed',413:'Payload Too Large',414:'URI Too Long',415:'Unsupported Media Type',416:'Range Not Satisfiable',417:'Expectation Failed',418:"I'm a Teapot",421:'Misdirected Request',422:'Unprocessable Entity',423:'Locked',424:'Failed Dependency',425:'Too Early',426:'Upgrade Required',428:'Precondition Required',429:'Too Many Requests',431:'Request Header Fields Too Large',451:'Unavailable For Legal Reasons',
        500:'Internal Server Error',501:'Not Implemented',502:'Bad Gateway',503:'Service Unavailable',504:'Gateway Timeout',505:'HTTP Version Not Supported',506:'Variant Also Negotiates',507:'Insufficient Storage',508:'Loop Detected',510:'Not Extended',511:'Network Authentication Required'
    };
    const phrase = phrases[code] || (code >= 100 && code < 200 ? 'Informational' : code < 300 ? 'Success' : code < 400 ? 'Redirection' : code < 500 ? 'Client Error' : 'Server Error');
    const category = code < 200 ? '1xx' : code < 300 ? '2xx' : code < 400 ? '3xx' : code < 500 ? '4xx' : '5xx';
    return { code, phrase, category };
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
        // Handle 204 No Content or non-JSON responses gracefully
        if (response.status === 204 || response.status === 205) {
            return null;
        }
        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            return await response.json();
        }
        // Fallback: return text for other content types
        return await response.text();
    } catch (error) {
        console.error('API request failed:', error);
        throw error;
    }
}

// Helpers for Spring Data Page responses (support multiple JSON shapes)
function pageTotal(resp) {
    if (!resp) return 0;
    if (typeof resp.totalElements === 'number') return resp.totalElements;
    if (resp.page && typeof resp.page.totalElements === 'number') return resp.page.totalElements;
    if (Array.isArray(resp.content)) return resp.content.length;
    return 0;
}

function pageContent(resp) {
    if (!resp) return [];
    if (Array.isArray(resp.content)) return resp.content;
    // Fallback if content not present
    return [];
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

// Fetch single log by ID
async function getLog(id) {
    return await apiRequest(`${API_BASE}/${id}`);
}

// Common event handlers
function confirmDelete(id, callback, message) {
    const prompt = message || 'Are you sure you want to delete this log entry? This action cannot be undone.';
    if (confirm(prompt)) {
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
