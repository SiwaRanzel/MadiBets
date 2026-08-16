// Base API URL config
const API_BASE = 'http://localhost:8081/api';

// ── Button Loading Spinner Utility ──
function setButtonLoading(buttonEl, isLoading) {
    if (!buttonEl) return;
    if (isLoading) {
        buttonEl.classList.add('btn-loading');
        // Use light spinner for dark-background buttons
        const bgColor = getComputedStyle(buttonEl).backgroundColor;
        if (bgColor && bgColor !== 'rgba(0, 0, 0, 0)' && bgColor !== 'transparent') {
            const rgb = bgColor.match(/\d+/g);
            if (rgb && (parseInt(rgb[0]) + parseInt(rgb[1]) + parseInt(rgb[2])) / 3 < 128) {
                buttonEl.classList.add('btn-loading-light');
            }
        }
        buttonEl.disabled = true;
    } else {
        buttonEl.classList.remove('btn-loading', 'btn-loading-light');
        buttonEl.disabled = false;
    }
}

// ── Fetch & display live user / student counts ──
async function fetchUserCounts() {
    try {
        const res  = await fetch(`${API_BASE}/users/count`);
        const data = await res.json();
        if (!res.ok) return;

        const totalUsers    = Number(data.totalUsers).toLocaleString();
        const totalStudents = Number(data.totalStudents).toLocaleString();

        // Student dashboard stat card
        const dashboardEl = document.getElementById('dashboard-total-students');
        if (dashboardEl) dashboardEl.textContent = totalStudents;

        // Account page stat card (students & lecturers both show student count)
        const accountValue = document.getElementById('account-stat-1-value');
        if (accountValue) accountValue.textContent = totalStudents;

        // Admin dashboard stat card
        const adminEl = document.getElementById('admin-total-users');
        if (adminEl) adminEl.textContent = totalUsers;

        // Dashboard student weekly trend
        const studentTrendEl = document.getElementById('dashboard-total-students-trend');
        if (studentTrendEl && data.studentWeeklyGrowth !== undefined) {
            const growth = Math.round(data.studentWeeklyGrowth);
            if (growth >= 0) {
                studentTrendEl.textContent = `↑ ${growth}%`;
                studentTrendEl.style.color = '#28A745'; // Green
            } else {
                studentTrendEl.textContent = `↓ ${Math.abs(growth)}%`;
                studentTrendEl.style.color = '#D9534F'; // Red
            }
        }
    } catch (err) {
        console.error('Failed to load user counts:', err);
    }
}

// ── Fetch & display live user rank ──
async function fetchUserRank() {
    const userStr = localStorage.getItem('madibets_user');
    if (!userStr) return;
    try {
        const user = JSON.parse(userStr);
        if (user.userType === 'ADMIN') return;

        const res = await fetch(`${API_BASE}/leaderboard/stats/${user.userID}`);
        if (!res.ok) return;
        const data = await res.json();

        const rankNum = Number(data.rank).toLocaleString();
        const totalNum = data.totalPlayers ? Number(data.totalPlayers).toLocaleString() : '-';
        const rankStr = `${rankNum} / ${totalNum}`;
        
        const dashboardPos = document.getElementById('dashboard-current-pos');
        if (dashboardPos) dashboardPos.textContent = rankStr;

        const accountPos = document.getElementById('account-current-pos');
        if (accountPos) accountPos.textContent = rankStr;

        const trendEl = document.getElementById('dashboard-current-pos-trend');
        if (trendEl && data.weeklyGrowth !== undefined) {
            const growth = Math.round(data.weeklyGrowth);
            if (growth >= 0) {
                trendEl.textContent = `↑ ${growth}%`;
                trendEl.style.color = '#28A745'; // Green
            } else {
                trendEl.textContent = `↓ ${Math.abs(growth)}%`;
                trendEl.style.color = '#D9534F'; // Red
            }
        }
    } catch (err) {
        console.error('Failed to load user rank:', err);
    }
}

// ── About Us Modal ──
function openAboutModal() {
    document.getElementById('about-modal').classList.remove('hidden');
}
function closeAboutModal() {
    document.getElementById('about-modal').classList.add('hidden');
}

// ── Inline Registration Validation ──
function validateRegField(input) {
    const id = input.id;
    const val = input.value.trim();
    const errSpan = document.getElementById('err-' + id);
    let msg = '';

    // Clear previous state
    input.classList.remove('input-error');
    if (errSpan) errSpan.textContent = '';

    // Skip validation if field is empty and not yet touched (optional fields)
    // But validate if the field has content or is required

    switch (id) {
        case 'reg-name':
            if (val.length === 0) {
                msg = 'Name is required.';
            } else if (val.length < 2) {
                msg = 'Name must be at least 2 characters.';
            }
            break;

        case 'reg-surname':
            if (val.length === 0) {
                msg = 'Surname is required.';
            } else if (val.length < 2) {
                msg = 'Surname must be at least 2 characters.';
            }
            break;

        case 'reg-student-no':
            if (val.length === 0 && document.getElementById('reg-usertype').value === 'STUDENT') {
                msg = 'Student number is required.';
            } else if (val.length > 0 && !/^\d{9}$/.test(val)) {
                if (!/^\d+$/.test(val)) {
                    msg = 'Student number must contain only digits.';
                } else {
                    msg = 'Student number must be exactly 9 digits (currently ' + val.length + ').';
                }
            }
            break;

        case 'reg-staff-no':
            if (val.length === 0 && document.getElementById('reg-usertype').value === 'LECTURER') {
                msg = 'Staff number is required.';
            }
            break;

        case 'reg-email':
            if (val.length === 0) {
                msg = 'Email is required.';
            } else if (!/^[a-zA-Z0-9._%+\-]+@mandela\.ac\.za$/.test(val)) {
                msg = 'Only @mandela.ac.za email addresses are allowed.';
            }
            break;

        case 'reg-password':
            if (val.length === 0) {
                msg = 'Password is required.';
            } else if (val.length < 6) {
                msg = 'Password must be at least 6 characters.';
            }
            // Also re-validate confirm if it has a value
            const confirmInput = document.getElementById('reg-confirm-password');
            if (confirmInput && confirmInput.value.trim().length > 0) {
                validateRegField(confirmInput);
            }
            break;

        case 'reg-confirm-password':
            if (val.length === 0) {
                msg = 'Please confirm your password.';
            } else if (val !== document.getElementById('reg-password').value) {
                msg = 'Passwords do not match.';
            }
            break;
    }

    if (msg) {
        input.classList.add('input-error');
        if (errSpan) errSpan.textContent = '⚠ ' + msg;
    }

    return msg === '';
}

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

// -------------------- Groups UI / API Helpers --------------------
let groupSearchTimeout = null;

function debouncedSearchGroups() {
    if (groupSearchTimeout) clearTimeout(groupSearchTimeout);
    groupSearchTimeout = setTimeout(() => {
        const q = document.getElementById('group-search').value.trim();
        searchGroups(q);
    }, 300);
}

