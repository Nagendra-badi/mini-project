// Global State
let currentUser = null;
let currentDocumentsPage = 0;
let documentsTotalPages = 1;
let selectedUploadFile = null;
let fileTypeChart = null;
let categoryChart = null;

// ==========================================
// Authentication Page Logic (Login & Register)
// ==========================================
function initAuthPages(page) {
    const errorAlert = document.getElementById('error-alert');
    const successAlert = document.getElementById('success-alert');

    // Theme toggle matching dashboard
    const themeToggle = document.getElementById('theme-toggle');
    const icon = themeToggle.querySelector('i');
    if (localStorage.getItem('theme') === 'dark') {
        document.body.classList.add('dark-mode');
        icon.classList.replace('fa-moon', 'fa-sun');
    }
    themeToggle.addEventListener('click', () => {
        document.body.classList.toggle('dark-mode');
        if (document.body.classList.contains('dark-mode')) {
            localStorage.setItem('theme', 'dark');
            icon.classList.replace('fa-moon', 'fa-sun');
        } else {
            localStorage.setItem('theme', 'light');
            icon.classList.replace('fa-sun', 'fa-moon');
        }
    });

    if (page === 'login') {
        const loginForm = document.getElementById('login-form');
        const forgotPasswordLink = document.getElementById('forgot-password-link');
        const forgotPasswordForm = document.getElementById('forgot-password-form');
        const forgotModal = new bootstrap.Modal(document.getElementById('forgotPasswordModal'));

        // Handle Login Submission
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            errorAlert.classList.add('d-none');
            const username = document.getElementById('username').value.trim();
            const password = document.getElementById('password').value;

            try {
                const response = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password })
                });
                const data = await response.json();
                if (response.ok) {
                    // Successful login
                    window.location.href = 'dashboard.html';
                } else {
                    errorAlert.textContent = data.error || 'Invalid credentials.';
                    errorAlert.classList.remove('d-none');
                }
            } catch (err) {
                errorAlert.textContent = 'Server connection error.';
                errorAlert.classList.remove('d-none');
            }
        });

        // Trigger Forgot Password Modal
        forgotPasswordLink.addEventListener('click', (e) => {
            e.preventDefault();
            forgotModal.show();
        });

        // Handle Forgot Password Form Submission
        forgotPasswordForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const forgotError = document.getElementById('forgot-error-alert');
            const forgotSuccess = document.getElementById('forgot-success-alert');
            forgotError.classList.add('d-none');
            forgotSuccess.classList.add('d-none');

            const username = document.getElementById('forgot-username').value.trim();
            const email = document.getElementById('forgot-email').value.trim();
            const newPassword = document.getElementById('forgot-new-password').value;

            try {
                const response = await fetch('/api/auth/forgot-password', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, email, newPassword })
                });
                const data = await response.json();
                if (response.ok) {
                    forgotSuccess.textContent = data.message;
                    forgotSuccess.classList.remove('d-none');
                    setTimeout(() => {
                        forgotModal.hide();
                        forgotPasswordForm.reset();
                    }, 2000);
                } else {
                    forgotError.textContent = data.error || 'Password reset failed.';
                    forgotError.classList.remove('d-none');
                }
            } catch (err) {
                forgotError.textContent = 'Connection error.';
                forgotError.classList.remove('d-none');
            }
        });
    }

    if (page === 'register') {
        const registerForm = document.getElementById('register-form');

        // Handle Registration
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            errorAlert.classList.add('d-none');
            successAlert.classList.add('d-none');

            const username = document.getElementById('reg-username').value.trim();
            const email = document.getElementById('reg-email').value.trim();
            const fullName = document.getElementById('reg-fullname').value.trim();
            const phone = document.getElementById('reg-phone').value.trim();
            const password = document.getElementById('reg-password').value;
            const confirm = document.getElementById('reg-confirm-password').value;
            const role = 'USER';
            
            if (password !== confirm) {
                errorAlert.textContent = 'Passwords do not match.';
                errorAlert.classList.remove('d-none');
                return;
            }
            
            try {
                const response = await fetch('/api/auth/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, email, fullName, phone, password, role })
                });
                const data = await response.json();
                if (response.ok) {
                    successAlert.textContent = data.message + ' Redirecting to Login...';
                    successAlert.classList.remove('d-none');
                    setTimeout(() => {
                        window.location.href = 'login.html';
                    }, 2000);
                } else {
                    errorAlert.textContent = data.error || 'Registration failed.';
                    errorAlert.classList.remove('d-none');
                }
            } catch (err) {
                errorAlert.textContent = 'Connection error.';
                errorAlert.classList.remove('d-none');
            }
        });
    }
}

