// Logs page JavaScript

let currentPage = 0;
let currentFilters = {};
let totalPages = 0;
let selectedLevels = [];

async function loadServers() {
    try {
        const response = await apiRequest('/api/servers?size=100');
        const servers = response.content || [];
        
        const select = document.getElementById('serverFilter');
        const defaultOption = select.querySelector('option[value=""]');
        
        // Clear existing options except the default
        select.innerHTML = '';
        select.appendChild(defaultOption);
        
        servers.forEach(server => {
            const option = document.createElement('option');
            option.value = server.id;
            option.textContent = `${server.name}${server.hostname ? ' (' + server.hostname + ')' : ''}`;
            select.appendChild(option);
        });
        // Load available levels for initial selection
        await loadAvailableLevels(select.value || null);
    } catch (error) {
        console.error('Failed to load servers:', error);
    }
}

document.addEventListener('DOMContentLoaded', function() {
    loadServers();
    loadLogs();
    // Prevent dropdown from closing when clicking inside
    const levelMenu = document.getElementById('levelDropdownMenu');
    if (levelMenu) {
        levelMenu.addEventListener('click', function(e) { e.stopPropagation(); });
    }
    
    // Set up filter form submission
    document.getElementById('filterForm').addEventListener('submit', function(e) {
        e.preventDefault();
        applyFilters();
    });
    
    // Set up enter key on search inputs
    const searchInputs = ['sourceFilter', 'searchQuery'];
    searchInputs.forEach(inputId => {
        document.getElementById(inputId).addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                applyFilters();
            }
        });
    });
    
    // Update levels when server selection changes
    document.getElementById('serverFilter').addEventListener('change', async function() {
        await loadAvailableLevels(this.value || null);
        applyFilters();
    });
    
    // Check if server is pre-selected from URL
    const urlParams = new URLSearchParams(window.location.search);
    const serverId = urlParams.get('server');
    if (serverId) {
        setTimeout(() => {
            document.getElementById('serverFilter').value = serverId;
            applyFilters();
        }, 500);
    }
});

async function loadLogs(page = 0, filters = {}) {
    try {
        const container = document.getElementById('logsContainer');
        showLoading(container);
        
        const params = { page, size: 20, ...filters };
        
        // Convert datetime-local values to ISO format
        if (params.from) {
            params.from = new Date(params.from).toISOString();
        }
        if (params.to) {
            params.to = new Date(params.to).toISOString();
        }
        
        let response;
        if (params.serverId) {
            const { serverId, ...rest } = params;
            const qs = toQueryString(rest);
            response = await apiRequest(`/api/servers/${serverId}/logs${qs ? '?' + qs : ''}`);
        } else {
            const qs = toQueryString(params);
            response = await apiRequest(`/api/logs${qs ? '?' + qs : ''}`);
        }
        const logs = response.content || [];
        
        hideLoading(container);
        
        currentPage = page;
        totalPages = response.totalPages || 0;
        currentFilters = filters;
        
        // Update result count
        document.getElementById('resultCount').textContent = 
            `${response.totalElements || 0} total entries`;
        
        if (logs.length === 0) {
            container.innerHTML = `
                <div class="text-center text-muted py-5">
                    <i class="fas fa-search fa-3x mb-3"></i>
                    <h5>No log entries found</h5>
                    <p>Try adjusting your filters or create a new log entry.</p>
                    <a href="/create" class="btn btn-primary">
                        <i class="fas fa-plus"></i> Create Log Entry
                    </a>
                </div>
            `;
        } else {
            renderLogs(logs);
        }
        
        renderPagination();

    } catch (error) {
        console.error('Failed to load logs:', error);
        const container = document.getElementById('logsContainer');
        hideLoading(container);
        container.innerHTML = `
            <div class="text-center text-danger py-5">
                <i class="fas fa-exclamation-triangle fa-3x mb-3"></i>
                <h5>Failed to load logs</h5>
                <p>Please check your connection and try again.</p>
                <button class="btn btn-primary" onclick="loadLogs()">
                    <i class="fas fa-retry"></i> Retry
                </button>
            </div>
        `;
    }
}