async function searchGroups(q) {
    const saved = sessionStorage.getItem('user');
    const user = saved ? JSON.parse(saved) : null;
    try {
        let url = `${API_BASE}/groups`;
        const params = new URLSearchParams();
        if (q && q.length > 0) {
            params.set('q', q);
        }
        if (user) {
            params.set('userId', user.userID);
        }
        if ([...params].length > 0) {
            url += `?${params.toString()}`;
        }
        const resp = await fetch(url);
        const data = await resp.json();
        if (resp.ok) {
            renderGroupList(data);
        } else {
            showToast(data.error || 'Failed to load groups', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Server error loading groups', 'error');
    }
}

function renderGroupList(items) {
    const container = document.getElementById('groups-list');
    container.innerHTML = '';
    if (!items || items.length === 0) {
        container.innerHTML = '<p style="color:#6C7D93; text-align:center; padding:40px 8px;">No groups found.</p>';
        return;
    }

    items.forEach(it => {
        const groupID = it.groupID !== undefined ? it.groupID : it.groupId || 0;
        const name = it.groupName || it.group_name || `Group ${groupID}`;
        const desc = it.description || '';

        const row = document.createElement('div');
        row.className = 'group-row';
        row.style = 'padding:12px; border-bottom:1px solid #F1F6FB; display:flex; justify-content:space-between; align-items:center; cursor:pointer;';
        row.onclick = () => loadGroupDetail(groupID);
        row.innerHTML = `<div style="flex:1;"><div style="font-weight:700; color:#1B2F5E;">${escapeHtml(name)}</div><div style="font-size:0.9rem; color:#6C7D93;">${escapeHtml(desc)}</div></div><div style="margin-left:12px; color:#9FB0D1; font-weight:700">${groupID > 0 ? 'Group' : 'Default'}</div>`;
        container.appendChild(row);
    });
}

function escapeHtml(s) {
    if (!s) return '';
    return s.replace(/&/g, '&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

async function loadGroupDetail(groupID) {
    try {
        const saved = sessionStorage.getItem('user');
        const user = saved ? JSON.parse(saved) : null;
        let url = `${API_BASE}/groups/${groupID}`;
        if (user) url += `?userId=${user.userID}`;
        const resp = await fetch(url);
        const data = await resp.json();
        if (!resp.ok) {
            showToast(data.error || 'Failed to load group details', 'error');
            return;
        }

        const detail = document.getElementById('group-detail');
        const placeholder = document.getElementById('group-detail-placeholder');
        placeholder.style.display = 'none';
        detail.style.display = 'block';
        detail.innerHTML = '';

        const title = document.createElement('h3');
        title.textContent = data.groupName || data.group_name || data.groupName || `Group ${groupID}`;
        title.style.marginTop = '0';
        detail.appendChild(title);

        const p = document.createElement('p');
        p.style.color = '#6C7D93';
        p.textContent = data.description || data.description || '';
        detail.appendChild(p);

        const meta = document.createElement('div');
        meta.style = 'margin-top:12px; color:#6C7D93; font-size:0.9rem;';
        meta.textContent = `Members: ${data.memberCount !== undefined ? data.memberCount : '—'}`;
        detail.appendChild(meta);

        // Join button only for real groups (id>0)
        if (groupID > 0) {
            const btn = document.createElement('button');
            btn.className = 'btn btn-gold-cta';
            btn.style = 'margin-top:16px; padding:10px 16px;';
            btn.textContent = 'Join Group';
            btn.onclick = async () => {
                await joinGroup(groupID);
            };
            detail.appendChild(btn);
        } else {
            const info = document.createElement('div');
            info.style = 'margin-top:16px; color:#6C7D93;';
            info.textContent = 'This is a default group. Joining is not required.';
            detail.appendChild(info);
        }
    } catch (err) {
        console.error(err);
        showToast('Error loading group detail', 'error');
    }
}

function openCreateGroupModal() {
    document.getElementById('create-group-modal').classList.remove('hidden');
}

function closeCreateGroupModal(event) {
    if (event && event.target && event.target.id !== 'create-group-modal') {
        return;
    }
    const modal = document.getElementById('create-group-modal');
    if (modal) modal.classList.add('hidden');
    const nameInput = document.getElementById('new-group-name');
    const descInput = document.getElementById('new-group-desc');
    if (nameInput) nameInput.value = '';
    if (descInput) descInput.value = '';
}

async function createGroup() {
    const name = document.getElementById('new-group-name').value.trim();
    const desc = document.getElementById('new-group-desc').value.trim();
    const saved = sessionStorage.getItem('user');
    if (!saved) { showToast('You must be logged in to create groups.', 'error'); return; }
    const user = JSON.parse(saved);
    if (!name) { showToast('Please provide a group name.', 'error'); return; }

    try {
        const resp = await fetch(`${API_BASE}/groups`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ groupName: name, description: desc, createdBy: user.userID })
        });
        const data = await resp.json();
        if (resp.status === 201) {
            showToast('Group created successfully!', 'success');
            closeCreateGroupModal();
            searchGroups('');
        } else {
            showToast(data.error || 'Failed to create group', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Server error creating group', 'error');
    }
}

async function joinGroup(groupID) {
    const saved = sessionStorage.getItem('user');
    if (!saved) { showToast('You must be logged in to join groups.', 'error'); return; }
    const user = JSON.parse(saved);
    try {
        const resp = await fetch(`${API_BASE}/groups/${groupID}/join`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userID: user.userID })
        });
        const data = await resp.json();
        if (resp.ok) {
            showToast('Joined group successfully', 'success');
            loadGroupDetail(groupID);
            searchGroups('');
        } else if (resp.status === 409) {
            showToast('You are already a member of this group.', 'error');
        } else {
            showToast(data.error || 'Failed to join group', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Server error joining group', 'error');
    }
}

// Initialize groups view when panel is shown via switchPanel
document.addEventListener('click', (e) => {
    // if the groups panel is visible, ensure its list is loaded
    const pg = document.getElementById('panel-groups');
    if (pg && !pg.classList.contains('hidden')) {
        // load once
        if (!pg.dataset.loaded) {
            searchGroups('');
            pg.dataset.loaded = '1';
        }
    }
});

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
const PANELS = ['panel-dashboard', 'panel-dashboard-lecturer', 'panel-groups', 'panel-help', 'panel-dashboard-admin', 'panel-delete-request', 'panel-user-management', 'panel-madibucks', 'panel-accounting', 'panel-admin-groups', 'panel-reports', 'panel-settings', 'panel-query', 'panel-account', 'panel-friends', 'panel-leaderboard', 'panel-bets', 'panel-task'];

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
        'panel-groups':             ['nav-groups', 'nav-groups-student', 'nav-admin-groups'],
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
        'panel-account':            ['nav-account-student', 'nav-account-lecturer'],
        'panel-friends':            'nav-friends',
        'panel-leaderboard':        ['nav-leaderboard', 'nav-leaderboard-lecturer'],
        'panel-bets':               'nav-bets',
        'panel-task':               'nav-task-lecturer',
    };
    const mapped = navMap[panelId];
    if (Array.isArray(mapped)) {
        mapped.forEach(id => { const el = document.getElementById(id); if (el) el.classList.add('item-active'); });
    } else if (mapped) {
        const activeNav = document.getElementById(mapped);
        if (activeNav) activeNav.classList.add('item-active');
    }

    // Load specific data when switching to certain panels
    if (panelId === 'panel-account') {
        loadAccountData();
    } else if (panelId === 'panel-user-management') {
        loadUserManagementData();
    } else if (panelId === 'panel-friends') {
        loadFriendsData();
    } else if (panelId === 'panel-leaderboard') {
        loadLeaderboardData();
    } else if (panelId === 'panel-groups') {
        searchGroups('');
    } else if (panelId === 'panel-query') {
        loadAdminQueries();
    } else if (panelId === 'panel-delete-request') {
        loadDeleteRequests();
    } else if (panelId === 'panel-dashboard' || panelId === 'panel-dashboard-admin') {
        loadAdminDashboardStats();
    } else if (panelId === 'panel-dashboard-lecturer') {
        loadLecturerDashboardStats();
    } else if (panelId === 'panel-bets') {
        loadBetsPanel();
    } else if (panelId === 'panel-accounting') {
        loadAccountingPanel();
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
    document.getElementById('type-lecturer').classList.remove('active');

    const hiddenRoleInput = document.getElementById('reg-usertype');
    if (hiddenRoleInput) hiddenRoleInput.value = role;

    if (role === 'STUDENT') {
        document.getElementById('type-student').classList.add('active');
    } else {
        document.getElementById('type-lecturer').classList.add('active');
    }

    // Update email placeholders based on role
    const loginEmail = document.getElementById('login-email');
    const regEmail = document.getElementById('reg-email');
    const emailPlaceholder = role === 'STUDENT' ? 'e.g. s221234567@mandela.ac.za' : 'e.g. Kie.Whi@mandela.ac.za';
    
    if (loginEmail) loginEmail.placeholder = emailPlaceholder;
    if (regEmail) regEmail.placeholder = emailPlaceholder;

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
    const btn = document.getElementById('btn-login');
    setButtonLoading(btn, true);

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
    } finally {
        setButtonLoading(btn, false);
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

    const btn = document.getElementById('btn-register');
    setButtonLoading(btn, true);

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
    } finally {
        setButtonLoading(btn, false);
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
    setElText('user-role-display', user.userType === 'LECTURER' ? 'Lecturer' : (user.userType === 'ADMIN' ? 'Administrator' : 'Student'));

    // Wallet display
    setElText('wallet-balance', `${parseFloat(balance).toFixed(2)} MB`);
    setElText('wallet-card-holder', `${user.name} ${user.surname}`);
    setElText('wallet-card-type', user.userType);

    // Detailed Profile Display
    setElText('profile-fullname', `${user.name} ${user.surname}`);
    setElText('profile-email', user.email);
    setElText('profile-usertype', user.userType);

    // Update avatars
    const headerAvatar = document.getElementById('header-avatar');
    const sidebarImg = document.getElementById('sidebar-avatar-img');
    const sidebarSvg = document.getElementById('sidebar-avatar-svg');

    if (user.avatarPath) {
        const imgUrl = `http://localhost:8081${user.avatarPath}?t=${new Date().getTime()}`;
        // if (headerAvatar) headerAvatar.src = imgUrl; // Ensure it stays as logo
        
        if (sidebarImg && sidebarSvg) {
            sidebarImg.src = imgUrl;
            sidebarImg.style.display = 'block';
            sidebarSvg.style.display = 'none';
        }
    } else {
        if (headerAvatar) headerAvatar.src = 'logo.png';
        if (sidebarImg && sidebarSvg) {
            sidebarImg.style.display = 'none';
            sidebarSvg.style.display = 'block';
        }
    }

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
        loadDeleteRequests();
    } else if (isLecturer) {
        switchPanel('panel-dashboard-lecturer');
    } else {
        switchPanel('panel-dashboard');
    }

    showView('dashboard-view');

    // Populate live user/student counts across all stat cards
    fetchUserCounts();
    fetchUserRank();
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

    // Find the submit button inside the edit profile form
    const form = event.target;
    const btn = form ? form.querySelector('button[type="submit"]') : null;
    setButtonLoading(btn, true);

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
    } finally {
        setButtonLoading(btn, false);
    }
}