// ==========================================
// Dashboard Workspace Page Logic
// ==========================================
async function initDashboard() {
    // 1. Verify User Login status & details
    try {
        const response = await fetch('/api/auth/profile');
        if (!response.ok) {
            window.location.href = 'login.html';
            return;
        }
        currentUser = await response.json();
        setupUserUI(currentUser);
    } catch (err) {
        window.location.href = 'login.html';
        return;
    }

    // 2. Setup Nav Links Tab Switcher
    const navLinks = document.querySelectorAll('.nav-link-item');
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const tabId = link.id.replace('nav-', '');
            switchTab(tabId);
        });
    });

    // 3. Setup Dark Mode Trigger
    const themeToggle = document.getElementById('theme-toggle');
    const icon = themeToggle.querySelector('i');
    if (localStorage.getItem('theme') === 'dark') {
        document.body.classList.add('dark-mode');
        icon.classList.replace('fa-moon', 'fa-sun');
    }
    themeToggle.addEventListener('click', () => {
        document.body.classList.toggle('dark-mode');
        if (document.body.classList.contains('dark-mode')) {
            localStorage.setItem('theme', 'dark');
            icon.classList.replace('fa-moon', 'fa-sun');
        } else {
            localStorage.setItem('theme', 'light');
            icon.classList.replace('fa-sun', 'fa-moon');
        }
        // Redraw charts for better color schemes on dark mode
        renderCharts(true);
    });

    // 4. Setup Logout Button
    document.getElementById('logout-btn').addEventListener('click', async () => {
        await fetch('/api/auth/logout', { method: 'POST' });
        window.location.href = 'login.html';
    });

    // 5. Initialize Drag & Drop Handlers
    initDragAndDrop();

    // 6. Setup Document Search Filters
    document.getElementById('search-filename').addEventListener('input', debounce(() => fetchUserDocuments(0), 400));
    document.getElementById('filter-category').addEventListener('change', () => fetchUserDocuments(0));
    document.getElementById('filter-type').addEventListener('change', () => fetchUserDocuments(0));
    document.getElementById('filter-date').addEventListener('change', () => fetchUserDocuments(0));

    // 7. Load Categories & Global Data
    await fetchCategories();
    await loadDashboardStats();
    await fetchUserDocuments(0);

    // If Admin, load Admin views
    if (currentUser.role === 'ADMIN') {
        await fetchAdminUsers();
        await fetchAdminCategories();
        await fetchAdminLogs();
    }

    // Stop media playback when preview modal is closed
    const modalEl = document.getElementById('docPreviewModal');
    if (modalEl) {
        modalEl.addEventListener('hidden.bs.modal', () => {
            const frameContainer = document.getElementById('preview-frame-container');
            if (frameContainer) {
                const mediaElements = frameContainer.querySelectorAll('video, audio');
                mediaElements.forEach(media => media.pause());
                frameContainer.innerHTML = '';
            }
        });
    }
}

// UI Configuration based on Logged-in User Roles
function setupUserUI(user) {
    document.getElementById('user-display-name').textContent = user.fullName || user.username;
    document.getElementById('user-display-role').textContent = user.role === 'ADMIN' ? 'Administrator' : 'Standard User';
    document.getElementById('user-avatar').textContent = (user.fullName || user.username).substring(0, 1).toUpperCase();

    // Populate profile forms
    document.getElementById('profile-username').value = user.username;
    document.getElementById('profile-fullname').value = user.fullName || '';
    document.getElementById('profile-email').value = user.email || '';
    document.getElementById('profile-phone').value = user.phone || '';

    if (user.role === 'ADMIN') {
        const adminElements = document.querySelectorAll('.admin-only');
        adminElements.forEach(el => el.classList.remove('d-none'));
    }

    // Setup forms listeners
    document.getElementById('profile-form').addEventListener('submit', handleProfileUpdate);
    document.getElementById('change-password-form').addEventListener('submit', handleChangePassword);
    document.getElementById('edit-doc-form').addEventListener('submit', handleEditDocSubmit);
    document.getElementById('rename-doc-form').addEventListener('submit', handleRenameDocSubmit);
    document.getElementById('create-category-form').addEventListener('submit', handleCreateCategorySubmit);
}

// Switch dashboard view tabs
function switchTab(tabId) {
    // Nav links
    const navLinks = document.querySelectorAll('.nav-link-item');
    navLinks.forEach(link => {
        if (link.id === `nav-${tabId}`) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });

    // Content sections
    const sections = document.querySelectorAll('.dashboard-page');
    sections.forEach(sec => {
        if (sec.id === `page-${tabId}`) {
            sec.classList.remove('d-none');
        } else {
            sec.classList.add('d-none');
        }
    });

    // Reload tab specific data
    if (tabId === 'dashboard') {
        loadDashboardStats();
    } else if (tabId === 'documents') {
        fetchUserDocuments(currentDocumentsPage);
    } else if (tabId === 'admin-users') {
        fetchAdminUsers();
    } else if (tabId === 'admin-categories') {
        fetchAdminCategories();
    } else if (tabId === 'admin-logs') {
        fetchAdminLogs();
    } else if (tabId === 'admin-reports') {
        fetchAdminUserReports();
    }
}

// ==========================================
// Dashboard Statistics & Analytics (Chart.js)
// ==========================================
async function loadDashboardStats() {
    try {
        let statsUrl = currentUser.role === 'ADMIN' ? '/api/admin/statistics' : '/api/documents?size=1000';
        const response = await fetch(statsUrl);
        if (response.status === 401) {
            window.location.href = 'login.html';
            return;
        }
        const data = await response.json();
        
        if (currentUser.role === 'ADMIN') {
            document.getElementById('stat-total-docs').textContent = data.totalDocuments;
            document.getElementById('stat-storage-used').textContent = formatBytes(data.storageUsed);
            document.getElementById('stat-recent-uploads').textContent = data.recentUploads.length;
            document.getElementById('stat-total-users').textContent = data.totalUsers;

            renderRecentUploadsTable(data.recentUploads);
            renderChartsFromData(data.fileTypeStats, data.categoryStats);
        } else {
            const docsList = data.content || [];

            document.getElementById('stat-total-docs').textContent = docsList.length;
            let totalBytes = docsList.reduce((acc, doc) => acc + (doc.fileSize || 0), 0);
            document.getElementById('stat-storage-used').textContent = formatBytes(totalBytes);

            // Calculate active timelines
            let recentDocs = [...docsList].sort((a,b) => new Date(b.uploadDate) - new Date(a.uploadDate)).slice(0, 5);
            document.getElementById('stat-recent-uploads').textContent = recentDocs.length;

            renderRecentUploadsTable(recentDocs.map(d => ({
                id: d.id,
                originalName: d.originalName,
                uploadedBy: d.uploadedBy.username,
                uploadDate: d.uploadDate,
                fileSize: d.fileSize
            })));

            // Calculate aggregations
            let typeStats = {};
            let catStats = {};
            docsList.forEach(d => {
                let type = d.fileType || 'UNKNOWN';
                typeStats[type] = (typeStats[type] || 0) + 1;

                let cat = d.category ? d.category.name : 'Uncategorized';
                catStats[cat] = (catStats[cat] || 0) + 1;
            });

            renderChartsFromData(typeStats, catStats);
        }
    } catch (err) {
        console.error('Failed to load statistics: ', err);
    }
}

