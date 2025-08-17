// Upload page JavaScript

document.addEventListener('DOMContentLoaded', function() {
    loadServers();
    setupUploadForm();
    
    // Check if server is pre-selected from URL
    const urlParams = new URLSearchParams(window.location.search);
    const serverId = urlParams.get('server');
    if (serverId) {
        setTimeout(() => {
            document.getElementById('serverId').value = serverId;
        }, 500);
    }
});

async function loadServers() {
    try {
        const response = await apiRequest('/api/servers?size=100');
        const servers = response.content || [];
        
        const select = document.getElementById('serverId');
        select.innerHTML = '<option value="">Select a server...</option>';
        
        servers.forEach(server => {
            const option = document.createElement('option');
            option.value = server.id;
            option.textContent = `${server.name}${server.hostname ? ' (' + server.hostname + ')' : ''}`;
            select.appendChild(option);
        });
        
        if (servers.length === 0) {
            select.innerHTML = '<option value="">No servers available - Create one first</option>';
            select.disabled = true;
        }
        
    } catch (error) {
        console.error('Failed to load servers:', error);
        const select = document.getElementById('serverId');
        select.innerHTML = '<option value="">Failed to load servers</option>';
        select.disabled = true;
    }
}

function setupUploadForm() {
    const form = document.getElementById('uploadForm');
    const fileInput = document.getElementById('logFiles');
    
    // File selection handler
    fileInput.addEventListener('change', function() {
        updateFilePreview();
        validateField(this);
    });
    
    // Form submission handler
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        uploadFiles();
    });
    
    // Server selection validation
    document.getElementById('serverId').addEventListener('change', function() {
        validateField(this);
    });
}

function updateFilePreview() {
    const fileInput = document.getElementById('logFiles');
    const preview = document.getElementById('filePreview');
    const fileList = document.getElementById('fileList');
    
    if (fileInput.files.length === 0) {
        preview.style.display = 'none';
        return;
    }
    
    preview.style.display = 'block';
    
    const files = Array.from(fileInput.files);
    const filesHtml = files.map((file, index) => `
        <div class="list-group-item d-flex justify-content-between align-items-center">
            <div>
                <i class="fas fa-file-alt text-primary me-2"></i>
                <strong>${escapeHtml(file.name)}</strong>
                <small class="text-muted ms-2">(${formatFileSize(file.size)})</small>
            </div>
            <button type="button" class="btn btn-sm btn-outline-danger" onclick="removeFile(${index})">
                <i class="fas fa-times"></i>
            </button>
        </div>
    `).join('');
    
    fileList.innerHTML = filesHtml;
}

function removeFile(index) {
    const fileInput = document.getElementById('logFiles');
    const dt = new DataTransfer();
    
    Array.from(fileInput.files).forEach((file, i) => {
        if (i !== index) {
            dt.items.add(file);
        }
    });
    
    fileInput.files = dt.files;
    updateFilePreview();
    validateField(fileInput);
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

function validateUploadForm() {
    const serverId = document.getElementById('serverId');
    const logFiles = document.getElementById('logFiles');
    
    let isValid = true;
    
    if (!validateField(serverId)) {
        isValid = false;
    }
    
    if (!validateField(logFiles)) {
        isValid = false;
    }
    
    return isValid;
}

async function uploadFiles() {
    if (!validateUploadForm()) {
        showToast('Please fix the validation errors before uploading.', 'danger');
        return;
    }
    
    const serverId = document.getElementById('serverId').value;
    const fileInput = document.getElementById('logFiles');
    const files = fileInput.files;
    
    if (files.length === 0) {
        showToast('Please select at least one file to upload.', 'danger');
        return;
    }
    
    try {
        // Show progress
        showUploadProgress();
        
        const formData = new FormData();
        Array.from(files).forEach(file => {
            formData.append('files', file);
        });
        
        updateProgress(10, 'Uploading files...');
        
        const response = await fetch(`/api/servers/${serverId}/logs/upload`, {
            method: 'POST',
            body: formData
        });
        
        updateProgress(90, 'Processing files...');
        
        if (!response.ok) {
            let detail = '';
            try {
                const text = await response.text();
                detail = text?.slice(0, 300) || '';
            } catch (_) {}
            throw new Error(`Upload failed: ${response.status} ${detail}`);
        }
        
        const result = await response.json();
        
        updateProgress(100, 'Upload complete!');
        
        // Hide progress after a short delay
        setTimeout(() => {
            hideUploadProgress();
            showUploadResults(result);
        }, 1000);
        
    } catch (error) {
        console.error('Upload failed:', error);
        hideUploadProgress();
        showToast(`Upload failed. ${error?.message || 'Please try again.'}`, 'danger');
    }
}

function showUploadProgress() {
    document.getElementById('uploadProgress').style.display = 'block';
    document.getElementById('uploadBtn').disabled = true;
}

function hideUploadProgress() {
    document.getElementById('uploadProgress').style.display = 'none';
    document.getElementById('uploadBtn').disabled = false;
}

function updateProgress(percent, text) {
    const progressBar = document.getElementById('progressBar');
    const progressText = document.getElementById('progressText');
    
    progressBar.style.width = percent + '%';
    progressBar.setAttribute('aria-valuenow', percent);
    progressBar.textContent = percent + '%';
    progressText.textContent = text;
}

function showUploadResults(result) {
    const resultsDiv = document.getElementById('uploadResults');
    const contentDiv = document.getElementById('resultsContent');
    
    const totalFiles = result.results.length;
    const totalEntries = result.total;
    
    let resultsHtml = `
        <div class="alert alert-success">
            <h6><i class="fas fa-check-circle"></i> Upload Successful!</h6>
            <p class="mb-0">
                Processed <strong>${totalFiles}</strong> file${totalFiles !== 1 ? 's' : ''} 
                and imported <strong>${totalEntries}</strong> log entr${totalEntries !== 1 ? 'ies' : 'y'}.
            </p>
        </div>
    `;
    
    if (result.results.length > 0) {
        resultsHtml += `
            <h6>File Details:</h6>
            <div class="list-group">
        `;
        
        result.results.forEach(fileResult => {
            resultsHtml += `
                <div class="list-group-item">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <i class="fas fa-file-alt text-primary me-2"></i>
                            <strong>${escapeHtml(fileResult.file)}</strong>
                        </div>
                        <span class="badge bg-success">
                            ${fileResult.imported} entr${fileResult.imported !== 1 ? 'ies' : 'y'}
                        </span>
                    </div>
                </div>
            `;
        });
        
        resultsHtml += '</div>';
    }
    
    contentDiv.innerHTML = resultsHtml;
    resultsDiv.style.display = 'block';
    
    // Scroll to results
    resultsDiv.scrollIntoView({ behavior: 'smooth' });
}

function resetUploadForm() {
    document.getElementById('uploadForm').reset();
    document.getElementById('filePreview').style.display = 'none';
    document.getElementById('uploadResults').style.display = 'none';
    hideUploadProgress();
    
    // Reset validation classes
    document.getElementById('uploadForm').querySelectorAll('.is-valid, .is-invalid')
        .forEach(field => field.classList.remove('is-valid', 'is-invalid'));
}

function uploadAnother() {
    document.getElementById('uploadResults').style.display = 'none';
    resetUploadForm();
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
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