// Confirm and execute Account deletion
async function confirmDelete() {
    const user = JSON.parse(sessionStorage.getItem('user'));
    if (!user) return;

    const confirmAction = confirm("Are you sure you want to permanently delete your MadiBets account? This cannot be undone.");
    if (!confirmAction) return;

    // Find the delete button that triggered this
    const btn = event && event.target ? event.target.closest('button') : null;
    setButtonLoading(btn, true);

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
    } finally {
        setButtonLoading(btn, false);
    }
}

// Log out user and clear storage
function logout() {
    sessionStorage.removeItem('user');
    showView('auth-view');
    document.getElementById('login-form').reset();
    switchTab('login');
}



// ── Account Page Logic ──
async function loadAccountData() {
    const user = JSON.parse(sessionStorage.getItem('user'));
    if (!user) return;

    try {
        const response = await fetch(`${API_BASE}/users/${user.userID}`);
        const data = await response.json();
        
        if (response.ok) {
            const u = data.user;
            
            // Update avatars if they exist, otherwise clear them
            const headerAvatar = document.getElementById('header-avatar');
            const sidebarImg = document.getElementById('sidebar-avatar-img');
            const sidebarSvg = document.getElementById('sidebar-avatar-svg');

            if (u.avatarPath) {
                const imgUrl = `http://localhost:8081${u.avatarPath}?t=${new Date().getTime()}`;
                // if (headerAvatar) headerAvatar.src = imgUrl; // Ensure it stays as logo
                
                if (sidebarImg && sidebarSvg) {
                    sidebarImg.src = imgUrl;
                    sidebarImg.style.display = 'block';
                    sidebarSvg.style.display = 'none';
                }
            } else {
                if (headerAvatar) headerAvatar.src = 'logo.png';
                if (sidebarImg && sidebarSvg) {
                    sidebarImg.style.display = 'none';
                    sidebarSvg.style.display = 'block';
                }
            }

            // Populate table conditionally based on role
            const tbody = document.getElementById('account-table-body');
            const tableTitle = document.getElementById('account-table-title');
            const stat2 = document.getElementById('account-stat-2');
            const stat1Label = document.getElementById('account-stat-1-label');
            const tableHeaderRow = document.getElementById('account-table-header');
            
            tbody.innerHTML = '';
            
            if (u.userType === 'LECTURER') {
                tableTitle.textContent = 'Task';
                stat1Label.textContent = 'Total Students';
                if (stat2) stat2.style.display = 'none'; // Hide Pos for lecturer
                
                tableHeaderRow.innerHTML = `
                    <th style="text-align: left; padding: 15px 10px; color: #A0B2D6; font-weight: 500; font-size: 0.85rem;">Type</th>
                    <th style="text-align: center; padding: 15px 10px; color: #A0B2D6; font-weight: 500; font-size: 0.85rem;">MadiBucks</th>
                    <th style="text-align: center; padding: 15px 10px; color: #A0B2D6; font-weight: 500; font-size: 0.85rem;">Tasks</th>
                    <th style="text-align: center; padding: 15px 10px; color: #A0B2D6; font-weight: 500; font-size: 0.85rem;">Status</th>
                `;

                // Dummy data matching screenshot
                const tasks = [
                    { type: 'Academics', bucks: '150', task: 'Quiz 2', status: 'Over', statusColor: '#E0F2E9', textColor: '#28A745' },
                    { type: 'Academics', bucks: '200', task: 'Quiz 5', status: 'Active', statusColor: '#E2E8F0', textColor: '#6C7D93' }
                ];
                
                tasks.forEach(t => {
                    tbody.innerHTML += `
                        <tr style="border-bottom: 1px solid #F0F2F5;">
                            <td style="padding: 15px 10px; color: #1B2F5E; font-size: 0.95rem;">${t.type}</td>
                            <td style="text-align: center; padding: 15px 10px; color: #1B2F5E; font-weight: 600;">${t.bucks}</td>
                            <td style="text-align: center; padding: 15px 10px; color: #1B2F5E;">${t.task}</td>
                            <td style="text-align: center; padding: 15px 10px;">
                                <span style="background: ${t.statusColor}; color: ${t.textColor}; padding: 6px 12px; border-radius: 6px; font-size: 0.8rem; font-weight: 600;">${t.status}</span>
                            </td>
                        </tr>
                    `;
                });
            } else {
                tableTitle.textContent = 'Bets Placed';
                stat1Label.textContent = 'Total Students';
                if (stat2) stat2.style.display = 'flex'; // Show Pos for student

                tableHeaderRow.innerHTML = `
                    <th style="text-align: left; padding: 15px 10px; color: #A0B2D6; font-weight: 500; font-size: 0.85rem;">Event</th>
                    <th style="text-align: center; padding: 15px 10px; color: #A0B2D6; font-weight: 500; font-size: 0.85rem;">Odds</th>
                    <th style="text-align: center; padding: 15px 10px; color: #A0B2D6; font-weight: 500; font-size: 0.85rem;">Potential Win</th>
                    <th style="text-align: center; padding: 15px 10px; color: #A0B2D6; font-weight: 500; font-size: 0.85rem;">Date</th>
                    <th style="text-align: center; padding: 15px 10px; color: #A0B2D6; font-weight: 500; font-size: 0.85rem;">Status</th>
                `;

                // Fetch real bet history
                const betsResponse = await fetch(`${API_BASE}/leaderboard/history/${user.userID}`);
                if (betsResponse.ok) {
                    const bets = await betsResponse.json();
                    if (bets.length === 0) {
                        tbody.innerHTML += `<tr><td colspan="5" style="padding: 20px; text-align: center; color: #A0B2D6;">No bets placed yet.</td></tr>`;
                    } else {
                        bets.forEach(b => {
                            let statusColor, statusBg;
                            switch (b.outcome) {
                                case 'YES':
                                    statusColor = '#28A745'; statusBg = '#E0F2E9'; break;
                                case 'NO':
                                    statusColor = '#D9534F'; statusBg = '#FEE2E2'; break;
                                case 'CANCELLED':
                                    statusColor = '#6C7D93'; statusBg = '#E2E8F0'; break;
                                default:
                                    statusColor = '#F5A623'; statusBg = '#FFF9E6'; break;
                            }
                            const outcomeLabel = b.outcome === 'YES' ? 'Won' : b.outcome === 'NO' ? 'Lost' : b.outcome === 'CANCELLED' ? 'Cancelled' : 'Pending';
                            const dateStr = b.placedDate ? new Date(b.placedDate).toLocaleDateString() : 'N/A';

                            tbody.innerHTML += `
                                <tr style="border-bottom: 1px solid #F0F2F5;">
                                    <td style="padding: 15px 10px; color: #1B2F5E; font-size: 0.95rem;">${b.description || b.eventDescription || 'N/A'}</td>
                                    <td style="text-align: center; padding: 15px 10px; color: #1B2F5E; font-weight: 600;">${b.odds ? b.odds.toFixed(2) : '-'}</td>
                                    <td style="text-align: center; padding: 15px 10px; color: #1B2F5E;">${b.amountToBeWon ? b.amountToBeWon.toFixed(2) + ' MB' : '-'}</td>
                                    <td style="text-align: center; padding: 15px 10px; color: #1B2F5E;">${dateStr}</td>
                                    <td style="text-align: center; padding: 15px 10px;">
                                        <span style="background: ${statusBg}; color: ${statusColor}; padding: 6px 12px; border-radius: 6px; font-size: 0.8rem; font-weight: 600;">${outcomeLabel}</span>
                                    </td>
                                </tr>
                            `;
                        });
                    }
                } else {
                    tbody.innerHTML += `<tr><td colspan="5" style="padding: 20px; text-align: center; color: #D9534F;">Failed to load bets.</td></tr>`;
                }
            }
        }
    } catch (err) {
        console.error("Error loading account data:", err);
    }

    // Refresh live counts (label is already set for the correct role)
    fetchUserCounts();
    fetchUserRank();
}