function renderRecentUploadsTable(uploads) {
    const tbody = document.getElementById('recent-uploads-table');
    tbody.innerHTML = '';
    if (uploads.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-secondary">No files uploaded recently</td></tr>';
        return;
    }
    uploads.forEach(u => {
        const tr = document.createElement('tr');
        const canDelete = currentUser.role === 'ADMIN' || currentUser.username === u.uploadedBy;
        const deleteButton = canDelete 
            ? `<button class="btn btn-sm btn-outline-danger" onclick="deleteDocument(${u.id})" title="Delete"><i class="fa-solid fa-trash-can"></i></button>`
            : `<button class="btn btn-sm btn-outline-danger" disabled title="Delete"><i class="fa-solid fa-trash-can"></i></button>`;

        tr.innerHTML = `
            <td><span class="fw-bold">${u.originalName}</span></td>
            <td><span class="badge bg-light text-dark">${u.uploadedBy}</span></td>
            <td>${formatBytes(u.fileSize)}</td>
            <td>${formatDate(u.uploadDate)}</td>
            <td class="text-end">
                <div class="d-flex gap-2 justify-content-end">
                    <button class="btn btn-sm btn-outline-primary" onclick="previewDocument(${u.id})" title="Preview & Details"><i class="fa-solid fa-eye"></i></button>
                    ${deleteButton}
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function renderChartsFromData(fileTypeStats, categoryStats) {
    const isDark = document.body.classList.contains('dark-mode');
    const labelColor = isDark ? '#94a3b8' : '#64748b';
    const gridColor = isDark ? 'rgba(71, 85, 105, 0.2)' : 'rgba(226, 232, 240, 0.8)';

    // 1. File Type Chart (Doughnut)
    const typeCanvas = document.getElementById('chart-file-types');
    if (fileTypeChart) fileTypeChart.destroy();
    
    const typeLabels = Object.keys(fileTypeStats);
    const typeValues = Object.values(fileTypeStats);

    if (typeLabels.length === 0) {
        // Placeholder
        typeLabels.push('No Files');
        typeValues.push(1);
    }

    fileTypeChart = new Chart(typeCanvas, {
        type: 'doughnut',
        data: {
            labels: typeLabels,
            datasets: [{
                data: typeValues,
                backgroundColor: ['#6366f1', '#10b981', '#ef4444', '#f59e0b', '#06b6d4', '#a855f7', '#64748b', '#0284c7'],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { color: labelColor, boxWidth: 12, font: { family: 'Inter' } }
                }
            }
        }
    });

    // 2. Category Chart (Bar Chart)
    const catCanvas = document.getElementById('chart-categories');
    if (categoryChart) categoryChart.destroy();

    const catLabels = Object.keys(categoryStats);
    const catValues = Object.values(categoryStats);

    if (catLabels.length === 0) {
        catLabels.push('No Categories');
        catValues.push(0);
    }

    categoryChart = new Chart(catCanvas, {
        type: 'bar',
        data: {
            labels: catLabels,
            datasets: [{
                label: 'Files in Folder',
                data: catValues,
                backgroundColor: '#6366f1',
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: {
                    grid: { color: gridColor },
                    ticks: { color: labelColor, stepSize: 1 }
                },
                x: {
                    grid: { display: false },
                    ticks: { color: labelColor }
                }
            }
        }
    });
}

// ==========================================
// Document Management Module (Search, Download, Edit)
// ==========================================
async function fetchUserDocuments(page) {
    currentDocumentsPage = page;
    const query = document.getElementById('search-filename').value.trim();
    const category = document.getElementById('filter-category').value;
    const type = document.getElementById('filter-type').value;
    const date = document.getElementById('filter-date').value;

    let url = `/api/documents?page=${page}&size=10`;
    if (query) url += `&query=${encodeURIComponent(query)}`;
    if (category) url += `&category=${encodeURIComponent(category)}`;
    if (type) url += `&type=${encodeURIComponent(type)}`;
    if (date) url += `&date=${encodeURIComponent(date)}`;

    try {
        const response = await fetch(url);
        if (response.status === 401) {
            window.location.href = 'login.html';
            return;
        }
        const data = await response.json();
        renderDocumentsTable(data.content || []);
        renderPagination(data);
    } catch (err) {
        console.error('Failed to fetch documents: ', err);
    }
}

function renderDocumentsTable(docs) {
    const tbody = document.getElementById('documents-table-body');
    tbody.innerHTML = '';

    if (docs.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-secondary py-5">
                    <i class="fa-regular fa-folder-open display-4 mb-3 d-block"></i>
                    No files match your search criteria.
                </td>
            </tr>`;
        return;
    }

    docs.forEach(doc => {
        const tr = document.createElement('tr');
        tr.className = 'file-list-item';
        
        const badgeClass = getFileTypeBadgeClass(doc.fileType);
        const categoryName = doc.category ? doc.category.name : 'Uncategorized';

        tr.innerHTML = `
            <td><span class="badge ${badgeClass} px-2 py-1.5 fw-semibold">${doc.fileType || 'TXT'}</span></td>
            <td>
                <div class="fw-bold">${doc.originalName}</div>
                <div class="x-small text-secondary text-truncate" style="max-width: 300px;">${doc.description || 'No description provided'}</div>
            </td>
            <td><span class="badge bg-light text-dark border border-secondary border-opacity-10">${categoryName}</span></td>
            <td>${formatBytes(doc.fileSize)}</td>
            <td>${formatDate(doc.uploadDate)}</td>
            <td class="text-end">
                <div class="d-flex gap-2 justify-content-end">
                    <button class="btn btn-sm btn-outline-primary" onclick="previewDocument(${doc.id})" title="Preview & Details"><i class="fa-solid fa-eye"></i></button>
                    <a class="btn btn-sm btn-outline-success" href="/api/documents/${doc.id}/download" title="Download"><i class="fa-solid fa-download"></i></a>
                    <button class="btn btn-sm btn-outline-warning" onclick="editDocumentDetails(${doc.id}, '${escapeHtml(doc.description || '')}', ${doc.category ? doc.category.id : 'null'})" title="Edit Metadata"><i class="fa-solid fa-pencil"></i></button>
                    <button class="btn btn-sm btn-outline-info" onclick="renameDocumentModal(${doc.id}, '${escapeHtml(doc.originalName)}')" title="Rename File"><i class="fa-solid fa-signature"></i></button>
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteDocument(${doc.id})" title="Delete"><i class="fa-solid fa-trash-can"></i></button>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function renderPagination(pageData) {
    const totalElements = pageData.totalElements;
    const size = pageData.size;
    const number = pageData.number;
    documentsTotalPages = pageData.totalPages || 1;

    const start = totalElements === 0 ? 0 : (number * size) + 1;
    const end = Math.min((number + 1) * size, totalElements);
    document.getElementById('pagination-info').textContent = `Showing ${start} to ${end} of ${totalElements} entries`;

    const paginationUl = document.getElementById('pagination-controls');
    paginationUl.innerHTML = '';

    // Previous Button
    const prevLi = document.createElement('li');
    prevLi.className = `page-item ${number === 0 ? 'disabled' : ''}`;
    prevLi.innerHTML = `<a class="page-link" href="dashboard.html#" onclick="fetchUserDocuments(${number - 1}); return false;">Previous</a>`;
    paginationUl.appendChild(prevLi);

    // Number Buttons
    for (let i = 0; i < documentsTotalPages; i++) {
        const li = document.createElement('li');
        li.className = `page-item ${number === i ? 'active' : ''}`;
        li.innerHTML = `<a class="page-link" href="dashboard.html#" onclick="fetchUserDocuments(${i}); return false;">${i + 1}</a>`;
        paginationUl.appendChild(li);
    }

    // Next Button
    const nextLi = document.createElement('li');
    nextLi.className = `page-item ${number === documentsTotalPages - 1 || totalElements === 0 ? 'disabled' : ''}`;
    nextLi.innerHTML = `<a class="page-link" href="dashboard.html#" onclick="fetchUserDocuments(${number + 1}); return false;">Next</a>`;
    paginationUl.appendChild(nextLi);
}

// Edit Document Modal helpers
function editDocumentDetails(docId, description, categoryId) {
    document.getElementById('edit-doc-id').value = docId;
    document.getElementById('edit-doc-description').value = description;
    document.getElementById('edit-doc-category').value = categoryId || '';
    const modal = new bootstrap.Modal(document.getElementById('docEditModal'));
    modal.show();
}

async function handleEditDocSubmit(e) {
    e.preventDefault();
    const docId = document.getElementById('edit-doc-id').value;
    const description = document.getElementById('edit-doc-description').value.trim();
    const categoryId = document.getElementById('edit-doc-category').value || null;

    try {
        const response = await fetch(`/api/documents/${docId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ description, categoryId })
        });
        if (response.ok) {
            bootstrap.Modal.getInstance(document.getElementById('docEditModal')).hide();
            fetchUserDocuments(currentDocumentsPage);
            loadDashboardStats();
        } else {
            const data = await response.json();
            alert(data.error || 'Failed to update details.');
        }
    } catch (err) {
        alert('Server connection error.');
    }
}

