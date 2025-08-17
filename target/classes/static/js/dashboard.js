// Dashboard-specific JavaScript

let logLevelChart;

document.addEventListener('DOMContentLoaded', function() {
    loadDashboardData();
    loadRecentLogs();
    initializeLogLevelChart();
});

async function loadDashboardData() {
    try {
        // Load statistics
        const allLogs = await fetchLogs({ size: 1000 });
        const serversResponse = await apiRequest('/api/servers?size=1000');
        
        // Calculate statistics
        const totalLogs = allLogs.totalElements || 0;
        const totalServers = serversResponse.totalElements || 0;
        const errorLogs = allLogs.content?.filter(log => 
            log.logLevel === 'ERROR' || log.logLevel === 'FATAL'
        ).length || 0;
        
        // Update dashboard cards
        document.getElementById('totalLogs').textContent = totalLogs;
        document.getElementById('totalServers').textContent = totalServers;
        document.getElementById('errorLogs').textContent = errorLogs;
        // Resolved card removed; no status tracking
        
        // Update chart data
        updateLogLevelChart(allLogs.content || []);
        
    } catch (error) {
        console.error('Failed to load dashboard data:', error);
        showToast('Failed to load dashboard data', 'danger');
    }
}

async function loadRecentLogs() {
    try {
        const recentLogsContainer = document.getElementById('recentLogs');
        showLoading(recentLogsContainer);
        
        const response = await fetchLogs({ size: 10, page: 0 });
        const logs = response.content || [];
        
        hideLoading(recentLogsContainer);
        
        if (logs.length === 0) {
            recentLogsContainer.innerHTML = `
                <div class="text-center text-muted">
                    <i class="fas fa-inbox fa-2x mb-2"></i>
                    <p>No log entries found</p>
                </div>
            `;
            return;
        }
        
        const logsHtml = logs.map(log => `
            <div class="recent-log-item d-flex align-items-start">
                <div class="recent-log-level me-3">
                    ${formatLogLevel(log.logLevel)}
                </div>
                <div class="flex-grow-1">
                    <p class="recent-log-message mb-1">${escapeHtml(log.message)}</p>
                    <div class="d-flex justify-content-between align-items-center">
                        <small class="text-muted">
                            <i class="fas fa-code"></i> ${escapeHtml(log.source)}
                        </small>
                        <small class="recent-log-time">
                            <i class="fas fa-clock"></i> ${formatDate(log.timestamp)}
                        </small>
                    </div>
                </div>
            </div>
        `).join('');
        
        recentLogsContainer.innerHTML = logsHtml;
        
    } catch (error) {
        console.error('Failed to load recent logs:', error);
        const recentLogsContainer = document.getElementById('recentLogs');
        hideLoading(recentLogsContainer);
        recentLogsContainer.innerHTML = `
            <div class="text-center text-danger">
                <i class="fas fa-exclamation-triangle fa-2x mb-2"></i>
                <p>Failed to load recent logs</p>
                <button class="btn btn-sm btn-outline-primary" onclick="loadRecentLogs()">
                    <i class="fas fa-retry"></i> Retry
                </button>
            </div>
        `;
    }
}

function initializeLogLevelChart() {
    const ctx = document.getElementById('logLevelChart').getContext('2d');
    logLevelChart = new Chart(ctx, {
        type: 'doughnut',
        data: { labels: [], datasets: [{ data: [], backgroundColor: [], borderWidth: 2, borderColor: '#fff' }] },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'bottom', labels: { padding: 10, usePointStyle: true } },
                tooltip: { callbacks: { label: (ctx) => `${ctx.label || ''}: ${ctx.parsed || 0}` } }
            }
        }
    });
}

function updateLogLevelChart(logs) {
    const counts = new Map();
    const inc = (k) => counts.set(k, (counts.get(k) || 0) + 1);
    logs.forEach(log => {
        const lvl = (log.logLevel || '').toString();
        if (/^\d{3}$/.test(lvl)) {
            const cat = `HTTP ${lvl[0]}xx`;
            inc(cat);
        } else {
            inc(lvl.toUpperCase());
        }
    });
    const order = [ 'HTTP 5xx','HTTP 4xx','HTTP 3xx','HTTP 2xx','ERROR','FATAL','WARN','INFO','LOG','DEBUG','TRACE' ];
    const colors = {
        'HTTP 5xx':'#dc3545','HTTP 4xx':'#ffc107','HTTP 3xx':'#0dcaf0','HTTP 2xx':'#198754',
        'ERROR':'#dc3545','FATAL':'#dc3545','WARN':'#ffc107','INFO':'#0dcaf0','LOG':'#6c757d','DEBUG':'#6c757d','TRACE':'#adb5bd'
    };
    const entries = Array.from(counts.entries());
    entries.sort((a,b) => {
        const ia = order.indexOf(a[0]);
        const ib = order.indexOf(b[0]);
        if (ia !== -1 && ib !== -1) return ia - ib;
        if (ia !== -1) return -1;
        if (ib !== -1) return 1;
        return b[1] - a[1];
    });
    const labels = entries.map(e => e[0]);
    const data = entries.map(e => e[1]);
    const bg = labels.map(l => colors[l] || '#adb5bd');
    logLevelChart.data.labels = labels;
    logLevelChart.data.datasets[0].data = data;
    logLevelChart.data.datasets[0].backgroundColor = bg;
    logLevelChart.update();
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

// Refresh dashboard data every 30 seconds
setInterval(function() {
    loadDashboardData();
    loadRecentLogs();
}, 30000);