async function uploadSelectedAvatar() {
    const input = document.getElementById('avatar-upload-input');
    if (!input || !input.files || input.files.length === 0) {
        showToast('Please select an image file first.', 'error');
        return;
    }
    
    const file = input.files[0];
    const user = JSON.parse(sessionStorage.getItem('user'));
    if (!user) return;

    const formData = new FormData();
    formData.append("avatar", file);

    const btn = document.getElementById('btn-upload-avatar');
    setButtonLoading(btn, true);

    try {
        const response = await fetch(`${API_BASE}/users/${user.userID}/avatar`, {
            method: 'POST',
            body: formData
        });

        const data = await response.json();
        
        if (response.ok && data.avatarUrl) {
            showToast('Avatar updated successfully!', 'success');
            const imgUrl = `http://localhost:8081${data.avatarUrl}?t=${new Date().getTime()}`;
            
            // Update UI
            const headerAvatar = document.getElementById('header-avatar');
            // if (headerAvatar) headerAvatar.src = imgUrl; // Ensure it stays as logo
            
            const sidebarImg = document.getElementById('sidebar-avatar-img');
            const sidebarSvg = document.getElementById('sidebar-avatar-svg');
            if (sidebarImg && sidebarSvg) {
                sidebarImg.src = imgUrl;
                sidebarImg.style.display = 'block';
                sidebarSvg.style.display = 'none';
            }
            
            // Update session storage
            user.avatarPath = data.avatarUrl;
            sessionStorage.setItem('user', JSON.stringify(user));

            // Clear file input
            input.value = '';
        } else {
            showToast(data.error || 'Failed to upload avatar', 'error');
        }
    } catch (err) {
        showToast('Network error while uploading.', 'error');
        console.error(err);
    } finally {
        setButtonLoading(btn, false);
    }
}

async function updateAccountPassword() {
    const p1 = document.getElementById('account-new-password').value;
    const p2 = document.getElementById('account-confirm-password').value;
    
    if (!p1 || !p2) {
        showToast('Please fill in both password fields', 'error');
        return;
    }
    
    if (p1 !== p2) {
        showToast('Passwords do not match', 'error');
        return;
    }

    const user = JSON.parse(sessionStorage.getItem('user'));
    if (!user) return;

    const btn = document.getElementById('btn-update-password');
    setButtonLoading(btn, true);

    try {
        const response = await fetch(`${API_BASE}/users/${user.userID}/password`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ password: p1 })
        });

        const data = await response.json();
        if (response.ok) {
            showToast('Password updated successfully!', 'success');
            document.getElementById('account-new-password').value = '';
            document.getElementById('account-confirm-password').value = '';
        } else {
            showToast(data.error || 'Failed to update password', 'error');
        }
    } catch (err) {
        showToast('Network error.', 'error');
        console.error(err);
    } finally {
        setButtonLoading(btn, false);
    }
}

// ── User Management Logic ──
let umUsers = [];
let umActiveTab = 'STUDENT';

async function loadUserManagementData() {
    try {
        const response = await fetch(`${API_BASE}/users/all`);
        if (response.ok) {
            umUsers = await response.json();
            renderUMTable();
        } else {
            let errMsg = 'Failed to load users';
            try {
                const errData = await response.json();
                errMsg = errData.error || errMsg;
            } catch(e) {
                errMsg += ` (HTTP ${response.status})`;
            }
            console.error('User management API error:', response.status, errMsg);
            showToast(errMsg, 'error');
        }
    } catch (err) {
        showToast('Network error while fetching users', 'error');
        console.error('User management fetch error:', err);
    }
}

function switchUMTab(role, btnEl) {
    umActiveTab = role;
    
    // Update active class on buttons
    document.querySelectorAll('.um-tab-btn').forEach(btn => btn.classList.remove('active'));
    if (btnEl) btnEl.classList.add('active');
    
    // Update title
    const titleEl = document.getElementById('um-tab-title');
    const colNo = document.getElementById('um-col-no');
    if (role === 'STUDENT') {
        titleEl.textContent = 'Student';
        colNo.textContent = 'Student No.';
    } else if (role === 'LECTURER') {
        titleEl.textContent = 'Lecturer';
        colNo.textContent = 'Staff No.';
    } else {
        titleEl.textContent = 'Administrator';
        colNo.textContent = 'User ID';
    }

    renderUMTable();
}

function filterUMTable() {
    renderUMTable();
}

function renderUMTable() {
    const tbody = document.getElementById('um-table-body');
    const searchInput = document.getElementById('um-search-input');
    const searchTerm = searchInput ? searchInput.value.toLowerCase() : '';
    
    if (!tbody) return;
    
    tbody.innerHTML = '';
    
    // Filter by role and search
    let filteredUsers = umUsers.filter(u => u.userType === umActiveTab);
    
    if (searchTerm) {
        filteredUsers = filteredUsers.filter(u => {
            const fullName = `${u.name} ${u.surname}`.toLowerCase();
            const email = u.email.toLowerCase();
            const no = String(u.studentNo || u.staffNo || u.userID).toLowerCase();
            return fullName.includes(searchTerm) || email.includes(searchTerm) || no.includes(searchTerm);
        });
    }

    // Pagination info
    const pageInfo = document.getElementById('um-pagination-info');
    if (pageInfo) {
        pageInfo.textContent = `1 - ${filteredUsers.length} of ${filteredUsers.length}`;
    }

    filteredUsers.forEach(u => {
        const no = u.userType === 'STUDENT' ? (u.studentNo || 'N/A') : (u.userType === 'LECTURER' ? (u.staffNo || 'N/A') : u.userID);
        const date = '17/03/2026'; // Placeholder as registration date isn't in the schema

        tbody.innerHTML += `
            <tr>
                <td><input type="checkbox" style="width: 16px; height: 16px;"></td>
                <td style="color: #A0B2D6;">#${no}</td>
                <td style="color: #1B2F5E; font-weight: 600;">${u.name} ${u.surname}</td>
                <td><a href="mailto:${u.email}" style="color: #6C7D93; text-decoration: underline;">${u.email}</a></td>
                <td style="color: #1B2F5E; font-weight: 600;">${date}</td>
                <td style="text-align: center;"><button class="um-action-btn">View</button></td>
                <td style="text-align: center; color: #A0B2D6; font-size: 1.2rem; cursor: pointer;">⋮</td>
            </tr>
        `;
    });
}


// ══════════════════════════════════════════════════════════════
// ── Friends Panel Logic (Jason C-series: C100, C200, C300) ──
// ══════════════════════════════════════════════════════════════

async function loadFriendsData() {
    const user = JSON.parse(sessionStorage.getItem('user'));
    if (!user) return;

    await Promise.all([
        loadFriendsList(user.userID),
        loadPendingRequests(user.userID)
    ]);
}

// C100 — Load accepted friends list
async function loadFriendsList(userID) {
    const container = document.getElementById('friends-list-container');
    const countEl = document.getElementById('friends-count');
    if (!container) return;

    try {
        const response = await fetch(`${API_BASE}/friends/${userID}`);
        const friends = await response.json();

        if (!response.ok) {
            container.innerHTML = `<p style="color: #D9534F; font-size: 0.95rem;">Failed to load friends.</p>`;
            return;
        }

        if (friends.length === 0) {
            container.innerHTML = `<p style="color: #A0B2D6; font-size: 0.95rem;">You haven't added any friends yet.</p>`;
            if (countEl) countEl.textContent = '0 friends';
            return;
        }

        if (countEl) countEl.textContent = `${friends.length} friend${friends.length !== 1 ? 's' : ''}`;

        container.innerHTML = friends.map(f => {
            // Show the OTHER person's name (not your own)
            const friendName = f.requesterID === userID ? f.addresseName : f.requesterName;
            return `
                <div style="display: flex; justify-content: space-between; align-items: center; padding: 15px 20px; background: #F8FAFC; border-radius: 12px;">
                    <div style="display: flex; align-items: center; gap: 12px;">
                        <div style="width: 40px; height: 40px; background: #1B2F5E; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#F5A623" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/>
                            </svg>
                        </div>
                        <span style="color: #1B2F5E; font-weight: 600; font-size: 0.95rem;">${friendName}</span>
                    </div>
                    <button onclick="removeFriend(${f.friendshipID})" style="background: none; border: 1px solid #D9534F; color: #D9534F; padding: 6px 16px; border-radius: 6px; font-size: 0.8rem; font-weight: 600; cursor: pointer;">Remove</button>
                </div>
            `;
        }).join('');
    } catch (err) {
        container.innerHTML = `<p style="color: #D9534F; font-size: 0.95rem;">Error loading friends.</p>`;
        console.error(err);
    }
}