function renderLogs(logs) {
    const container = document.getElementById('logsContainer');
    
    const logsHtml = logs.map(log => `
        <div class="log-entry ${levelClass(log.logLevel)}">
            <div class="d-flex justify-content-between align-items-start mb-2">
                <div class="d-flex gap-2 align-items-center">
                    ${formatLogLevel(log.logLevel)}
                    <span class="log-timestamp">
                        <i class="fas fa-clock"></i> ${formatDate(log.timestamp)}
                    </span>
                </div>
                <div class="log-actions">
                    <button class="btn btn-sm btn-outline-primary" onclick="editLog(${log.id})" 
                            data-bs-toggle="tooltip" title="Edit">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger" 
                            onclick="confirmDelete(${log.id}, deleteLogEntry)"
                            data-bs-toggle="tooltip" title="Delete">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
            
            <div class="log-source mb-2">
                <i class="fas fa-code"></i> <strong>Source:</strong> ${escapeHtml(log.source)}
                ${log.server ? `
                    <span class="ms-3">
                        <i class="fas fa-server"></i> <strong>Server:</strong> 
                        <a href="/servers" class="text-decoration-none">${escapeHtml(log.server.name)}</a>
                    </span>
                ` : ''}
            </div>
            
            <div class="log-message mb-2">
                ${escapeHtml(log.message)}
            </div>
            
            <div class="row">
                ${log.category ? `
                    <div class="col-md-6">
                        <small class="text-muted">
                            <i class="fas fa-tag"></i> <strong>Category:</strong> ${escapeHtml(log.category)}
                        </small>
                    </div>
                ` : ''}
            </div>
            
            <small class="text-muted">
                <i class="fas fa-key"></i> ID: ${log.id}
            </small>
        </div>
    `).join('');
    
    container.innerHTML = logsHtml;
    
    // Re-initialize tooltips
    const tooltipTriggerList = container.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltipTriggerList.forEach(function (tooltipTriggerEl) {
        new bootstrap.Tooltip(tooltipTriggerEl);
    });
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
    
    if (startPage > 0) {
        paginationHtml += `
            <li class="page-item">
                <a class="page-link" href="#" onclick="changePage(0)">1</a>
            </li>
        `;
        if (startPage > 1) {
            paginationHtml += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }
    
    for (let i = startPage; i <= endPage; i++) {
        paginationHtml += `
            <li class="page-item ${i === currentPage ? 'active' : ''}">
                <a class="page-link" href="#" onclick="changePage(${i})">${i + 1}</a>
            </li>
        `;
    }
    
    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            paginationHtml += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
        paginationHtml += `
            <li class="page-item">
                <a class="page-link" href="#" onclick="changePage(${totalPages - 1})">${totalPages}</a>
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
        loadLogs(page, currentFilters);
    }
}

function applyFilters() {
    const filters = {
        serverId: document.getElementById('serverFilter').value,
        level: [...selectedLevels],
        source: document.getElementById('sourceFilter').value,
        from: document.getElementById('fromDate').value,
        to: document.getElementById('toDate').value,
        q: document.getElementById('searchQuery').value
    };
    
    // Remove empty filters
    Object.keys(filters).forEach(key => {
        if (!filters[key]) {
            delete filters[key];
        }
    });
    
    loadLogs(0, filters);
}

function clearFilters() {
    document.getElementById('filterForm').reset();
    // clear selected levels UI
    selectedLevels = [];
    updateLevelDropdownLabel();
    const opts = document.querySelectorAll('#levelOptions input.level-option');
    opts.forEach(chk => chk.checked = false);
    const selectAll = document.getElementById('levelSelectAll');
    if (selectAll) { selectAll.checked = false; selectAll.indeterminate = false; }
    loadLogs(0, {});
}

function exportFilteredLogs() {
    const format = 'csv';
    const p = { ...currentFilters, format, limit: '1000' };
    const qs = toQueryString(p);
    const url = `${API_BASE}/export?${qs}`;
    window.open(url, '_blank');
}

function editLog(id) {
    // Load single log by ID to avoid pagination issues
    getLog(id)
        .then(log => {
            if (log) {
                document.getElementById('editId').value = log.id;
                document.getElementById('editLevel').value = log.logLevel;
                document.getElementById('editSource').value = log.source;
                document.getElementById('editMessage').value = log.message;
                document.getElementById('editCategory').value = log.category || '';

                const modal = new bootstrap.Modal(document.getElementById('editModal'));
                modal.show();
            }
        })
        .catch(error => {
            console.error('Failed to load log for editing:', error);
            showToast('Failed to load log entry for editing', 'danger');
        });
}

async function saveLogEntry() {
    try {
        const id = document.getElementById('editId').value;
        const logData = {
            logLevel: document.getElementById('editLevel').value,
            source: document.getElementById('editSource').value,
            message: document.getElementById('editMessage').value,
            category: document.getElementById('editCategory').value || null,
            timestamp: new Date().toISOString() // Keep current timestamp for updates
        };
        
        await updateLog(id, logData);
        
        showToast('Log entry updated successfully!');
        
        // Close modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('editModal'));
        modal.hide();
        
        // Reload current page
        loadLogs(currentPage, currentFilters);
        
    } catch (error) {
        console.error('Failed to update log:', error);
        showToast('Failed to update log entry', 'danger');
    }
}

async function deleteLogEntry(id) {
    try {
        await deleteLog(id);
        showToast('Log entry deleted successfully!');
        
        // Reload current page (or go to previous page if current page becomes empty)
        loadLogs(currentPage, currentFilters);
        
    } catch (error) {
        console.error('Failed to delete log:', error);
        showToast('Failed to delete log entry', 'danger');
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

function levelClass(level) {
    if (!level) return '';
    const info = (typeof httpStatusInfo === 'function') ? httpStatusInfo(String(level)) : null;
    if (info) {
        return `level-http-${info.category}`;
    }
    return `level-${level}`;
}

function toQueryString(params) {
    const sp = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
        if (v == null || v === '' || (Array.isArray(v) && v.length === 0)) return;
        if (k === 'level' && Array.isArray(v)) {
            v.forEach(val => sp.append('level', val));
        } else {
            sp.append(k, v);
        }
    });
    return sp.toString();
}

async function loadAvailableLevels(serverId) {
    try {
        const container = document.getElementById('levelOptions');
        const prev = [...selectedLevels];
        container.innerHTML = '';
        let levels;
        if (serverId) {
            levels = await apiRequest(`/api/servers/${serverId}/log-levels`);
        } else {
            levels = await apiRequest(`/api/logs/levels`);
        }
        const fragment = document.createDocumentFragment();
        (levels || []).forEach(level => {
            const id = `level-${level.replace(/[^a-zA-Z0-9_-]/g, '_')}`;
            const div = document.createElement('div');
            div.className = 'form-check ms-1';
            div.innerHTML = `
                <input class="form-check-input level-option" type="checkbox" value="${level}" id="${id}">
                <label class="form-check-label" for="${id}">${level}</label>
            `;
            fragment.appendChild(div);
        });
        container.appendChild(fragment);

        // Restore selections where possible
        selectedLevels = prev.filter(v => (levels || []).includes(v));
        selectedLevels.forEach(v => {
            const el = document.querySelector(`#levelOptions input.level-option[value="${CSS.escape(v)}"]`);
            if (el) el.checked = true;
        });
        wireLevelEvents();
        updateSelectAllState();
        updateLevelDropdownLabel();
    } catch (e) {
        console.error('Failed to load levels', e);
    }
}