// Rename Document Modal helpers
function renameDocumentModal(docId, currentName) {
    document.getElementById('rename-doc-id').value = docId;
    document.getElementById('new-doc-name').value = currentName;
    const modal = new bootstrap.Modal(document.getElementById('docRenameModal'));
    modal.show();
}

async function handleRenameDocSubmit(e) {
    e.preventDefault();
    const docId = document.getElementById('rename-doc-id').value;
    const newName = document.getElementById('new-doc-name').value.trim();

    try {
        const response = await fetch(`/api/documents/${docId}/rename`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ newName })
        });
        if (response.ok) {
            bootstrap.Modal.getInstance(document.getElementById('docRenameModal')).hide();
            fetchUserDocuments(currentDocumentsPage);
        } else {
            const data = await response.json();
            alert(data.error || 'Failed to rename document.');
        }
    } catch (err) {
        alert('Server connection error.');
    }
}

// Delete Document
async function deleteDocument(docId) {
    if (!confirm('Are you sure you want to delete this document permanently?')) {
        return;
    }
    try {
        const response = await fetch(`/api/documents/${docId}`, { method: 'DELETE' });
        if (response.ok) {
            fetchUserDocuments(currentDocumentsPage);
            loadDashboardStats();
        } else {
            const data = await response.json();
            alert(data.error || 'Failed to delete file.');
        }
    } catch (err) {
        alert('Server connection error.');
    }
}

