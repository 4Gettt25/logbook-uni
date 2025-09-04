// Servers page JavaScript
// Show confirmation dialog before deleting
function confirmDelete(id, deleteFn, message) {
    if (window.confirm(message || 'Are you sure you want to delete this item?')) {
        deleteFn(id);
    }
}

let currentPage = 0;
let totalPages = 0;

document.addEventListener('DOMContentLoaded', function() {
    loadServers();
    
    // Set up form validation
    setupServerFormValidation();
});

async function loadServers(page = 0) {
    try {
        const container = document.getElementById('serversContainer');
        showLoading(container);
        
        const response = await apiRequest(`/api/servers?page=${page}&size=20`);
        const servers = response.content || [];
        
        hideLoading(container);
        
        currentPage = page;
        totalPages = response.totalPages || 0;
        
        // Update server count
        document.getElementById('serverCount').textContent = 
            `${response.totalElements || 0} servers`;
        
        if (servers.length === 0) {
            container.innerHTML = `
                <div class="text-center text-muted py-5">
                    <i class="fas fa-server fa-3x mb-3"></i>
                    <h5>No servers found</h5>
                    <p>Add your first server to start uploading log files.</p>
                    <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addServerModal">
                        <i class="fas fa-plus"></i> Add Server
                    </button>
                </div>
            `;
        } else {
            renderServers(servers);
        }
        
        renderPagination();
        
    } catch (error) {
        console.error('Failed to load servers:', error);
        const container = document.getElementById('serversContainer');
        hideLoading(container);
        container.innerHTML = `
            <div class="text-center text-danger py-5">
                <i class="fas fa-exclamation-triangle fa-3x mb-3"></i>
                <h5>Failed to load servers</h5>
                <p>Please check your connection and try again.</p>
                <button class="btn btn-primary" onclick="loadServers()">
                    <i class="fas fa-retry"></i> Retry
                </button>
            </div>
        `;
    }
}