function wireLevelEvents() {
    const opts = document.querySelectorAll('#levelOptions input.level-option');
    opts.forEach(chk => {
        chk.addEventListener('change', () => {
            const val = chk.value;
            if (chk.checked) {
                if (!selectedLevels.includes(val)) selectedLevels.push(val);
            } else {
                selectedLevels = selectedLevels.filter(v => v !== val);
            }
            updateSelectAllState();
            updateLevelDropdownLabel();
        });
    });
    const selectAll = document.getElementById('levelSelectAll');
    if (selectAll) {
        selectAll.addEventListener('change', () => {
            const all = document.querySelectorAll('#levelOptions input.level-option');
            selectedLevels = [];
            all.forEach(chk => {
                chk.checked = selectAll.checked;
                if (selectAll.checked) selectedLevels.push(chk.value);
            });
            selectAll.indeterminate = false;
            updateLevelDropdownLabel();
        });
    }
}

function updateSelectAllState() {
    const selectAll = document.getElementById('levelSelectAll');
    const all = document.querySelectorAll('#levelOptions input.level-option');
    const checked = document.querySelectorAll('#levelOptions input.level-option:checked');
    if (!selectAll) return;
    if (checked.length === 0) { selectAll.checked = false; selectAll.indeterminate = false; return; }
    if (checked.length === all.length) { selectAll.checked = true; selectAll.indeterminate = false; return; }
    selectAll.checked = false; selectAll.indeterminate = true;
}

function updateLevelDropdownLabel() {
    const btn = document.getElementById('levelDropdown');
    if (!btn) return;
    if (!selectedLevels || selectedLevels.length === 0) {
        btn.textContent = 'All Levels';
    } else {
        btn.textContent = `${selectedLevels.length} selected`;
    }
}
