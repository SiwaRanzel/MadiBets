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

let currentGroupID = null;
let taskCorrectAnswer = true;

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

        currentGroupID = groupID;

        const detail = document.getElementById('group-detail');
        const placeholder = document.getElementById('group-detail-placeholder');
        placeholder.style.display = 'none';
        detail.style.display = 'block';
        detail.innerHTML = '';

        // ── Header: group name + description ──
        const header = document.createElement('div');
        header.className = 'group-detail-header';

        const title = document.createElement('h3');
        title.className = 'group-detail-title';
        title.textContent = data.groupName || `Group ${groupID}`;
        header.appendChild(title);

        const desc = document.createElement('p');
        desc.className = 'group-detail-desc';
        desc.textContent = data.description || 'No description provided.';
        header.appendChild(desc);

        const meta = document.createElement('div');
        meta.className = 'group-detail-meta';
        meta.textContent = `👥 ${data.memberCount !== undefined ? data.memberCount : '—'} members`;
        header.appendChild(meta);

        detail.appendChild(header);

        // Virtual groups (0 / -1) keep a simple summary
        if (groupID <= 0) {
            const info = document.createElement('div');
            info.style = 'margin-top:16px; color:#6C7D93;';
            info.textContent = 'This is a default group. Joining is not required.';
            detail.appendChild(info);
            return;
        }

        // ── Body: members (left) + tasks (right) ──
        const body = document.createElement('div');
        body.className = 'group-detail-body';

        // Members panel
        const membersPanel = document.createElement('div');
        membersPanel.className = 'group-members-panel';
        const membersTitle = document.createElement('h4');
        membersTitle.textContent = 'Members';
        membersPanel.appendChild(membersTitle);

        const members = data.members || [];
        if (members.length === 0) {
            const empty = document.createElement('div');
            empty.className = 'group-empty-state';
            empty.textContent = 'No members yet.';
            membersPanel.appendChild(empty);
        } else {
            members.forEach(m => {
                const row = document.createElement('div');
                row.className = 'group-member-row';

                const avatar = document.createElement('div');
                avatar.className = 'group-member-avatar';
                avatar.textContent = ((m.name || '?')[0] + (m.surname || '?')[0]).toUpperCase();
                row.appendChild(avatar);

                const info = document.createElement('div');
                info.className = 'group-member-info';

                const name = document.createElement('div');
                name.className = 'group-member-name';
                name.textContent = `${m.name || ''} ${m.surname || ''}`.trim();
                info.appendChild(name);

                const role = document.createElement('div');
                role.className = 'group-member-role';
                role.textContent = m.role === 'OWNER' ? 'Owner' : 'Member';
                info.appendChild(role);

                row.appendChild(info);
                membersPanel.appendChild(row);
            });
        }
        body.appendChild(membersPanel);

        // Tasks panel
        const tasksPanel = document.createElement('div');
        tasksPanel.className = 'group-tasks-panel';
        const tasksTitle = document.createElement('h4');
        tasksTitle.textContent = 'Tasks';
        tasksPanel.appendChild(tasksTitle);

        // Load tasks for this group
        try {
            const tasksResp = await fetch(`${API_BASE}/groups/${groupID}/tasks?userId=${user ? user.userID : 0}`);
            const tasksData = await tasksResp.json();
            if (tasksResp.ok && Array.isArray(tasksData)) {
                if (tasksData.length === 0) {
                    const empty = document.createElement('div');
                    empty.className = 'group-empty-state';
                    empty.textContent = 'No tasks yet.';
                    tasksPanel.appendChild(empty);
                } else {
                    tasksData.forEach(task => {
                        tasksPanel.appendChild(renderTaskCard(task, user));
                    });
                }
            } else {
                const empty = document.createElement('div');
                empty.className = 'group-empty-state';
                empty.textContent = 'Could not load tasks.';
                tasksPanel.appendChild(empty);
            }
        } catch (err) {
            console.error(err);
            const empty = document.createElement('div');
            empty.className = 'group-empty-state';
            empty.textContent = 'Could not load tasks.';
            tasksPanel.appendChild(empty);
        }

        body.appendChild(tasksPanel);
        detail.appendChild(body);

        // ── Actions: Join (non-members) / Create Task (lecturers) ──
        const actions = document.createElement('div');
        actions.className = 'group-detail-actions';

        const isMember = data.isMember === true;
        if (!isMember) {
            const joinBtn = document.createElement('button');
            joinBtn.className = 'group-join-btn';
            joinBtn.textContent = 'Join Group';
            joinBtn.onclick = async () => {
                await joinGroup(groupID);
            };
            actions.appendChild(joinBtn);
        }

        if (user && user.userType === 'LECTURER') {
            const createTaskBtn = document.createElement('button');
            createTaskBtn.className = 'group-create-task-btn';
            createTaskBtn.textContent = '+ Create Task';
            createTaskBtn.onclick = () => openCreateTaskModal(groupID);
            actions.appendChild(createTaskBtn);
        }

        if (actions.children.length > 0) {
            detail.appendChild(actions);
        }
    } catch (err) {
        console.error(err);
        showToast('Error loading group detail', 'error');
    }
}

