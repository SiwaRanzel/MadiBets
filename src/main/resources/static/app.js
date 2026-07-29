// Base API URL config
const API_BASE = 'http://localhost:8081/api';

// ── Sidebar Toggle ──
function toggleSidebar() {
    const sidebar = document.querySelector('.app-navigation-sidebar');
    const icon    = document.getElementById('sidebar-toggle-icon');
    const collapsed = sidebar.classList.toggle('sidebar-collapsed');

    // Swap icon: hamburger ↔ chevron-right
    if (collapsed) {
        icon.innerHTML = `
            <polyline points="15 18 9 12 15 6" stroke="currentColor" stroke-width="2.2"
                stroke-linecap="round" stroke-linejoin="round"/>`;
    } else {
        icon.innerHTML = `
            <line x1="3" y1="6" x2="21" y2="6"/>
            <line x1="3" y1="12" x2="21" y2="12"/>
            <line x1="3" y1="18" x2="21" y2="18"/>`;
    }
}

// On page load: check if the user is already logged in
document.addEventListener('DOMContentLoaded', () => {
    const savedUser = sessionStorage.getItem('user');
    if (savedUser) {
        const user = JSON.parse(savedUser);
        loadDashboard(user.userID);
    } else {
        showView('landing-view');
    }

    // Wire up sidebar nav clicks (handled via onclick attributes in HTML)
});

// UI View Switcher
function showView(viewId) {
    document.querySelectorAll('.view').forEach(view => {
        view.classList.add('hidden');
    });
    document.getElementById(viewId).classList.remove('hidden');
}

// Dashboard Panel Switcher (Dashboard / Help / etc.)
const PANELS = ['panel-dashboard', 'panel-dashboard-lecturer', 'panel-groups', 'panel-help', 'panel-dashboard-admin', 'panel-delete-request', 'panel-user-management', 'panel-madibucks', 'panel-accounting', 'panel-admin-groups', 'panel-reports', 'panel-settings', 'panel-query'];

function switchPanel(panelId) {
    PANELS.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.add('hidden');
    });
    const target = document.getElementById(panelId);
    if (target) target.classList.remove('hidden');

    // Update active nav item in whichever nav is visible
    document.querySelectorAll('.menu-item').forEach(item => {
        item.classList.remove('item-active');
    });
    const navMap = {
        'panel-dashboard':          'nav-dashboard-student',
        'panel-dashboard-lecturer': 'nav-dashboard-lecturer',
        'panel-groups':             'nav-groups',
        'panel-dashboard-admin':    'nav-dashboard-admin',
        'panel-delete-request':     'nav-delete-request',
        'panel-user-management':    'nav-user-management',
        'panel-madibucks':          'nav-madibucks',
        'panel-accounting':         'nav-accounting',
        'panel-admin-groups':       'nav-admin-groups',
        'panel-reports':            'nav-reports',
        'panel-settings':           'nav-settings',
        'panel-query':              'nav-query',
        'panel-help':               ['nav-help-student', 'nav-help-lecturer'],
    };
    const mapped = navMap[panelId];
    if (Array.isArray(mapped)) {
        mapped.forEach(id => { const el = document.getElementById(id); if (el) el.classList.add('item-active'); });
    } else if (mapped) {
        const activeNav = document.getElementById(mapped);
        if (activeNav) activeNav.classList.add('item-active');
    }
}