// Preview Modal Details builder
async function previewDocument(docId) {
    try {
        const response = await fetch(`/api/documents/${docId}`);
        const doc = await response.json();
        
        document.getElementById('preview-modal-title').textContent = doc.originalName;
        document.getElementById('preview-file-size').textContent = formatBytes(doc.fileSize);
        document.getElementById('preview-file-type').textContent = doc.fileType;
        document.getElementById('preview-upload-date').textContent = formatDate(doc.uploadDate);
        document.getElementById('preview-owner').textContent = doc.uploadedBy.username;
        document.getElementById('preview-description').textContent = doc.description || 'No description provided.';
        
        // Setup direct download button
        const downloadBtn = document.getElementById('btn-modal-download');
        downloadBtn.href = `/api/documents/${doc.id}/download`;

        // Attempt preview based on extension type
        const frameContainer = document.getElementById('preview-frame-container');
        frameContainer.innerHTML = '';
        
        const type = doc.fileType.toLowerCase();
        if (type === 'jpg' || type === 'jpeg' || type === 'png' || type === 'gif') {
            frameContainer.innerHTML = `<img src="${doc.s3Url}?inline=true" class="img-fluid rounded border border-secondary border-opacity-10" alt="Image preview" style="max-height: 400px; object-fit: contain; width: 100%;">`;
        } else if (type === 'pdf') {
            frameContainer.innerHTML = `<iframe src="/api/documents/${doc.id}/download?inline=true" style="width: 100%; height: 400px; border: 0;" class="rounded border border-secondary border-opacity-10"></iframe>`;
        } else if (type === 'mp4' || type === 'webm') {
            frameContainer.innerHTML = `<video src="${doc.s3Url}?inline=true" controls class="w-100 rounded border border-secondary border-opacity-10" style="max-height: 400px; outline: none;"></video>`;
        } else if (type === 'mp3' || type === 'wav') {
            frameContainer.innerHTML = `
                <div class="text-center py-5 bg-light rounded border border-secondary border-opacity-10">
                    <i class="fa-solid fa-music display-1 text-primary mb-4 d-block"></i>
                    <audio src="${doc.s3Url}?inline=true" controls style="width: 85%; outline: none;"></audio>
                </div>`;
        } else if (['txt', 'html', 'css', 'js', 'json', 'xml', 'csv', 'java', 'py', 'sql', 'md'].includes(type)) {
            frameContainer.innerHTML = `
                <div class="text-center p-4 text-secondary">
                    <i class="fa-solid fa-spinner fa-spin display-6 mb-2"></i>
                    <div>Loading text content...</div>
                </div>`;
            fetch(`/api/documents/${doc.id}/download?inline=true`)
                .then(r => r.text())
                .then(text => {
                    frameContainer.innerHTML = `<pre class="bg-dark text-light p-3 rounded small border-0" style="max-height: 400px; overflow-y: auto; text-align: left; white-space: pre-wrap; word-break: break-all; margin: 0; font-family: monospace;"><code>${escapeHtml(text)}</code></pre>`;
                })
                .catch(err => {
                    frameContainer.innerHTML = `<div class="text-danger text-center p-4">Failed to load text content.</div>`;
                });
        } else {
            // No direct inline preview for ZIP/DOCX/XLSX
            frameContainer.innerHTML = `
                <div class="text-secondary text-center p-4">
                    <i class="fa-solid fa-file-arrow-down display-1 mb-3"></i>
                    <div>Direct preview not supported for <strong>.${type.toUpperCase()}</strong> files.</div>
                    <div class="small mt-1 text-muted">Please download the file to inspect contents.</div>
                </div>`;
        }

        const modal = new bootstrap.Modal(document.getElementById('docPreviewModal'));
        modal.show();
    } catch (err) {
        alert('Failed to load file preview details.');
    }
}