// C100 — Load pending friend requests
async function loadPendingRequests(userID) {
    const container = document.getElementById('pending-requests-list');
    if (!container) return;

    try {
        const response = await fetch(`${API_BASE}/friends/${userID}/pending`);
        const pending = await response.json();

        if (!response.ok) {
            container.innerHTML = `<p style="color: #D9534F; font-size: 0.95rem;">Failed to load requests.</p>`;
            return;
        }

        if (pending.length === 0) {
            container.innerHTML = `<p style="color: #A0B2D6; font-size: 0.95rem;">No pending requests.</p>`;
            return;
        }

        container.innerHTML = pending.map(f => `
            <div style="display: flex; justify-content: space-between; align-items: center; padding: 15px 20px; background: #FFF9E6; border-radius: 12px; border: 1px solid #F5A623;">
                <div style="display: flex; align-items: center; gap: 12px;">
                    <div style="width: 40px; height: 40px; background: #F5A623; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/>
                        </svg>
                    </div>
                    <span style="color: #1B2F5E; font-weight: 600; font-size: 0.95rem;">${f.requesterName}</span>
                </div>
                <div style="display: flex; gap: 8px;">
                    <button onclick="acceptFriendRequest(${f.friendshipID})" style="background: #28A745; color: white; border: none; padding: 6px 16px; border-radius: 6px; font-size: 0.8rem; font-weight: 600; cursor: pointer;">Accept</button>
                    <button onclick="rejectFriendRequest(${f.friendshipID})" style="background: none; border: 1px solid #D9534F; color: #D9534F; padding: 6px 16px; border-radius: 6px; font-size: 0.8rem; font-weight: 600; cursor: pointer;">Reject</button>
                </div>
            </div>
        `).join('');
    } catch (err) {
        container.innerHTML = `<p style="color: #D9534F; font-size: 0.95rem;">Error loading requests.</p>`;
        console.error(err);
    }
}

// C200 — Send friend request by email
async function sendFriendRequest() {
    const emailInput = document.getElementById('friend-email-input');
    const email = emailInput ? emailInput.value.trim() : '';
    const btn = document.getElementById('btn-send-friend-request');

    if (!email) {
        showToast('Please enter an email address.', 'error');
        return;
    }

    const user = JSON.parse(sessionStorage.getItem('user'));
    if (!user) return;

    setButtonLoading(btn, true);

    try {
        // First, look up the user by email
        const lookupRes = await fetch(`${API_BASE}/users/lookup?email=${encodeURIComponent(email)}`);

        if (!lookupRes.ok) {
            const errData = await lookupRes.json();
            showToast(errData.error || 'User not found.', 'error');
            return;
        }

        const targetUser = await lookupRes.json();

        // Now send the friend request
        const response = await fetch(`${API_BASE}/friends/request`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ requesterID: user.userID, addresseID: targetUser.userID })
        });

        const data = await response.json();

        if (response.ok) {
            showToast(data.message || 'Friend request sent!', 'success');
            emailInput.value = '';
        } else {
            showToast(data.error || 'Failed to send request.', 'error');
        }
    } catch (err) {
        showToast('Network error. Please try again.', 'error');
        console.error(err);
    } finally {
        setButtonLoading(btn, false);
    }
}

// C200 — Accept friend request
async function acceptFriendRequest(friendshipID) {
    try {
        const response = await fetch(`${API_BASE}/friends/${friendshipID}/accept`, {
            method: 'PUT'
        });

        const data = await response.json();

        if (response.ok) {
            showToast('Friend request accepted!', 'success');
            loadFriendsData();
        } else {
            showToast(data.error || 'Failed to accept request.', 'error');
        }
    } catch (err) {
        showToast('Network error.', 'error');
        console.error(err);
    }
}

// C200 — Reject friend request
async function rejectFriendRequest(friendshipID) {
    try {
        const response = await fetch(`${API_BASE}/friends/${friendshipID}/reject`, {
            method: 'PUT'
        });

        const data = await response.json();

        if (response.ok) {
            showToast('Friend request rejected.', 'success');
            loadFriendsData();
        } else {
            showToast(data.error || 'Failed to reject request.', 'error');
        }
    } catch (err) {
        showToast('Network error.', 'error');
        console.error(err);
    }
}

// C300 — Remove friend
async function removeFriend(friendshipID) {
    const confirmed = await showConfirmModal('Remove Friend', 'Are you sure you want to remove this friend? This action cannot be undone.');
    if (!confirmed) return;

    try {
        const response = await fetch(`${API_BASE}/friends/${friendshipID}`, {
            method: 'DELETE'
        });

        const data = await response.json();

        if (response.ok) {
            showToast('Friend removed.', 'success');
            loadFriendsData();
        } else {
            showToast(data.error || 'Failed to remove friend.', 'error');
        }
    } catch (err) {
        showToast('Network error.', 'error');
        console.error(err);
    }
}

// ── Custom Confirm Modal Logic ──
let confirmResolve = null;

function showConfirmModal(title, message) {
    return new Promise(resolve => {
        confirmResolve = resolve;
        document.getElementById('confirm-modal-title').textContent = title;
        document.getElementById('confirm-modal-message').textContent = message;
        document.getElementById('confirm-modal').classList.remove('hidden');
    });
}

function closeConfirmModal(result) {
    document.getElementById('confirm-modal').classList.add('hidden');
    if (confirmResolve) {
        confirmResolve(result);
        confirmResolve = null;
    }
}


// ══════════════════════════════════════════════════════════════
// ── Leaderboard Panel Logic (Jason C-series: C400, C500) ────
// ══════════════════════════════════════════════════════════════

async function loadLeaderboardData() {
    const user = JSON.parse(sessionStorage.getItem('user'));
    if (!user) return;

    await Promise.all([
        loadUserStats(user.userID),
        loadRankings(),
        loadBetHistory(user.userID)
    ]);
}

// C500 — Load user's personal stats (rank, wins, losses, pending)
async function loadUserStats(userID) {
    try {
        const response = await fetch(`${API_BASE}/leaderboard/stats/${userID}`);
        const stats = await response.json();

        if (response.ok) {
            const rankEl = document.getElementById('lb-user-rank');
            const winsEl = document.getElementById('lb-user-wins');
            const lossesEl = document.getElementById('lb-user-losses');
            const pendingEl = document.getElementById('lb-user-pending');

            if (rankEl) rankEl.textContent = `#${stats.rank}`;
            if (winsEl) winsEl.textContent = stats.wins || 0;
            if (lossesEl) lossesEl.textContent = stats.losses || 0;
            if (pendingEl) pendingEl.textContent = stats.pending || 0;
        }
    } catch (err) {
        console.error('Failed to load user stats:', err);
    }
}

// ── Fetch & display live lecturer dashboard stats ──
async function loadLecturerDashboardStats() {
    try {
        const user = JSON.parse(sessionStorage.getItem('user'));
        if (!user || user.userType !== 'LECTURER') return;
        const res = await fetch(`${API_BASE}/lecturers/${user.userID}/dashboard-stats`);
        if (!res.ok) return;
        const stats = await res.json();

        const grpEl = document.getElementById('lect-stat-groups');
        if (grpEl) grpEl.innerText = stats.totalGroups;

        const stuEl = document.getElementById('lect-stat-students');
        if (stuEl) stuEl.innerText = stats.totalStudents;
        
        const stuTrendEl = document.getElementById('lect-stat-students-trend');
        if (stuTrendEl) stuTrendEl.innerHTML = `&uarr; ${stats.newStudentsThisWeek}`;

        const actEl = document.getElementById('lect-stat-active');
        if (actEl) actEl.innerText = stats.activeToday;

        const engEl = document.getElementById('lect-stat-engagement');
        if (engEl) {
            let pct = 0;
            if (stats.totalStudents > 0) {
                pct = Math.round((stats.activeToday / stats.totalStudents) * 100);
            }
            engEl.innerText = `${pct}%`;
        }
    } catch (e) {
        console.error("Failed to load lecturer stats", e);
    }
}

// C500 — Load top rankings table
let currentLeaderboardSort = 'balance';

