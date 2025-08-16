// Create log page JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // Set current timestamp as default
    setCurrentTimestamp();
    
    // Set up form submission
    document.getElementById('createForm').addEventListener('submit', function(e) {
        e.preventDefault();
        createLogEntry();
    });
    
    // Set up form validation
    setupFormValidation();
});

function setCurrentTimestamp() {
    const now = new Date();
    // Format for datetime-local input
    const timestamp = now.getFullYear() + '-' +
        String(now.getMonth() + 1).padStart(2, '0') + '-' +
        String(now.getDate()).padStart(2, '0') + 'T' +
        String(now.getHours()).padStart(2, '0') + ':' +
        String(now.getMinutes()).padStart(2, '0');
    
    document.getElementById('timestamp').value = timestamp;
}

function setupFormValidation() {
    const form = document.getElementById('createForm');
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

function validateForm() {
    const form = document.getElementById('createForm');
    const inputs = form.querySelectorAll('input[required], select[required], textarea[required]');
    let isValid = true;
    
    inputs.forEach(input => {
        if (!validateField(input)) {
            isValid = false;
        }
    });
    
    return isValid;
}

async function createLogEntry() {
    if (!validateForm()) {
        showToast('Please fill in all required fields correctly.', 'danger');
        return;
    }
    
    try {
        const submitButton = document.querySelector('#createForm button[type="submit"]');
        const originalText = submitButton.innerHTML;
        
        // Show loading state
        submitButton.disabled = true;
        submitButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Creating...';
        
        const timestamp = document.getElementById('timestamp').value;
        
        const logData = {
            logLevel: document.getElementById('logLevel').value,
            status: document.getElementById('status').value,
            source: document.getElementById('source').value.trim(),
            message: document.getElementById('message').value.trim(),
            username: document.getElementById('username').value.trim() || null,
            category: document.getElementById('category').value.trim() || null,
            timestamp: timestamp ? new Date(timestamp).toISOString() : new Date().toISOString()
        };
        
        const result = await createLog(logData);
        
        // Reset button state
        submitButton.disabled = false;
        submitButton.innerHTML = originalText;
        
        // Show success modal
        document.getElementById('createdLogId').textContent = result.id;
        const modal = new bootstrap.Modal(document.getElementById('successModal'));
        modal.show();
        
        // Reset form
        resetForm();
        
    } catch (error) {
        console.error('Failed to create log:', error);
        
        // Reset button state
        const submitButton = document.querySelector('#createForm button[type="submit"]');
        submitButton.disabled = false;
        submitButton.innerHTML = '<i class="fas fa-save"></i> Create Log Entry';
        
        showToast('Failed to create log entry. Please try again.', 'danger');
    }
}

function resetForm() {
    const form = document.getElementById('createForm');
    form.reset();
    
    // Reset validation classes
    const fields = form.querySelectorAll('.is-valid, .is-invalid');
    fields.forEach(field => {
        field.classList.remove('is-valid', 'is-invalid');
    });
    
    // Set defaults
    document.getElementById('logLevel').value = 'INFO';
    document.getElementById('status').value = 'OPEN';
    setCurrentTimestamp();
}

function createAnother() {
    const modal = bootstrap.Modal.getInstance(document.getElementById('successModal'));
    modal.hide();
    
    // Focus on the first field
    document.getElementById('logLevel').focus();
}

function loadTemplate(type) {
    const templates = {
        error: {
            logLevel: 'ERROR',
            status: 'OPEN',
            source: 'com.example.service.UserService',
            message: 'Failed to process user request: Database connection timeout',
            category: 'database'
        },
        warning: {
            logLevel: 'WARN',
            status: 'OPEN',
            source: 'com.example.controller.AuthController',
            message: 'User authentication attempt with invalid credentials',
            category: 'security'
        },
        info: {
            logLevel: 'INFO',
            status: 'RESOLVED',
            source: 'com.example.service.EmailService',
            message: 'Email notification sent successfully to user',
            category: 'notification'
        }
    };
    
    const template = templates[type];
    if (template) {
        document.getElementById('logLevel').value = template.logLevel;
        document.getElementById('status').value = template.status;
        document.getElementById('source').value = template.source;
        document.getElementById('message').value = template.message;
        document.getElementById('category').value = template.category;
        
        // Show toast
        showToast(`${type.charAt(0).toUpperCase() + type.slice(1)} template loaded!`);
        
        // Focus on message field for editing
        document.getElementById('message').focus();
        document.getElementById('message').select();
    }
}

// Auto-resize textarea
document.addEventListener('DOMContentLoaded', function() {
    const messageTextarea = document.getElementById('message');
    
    messageTextarea.addEventListener('input', function() {
        this.style.height = 'auto';
        this.style.height = this.scrollHeight + 'px';
    });
});