// ==========================================
// Drag & Drop File Upload Handler
// ==========================================
function initDragAndDrop() {
    const dropZone = document.getElementById('file-drop-zone');
    const fileInput = document.getElementById('file-input');
    const detailsContainer = document.getElementById('selected-file-details');
    const filenameLabel = document.getElementById('selected-filename');
    const filesizeLabel = document.getElementById('selected-filesize');
    const clearBtn = document.getElementById('btn-clear-file');
    const fileIcon = document.getElementById('file-icon-preview');

    ['dragenter', 'dragover'].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            dropZone.classList.add('drag-over');
        }, false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            dropZone.classList.remove('drag-over');
        }, false);
    });

    dropZone.addEventListener('drop', (e) => {
        const dt = e.dataTransfer;
        const files = dt.files;
        if (files.length > 0) {
            handleSelectedFile(files[0]);
        }
    }, false);

    fileInput.addEventListener('change', (e) => {
        if (fileInput.files.length > 0) {
            handleSelectedFile(fileInput.files[0]);
        }
    });

    clearBtn.addEventListener('click', (e) => {
        e.preventDefault();
        clearFileSelection();
    });

    function handleSelectedFile(file) {
        selectedUploadFile = file;
        filenameLabel.textContent = file.name;
        filesizeLabel.textContent = formatBytes(file.size);
        
        // Determine file icon class
        const ext = file.name.split('.').pop().toLowerCase();
        fileIcon.innerHTML = getFileIconForExtension(ext);
        
        detailsContainer.classList.remove('d-none');
    }

    function clearFileSelection() {
        selectedUploadFile = null;
        fileInput.value = '';
        detailsContainer.classList.add('d-none');
        resetProgressBar();
    }

    // Handle Upload Submission with Progress Display
    document.getElementById('upload-form').addEventListener('submit', (e) => {
        e.preventDefault();
        if (!selectedUploadFile) {
            alert('Please select or drop a file to upload.');
            return;
        }

        const categoryId = document.getElementById('upload-category').value;
        const description = document.getElementById('upload-description').value.trim();

        const formData = new FormData();
        formData.append('file', selectedUploadFile);
        formData.append('description', description);
        if (categoryId) formData.append('categoryId', categoryId);

        const progressContainer = document.getElementById('upload-progress-container');
        const progressFill = document.getElementById('upload-progress-fill');
        const progressTextContainer = document.getElementById('progress-text-container');
        const progressPercent = document.getElementById('progress-percent');
        const submitBtn = document.getElementById('btn-submit-upload');

        progressContainer.style.display = 'block';
        progressTextContainer.classList.remove('d-none');
        submitBtn.disabled = true;

        const xhr = new XMLHttpRequest();
        xhr.open('POST', '/api/documents/upload', true);

        // Upload progress listener
        xhr.upload.addEventListener('progress', (e) => {
            if (e.lengthComputable) {
                const percentComplete = Math.round((e.loaded / e.total) * 100);
                progressFill.style.width = percentComplete + '%';
                progressPercent.textContent = percentComplete + '%';
            }
        });

        xhr.onload = function () {
            submitBtn.disabled = false;
            if (xhr.status === 200) {
                alert('File uploaded successfully!');
                clearFileSelection();
                document.getElementById('upload-description').value = '';
                document.getElementById('upload-category').value = '';
                switchTab('documents');
                loadDashboardStats();
            } else {
                let errorMsg = 'Upload failed.';
                try {
                    const res = JSON.parse(xhr.responseText);
                    errorMsg = res.error || errorMsg;
                } catch(e) {}
                alert(errorMsg);
                resetProgressBar();
            }
        };

        xhr.onerror = function () {
            submitBtn.disabled = false;
            alert('Upload failed due to connection error.');
            resetProgressBar();
        };

        xhr.send(formData);
    });
}

function resetProgressBar() {
    const progressContainer = document.getElementById('upload-progress-container');
    const progressFill = document.getElementById('upload-progress-fill');
    const progressTextContainer = document.getElementById('progress-text-container');
    const progressPercent = document.getElementById('progress-percent');
    
    progressContainer.style.display = 'none';
    progressFill.style.width = '0%';
    progressPercent.textContent = '0%';
    progressTextContainer.classList.add('d-none');
}

// ==========================================
// User Profile Module
// ==========================================
async function handleProfileUpdate(e) {
    e.preventDefault();
    const successAlert = document.getElementById('profile-success-alert');
    const errorAlert = document.getElementById('profile-error-alert');
    successAlert.classList.add('d-none');
    errorAlert.classList.add('d-none');

    const fullName = document.getElementById('profile-fullname').value.trim();
    const email = document.getElementById('profile-email').value.trim();
    const phone = document.getElementById('profile-phone').value.trim();

    try {
        const response = await fetch('/api/auth/profile', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, email, phone })
        });
        const data = await response.json();
        if (response.ok) {
            successAlert.textContent = 'Profile updated successfully.';
            successAlert.classList.remove('d-none');
            currentUser = data; // update global user details
            setupUserUI(currentUser);
        } else {
            errorAlert.textContent = data.error || 'Failed to save changes.';
            errorAlert.classList.remove('d-none');
        }
    } catch (err) {
        errorAlert.textContent = 'Connection error.';
        errorAlert.classList.remove('d-none');
    }
}

async function handleChangePassword(e) {
    e.preventDefault();
    const successAlert = document.getElementById('pw-success-alert');
    const errorAlert = document.getElementById('pw-error-alert');
    successAlert.classList.add('d-none');
    errorAlert.classList.add('d-none');

    const oldPassword = document.getElementById('old-password').value;
    const newPassword = document.getElementById('new-password').value;
    const confirm = document.getElementById('confirm-new-password').value;

    if (newPassword !== confirm) {
        errorAlert.textContent = 'New passwords do not match.';
        errorAlert.classList.remove('d-none');
        return;
    }

    try {
        const response = await fetch('/api/auth/change-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ oldPassword, newPassword })
        });
        const data = await response.json();
        if (response.ok) {
            successAlert.textContent = data.message;
            successAlert.classList.remove('d-none');
            document.getElementById('change-password-form').reset();
        } else {
            errorAlert.textContent = data.error || 'Failed to update password.';
            errorAlert.classList.remove('d-none');
        }
    } catch (err) {
        errorAlert.textContent = 'Connection error.';
        errorAlert.classList.remove('d-none');
    }
}