// Feedback form stub
async function submitFeedback() {
    const title = document.getElementById('feedback-title').value.trim();
    const body  = document.getElementById('feedback-body').value.trim();

    if (!title || !body) {
        showToast('Please fill in both fields.', 'error');
        return;
    }

    const savedUser = sessionStorage.getItem('user');
    if (!savedUser) {
        showToast('You must be logged in to submit feedback.', 'error');
        return;
    }

    const user = JSON.parse(savedUser);
    const payload = {
        title,
        description: body,
        userID: user.userID,
        resolvedStatus: 'OPEN'
    };

    try {
        const response = await fetch(`${API_BASE}/queries`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await response.json();
        if (response.ok) {
            showToast('Feedback submitted! Thank you.', 'success');
            document.getElementById('feedback-title').value = '';
            document.getElementById('feedback-body').value = '';
        } else {
            showToast(data.error || 'Unable to submit feedback.', 'error');
        }
    } catch (error) {
        showToast('Server error while submitting feedback.', 'error');
        console.error(error);
    }
}

// Navigate from landing page to Auth View
function goToAuth(tab) {
    showView('auth-view');
    switchTab(tab);
}

// Authentication Tab Switcher (Login vs Register)
function switchTab(tab) {
    const titleElement = document.getElementById('auth-flow-title');
    document.querySelectorAll('.auth-form-panel').forEach(form => form.classList.add('hidden'));

    if (tab === 'login') {
        document.getElementById('login-form').classList.remove('hidden');
        if (titleElement) titleElement.textContent = "LOG IN";
    } else {
        document.getElementById('register-form').classList.remove('hidden');
        if (titleElement) titleElement.textContent = "REGISTER";
    }
}

// User Role Selector Toggle
function setUserRole(role) {
    // Update active button
    document.getElementById('type-student').classList.remove('active');
    document.getElementById('type-teacher').classList.remove('active');

    const hiddenRoleInput = document.getElementById('reg-usertype');
    if (hiddenRoleInput) hiddenRoleInput.value = role;

    if (role === 'STUDENT') {
        document.getElementById('type-student').classList.add('active');
    } else {
        document.getElementById('type-teacher').classList.add('active');
    }

    toggleSubtypeFields();
}

// Toggle password visibility
function togglePasswordVisibility(inputId, iconElement) {
    const input = document.getElementById(inputId);
    if (input.type === 'password') {
        input.type = 'text';
        // Eye with slash SVG
        iconElement.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24M1 1l22 22"></path></svg>`;
    } else {
        input.type = 'password';
        // Regular eye SVG
        iconElement.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>`;
    }
}

// Show/Hide Student/Lecturer Number inputs depending on chosen UserType
function toggleSubtypeFields() {
    const userTypeInput = document.getElementById('reg-usertype');
    if (!userTypeInput) return;
    const userType = userTypeInput.value;
    const studentField = document.getElementById('student-field');
    const lecturerField = document.getElementById('lecturer-field');

    if (userType === 'STUDENT') {
        studentField.classList.remove('hidden');
        document.getElementById('reg-student-no').setAttribute('required', 'true');

        lecturerField.classList.add('hidden');
        document.getElementById('reg-staff-no').removeAttribute('required');
    } else {
        lecturerField.classList.remove('hidden');
        document.getElementById('reg-staff-no').setAttribute('required', 'true');

        studentField.classList.add('hidden');
        document.getElementById('reg-student-no').removeAttribute('required');
    }
}

// Show toast notifications
function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast-popup-box show ${type}`;

    setTimeout(() => {
        toast.className = 'toast-popup-box hidden';
    }, 4000);
}

// Handle login submissions
async function handleLogin(event) {
    event.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;

    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        const data = await response.json();

        if (response.ok) {
            sessionStorage.setItem('user', JSON.stringify(data.user));
            showToast('Welcome to MadiBets!', 'success');
            loadDashboardData(data.user, data.balance);
        } else {
            showToast(data.error || 'Login failed. Please check credentials.', 'error');
        }
    } catch (err) {
        showToast('Network error, please try again.', 'error');
        console.error(err);
    }
}

// Handle registration submissions
async function handleRegister(event) {
    event.preventDefault();
    const name = document.getElementById('reg-name').value;
    const surname = document.getElementById('reg-surname').value;
    const email = document.getElementById('reg-email').value;
    const password = document.getElementById('reg-password').value;
    const userType = document.getElementById('reg-usertype').value;

    // Only allow @mandela.ac.za email addresses
    if (!email.toLowerCase().endsWith('@mandela.ac.za')) {
        showToast('Only @mandela.ac.za email addresses are allowed.', 'error');
        return;
    }

    const studentNo = userType === 'STUDENT' ? document.getElementById('reg-student-no').value.trim() : null;
    const staffNo = userType === 'LECTURER' ? document.getElementById('reg-staff-no').value.trim() : null;

    // Student number must be exactly 9 digits
    if (userType === 'STUDENT' && (!/^\d{9}$/.test(studentNo))) {
        showToast('Student number must be exactly 9 digits (e.g. 221234567).', 'error');
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, surname, email, password, userType, studentNo, staffNo })
        });

        const data = await response.json();

        if (response.ok) {
            showToast('Registration successful! Please log in.', 'success');
            switchTab('login');
            document.getElementById('login-email').value = email;
            document.getElementById('login-password').value = '';
            document.getElementById('register-form').reset();
            toggleSubtypeFields();
        } else {
            showToast(data.error || 'Registration failed.', 'error');
        }
    } catch (err) {
        showToast('Network error, please try again.', 'error');
        console.error(err);
    }
}