async function loadRankings(sortBy) {
    if (sortBy) currentLeaderboardSort = sortBy;
    const tbody = document.getElementById('leaderboard-table-body');
    if (!tbody) return;

    try {
        const response = await fetch(`${API_BASE}/leaderboard/rankings?limit=20&sortBy=${currentLeaderboardSort}`);
        const rankings = await response.json();

        if (!response.ok) {
            tbody.innerHTML = `<tr><td colspan="5" style="padding: 20px; text-align: center; color: #D9534F;">Failed to load rankings.</td></tr>`;
            return;
        }

        if (rankings.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" style="padding: 20px; text-align: center; color: #A0B2D6;">No users ranked yet.</td></tr>`;
            return;
        }

        const user = JSON.parse(sessionStorage.getItem('user'));
        const currentUserID = user ? user.userID : -1;

        tbody.innerHTML = rankings.map(r => {
            const isCurrentUser = r.userID === currentUserID;
            const rowBg = isCurrentUser ? 'background: #FFF9E6;' : '';
            const rankBadge = r.rank <= 3
                ? `<span style="background: ${r.rank === 1 ? '#F5A623' : r.rank === 2 ? '#C0C0C0' : '#CD7F32'}; color: white; width: 28px; height: 28px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; font-weight: 700; font-size: 0.8rem;">${r.rank}</span>`
                : `<span style="color: #1B2F5E; font-weight: 600;">${r.rank}</span>`;

            return `
                <tr style="border-bottom: 1px solid #F0F2F5; ${rowBg}">
                    <td style="padding: 14px 10px;">${rankBadge}</td>
                    <td style="padding: 14px 10px; color: #1B2F5E; font-weight: ${isCurrentUser ? '700' : '500'};">${r.name}${isCurrentUser ? ' (You)' : ''}</td>
                    <td style="text-align: center; padding: 14px 10px; color: #1B2F5E; font-weight: 600;">${parseFloat(r.balance).toFixed(2)} MB</td>
                    <td style="text-align: center; padding: 14px 10px; color: #6C7D93;">${r.totalBets}</td>
                    <td style="text-align: center; padding: 14px 10px; color: #28A745; font-weight: 600;">${r.betsWon}</td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="5" style="padding: 20px; text-align: center; color: #D9534F;">Error loading rankings.</td></tr>`;
        console.error(err);
    }
}

// C400 — Load bet history table
async function loadBetHistory(userID) {
    const tbody = document.getElementById('bet-history-table-body');
    if (!tbody) return;

    try {
        const response = await fetch(`${API_BASE}/leaderboard/history/${userID}`);
        const bets = await response.json();

        if (!response.ok) {
            tbody.innerHTML = `<tr><td colspan="5" style="padding: 20px; text-align: center; color: #D9534F;">Failed to load bet history.</td></tr>`;
            return;
        }

        if (bets.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" style="padding: 20px; text-align: center; color: #A0B2D6;">No bets placed yet.</td></tr>`;
            return;
        }

        tbody.innerHTML = bets.map(b => {
            let statusColor, statusBg;
            switch (b.outcome) {
                case 'YES':
                    statusColor = '#28A745'; statusBg = '#E0F2E9'; break;
                case 'NO':
                    statusColor = '#D9534F'; statusBg = '#FEE2E2'; break;
                case 'CANCELLED':
                    statusColor = '#6C7D93'; statusBg = '#E2E8F0'; break;
                default:
                    statusColor = '#F5A623'; statusBg = '#FFF9E6'; break;
            }

            const outcomeLabel = b.outcome === 'YES' ? 'Won' : b.outcome === 'NO' ? 'Lost' : b.outcome === 'CANCELLED' ? 'Cancelled' : 'Pending';
            const dateStr = b.placedDate ? new Date(b.placedDate).toLocaleDateString() : 'N/A';

            return `
                <tr style="border-bottom: 1px solid #F0F2F5;">
                    <td style="padding: 14px 10px; color: #1B2F5E; font-size: 0.95rem;">${b.description || b.eventDescription || 'N/A'}</td>
                    <td style="text-align: center; padding: 14px 10px; color: #1B2F5E; font-weight: 600;">${b.odds ? b.odds.toFixed(2) : '-'}</td>
                    <td style="text-align: center; padding: 14px 10px; color: #1B2F5E; font-weight: 600;">${b.amountToBeWon ? b.amountToBeWon.toFixed(2) + ' MB' : '-'}</td>
                    <td style="text-align: center; padding: 14px 10px; color: #6C7D93;">${dateStr}</td>
                    <td style="text-align: center; padding: 14px 10px;">
                        <span style="background: ${statusBg}; color: ${statusColor}; padding: 5px 12px; border-radius: 6px; font-size: 0.8rem; font-weight: 600;">${outcomeLabel}</span>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="5" style="padding: 20px; text-align: center; color: #D9534F;">Error loading bet history.</td></tr>`;
        console.error(err);
    }
}

// Switch leaderboard sort criteria
function changeLeaderboardSort(sortBy, btnEl) {
    // Update active button
    document.querySelectorAll('.lb-sort-btn').forEach(btn => btn.classList.remove('lb-sort-active'));
    if (btnEl) btnEl.classList.add('lb-sort-active');

    // Reload rankings with new sort
    loadRankings(sortBy);
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

// ── Account Deletion Request Logic ──

function showDeleteModal() {
    const modal = document.getElementById('delete-account-modal');
    if (modal) modal.classList.remove('hidden');
}

function closeDeleteModal() {
    const modal = document.getElementById('delete-account-modal');
    if (modal) modal.classList.add('hidden');
}

async function submitDeleteRequest() {
    const userStr = sessionStorage.getItem('user');
    if (!userStr) return;
    const user = JSON.parse(userStr);

    try {
        const response = await fetch(`${API_BASE}/users/${user.userID}/delete-request`, {
            method: 'POST'
        });
        
        if (response.ok) {
            closeDeleteModal();
            showToast('account will be deleted by admin', 'success');
            setTimeout(() => {
                logout();
            }, 1500);
        } else {
            const data = await response.json();
            showToast(data.error || 'Failed to submit request', 'error');
        }
    } catch (err) {
        showToast('Error submitting request', 'error');
        console.error(err);
    }
}

async function loadDeleteRequests() {
    try {
        const response = await fetch(`${API_BASE}/users/delete-requests`);
        if (!response.ok) return;
        const requests = await response.json();

        const tbody = document.getElementById('delete-request-tbody');
        if (!tbody) return;
        
        const newRequests = requests.filter(r => r.status === 'NEW');
        
        const uniqueRequests = [];
        const seenUsers = new Set();
        for (const req of newRequests) {
            if (!seenUsers.has(req.userID)) {
                seenUsers.add(req.userID);
                uniqueRequests.push(req);
            }
        }
        
        const newCount = uniqueRequests.length;
        const badge = document.getElementById('delete-request-badge');
        if (badge) {
            badge.textContent = newCount;
            badge.style.display = newCount > 0 ? 'inline-flex' : 'none';
        }
        
        const paginationInfo = document.getElementById('delete-request-pagination-info');
        if (paginationInfo) {
            if (newCount === 0) {
                paginationInfo.innerHTML = `<span style="font-weight: 700; color: #1B2F5E;">0</span> of 0`;
            } else {
                paginationInfo.innerHTML = `<span style="font-weight: 700; color: #1B2F5E;">1</span> of ${newCount}`;
            }
        }

        tbody.innerHTML = '';
        uniqueRequests.forEach(req => {
            const dateStr = new Date(req.requestDate).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
            
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td style="padding: 12px 15px; color: #6C7A9C; font-size: 0.9rem;">${dateStr}</td>
                <td style="padding: 12px 15px; font-weight: 600; color: #1B2F5E; font-size: 0.95rem;">${req.userName}</td>
                <td style="padding: 12px 15px;"><a href="mailto:${req.userEmail}" style="color: #6C7A9C; text-decoration: underline; font-size: 0.9rem;">${req.userEmail}</a></td>
                <td style="padding: 12px 15px; text-align: center;">
                    <div style="display: flex; gap: 8px; justify-content: center;">
                        <button onclick="reinstateUser(${req.requestID})" style="background: white; border: 1px solid #1B2F5E; color: #1B2F5E; padding: 4px 12px; border-radius: 20px; font-size: 0.75rem; font-weight: 600; cursor: pointer; transition: all 0.2s;">Reinstate</button>
                        <button onclick="deleteUserPermanently(${req.userID})" style="background: #D9534F; border: none; color: white; padding: 4px 12px; border-radius: 20px; font-size: 0.75rem; font-weight: 600; cursor: pointer; transition: all 0.2s;">Delete</button>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error('Failed to load delete requests:', err);
    }
}

async function reinstateUser(reqId) {
    try {
        const response = await fetch(`${API_BASE}/users/delete-requests/${reqId}/reinstate`, {
            method: 'PUT'
        });
        if (response.ok) {
            showToast('User reinstated successfully', 'success');
            loadDeleteRequests(); // Refresh table and badge
        } else {
            showToast('Failed to reinstate user', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Error reinstating user', 'error');
    }
}

async function deleteUserPermanently(userId) {
    if (!confirm('Are you absolutely sure you want to permanently delete this user? This cannot be undone.')) return;
    
    try {
        const response = await fetch(`${API_BASE}/users/${userId}`, {
            method: 'DELETE'
        });
        if (response.ok) {
            showToast('User deleted permanently', 'success');
            loadDeleteRequests(); // Refresh table and badge
        } else {
            showToast('Failed to delete user', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Error deleting user', 'error');
    }
}

// ── Fetch & display live admin dashboard stats ──
async function loadAdminDashboardStats() {
    try {
        const res = await fetch(`${API_BASE}/admin/dashboard-stats`);
        if (!res.ok) return;
        const stats = await res.json();
        
        const setStat = (id, value) => {
            const el = document.getElementById(id);
            if (el) el.textContent = value;
        };
        
        setStat('stat-bets-proposed', stats.betsProposedToday);
        setStat('stat-bets-placed', stats.betsPlacedToday);
        setStat('stat-bets-pending', stats.betsPendingReview);
        setStat('stat-upcoming-events', stats.upcomingEvents);
        
        setStat('stat-users-total', stats.totalUsers);
        setStat('stat-users-joined-today', stats.usersJoinedToday);
        setStat('stat-support-queries', stats.openSupportQueries);
        setStat('stat-bonus-awarded', stats.usersRewardedToday);
    } catch (err) {
        console.error('Failed to load admin dashboard stats:', err);
    }
}

/* =============================================================
 * B-SERIES: BETS PANEL + ACCOUNTING SYSTEM (Kieran)
 * ============================================================= */

const BET_TYPE_OPTIONS = ['Academics', 'Sports', 'Social', 'Class Room'];

function betTypeOf(description) {
    const idx = description.indexOf(':');
    if (idx > 0) {
        const prefix = description.slice(0, idx).trim();
        if (BET_TYPE_OPTIONS.some(t => t.toLowerCase() === prefix.toLowerCase())) return prefix;
    }
    return 'Other';
}

function betTextOf(description) {
    return betTypeOf(description) === 'Other'
        ? description
        : description.slice(description.indexOf(':') + 1).trim();
}

// ── Bets panel (student) ──
let betsCache = {};   // betID -> bet, so onclick handlers don't embed user text

function switchBetsTab(tab) {
    document.getElementById('bets-tab-place').classList.toggle('active', tab === 'place');
    document.getElementById('bets-tab-propose').classList.toggle('active', tab === 'propose');
    document.getElementById('bets-list-view').classList.toggle('hidden', tab !== 'place');
    document.getElementById('bets-propose-view').classList.toggle('hidden', tab !== 'propose');
    if (tab === 'place') loadBetsPanel();
}

/** "2026-08-20T18:00" -> "2026-08-20 18:00" for table cells. */
function fmtDeadline(dt) {
    return dt ? String(dt).slice(0, 16).replace('T', ' ') : '—';
}

async function loadBetsPanel() {
    const tbody = document.getElementById('bets-table-body');
    const empty = document.getElementById('bets-empty-msg');
    if (!tbody) return;

    try {
        const res = await fetch(`${API_BASE}/bets/active`);
        const data = await res.json();
        if (!res.ok) {
            showToast(data.error || 'Failed to load bets.', 'error');
            return;
        }
        const bets = data.bets || [];
        betsCache = {};
        tbody.innerHTML = '';
        empty.classList.toggle('hidden', bets.length > 0);

        bets.forEach(b => {
            betsCache[b.betID] = b;
            // One odds button per outcome (FSSB p.25: side-by-side odds).
            // Unpriced outcomes (odds null — only on pre-migration bets) are
            // not wagerable, so they get no button.
            const action = b.open
                ? (b.outcomes || []).filter(o => o.odds != null).map(o =>
                    `<button class="bet-action-btn bet-outcome-btn"
                        onclick="openWagerModal(${b.betID}, ${o.outcomeID})">
                        <span class="bet-outcome-label">${escapeHtml(o.label)}</span>
                        <span class="bet-outcome-odds">${Number(o.odds).toFixed(2)}</span></button>`).join('')
                : '<span class="bet-taken-pill">Closed — awaiting grading</span>';
            tbody.innerHTML += `
                <tr>
                    <td>${escapeHtml(betTypeOf(b.description))}</td>
                    <td>${escapeHtml(betTextOf(b.description))}</td>
                    <td>${fmtDeadline(b.deadline)}</td>
                    <td>${action}</td>
                </tr>
            `;
        });
        filterBetsTable();
    } catch (err) {
        showToast('Network error — is the API server running?', 'error');
        console.error(err);
    }
}

// B700 mockup: search field filters the open-bets list client-side
function filterBetsTable() {
    const input = document.getElementById('bets-search');
    if (!input) return;
    const q = input.value.trim().toLowerCase();
    document.querySelectorAll('#bets-table-body tr').forEach(tr => {
        tr.style.display = tr.textContent.toLowerCase().includes(q) ? '' : 'none';
    });
}

// ── Wager modal (B100) ──
let wagerBetID = null;
let wagerOutcomeID = null;

function wagerOutcome() {
    const b = betsCache[wagerBetID];
    return b ? (b.outcomes || []).find(o => o.outcomeID === wagerOutcomeID) : null;
}

function openWagerModal(betID, outcomeID) {
    const b = betsCache[betID];
    if (!b) return;
    wagerBetID = betID;
    wagerOutcomeID = outcomeID;
    const o = wagerOutcome();
    document.getElementById('wager-bet-desc').textContent = betTextOf(b.description);
    document.getElementById('wager-bet-outcome').textContent = o ? `Your pick: ${o.label}` : '';
    document.getElementById('wager-amount').value = '';
    updateWagerPayout();
    document.getElementById('wager-modal').classList.remove('hidden');
}

function closeWagerModal() {
    wagerBetID = null;
    wagerOutcomeID = null;
    document.getElementById('wager-modal').classList.add('hidden');
}

function updateWagerPayout() {
    const o = wagerOutcome();
    if (!o) return;
    const odds = Number(o.odds);
    const amount = parseFloat(document.getElementById('wager-amount').value);
    document.getElementById('wager-bet-payout').textContent = (amount > 0)
        ? `Odds ${odds.toFixed(2)} — potential payout ${(amount * odds).toFixed(2)} MB`
        : `Odds ${odds.toFixed(2)}`;
}

async function submitWager() {
    const user = JSON.parse(sessionStorage.getItem('user'));
    if (!user || wagerBetID === null) return;
    const amount = parseFloat(document.getElementById('wager-amount').value);
    if (!(amount > 0)) {
        showToast('Please enter a stake amount.', 'error');
        return;
    }

    const btn = document.getElementById('btn-place-wager');
    setButtonLoading(btn, true);
    try {
        const res = await fetch(`${API_BASE}/bets/${wagerBetID}/wager`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userID: user.userID, outcomeID: wagerOutcomeID, stake: amount })
        });
        const data = await res.json();
        if (res.ok) {
            showToast(`Bet placed! ${amount.toFixed(2)} MB staked.`, 'success');
            if (data.newBalance != null) {
                const walletEl = document.getElementById('wallet-balance');
                if (walletEl) walletEl.textContent = `${parseFloat(data.newBalance).toFixed(2)} MB`;
            }
            closeWagerModal();
            loadBetsPanel();
        } else {
            showToast(data.error || 'Failed to place bet.', 'error');
        }
    } catch (err) {
        showToast('Network error, please try again.', 'error');
        console.error(err);
    } finally {
        setButtonLoading(btn, false);
    }
}

// ── Proposal form (B200) ──
// Placeholder examples per bet type, so an Academics proposal never shows
// rugby examples. Index = outcome slot; slots past the list fall back to
// "Another outcome".
const OUTCOME_EXAMPLES = {
    'Academics':  ['e.g. Above 60%', 'e.g. Below 60%', 'e.g. Exactly 60% (optional)'],
    'Sports':     ['e.g. Madibaz win', 'e.g. Wits win', 'e.g. Draw (optional)'],
    'Social':     ['e.g. Over 100 attend', 'e.g. Under 100 attend', 'e.g. Exactly 100 (optional)'],
    'Class Room': ['e.g. Lecture happens', 'e.g. Lecture cancelled', 'e.g. Moved online (optional)'],
};

function updateOutcomePlaceholders() {
    const type = document.getElementById('propose-type').value;
    const examples = OUTCOME_EXAMPLES[type] || [];
    document.querySelectorAll('.propose-outcome-input').forEach((input, i) => {
        input.placeholder = examples[i] || 'Another outcome';
    });
}

function addOutcomeField() {
    const container = document.getElementById('propose-outcomes');
    if (container.querySelectorAll('.propose-outcome-input').length >= 4) {
        showToast('A bet can have at most 4 outcomes.', 'error');
        return;
    }
    const input = document.createElement('input');
    input.type = 'text';
    input.className = 'propose-outcome-input';
    input.maxLength = 100;
    container.appendChild(input);
    updateOutcomePlaceholders();
}

async function submitProposal() {
    const user = JSON.parse(sessionStorage.getItem('user'));
    if (!user) return;
    const type = document.getElementById('propose-type').value;
    const text = document.getElementById('propose-description').value.trim();
    if (!text) {
        showToast('Please describe the bet you want to propose.', 'error');
        return;
    }
    const outcomes = Array.from(document.querySelectorAll('.propose-outcome-input'))
        .map(i => i.value.trim())
        .filter(v => v);
    if (outcomes.length < 2) {
        showToast('List at least 2 possible outcomes.', 'error');
        return;
    }

    const btn = document.getElementById('btn-submit-proposal');
    setButtonLoading(btn, true);
    try {
        const res = await fetch(`${API_BASE}/bets/propose`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userID: user.userID, eventID: null,
                                   description: `${type}: ${text}`, outcomes: outcomes })
        });
        const data = await res.json();
        if (res.ok) {
            showToast('Proposal sent to the admins for review!', 'success');
            document.getElementById('propose-description').value = '';
            document.querySelectorAll('.propose-outcome-input').forEach(i => { i.value = ''; });
            switchBetsTab('place');
        } else {
            showToast(data.error || 'Failed to submit proposal.', 'error');
        }
    } catch (err) {
        showToast('Network error, please try again.', 'error');
        console.error(err);
    } finally {
        setButtonLoading(btn, false);
    }
}

// ── Accounting System panel (admin: B300/B400/B500) ──
async function loadAccountingPanel() {
    try {
        const [pRes, aRes] = await Promise.all([
            fetch(`${API_BASE}/bets/proposed`),
            fetch(`${API_BASE}/bets/active`)
        ]);
        const pData = await pRes.json();
        const aData = await aRes.json();
        if (!pRes.ok || !aRes.ok) {
            showToast(pData.error || aData.error || 'Failed to load bets.', 'error');
            return;
        }
        renderProposedTable(pData.bets || []);
        renderActiveAdminTable(aData.bets || []);
    } catch (err) {
        showToast('Network error — is the API server running?', 'error');
        console.error(err);
    }
}

let acctCache = {};   // betID -> bet (with outcomes), for approve/grade handlers

function renderProposedTable(bets) {
    const tbody = document.getElementById('acct-proposed-body');
    document.getElementById('acct-proposed-empty').classList.toggle('hidden', bets.length > 0);
    tbody.innerHTML = '';
    bets.forEach(b => {
        acctCache[b.betID] = b;
        const date = b.proposedDate ? String(b.proposedDate).slice(0, 10) : '—';
        // One odds input per proposed outcome — all required to approve.
        const oddsInputs = (b.outcomes || []).map(o => `
            <div style="display: flex; align-items: center; gap: 8px; margin: 3px 0;">
                <span style="min-width: 90px; font-size: 0.85rem;">${escapeHtml(o.label)}</span>
                <input type="number" id="odds-input-${b.betID}-${o.outcomeID}" class="acct-odds-input"
                    min="1.01" step="0.01" placeholder="e.g. 2.50">
            </div>`).join('');
        tbody.innerHTML += `
            <tr>
                <td>${date}</td>
                <td>${escapeHtml(betTypeOf(b.description))}</td>
                <td>${escapeHtml(betTextOf(b.description))}</td>
                <td>${oddsInputs}</td>
                <td><input type="datetime-local" id="deadline-input-${b.betID}" class="acct-odds-input"
                        style="width: 175px;"></td>
                <td>
                    <div class="acct-btn-stack">
                        <button class="acct-btn acct-btn-approve" onclick="approveProposalUI(${b.betID})">Approve</button>
                        <button class="acct-btn acct-btn-reject" onclick="rejectProposalUI(${b.betID})">Reject</button>
                    </div>
                </td>
            </tr>
        `;
    });
}

function renderActiveAdminTable(bets) {
    const tbody = document.getElementById('acct-active-body');
    document.getElementById('acct-active-empty').classList.toggle('hidden', bets.length > 0);
    tbody.innerHTML = '';
    bets.forEach(b => {
        acctCache[b.betID] = b;
        const outcomes = (b.outcomes || []).map(o =>
            `${escapeHtml(o.label)} @ ${o.odds != null ? Number(o.odds).toFixed(2) : 'unpriced'}`).join('<br>');
        const wager = b.wagerCount > 0
            ? `${b.wagerCount} wager${b.wagerCount === 1 ? '' : 's'} · ${Number(b.totalStaked).toFixed(2)} MB staked`
            : '<span style="color:#A0B2D6;">No wagers yet</span>';
        const closes = b.open
            ? fmtDeadline(b.deadline)
            : `${fmtDeadline(b.deadline)}<br><span style="color:#C0392B; font-weight:600;">Closed</span>`;
        // One "winner" button per outcome, plus cancel/delete.
        const winnerBtns = (b.outcomes || []).map(o =>
            `<button class="acct-btn acct-btn-yes"
                onclick="gradeBetUI(${b.betID}, ${o.outcomeID})">${escapeHtml(o.label)}</button>`).join('');
        tbody.innerHTML += `
            <tr>
                <td>${escapeHtml(b.description)}</td>
                <td style="font-size: 0.85rem;">${outcomes}</td>
                <td>${wager}</td>
                <td style="font-size: 0.85rem;">${closes}</td>
                <td>
                    <div style="display: flex; align-items: flex-start; gap: 6px;">
                        <div style="flex: 1;">
                            <div style="font-size: 0.72rem; color: #A0B2D6; margin-bottom: 3px;">Winner:</div>
                            ${winnerBtns}
                        </div>
                        <div class="acct-menu">
                            <button class="acct-menu-btn" title="More actions"
                                onclick="toggleAcctMenu(event, ${b.betID})">&#8942;</button>
                            <div class="acct-menu-dropdown hidden" id="acct-menu-${b.betID}">
                                <button onclick="cancelBetUI(${b.betID})">Cancel bet &mdash; refund stakes</button>
                                <button class="acct-menu-danger" onclick="deleteBetUI(${b.betID})">Delete bet</button>
                            </div>
                        </div>
                    </div>
                </td>
            </tr>
        `;
    });
}

async function acctCall(request, okMsg) {
    try {
        const res = await request();
        const data = await res.json();
        if (res.ok) {
            showToast(okMsg, 'success');
            loadAccountingPanel();
        } else {
            showToast(data.error || 'Action failed.', 'error');
        }
    } catch (err) {
        showToast('Network error, please try again.', 'error');
        console.error(err);
    }
}

function adminID() {
    const user = JSON.parse(sessionStorage.getItem('user'));
    return user ? user.userID : null;
}

// ── Overflow (⋮) menu on the admin Active Bets rows ──
function closeAllAcctMenus() {
    document.querySelectorAll('.acct-menu-dropdown').forEach(m => m.classList.add('hidden'));
}

function toggleAcctMenu(event, betID) {
    event.stopPropagation();
    const menu = document.getElementById(`acct-menu-${betID}`);
    const wasOpen = menu && !menu.classList.contains('hidden');
    closeAllAcctMenus();
    if (menu && !wasOpen) menu.classList.remove('hidden');
}

// Any click outside a menu closes it.
document.addEventListener('click', closeAllAcctMenus);

async function approveProposalUI(betID) {
    const b = acctCache[betID];
    if (!b) return;
    const odds = {};
    for (const o of (b.outcomes || [])) {
        const v = parseFloat(document.getElementById(`odds-input-${betID}-${o.outcomeID}`).value);
        if (!(v > 1)) {
            showToast(`Enter odds greater than 1.00 for "${o.label}".`, 'error');
            return;
        }
        odds[o.outcomeID] = v;
    }
    const deadline = document.getElementById(`deadline-input-${betID}`).value;
    if (!deadline) {
        showToast('Set the wagering deadline before approving.', 'error');
        return;
    }
    acctCall(() => fetch(`${API_BASE}/bets/${betID}/approve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ odds: odds, deadline: deadline, adminUserID: adminID() })
    }), `Bet #${betID} approved — open until ${deadline.replace('T', ' ')}.`);
}

async function rejectProposalUI(betID) {
    if (!await showConfirmModal('Reject this proposal?',
            `Proposal #${betID} will be removed and the proposer will not see it again.`)) return;
    acctCall(() => fetch(`${API_BASE}/bets/${betID}/reject`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ adminUserID: adminID() })
    }), `Proposal #${betID} rejected.`);
}

async function gradeBetUI(betID, outcomeID) {
    const b = acctCache[betID];
    const o = b ? (b.outcomes || []).find(x => x.outcomeID === outcomeID) : null;
    const label = o ? o.label : `outcome ${outcomeID}`;
    if (!await showConfirmModal(`Grade "${label}" as the winner?`,
            `Bet #${betID} will be settled: wagers on "${label}" are paid out and all others lose. This cannot be undone.`)) return;
    acctCall(() => fetch(`${API_BASE}/bets/${betID}/grade`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ winningOutcomeID: outcomeID, adminUserID: adminID() })
    }), `Bet #${betID} graded — "${label}" wins.`);
}

async function cancelBetUI(betID) {
    closeAllAcctMenus();
    if (!await showConfirmModal('Cancel this bet?',
            `Bet #${betID} will be closed and every stake refunded to the students who wagered.`)) return;
    acctCall(() => fetch(`${API_BASE}/bets/${betID}/grade`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cancelled: true, adminUserID: adminID() })
    }), `Bet #${betID} cancelled — stakes refunded.`);
}

async function deleteBetUI(betID) {
    closeAllAcctMenus();
    if (!await showConfirmModal('Delete this bet?',
            `Bet #${betID} will be removed from every view. Any live wagers are refunded.`)) return;
    acctCall(() => fetch(`${API_BASE}/bets/${betID}?adminUserID=${adminID()}`, {
        method: 'DELETE'
    }), `Bet #${betID} deleted.`);
}