// ==========================================
// Admin Control Panels Logic
// ==========================================
async function fetchAdminUsers() {
    try {
        const response = await fetch('/api/admin/users');
        if (!response.ok) return;
        const users = await response.json();
        
        // Sort users by database ID to guarantee chronological order
        users.sort((a, b) => a.id - b.id);
        
        const tbody = document.getElementById('admin-users-table-body');
        tbody.innerHTML = '';

        if (users.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-secondary">No registered users in the database.</td></tr>';
            return;
        }

        users.forEach(u => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${u.id}</td>
                <td><span class="fw-bold">${u.username}</span></td>
                <td>${u.email}</td>
                <td>${u.fullName || 'N/A'}</td>
                <td>${u.phone || 'N/A'}</td>
                <td><span class="badge ${u.role === 'ADMIN' ? 'bg-danger' : 'bg-primary'}">${u.role}</span></td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteUser(${u.id}, '${u.username}')" ${u.role === 'ADMIN' ? 'disabled' : ''}>
                        <i class="fa-solid fa-user-minus"></i> Delete
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch(e) {
        console.error('Failed to load user list.');
    }
}

async function deleteUser(userId, username) {
    if (!confirm(`Are you sure you want to delete user account '${username}'? All their document records will remain but will be orphaned.`)) {
        return;
    }
    try {
        const response = await fetch(`/api/admin/users/${userId}`, { method: 'DELETE' });
        if (response.ok) {
            fetchAdminUsers();
            loadDashboardStats();
        } else {
            const data = await response.json();
            alert(data.error || 'Failed to delete user.');
        }
    } catch(e) {
        alert('Server error.');
    }
}

async function fetchAdminCategories() {
    try {
        const response = await fetch('/api/categories');
        const categories = await response.json();
        const tbody = document.getElementById('admin-categories-table-body');
        tbody.innerHTML = '';

        if (categories.length === 0) {
            tbody.innerHTML = '<tr><td colspan="3" class="text-center text-secondary">No categories created yet</td></tr>';
            return;
        }

        categories.forEach(c => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${c.id}</td>
                <td><span class="fw-bold">${c.name}</span></td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteCategory(${c.id})"><i class="fa-solid fa-trash-can"></i> Delete</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch(e) {
        console.error('Failed to load categories.');
    }
}

async function handleCreateCategorySubmit(e) {
    e.preventDefault();
    const errorAlert = document.getElementById('category-error-alert');
    errorAlert.classList.add('d-none');

    const name = document.getElementById('new-category-name').value.trim();

    try {
        const response = await fetch('/api/categories', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });
        if (response.ok) {
            document.getElementById('new-category-name').value = '';
            fetchAdminCategories();
            fetchCategories(); // update dropdown lists
        } else {
            const data = await response.json();
            errorAlert.textContent = data.error || 'Failed to create folder category.';
            errorAlert.classList.remove('d-none');
        }
    } catch (err) {
        errorAlert.textContent = 'Server connection error.';
        errorAlert.classList.remove('d-none');
    }
}

async function deleteCategory(catId) {
    if (!confirm('Are you sure you want to delete this folder category? Any documents in it will become uncategorized.')) {
        return;
    }
    try {
        const response = await fetch(`/api/categories/${catId}`, { method: 'DELETE' });
        if (response.ok) {
            fetchAdminCategories();
            fetchCategories(); // update dropdowns
        } else {
            const data = await response.json();
            alert(data.error || 'Failed to delete category.');
        }
    } catch(e) {
        alert('Server error.');
    }
}

async function fetchAdminLogs() {
    try {
        const response = await fetch('/api/admin/activity-logs');
        const logs = await response.json();
        const tbody = document.getElementById('admin-logs-table-body');
        tbody.innerHTML = '';

        if (logs.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-secondary">No logs recorded yet.</td></tr>';
            return;
        }

        logs.forEach(l => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><small>${formatDate(l.timestamp)}</small></td>
                <td><span class="badge bg-light text-dark">${l.username}</span></td>
                <td><span class="badge bg-info-subtle text-info-emphasis">${l.action}</span></td>
                <td><span class="text-secondary">${l.details || ''}</span></td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        console.error('Failed to fetch logs.');
    }
}

// ==========================================
// Shared Helpers & Utilities
// ==========================================
async function fetchCategories() {
    try {
        const response = await fetch('/api/categories');
        const categories = await response.json();

        // Populate search filter dropdown
        const filterSelect = document.getElementById('filter-category');
        filterSelect.innerHTML = '<option value="">All Categories</option>';

        // Populate upload form dropdown
        const uploadSelect = document.getElementById('upload-category');
        uploadSelect.innerHTML = '<option value="">Select Folder</option>';

        // Populate metadata edit dropdown
        const editSelect = document.getElementById('edit-doc-category');
        editSelect.innerHTML = '<option value="">Uncategorized</option>';

        categories.forEach(cat => {
            const opt1 = document.createElement('option');
            opt1.value = cat.name;
            opt1.textContent = cat.name;
            filterSelect.appendChild(opt1);

            const opt2 = document.createElement('option');
            opt2.value = cat.id;
            opt2.textContent = cat.name;
            uploadSelect.appendChild(opt2);

            const opt3 = document.createElement('option');
            opt3.value = cat.id;
            opt3.textContent = cat.name;
            editSelect.appendChild(opt3);
        });
    } catch(e) {
        console.error('Could not populate categories dropdown list.');
    }
}

function getFileTypeBadgeClass(ext) {
    if (!ext) return 'badge-txt';
    const type = ext.toLowerCase();
    if (type === 'pdf') return 'badge-pdf';
    if (type === 'docx' || type === 'doc') return 'badge-docx';
    if (type === 'xlsx' || type === 'xls') return 'badge-xlsx';
    if (type === 'ppt' || type === 'pptx') return 'badge-ppt';
    if (type === 'zip' || type === 'rar') return 'badge-zip';
    if (type === 'jpg' || type === 'jpeg' || type === 'png' || type === 'gif') return 'badge-img';
    return 'badge-txt';
}

function getFileIconForExtension(ext) {
    if (ext === 'pdf') return '<i class="fa-solid fa-file-pdf text-danger"></i>';
    if (ext === 'docx' || ext === 'doc') return '<i class="fa-solid fa-file-word text-primary"></i>';
    if (ext === 'xlsx' || ext === 'xls') return '<i class="fa-solid fa-file-excel text-success"></i>';
    if (ext === 'ppt' || ext === 'pptx') return '<i class="fa-solid fa-file-powerpoint text-warning"></i>';
    if (ext === 'zip' || ext === 'rar') return '<i class="fa-solid fa-file-zipper text-purple"></i>';
    if (['jpg', 'jpeg', 'png', 'gif'].includes(ext)) return '<i class="fa-solid fa-file-image text-info"></i>';
    return '<i class="fa-solid fa-file-lines text-secondary"></i>';
}

function formatBytes(bytes, decimals = 2) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

function formatDate(isoString) {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleString();
}

function escapeHtml(text) {
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// ==========================================
// Admin User File Reports Page View
// ==========================================
async function fetchAdminUserReports() {
    try {
        const response = await fetch('/api/admin/user-file-reports');
        if (response.status === 401) {
            window.location.href = 'login.html';
            return;
        }
        const data = await response.json();
        renderAdminUserReports(data);
    } catch (err) {
        console.error('Failed to fetch user file reports:', err);
        document.getElementById('admin-user-reports-container').innerHTML = 
            `<div class="text-danger text-center py-4">Failed to load reports from server.</div>`;
    }
}

function renderAdminUserReports(reports) {
    const container = document.getElementById('admin-user-reports-container');
    container.innerHTML = '';

    if (reports.length === 0) {
        container.innerHTML = `<div class="text-secondary text-center py-4">No standard users registered yet.</div>`;
        return;
    }

    const accordion = document.createElement('div');
    accordion.className = 'accordion accordion-flush';
    accordion.id = 'userReportsAccordion';

    reports.forEach((rep, index) => {
        const item = document.createElement('div');
        item.className = 'accordion-item border border-secondary border-opacity-10 rounded-3 mb-3 overflow-hidden shadow-sm bg-white bg-opacity-25';

        const headerId = `heading-${rep.userId}`;
        const collapseId = `collapse-${rep.userId}`;
        const isExpanded = false;

        let fileRows = '';
        if (rep.documents.length === 0) {
            fileRows = `<tr><td colspan="5" class="text-center text-secondary py-4"><i class="fa-regular fa-folder-open display-6 mb-2 d-block"></i>No files uploaded by this user.</td></tr>`;
        } else {
            rep.documents.forEach(doc => {
                const badgeClass = getFileTypeBadgeClass(doc.fileType);
                fileRows += `
                    <tr>
                        <td><span class="badge ${badgeClass} px-2 py-1">${doc.fileType || 'TXT'}</span></td>
                        <td>
                            <div class="fw-bold">${doc.originalName}</div>
                            <div class="x-small text-secondary text-truncate" style="max-width: 250px;">${doc.description || 'No description'}</div>
                        </td>
                        <td>${formatBytes(doc.fileSize)}</td>
                        <td>${formatDate(doc.uploadDate)}</td>
                        <td class="text-end">
                            <div class="d-flex gap-2 justify-content-end">
                                <button class="btn btn-sm btn-outline-primary" onclick="previewDocument(${doc.id})" title="Preview"><i class="fa-solid fa-eye"></i></button>
                                <a class="btn btn-sm btn-outline-success" href="/api/documents/${doc.id}/download" title="Download"><i class="fa-solid fa-download"></i></a>
                                <button class="btn btn-sm btn-outline-danger" onclick="deleteDocumentFromReport(${doc.id})" title="Delete"><i class="fa-solid fa-trash-can"></i></button>
                            </div>
                        </td>
                    </tr>
                `;
            });
        }

        item.innerHTML = `
            <h2 class="accordion-header" id="${headerId}">
                <button class="accordion-button collapsed fw-bold d-flex align-items-center justify-content-between p-3" type="button" data-bs-toggle="collapse" data-bs-target="#${collapseId}" aria-expanded="${isExpanded}" aria-controls="${collapseId}" style="background: var(--bg-surface); color: var(--text-primary); outline: none; border: 0;">
                    <div class="d-flex align-items-center gap-3 w-100 me-3">
                        <div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center fw-bold" style="width: 38px; height: 38px;">
                            ${rep.fullName.substring(0, 1).toUpperCase()}
                        </div>
                        <div class="text-start">
                            <div class="fw-bold fs-6">${rep.fullName} <span class="small text-secondary fw-normal">(@${rep.username})</span></div>
                            <div class="x-small text-secondary">${rep.email || 'No email'} | ${rep.phone || 'No phone'}</div>
                        </div>
                        <div class="ms-auto text-end">
                            <span class="badge bg-primary px-3 py-1.5 rounded-pill fs-7">${rep.totalFiles} ${rep.totalFiles === 1 ? 'file' : 'files'} uploaded</span>
                        </div>
                    </div>
                </button>
            </h2>
            <div id="${collapseId}" class="accordion-collapse collapse" aria-labelledby="${headerId}" data-bs-parent="#userReportsAccordion">
                <div class="accordion-body p-3 bg-light bg-opacity-20 border-top border-secondary border-opacity-10">
                    <div class="table-responsive">
                        <table class="table align-middle table-sm small mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th>Type</th>
                                    <th>File Name</th>
                                    <th>Size</th>
                                    <th>Upload Date</th>
                                    <th class="text-end">Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${fileRows}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;
        accordion.appendChild(item);
    });

    container.appendChild(accordion);
}

async function deleteDocumentFromReport(docId) {
    if (!confirm('Are you sure you want to delete this document system-wide?')) return;
    try {
        const response = await fetch(`/api/documents/${docId}`, { method: 'DELETE' });
        if (response.status === 401) {
            window.location.href = 'login.html';
            return;
        }
        if (response.ok) {
            showAlert('Document deleted successfully', 'success');
            fetchAdminUserReports();
            loadDashboardStats();
        } else {
            const err = await response.json();
            showAlert(err.error || 'Failed to delete document', 'danger');
        }
    } catch (err) {
        showAlert('Network error occurred while deleting document', 'danger');
    }
}