// Load full profile and balance details from API
async function loadDashboard(userID) {
    try {
        const response = await fetch(`${API_BASE}/users/${userID}`);
        const data = await response.json();

        if (response.ok) {
            loadDashboardData(data.user, data.balance);
        } else {
            showToast('Session expired. Please log in again.', 'error');
            logout();
        }
    } catch (err) {
        showToast('Error loading profile info.', 'error');
        console.error(err);
    }
}

// Populate UI components with User profile & account details
function loadDashboardData(user, balance) {
    sessionStorage.setItem('user', JSON.stringify(user));

    const setElText = (id, text) => {
        const el = document.getElementById(id);
        if (el) el.textContent = text;
    };

    // Header Greeting
    setElText('user-display-name', `${user.name} ${user.surname}`);
    setElText('header-greeting-name', user.name);
    setElText('user-role-display', user.userType === 'LECTURER' ? 'Teacher' : (user.userType === 'ADMIN' ? 'Administrator' : 'Student'));

    // Wallet display
    setElText('wallet-balance', `${parseFloat(balance).toFixed(2)} MB`);
    setElText('wallet-card-holder', `${user.name} ${user.surname}`);
    setElText('wallet-card-type', user.userType);

    // Detailed Profile Display
    setElText('profile-fullname', `${user.name} ${user.surname}`);
    setElText('profile-email', user.email);
    setElText('profile-usertype', user.userType);

    const subtypeRow = document.getElementById('profile-subtype-row');
    const subtypeLabel = document.getElementById('profile-subtype-label');
    const subtypeValue = document.getElementById('profile-subtype-value');

    if (subtypeRow && subtypeLabel && subtypeValue) {
        if (user.userType === 'STUDENT') {
            subtypeRow.classList.remove('hidden');
            subtypeLabel.textContent = 'Student No';
            subtypeValue.textContent = user.studentNo || 'N/A';
        } else if (user.userType === 'LECTURER') {
            subtypeRow.classList.remove('hidden');
            subtypeLabel.textContent = 'Staff No';
            subtypeValue.textContent = user.staffNo || 'N/A';
        } else {
            subtypeRow.classList.add('hidden');
        }
    }

    // Populate the update form fields
    const editName = document.getElementById('edit-name');
    if (editName) editName.value = user.name;
    const editSurname = document.getElementById('edit-surname');
    if (editSurname) editSurname.value = user.surname;
    const editEmail = document.getElementById('edit-email');
    if (editEmail) editEmail.value = user.email;

    showEditForm(false);

    // Show the correct sidebar navigation and default dashboard panel
    const isLecturer = user.userType === 'LECTURER';
    const isAdmin = user.userType === 'ADMIN';

    const studentNav  = document.getElementById('nav-student');
    const lecturerNav = document.getElementById('nav-lecturer');
    const adminNav    = document.getElementById('nav-admin');
    const appHeader   = document.querySelector('.workspace-header');

    if (studentNav)  studentNav.style.display  = (user.userType === 'STUDENT') ? 'flex' : 'none';
    if (lecturerNav) lecturerNav.style.display = isLecturer ? 'flex' : 'none';
    if (adminNav)    adminNav.style.display    = isAdmin ? 'flex' : 'none';

    if (appHeader) {
        if (isAdmin) {
            appHeader.style.display = 'none';
        } else {
            appHeader.style.display = 'flex';
        }
    }

    // Default panel on login
    if (isAdmin) {
        switchPanel('panel-dashboard-admin');
        loadAdminQueries();
    } else if (isLecturer) {
        switchPanel('panel-dashboard-lecturer');
    } else {
        switchPanel('panel-dashboard');
    }

    showView('dashboard-view');
}

// Show/Hide Profile Editor Panel
function showEditForm(show) {
    const profileDisplay = document.getElementById('profile-display');
    const editProfileForm = document.getElementById('edit-profile-form');
    
    if (show) {
        if (profileDisplay) profileDisplay.classList.add('hidden');
        if (editProfileForm) editProfileForm.classList.remove('hidden');
    } else {
        if (profileDisplay) profileDisplay.classList.remove('hidden');
        if (editProfileForm) editProfileForm.classList.add('hidden');
    }
}