function renderServers(servers) {
    const container = document.getElementById('serversContainer');
    
    const serversHtml = servers.map(server => `
        <div class="card mb-3">
            <div class="card-body">
                <div class="row align-items-center">
                    <div class="col-md-8">
                        <h5 class="card-title mb-1">
                            <i class="fas fa-server text-primary"></i> ${escapeHtml(server.name)}
                        </h5>
                        ${server.hostname ? `
                            <p class="text-muted mb-1">
                                <i class="fas fa-globe"></i> ${escapeHtml(server.hostname)}
                            </p>
                        ` : ''}
                        ${server.description ? `
                            <p class="card-text mb-1">${escapeHtml(server.description)}</p>
                        ` : ''}
                        <small class="text-muted">
                            <i class="fas fa-calendar"></i> Created: ${formatDate(server.createdAt)}
                        </small>
                    </div>
                    <div class="col-md-4 text-end">
                        <div class="btn-group-vertical w-100">
                            <a href="/logs?server=${server.id}" class="btn btn-outline-primary btn-sm mb-1">
                                <i class="fas fa-list"></i> View Logs
                            </a>
                            <a href="/upload?server=${server.id}" class="btn btn-outline-success btn-sm mb-1">
                                <i class="fas fa-upload"></i> Upload Files
                            </a>
                            <button class="btn btn-outline-secondary btn-sm mb-1" onclick="reevaluateServerLogs(${server.id})" data-bs-toggle="tooltip" title="Recompute levels; optionally merge continuations">
                                <i class="fas fa-sync"></i> Reevaluate Logs
                            </button>
                            <button class="btn btn-outline-secondary btn-sm" onclick="editServer(${server.id})">
                                <i class="fas fa-edit"></i> Edit
                            </button>
                        </div>
                    </div>
                </div>
                <div class="row mt-2">
                    <div class="col-12">
                        <div class="d-flex justify-content-between align-items-center">
                            <span class="badge bg-primary" id="logCount-${server.id}">
                                Loading log count...
                            </span>
                            <button class="btn btn-outline-danger btn-sm" 
                                    onclick="confirmDelete(${server.id}, deleteServer, 'Are you sure you want to delete this server and all its logs? This action cannot be undone.')"
                                    data-bs-toggle="tooltip" title="Delete Server">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
    
    container.innerHTML = serversHtml;
    
    // Load log counts for each server
    servers.forEach(server => {
        loadServerLogCount(server.id);
    });
    
    // Re-initialize tooltips
    const tooltipTriggerList = container.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltipTriggerList.forEach(function (tooltipTriggerEl) {
        new bootstrap.Tooltip(tooltipTriggerEl);
    });
}

async function loadServerLogCount(serverId) {
    try {
        const response = await apiRequest(`/api/servers/${serverId}/logs?size=1`);
        const count = response.totalElements || 0;
        document.getElementById(`logCount-${serverId}`).textContent = 
            `${count} log ${count === 1 ? 'entry' : 'entries'}`;
    } catch (error) {
        console.error(`Failed to load log count for server ${serverId}:`, error);
        document.getElementById(`logCount-${serverId}`).textContent = 'Unable to load count';
    }
}

function renderPagination() {
    const paginationContainer = document.getElementById('paginationContainer');
    const pagination = document.getElementById('pagination');
    
    if (totalPages <= 1) {
        paginationContainer.style.display = 'none';
        return;
    }
    
    paginationContainer.style.display = 'block';
    
    let paginationHtml = '';
    
    // Previous button
    paginationHtml += `
        <li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="changePage(${currentPage - 1})" 
               ${currentPage === 0 ? 'tabindex="-1"' : ''}>
                <i class="fas fa-chevron-left"></i> Previous
            </a>
        </li>
    `;
    
    // Page numbers
    const startPage = Math.max(0, currentPage - 2);
    const endPage = Math.min(totalPages - 1, currentPage + 2);
    
    for (let i = startPage; i <= endPage; i++) {
        paginationHtml += `
            <li class="page-item ${i === currentPage ? 'active' : ''}">
                <a class="page-link" href="#" onclick="changePage(${i})">${i + 1}</a>
            </li>
        `;
    }
    
    // Next button
    paginationHtml += `
        <li class="page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="changePage(${currentPage + 1})"
               ${currentPage === totalPages - 1 ? 'tabindex="-1"' : ''}>
                Next <i class="fas fa-chevron-right"></i>
            </a>
        </li>
    `;
    
    pagination.innerHTML = paginationHtml;
}

function changePage(page) {
    if (page >= 0 && page < totalPages && page !== currentPage) {
        loadServers(page);
    }
}

function setupServerFormValidation() {
    const form = document.getElementById('addServerForm');
    const inputs = form.querySelectorAll('input[required], select[required], textarea[required]');
    
    inputs.forEach(input => {
        input.addEventListener('blur', function() {
            validateField(this);
        });
        
        input.addEventListener('input', function() {
            if (this.classList.contains('is-invalid')) {
                validateField(this);
            }
        });
    });
}

function validateField(field) {
    if (field.checkValidity()) {
        field.classList.remove('is-invalid');
        field.classList.add('is-valid');
        return true;
    } else {
        field.classList.remove('is-valid');
        field.classList.add('is-invalid');
        return false;
    }
}

function validateServerForm() {
    const form = document.getElementById('addServerForm');
    const inputs = form.querySelectorAll('input[required], select[required], textarea[required]');
    let isValid = true;
    
    inputs.forEach(input => {
        if (!validateField(input)) {
            isValid = false;
        }
    });
    
    return isValid;
}

async function createServer() {
    if (!validateServerForm()) {
        showToast('Please fill in all required fields correctly.', 'danger');
        return;
    }
    
    try {
        const serverData = {
            name: document.getElementById('serverName').value.trim(),
            hostname: document.getElementById('serverHostname').value.trim() || null,
            description: document.getElementById('serverDescription').value.trim() || null
        };
        
        const result = await apiRequest('/api/servers', {
            method: 'POST',
            body: JSON.stringify(serverData)
        });
        
        showToast(`Server '${result.name}' created successfully!`);
        
        // Close modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('addServerModal'));
        modal.hide();
        
        // Reset form
        document.getElementById('addServerForm').reset();
        document.getElementById('addServerForm').querySelectorAll('.is-valid, .is-invalid')
            .forEach(field => field.classList.remove('is-valid', 'is-invalid'));
        
        // Reload servers
        loadServers(currentPage);
        
    } catch (error) {
        console.error('Failed to create server:', error);
        showToast('Failed to create server. Please try again.', 'danger');
    }
}

async function reevaluateServerLogs(serverId) {
    try {
        const proceed = window.confirm('Reevaluate levels for all logs on this server? Optionally merge SQL continuation lines?');
        if (!proceed) return;
        const merge = window.confirm('Merge SQL continuation lines (recommended for PostgreSQL STATEMENT lines)? Click OK to merge, Cancel to skip.');
        const url = `/api/servers/${serverId}/logs/reevaluate?merge=${merge ? 'true' : 'false'}&dryRun=false`;
        const result = await apiRequest(url, { method: 'POST' });
        showToast(`Reevaluated ${result.scanned} logs. Updated ${result.updatedLevels}, merged ${result.merged}, removed ${result.deleted}.`);
        // Refresh levels and counts
        loadServerLogCount(serverId);
    } catch (e) {
        console.error('Failed to reevaluate logs', e);
        showToast('Failed to reevaluate logs', 'danger');
    }
}

function editServer(id) {
    // TODO: Implement edit functionality
    showToast('Edit functionality coming soon!', 'info');
}

async function deleteServer(id) {
    try {
        await apiRequest(`/api/servers/${id}`, {
            method: 'DELETE'
        });
        
        showToast('Server deleted successfully!');
        
        // Reload current page
        loadServers(currentPage);
        
    } catch (error) {
        console.error('Failed to delete server:', error);
        showToast('Failed to delete server. Please try again.', 'danger');
    }
}

function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text ? text.replace(/[&<>"']/g, function(m) { return map[m]; }) : '';
}