function renderTaskCard(task, user) {
    const card = document.createElement('div');
    card.className = 'task-card';

    const question = document.createElement('p');
    question.className = 'task-card-question';
    question.textContent = task.question || 'No question';
    card.appendChild(question);

    const reward = document.createElement('div');
    reward.className = 'task-card-reward';
    reward.textContent = `Reward: ${parseFloat(task.amount || 0).toFixed(2)} MB`;
    card.appendChild(reward);

    const answered = task.answered !== null && task.answered !== undefined;
    const isCorrect = task.isCorrect === true;

    const toggle = document.createElement('div');
    toggle.className = 'task-answer-toggle';

    const trueBtn = document.createElement('button');
    trueBtn.className = 'task-answer-btn';
    trueBtn.textContent = 'True';
    const falseBtn = document.createElement('button');
    falseBtn.className = 'task-answer-btn';
    falseBtn.textContent = 'False';

    if (answered) {
        trueBtn.disabled = true;
        falseBtn.disabled = true;
        if (task.answered === true) {
            trueBtn.classList.add(isCorrect ? 'correct' : 'incorrect');
        } else {
            falseBtn.classList.add(isCorrect ? 'correct' : 'incorrect');
        }
    } else {
        trueBtn.onclick = () => submitTaskAnswer(task.taskID, true);
        falseBtn.onclick = () => submitTaskAnswer(task.taskID, false);
    }

    toggle.appendChild(trueBtn);
    toggle.appendChild(falseBtn);
    card.appendChild(toggle);

    if (answered) {
        const note = document.createElement('div');
        note.className = `task-answered-note${isCorrect ? '' : ' incorrect'}`;
        note.textContent = isCorrect ? '✓ Correct answer!' : '✗ Incorrect answer';
        card.appendChild(note);
    }

    return card;
}

async function submitTaskAnswer(taskID, answer) {
    const saved = sessionStorage.getItem('user');
    if (!saved) { showToast('You must be logged in to answer tasks.', 'error'); return; }
    const user = JSON.parse(saved);

    try {
        const resp = await fetch(`${API_BASE}/tasks/${taskID}/answer`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userID: user.userID, answer })
        });
        const data = await resp.json();
        if (resp.ok) {
            showToast('Answer submitted!', 'success');
            if (currentGroupID) loadGroupDetail(currentGroupID);
        } else if (resp.status === 409) {
            showToast('You have already answered this task.', 'error');
            if (currentGroupID) loadGroupDetail(currentGroupID);
        } else {
            showToast(data.error || 'Failed to submit answer', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Server error submitting answer', 'error');
    }
}

// ── Create Task Modal (lecturer only) ──
function openCreateTaskModal(groupID) {
    currentGroupID = groupID;
    document.getElementById('new-task-question').value = '';
    document.getElementById('new-task-amount').value = '0';
    taskCorrectAnswer = true;
    setTaskCorrectAnswer(true);
    document.getElementById('create-task-modal').classList.remove('hidden');
}

function closeCreateTaskModal(event) {
    if (event && event.target && event.target.id !== 'create-task-modal') {
        return;
    }
    const modal = document.getElementById('create-task-modal');
    if (modal) modal.classList.add('hidden');
}

function setTaskCorrectAnswer(value) {
    taskCorrectAnswer = value;
    const trueBtn = document.getElementById('task-answer-true');
    const falseBtn = document.getElementById('task-answer-false');
    if (trueBtn && falseBtn) {
        trueBtn.classList.toggle('selected', value === true);
        falseBtn.classList.toggle('selected', value === false);
    }
}

async function createTask() {
    const saved = sessionStorage.getItem('user');
    if (!saved) { showToast('You must be logged in to create tasks.', 'error'); return; }
    const user = JSON.parse(saved);
    if (user.userType !== 'LECTURER') {
        showToast('Only lecturers can create tasks.', 'error');
        return;
    }

    const question = document.getElementById('new-task-question').value.trim();
    const amount = parseFloat(document.getElementById('new-task-amount').value) || 0;
    if (!question) {
        showToast('Please enter a question.', 'error');
        return;
    }

    try {
        const resp = await fetch(`${API_BASE}/groups/${currentGroupID}/tasks`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                question,
                correctAnswer: taskCorrectAnswer,
                amount,
                createdBy: user.userID
            })
        });
        const data = await resp.json();
        if (resp.status === 201) {
            showToast('Task created successfully!', 'success');
            closeCreateTaskModal();
            loadGroupDetail(currentGroupID);
        } else {
            showToast(data.error || 'Failed to create task', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Server error creating task', 'error');
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
    };
    const mapped = navMap[panelId];
    if (Array.isArray(mapped)) {
        mapped.forEach(id => { const el = document.getElementById(id); if (el) el.classList.add('item-active'); });
    } else if (mapped) {
        const activeNav = document.getElementById(mapped);
        if (activeNav) activeNav.classList.add('item-active');
    }

    if (panelId === 'panel-groups') {
        searchGroups('');
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