// Handle Profile Updates
async function handleUpdateProfile(event) {
    event.preventDefault();

    const user = JSON.parse(sessionStorage.getItem('user'));
    if (!user) return;

    const name = document.getElementById('edit-name').value;
    const surname = document.getElementById('edit-surname').value;
    const email = document.getElementById('edit-email').value;

    try {
        const response = await fetch(`${API_BASE}/users/${user.userID}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, surname, email })
        });

        const data = await response.json();

        if (response.ok) {
            showToast('Profile updated successfully!', 'success');
            loadDashboard(user.userID);
        } else {
            showToast(data.error || 'Failed to update profile.', 'error');
        }
    } catch (err) {
        showToast('Network error, please try again.', 'error');
        console.error(err);
    }
}

// Confirm and execute Account deletion
async function confirmDelete() {
    const user = JSON.parse(sessionStorage.getItem('user'));
    if (!user) return;

    const confirmAction = confirm("Are you sure you want to permanently delete your MadiBets account? This cannot be undone.");
    if (!confirmAction) return;

    try {
        const response = await fetch(`${API_BASE}/users/${user.userID}`, {
            method: 'DELETE'
        });

        const data = await response.json();

        if (response.ok) {
            showToast('Account successfully deleted.', 'success');
            logout();
        } else {
            showToast(data.error || 'Failed to delete account.', 'error');
        }
    } catch (err) {
        showToast('Network error, please try again.', 'error');
        console.error(err);
    }
}

// Log out user and clear storage
function logout() {
    sessionStorage.removeItem('user');
    showView('auth-view');
    document.getElementById('login-form').reset();
    switchTab('login');
}

// ── Admin Query Management ──

async function loadAdminQueries() {
    try {
        const response = await fetch(`${API_BASE}/queries`);
        const queries = await response.json();
        
        if (response.ok) {
            renderQueryTable(queries);
        } else {
            console.error('Failed to load queries');
        }
    } catch (error) {
        console.error('Error loading queries:', error);
    }
}

function renderQueryTable(queries) {
    const list = document.getElementById('admin-query-list');
    const paginationInfo = document.getElementById('query-pagination-info');
    if (!list) return;

    list.innerHTML = '';
    
    queries.forEach(q => {
        const date = new Date(q.queryDate);
        const dateStr = date.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
        
        const row = document.createElement('tr');
        row.innerHTML = `
            <td class="query-row-date">${dateStr}</td>
            <td class="query-row-title">${q.title}</td>
            <td class="query-row-email">${q.email}</td>
            <td><span class="status-pill ${q.resolvedStatus.toLowerCase()}">${q.resolvedStatus}</span></td>
            <td><button class="admin-view-query-btn" onclick="alert('${q.description.replace(/'/g, "\\'")}')">View</button></td>
            <td>
                <div class="admin-option-dropdown">
                    <button class="admin-option-trigger" onclick="toggleQueryOptions(event, ${q.queryID})">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="1"/><circle cx="12" cy="5" r="1"/><circle cx="12" cy="19" r="1"/></svg>
                    </button>
                    <div id="query-options-${q.queryID}" class="admin-dropdown-menu">
                        <div class="admin-dropdown-item" onclick="resolveQuery(${q.queryID})">Resolved</div>
                    </div>
                </div>
            </td>
        `;
        list.appendChild(row);
    });

    if (paginationInfo) {
        paginationInfo.textContent = `1 - ${queries.length} of ${queries.length}`;
    }
}

function toggleQueryOptions(event, queryID) {
    event.stopPropagation();
    // Close all other dropdowns
    document.querySelectorAll('.admin-dropdown-menu').forEach(menu => {
        if (menu.id !== `query-options-${queryID}`) {
            menu.classList.remove('show');
        }
    });
    const menu = document.getElementById(`query-options-${queryID}`);
    if (menu) menu.classList.toggle('show');
}

// Close dropdowns when clicking elsewhere
document.addEventListener('click', () => {
    document.querySelectorAll('.admin-dropdown-menu').forEach(menu => {
        menu.classList.remove('show');
    });
});

async function resolveQuery(queryID) {
    try {
        const response = await fetch(`${API_BASE}/queries/${queryID}/status`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: 'RESOLVED' })
        });

        if (response.ok) {
            showToast('Query marked as resolved', 'success');
            loadAdminQueries();
        } else {
            const data = await response.json();
            showToast(data.error || 'Failed to update query status', 'error');
        }
    } catch (error) {
        showToast('Error updating query status', 'error');
        console.error(error);
    }
}
