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
            } else if (val.length > 0 && !/^\d{7}$/.test(val)) {
                if (!/^\d+$/.test(val)) {
                    msg = 'Staff number must contain only digits.';
                } else {
                    msg = 'Staff number must be exactly 7 digits (currently ' + val.length + ').';
                }
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
let allGroupsSearchTimeout = null;
let myGroupsCache = [];   // full "my groups" list (for client-side filtering)

// Debounced server search for the All Groups column.
function debouncedSearchAllGroups() {
    if (allGroupsSearchTimeout) clearTimeout(allGroupsSearchTimeout);
    allGroupsSearchTimeout = setTimeout(() => {
        const q = (document.getElementById('all-groups-search').value || '').trim();
        loadAllGroups(q);
    }, 300);
}

// Load the groups the current user belongs to (left column).
async function loadMyGroups() {
    const saved = sessionStorage.getItem('user');
    const user = saved ? JSON.parse(saved) : null;
    const container = document.getElementById('my-groups-list');
    if (!container) return;
    if (!user) {
        container.innerHTML = '<p style="color:#6C7D93; text-align:center; padding:40px 8px;">Please log in to see your groups.</p>';
        return;
    }
    try {
        const resp = await fetch(`${API_BASE}/groups?userId=${user.userID}`);
        const data = await resp.json();
        if (!resp.ok) {
            container.innerHTML = '<p style="color:#D9534F; text-align:center; padding:40px 8px;">Could not load groups.</p>';
            return;
        }
        myGroupsCache = Array.isArray(data) ? data : [];
        filterMyGroups();
    } catch (err) {
        console.error(err);
        container.innerHTML = '<p style="color:#D9534F; text-align:center; padding:40px 8px;">Could not load groups.</p>';
    }
}

// Client-side filter of My Groups by the search box.
function filterMyGroups() {
    const container = document.getElementById('my-groups-list');
    if (!container) return;
    const term = (document.getElementById('my-groups-search')?.value || '').trim().toLowerCase();
    const filtered = term
        ? myGroupsCache.filter(g => (g.groupName || '').toLowerCase().includes(term))
        : myGroupsCache;
    renderGroupList(filtered, container, 'You are not a member of any groups yet.');
}

// Load all groups (right column), optionally filtered by a search term server-side.
async function loadAllGroups(q) {
    const container = document.getElementById('all-groups-list');
    if (!container) return;
    const qTrim = (q || '').trim();
    try {
        const url = qTrim ? `${API_BASE}/groups?q=${encodeURIComponent(qTrim)}` : `${API_BASE}/groups`;
        const resp = await fetch(url);
        const data = await resp.json();
        if (!resp.ok) throw new Error(data.error || 'Failed to load groups');
        renderGroupList(Array.isArray(data) ? data : [], container, 'No groups found.');
    } catch (err) {
        console.error(err);
        container.innerHTML = '<p style="color:#D9534F; text-align:center; padding:24px 8px; font-size:0.9rem;">Failed to load groups.</p>';
    }
}

// Refresh both columns.
function refreshGroups() {
    loadMyGroups();
    const allSearch = document.getElementById('all-groups-search');
    loadAllGroups(allSearch ? allSearch.value.trim() : '');
}

function renderGroupList(items, container, emptyMsg) {
    container.innerHTML = '';
    if (!items || items.length === 0) {
        container.innerHTML = `<p style="color:#6C7D93; text-align:center; padding:40px 8px;">${escapeHtml(emptyMsg || 'No groups found.')}</p>`;
        return;
    }

    items.forEach(it => {
        const groupID = it.groupID !== undefined ? it.groupID : it.groupId || 0;
        if (groupID <= 0) return; // skip any virtual/default groups
        const name = it.groupName || it.group_name || `Group ${groupID}`;
        const desc = it.description || '';
        const locked = it.hasPassword === true;

        const row = document.createElement('div');
        row.className = 'group-row-card';
        row.onclick = () => loadGroupDetail(groupID);

        const lock = locked
            ? '<span class="group-lock-badge" title="Password protected"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg></span>'
            : '';

        row.innerHTML =
            `<div style="flex:1; min-width:0;">
                <div class="group-row-name">${escapeHtml(name)} ${lock}</div>
                ${desc ? `<div class="group-row-desc">${escapeHtml(desc)}</div>` : ''}
            </div>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#A0B2D6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>`;
        container.appendChild(row);
    });
}

function escapeHtml(s) {
    if (!s) return '';
    return s.replace(/&/g, '&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

let currentGroupID = null;

// ── Group detail popup ──
function closeGroupDetailModal() {
    const m = document.getElementById('group-detail-modal');
    if (m) m.classList.add('hidden');
}

async function loadGroupDetail(groupID) {
    try {
        const saved = sessionStorage.getItem('user');
        const user = saved ? JSON.parse(saved) : null;
        let url = `${API_BASE}/groups/${groupID}`;
        if (user) url += `?userId=${user.userID}`;
        const resp = await fetch(url);
        const data = await resp.json();
        if (!resp.ok) { showToast(data.error || 'Failed to load group details', 'error'); return; }

        currentGroupID = groupID;
        const body = document.getElementById('group-detail-modal-body');
        body.innerHTML = '';

        // Header
        const header = document.createElement('div');
        // Capacity: cap excludes the owner, so show joined/cap using the non-owner count.
        let capacityText;
        if (data.maxMembers != null) {
            const joined = data.nonOwnerMemberCount != null ? data.nonOwnerMemberCount : '—';
            capacityText = `👥 ${joined} / ${data.maxMembers} members${(typeof joined === 'number' && joined >= data.maxMembers) ? ' · Full' : ''}`;
        } else {
            capacityText = `👥 ${data.memberCount !== undefined ? data.memberCount : '—'} members`;
        }
        header.innerHTML = `<h3 style="margin:0 0 4px; color:#1B2F5E;">${escapeHtml(data.groupName)}</h3>
            <p style="color:#6C7D93; margin:0 0 10px; font-size:0.9rem;">${escapeHtml(data.description || 'No description.')}</p>
            <div style="color:#A0B2D6; font-size:0.85rem; margin-bottom:18px;">${capacityText}</div>`;
        body.appendChild(header);

        // Actions
        const isMember = data.isMember === true;
        const actions = document.createElement('div');
        actions.style = 'display:flex; gap:10px; margin-bottom:20px; flex-wrap:wrap;';
        if (!isMember) {
            const joinBtn = document.createElement('button');
            joinBtn.className = 'group-join-btn';
            joinBtn.textContent = 'Join Group';
            joinBtn.onclick = () => joinGroup(groupID, data.hasPassword === true);
            actions.appendChild(joinBtn);
        } else {
            const leaveBtn = document.createElement('button');
            leaveBtn.style = 'padding:8px 16px; background:none; border:1px solid #D9534F; color:#D9534F; border-radius:8px; font-weight:600; cursor:pointer;';
            leaveBtn.textContent = 'Leave Group';
            leaveBtn.onclick = () => leaveGroup(groupID);
            actions.appendChild(leaveBtn);
        }
        if (isMember && user && user.userType === 'LECTURER') {
            const createBtn = document.createElement('button');
            createBtn.className = 'group-create-task-btn';
            createBtn.textContent = '+ Create Quiz';
            createBtn.onclick = () => openCreateTaskModal(groupID);
            actions.appendChild(createBtn);
        }
        if (actions.children.length) body.appendChild(actions);

        // Two-panel: leaderboard (left) + tasks (right)
        const panels = document.createElement('div');
        panels.style = 'display:flex; gap:20px;';

        // Leaderboard
        const lbPanel = document.createElement('div');
        lbPanel.style = 'flex:1; min-width:0;';
        lbPanel.innerHTML = '<h4 style="margin:0 0 12px; color:#1B2F5E;">Member Leaderboard</h4>';
        const members = data.members || [];
        if (members.length === 0) {
            lbPanel.innerHTML += '<div class="group-empty-state">No members yet.</div>';
        } else {
            members.forEach((m, i) => {
                const row = document.createElement('div');
                row.className = `group-leaderboard-row ${i < 3 ? 'rank-' + (i + 1) : ''}`;
                const balance = m.balance != null ? parseFloat(m.balance).toFixed(2) : '0.00';
                row.innerHTML =
                    `<div class="group-leaderboard-rank">${i + 1}</div>
                     <div class="group-leaderboard-name" onclick="showUserPopup(${m.userID})">${escapeHtml((m.name || '') + ' ' + (m.surname || ''))}</div>
                     <span class="group-leaderboard-role">${m.role === 'OWNER' ? 'Owner' : ''}</span>
                     <span class="group-leaderboard-balance">${balance} MB</span>`;
                lbPanel.appendChild(row);
            });
        }
        panels.appendChild(lbPanel);

        // Tasks (quizzes)
        const tkPanel = document.createElement('div');
        tkPanel.style = 'flex:1; min-width:0;';
        tkPanel.innerHTML = '<h4 style="margin:0 0 12px; color:#1B2F5E;">Quizzes</h4>';
        try {
            const tr = await fetch(`${API_BASE}/groups/${groupID}/tasks?userId=${user ? user.userID : 0}`);
            const td = await tr.json();
            if (tr.ok && Array.isArray(td)) {
                if (td.length === 0) {
                    tkPanel.innerHTML += '<div class="group-empty-state">No quizzes yet.</div>';
                } else {
                    td.forEach(task => tkPanel.appendChild(renderTaskCard(task, user)));
                }
            }
        } catch (err) {
            console.error(err);
            tkPanel.innerHTML += '<div class="group-empty-state">Could not load quizzes.</div>';
        }
        panels.appendChild(tkPanel);
        body.appendChild(panels);

        // Show the modal
        document.getElementById('group-detail-modal').classList.remove('hidden');
    } catch (err) {
        console.error(err);
        showToast('Error loading group detail', 'error');
    }
}

// ── Quiz task card in group detail ──
function renderTaskCard(task, user) {
    const card = document.createElement('div');
    card.style = 'border:1px solid #EEF2F9; border-radius:12px; padding:14px; margin-bottom:10px;';
    const qCount = task.questionCount || 0;
    const submitted = task.submitted === true;

    let statusHtml = '';
    if (submitted) {
        statusHtml = `<span style="color:#28A745; font-weight:700;">Score: ${task.score}/${qCount} — ${task.awardedMadibucks} MB</span>`;
    } else {
        statusHtml = `<span style="color:#6C7D93;">${qCount} question${qCount !== 1 ? 's' : ''}</span>`;
    }
    card.innerHTML =
        `<div style="font-weight:700; color:#1B2F5E; margin-bottom:4px;">${escapeHtml(task.title)}</div>
         <div style="font-size:0.85rem; margin-bottom:10px;">${statusHtml}</div>`;

    const actions = document.createElement('div');
    actions.style = 'display:flex; gap:8px; align-items:center; flex-wrap:wrap;';

    if (!submitted && user && user.userType === 'STUDENT') {
        const takeBtn = document.createElement('button');
        takeBtn.className = 'btn btn-gold-cta';
        takeBtn.style = 'padding:6px 14px; font-size:0.85rem; border-radius:8px;';
        takeBtn.textContent = 'Take Quiz';
        takeBtn.onclick = (e) => { e.stopPropagation(); openTakeQuiz(task.taskID); };
        actions.appendChild(takeBtn);
    } else if (submitted) {
        const viewBtn = document.createElement('button');
        viewBtn.className = 'btn';
        viewBtn.style = 'padding:6px 14px; font-size:0.85rem; border-radius:8px; border:1px solid #E6EDF7;';
        viewBtn.textContent = 'View Results';
        viewBtn.onclick = (e) => { e.stopPropagation(); openTakeQuiz(task.taskID); };
        actions.appendChild(viewBtn);
    }

    // Lecturers can delete a quiz (also enforced on the backend).
    if (user && user.userType === 'LECTURER') {
        const delBtn = document.createElement('button');
        delBtn.className = 'admin-delete-query-btn';
        delBtn.style = 'padding:6px 14px; font-size:0.85rem; border-radius:8px;';
        delBtn.textContent = 'Delete';
        delBtn.onclick = (e) => { e.stopPropagation(); deleteTask(task.taskID); };
        actions.appendChild(delBtn);
    }

    if (actions.children.length) card.appendChild(actions);
    return card;
}

// Delete a quiz (lecturer only). Backend also enforces the role.
async function deleteTask(taskID) {
    const saved = sessionStorage.getItem('user');
    if (!saved) { showToast('You must be logged in.', 'error'); return; }
    const user = JSON.parse(saved);
    if (user.userType !== 'LECTURER') { showToast('Only lecturers can delete quizzes.', 'error'); return; }

    const confirmed = await showConfirmModal('Delete Quiz', 'Are you sure you want to delete this quiz? This also removes all student submissions and cannot be undone.');
    if (!confirmed) return;

    try {
        const resp = await fetch(`${API_BASE}/tasks/${taskID}?userID=${user.userID}`, { method: 'DELETE' });
        const data = await resp.json().catch(() => ({}));
        if (resp.ok) {
            showToast('Quiz deleted.', 'success');
            if (currentGroupID) loadGroupDetail(currentGroupID);
        } else {
            showToast(data.error || 'Failed to delete quiz', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Server error deleting quiz', 'error');
    }
}

// ── Take / view quiz ──
let takeQuizData = null; // holds the fetched quiz while the student answers

function closeTakeQuizModal(event) {
    if (event && event.target && event.target.id !== 'take-quiz-modal') return;
    document.getElementById('take-quiz-modal').classList.add('hidden');
    takeQuizData = null;
}

async function openTakeQuiz(taskID) {
    const saved = sessionStorage.getItem('user');
    const user = saved ? JSON.parse(saved) : null;
    try {
        const resp = await fetch(`${API_BASE}/tasks/${taskID}/quiz?userId=${user ? user.userID : 0}`);
        const data = await resp.json();
        if (!resp.ok) { showToast(data.error || 'Failed to load quiz', 'error'); return; }
        takeQuizData = data;
        renderTakeQuiz(data, user);
        document.getElementById('take-quiz-modal').classList.remove('hidden');
    } catch (err) {
        console.error(err);
        showToast('Error loading quiz', 'error');
    }
}

function renderTakeQuiz(data, user) {
    document.getElementById('take-quiz-title').textContent = data.title || 'Quiz';
    const body = document.getElementById('take-quiz-body');
    body.innerHTML = '';
    const submitBtn = document.getElementById('take-quiz-submit-btn');
    const resultEl = document.getElementById('take-quiz-result');
    resultEl.textContent = '';

    const submitted = data.submitted === true;

    (data.questions || []).forEach((q, i) => {
        const qDiv = document.createElement('div');
        qDiv.className = 'quiz-take-question';
        qDiv.innerHTML = `<p class="quiz-take-prompt"><span class="quiz-take-qnum">Q${i + 1}.</span> ${escapeHtml(q.prompt)}</p>`;

        (q.options || []).forEach(o => {
            const oDiv = document.createElement('div');
            oDiv.className = 'quiz-take-option';
            oDiv.dataset.questionId = q.questionID;
            oDiv.dataset.optionId = o.optionID;
            oDiv.innerHTML = `<span>${escapeHtml(o.optionText)}</span>`;

            if (submitted) {
                oDiv.classList.add('disabled');
                if (o.isCorrect) oDiv.classList.add('correct');
                if (q.chosenOptionID === o.optionID && !o.isCorrect) oDiv.classList.add('incorrect');
                if (q.chosenOptionID === o.optionID) oDiv.classList.add('selected');
            } else if (user && user.userType === 'STUDENT') {
                oDiv.onclick = () => selectQuizOption(q.questionID, o.optionID);
            } else {
                oDiv.classList.add('disabled');
            }
            qDiv.appendChild(oDiv);
        });
        body.appendChild(qDiv);
    });

    if (submitted) {
        submitBtn.style.display = 'none';
        resultEl.textContent = `Score: ${data.score}/${(data.questions || []).length} — ${data.awardedMadibucks} MadiBucks earned`;
    } else if (user && user.userType === 'STUDENT') {
        submitBtn.style.display = '';
        submitBtn.disabled = true;
    } else {
        submitBtn.style.display = 'none';
    }
}

function selectQuizOption(questionID, optionID) {
    // deselect siblings, select this one
    document.querySelectorAll(`.quiz-take-option[data-question-id="${questionID}"]`).forEach(el => {
        el.classList.toggle('selected', parseInt(el.dataset.optionId) === optionID);
    });
    // enable submit if every question has a selection
    const questions = takeQuizData?.questions || [];
    const allAnswered = questions.every(q => {
        return document.querySelector(`.quiz-take-option[data-question-id="${q.questionID}"].selected`);
    });
    const btn = document.getElementById('take-quiz-submit-btn');
    if (btn) btn.disabled = !allAnswered;
}

async function submitQuiz() {
    const saved = sessionStorage.getItem('user');
    if (!saved) { showToast('You must be logged in.', 'error'); return; }
    const user = JSON.parse(saved);
    if (!takeQuizData) return;

    const answers = (takeQuizData.questions || []).map(q => {
        const sel = document.querySelector(`.quiz-take-option[data-question-id="${q.questionID}"].selected`);
        return { questionID: q.questionID, chosenOptionID: sel ? parseInt(sel.dataset.optionId) : null };
    });

    const btn = document.getElementById('take-quiz-submit-btn');
    if (btn) { btn.disabled = true; btn.textContent = 'Submitting…'; }

    try {
        const resp = await fetch(`${API_BASE}/tasks/${takeQuizData.taskID}/submit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userID: user.userID, answers })
        });
        const data = await resp.json();
        if (resp.ok) {
            showToast(`Quiz submitted! ${data.score}/${data.total} correct — ${data.awardedMadibucks} MadiBucks earned`, 'success');
            // Re-fetch the quiz to show results
            await openTakeQuiz(takeQuizData.taskID);
            // Refresh group detail behind the quiz modal
            if (currentGroupID) loadGroupDetail(currentGroupID);
        } else if (resp.status === 409) {
            showToast('You have already completed this quiz.', 'error');
        } else {
            showToast(data.error || 'Failed to submit quiz', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Server error submitting quiz', 'error');
    } finally {
        if (btn) { btn.disabled = false; btn.textContent = 'Submit Quiz'; }
    }
}

// ── Create Quiz Modal (lecturer only) ──
let quizQuestions = []; // builder state

function openCreateTaskModal(groupID) {
    currentGroupID = groupID;
    quizQuestions = [];
    document.getElementById('new-quiz-title').value = '';
    document.getElementById('quiz-questions-container').innerHTML = '';
    addQuizQuestion(); // start with one question
    document.getElementById('create-task-modal').classList.remove('hidden');
}

function closeCreateTaskModal(event) {
    if (event && event.target && event.target.id !== 'create-task-modal') return;
    document.getElementById('create-task-modal').classList.add('hidden');
}

function addQuizQuestion() {
    if (quizQuestions.length >= 10) return;
    const idx = quizQuestions.length;
    const q = { type: 'TRUE_FALSE', options: [
        { text: 'True', isCorrect: true },
        { text: 'False', isCorrect: false }
    ]};
    quizQuestions.push(q);
    renderQuizBuilder();
}

function removeQuizQuestion(idx) {
    quizQuestions.splice(idx, 1);
    renderQuizBuilder();
}

function setQuizQuestionType(idx, type) {
    const q = quizQuestions[idx];
    if (!q) return;
    q.type = type;
    if (type === 'TRUE_FALSE') {
        q.options = [{ text: 'True', isCorrect: true }, { text: 'False', isCorrect: false }];
    } else {
        q.options = [{ text: '', isCorrect: true }, { text: '', isCorrect: false }];
    }
    renderQuizBuilder();
}

function addQuizOption(qIdx) {
    const q = quizQuestions[qIdx];
    if (!q || q.options.length >= 4) return;
    q.options.push({ text: '', isCorrect: false });
    renderQuizBuilder();
}

function renderQuizBuilder() {
    const container = document.getElementById('quiz-questions-container');
    container.innerHTML = '';

    quizQuestions.forEach((q, qi) => {
        const block = document.createElement('div');
        block.className = 'quiz-question-builder';
        let html = `<div class="quiz-question-builder-header"><h4>Question ${qi + 1}</h4><button class="quiz-q-remove" onclick="removeQuizQuestion(${qi})">✕ Remove</button></div>`;
        html += `<div class="form-field-group"><label>Prompt</label><textarea class="quiz-q-prompt" data-qi="${qi}" rows="2" placeholder="Enter the question text">${escapeHtml(q.prompt || '')}</textarea></div>`;
        html += `<div class="quiz-type-toggle">
            <button type="button" class="quiz-type-btn ${q.type === 'TRUE_FALSE' ? 'selected' : ''}" onclick="setQuizQuestionType(${qi},'TRUE_FALSE')">True / False</button>
            <button type="button" class="quiz-type-btn ${q.type === 'MULTIPLE_CHOICE' ? 'selected' : ''}" onclick="setQuizQuestionType(${qi},'MULTIPLE_CHOICE')">Multiple Choice</button>
        </div>`;
        html += '<div class="quiz-options-list">';
        q.options.forEach((o, oi) => {
            const disabled = q.type === 'TRUE_FALSE' ? 'disabled' : '';
            html += `<div class="quiz-option-row">
                <input type="text" class="quiz-o-text" data-qi="${qi}" data-oi="${oi}" value="${escapeHtml(o.text)}" placeholder="Option ${oi + 1}" ${disabled} />
                <label class="quiz-option-correct-label"><input type="radio" name="quiz-correct-${qi}" data-qi="${qi}" data-oi="${oi}" ${o.isCorrect ? 'checked' : ''} onchange="setQuizCorrect(${qi},${oi})" /> Correct</label>
            </div>`;
        });
        html += '</div>';
        if (q.type === 'MULTIPLE_CHOICE' && q.options.length < 4) {
            html += `<button type="button" class="quiz-add-option-btn" onclick="addQuizOption(${qi})">+ Add Option</button>`;
        }
        block.innerHTML = html;
        container.appendChild(block);
    });

    // Sync prompt/option text on every input event
    container.querySelectorAll('.quiz-q-prompt').forEach(el => {
        el.oninput = () => { quizQuestions[parseInt(el.dataset.qi)].prompt = el.value; };
    });
    container.querySelectorAll('.quiz-o-text').forEach(el => {
        el.oninput = () => { quizQuestions[parseInt(el.dataset.qi)].options[parseInt(el.dataset.oi)].text = el.value; };
    });

    const addBtn = document.getElementById('quiz-add-question-btn');
    if (addBtn) addBtn.disabled = quizQuestions.length >= 10;
}

function setQuizCorrect(qi, oi) {
    const q = quizQuestions[qi];
    if (!q) return;
    q.options.forEach((o, i) => o.isCorrect = (i === oi));
}

async function createTask() {
    const saved = sessionStorage.getItem('user');
    if (!saved) { showToast('You must be logged in to create quizzes.', 'error'); return; }
    const user = JSON.parse(saved);
    if (user.userType !== 'LECTURER') { showToast('Only lecturers can create quizzes.', 'error'); return; }

    const title = (document.getElementById('new-quiz-title').value || '').trim();
    if (!title) { showToast('Please enter a quiz title.', 'error'); return; }
    if (quizQuestions.length === 0) { showToast('Add at least one question.', 'error'); return; }

    // Sync final prompt values from the textareas
    document.querySelectorAll('.quiz-q-prompt').forEach(el => {
        quizQuestions[parseInt(el.dataset.qi)].prompt = el.value;
    });
    document.querySelectorAll('.quiz-o-text').forEach(el => {
        quizQuestions[parseInt(el.dataset.qi)].options[parseInt(el.dataset.oi)].text = el.value;
    });

    // Validate
    for (let i = 0; i < quizQuestions.length; i++) {
        const q = quizQuestions[i];
        if (!(q.prompt || '').trim()) { showToast(`Question ${i + 1} needs a prompt.`, 'error'); return; }
        if (q.options.filter(o => o.isCorrect).length !== 1) { showToast(`Question ${i + 1} must have exactly one correct answer.`, 'error'); return; }
        for (const o of q.options) {
            if (!o.text.trim()) { showToast(`Question ${i + 1}: all options need text.`, 'error'); return; }
        }
    }

    const questions = quizQuestions.map(q => ({
        prompt: q.prompt.trim(),
        type: q.type,
        options: q.options.map(o => ({ text: o.text.trim(), isCorrect: o.isCorrect }))
    }));

    try {
        const resp = await fetch(`${API_BASE}/groups/${currentGroupID}/tasks`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title, createdBy: user.userID, questions })
        });
        const data = await resp.json();
        if (resp.status === 201) {
            showToast('Quiz created successfully!', 'success');
            closeCreateTaskModal();
            if (currentGroupID) loadGroupDetail(currentGroupID);
        } else {
            showToast(data.error || 'Failed to create quiz', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Server error creating quiz', 'error');
    }
}

// ── Group create + join (with password support) + leave ──
function openCreateGroupModal() {
    document.getElementById('create-group-modal').classList.remove('hidden');
}

function closeCreateGroupModal(event) {
    if (event && event.target && event.target.id !== 'create-group-modal') return;
    const modal = document.getElementById('create-group-modal');
    if (modal) modal.classList.add('hidden');
    ['new-group-name', 'new-group-desc', 'new-group-password', 'new-group-max-members'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
}

async function createGroup() {
    const name = (document.getElementById('new-group-name').value || '').trim();
    const desc = (document.getElementById('new-group-desc').value || '').trim();
    const password = (document.getElementById('new-group-password').value || '').trim();
    const maxMembersRaw = (document.getElementById('new-group-max-members').value || '').trim();
    const saved = sessionStorage.getItem('user');
    if (!saved) { showToast('You must be logged in to create groups.', 'error'); return; }
    const user = JSON.parse(saved);
    if (!name) { showToast('Please provide a group name.', 'error'); return; }

    let maxMembers = null;
    if (maxMembersRaw) {
        maxMembers = parseInt(maxMembersRaw, 10);
        if (isNaN(maxMembers) || maxMembers < 1) {
            showToast('Max members must be a positive number.', 'error');
            return;
        }
    }

    const payload = { groupName: name, description: desc, createdBy: user.userID };
    if (password) payload.password = password;
    if (maxMembers != null) payload.maxMembers = maxMembers;

    try {
        const resp = await fetch(`${API_BASE}/groups`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await resp.json();
        if (resp.status === 201) {
            showToast('Group created successfully!', 'success');
            closeCreateGroupModal();
            refreshGroups();
        } else {
            showToast(data.error || 'Failed to create group', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Server error creating group', 'error');
    }
}

// Join — prompt for password if the group is protected.
let pendingJoinGroupID = null;

async function joinGroup(groupID, hasPassword) {
    const saved = sessionStorage.getItem('user');
    if (!saved) { showToast('You must be logged in to join groups.', 'error'); return; }

    if (hasPassword) {
        pendingJoinGroupID = groupID;
        document.getElementById('group-password-input').value = '';
        document.getElementById('group-password-modal').classList.remove('hidden');
        document.getElementById('group-password-input').focus();
        return;
    }

    await doJoinGroup(groupID, null);
}

async function submitGroupPassword() {
    const pw = (document.getElementById('group-password-input').value || '').trim();
    if (!pw) { showToast('Please enter the group password.', 'error'); return; }
    closeGroupPasswordModal();
    await doJoinGroup(pendingJoinGroupID, pw);
    pendingJoinGroupID = null;
}

function closeGroupPasswordModal() {
    const m = document.getElementById('group-password-modal');
    if (m) m.classList.add('hidden');
}

async function doJoinGroup(groupID, password) {
    const saved = sessionStorage.getItem('user');
    const user = JSON.parse(saved);
    const payload = { userID: user.userID };
    if (password) payload.password = password;

    try {
        const resp = await fetch(`${API_BASE}/groups/${groupID}/join`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await resp.json();
        if (resp.ok) {
            showToast('Joined group successfully', 'success');
            loadGroupDetail(groupID);
            refreshGroups();
        } else if (resp.status === 409) {
            showToast('You are already a member of this group.', 'error');
        } else if (resp.status === 401 || resp.status === 403) {
            showToast(data.error || 'Incorrect password.', 'error');
        } else {
            showToast(data.error || 'Failed to join group', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Server error joining group', 'error');
    }
}

async function leaveGroup(groupID) {
    const saved = sessionStorage.getItem('user');
    if (!saved) { showToast('You must be logged in.', 'error'); return; }
    const user = JSON.parse(saved);

    const confirmed = await showConfirmModal('Leave Group', 'Are you sure you want to leave this group?');
    if (!confirmed) return;

    try {
        const resp = await fetch(`${API_BASE}/groups/${groupID}/leave`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userID: user.userID })
        });
        const data = await resp.json();
        if (resp.ok) {
            showToast('You have left the group.', 'success');
            loadGroupDetail(groupID);
            refreshGroups();
        } else {
            showToast(data.error || 'Failed to leave group.', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Server error leaving group', 'error');
    }
}

// Initialize groups view when panel is shown via switchPanel
document.addEventListener('click', (e) => {
    // if the groups panel is visible, ensure its list is loaded
    const pg = document.getElementById('panel-groups');
    if (pg && !pg.classList.contains('hidden')) {
        // load once
        if (!pg.dataset.loaded) {
            refreshGroups();
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
const PANELS = ['panel-dashboard', 'panel-dashboard-lecturer', 'panel-groups', 'panel-help', 'panel-dashboard-admin', 'panel-delete-request', 'panel-user-management', 'panel-accounting', 'panel-admin-groups', 'panel-query', 'panel-account', 'panel-friends', 'panel-leaderboard', 'panel-bets'];

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
        'panel-accounting':         'nav-accounting',
        'panel-admin-groups':       'nav-admin-groups',

        'panel-query':              'nav-query',
        'panel-help':               ['nav-help-student', 'nav-help-lecturer'],
        'panel-account':            ['nav-account-student', 'nav-account-lecturer'],
        'panel-friends':            'nav-friends',
        'panel-leaderboard':        ['nav-leaderboard', 'nav-leaderboard-lecturer'],
        'panel-bets':               'nav-bets',
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
        refreshGroups();
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
            checkLoginNotifications(data.user.userID);
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
    const confirmPassword = document.getElementById('reg-confirm-password').value;
    const userType = document.getElementById('reg-usertype').value;

    if (password !== confirmPassword) {
        showToast('Passwords do not match.', 'error');
        return;
    }

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
        refreshAccountingBadge();
    } else if (isLecturer) {
        switchPanel('panel-dashboard-lecturer');
    } else {
        switchPanel('panel-dashboard');
        refreshFriendsRequestBadge(user.userID);
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

    // Find the delete button that triggered this before await
    const btn = typeof event !== 'undefined' && event && event.target ? event.target.closest('button') : null;

    const confirmAction = await showConfirmModal(
        'Delete Account?', 
        'Are you sure you want to permanently delete your MadiBets account? This cannot be undone.'
    );
    if (!confirmAction) return;

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

                // Fetch real data from backend
                fetch(`http://localhost:8081/api/tasks?createdBy=${u.userID}`)
                    .then(res => res.json())
                    .then(data => {
                        let tasks = [];
                        if (Array.isArray(data)) {
                            tasks = data;
                        }

                        if (tasks.length === 0) {
                            tbody.innerHTML = `<tr><td colspan="4" style="text-align: center; padding: 20px; color: #6C7D93;">No tasks found.</td></tr>`;
                            return;
                        }

                        tasks.forEach(task => {
                            // Backend Task has: title, description, amount, etc.
                            // Default to "Academics" and "Active" since DB doesn't track these yet.
                            const type = 'Academics';
                            const status = 'Active';
                            const bucks = task.amount || 0;
                            const title = task.title || 'Untitled Task';
                            const statusColor = '#E2E8F0';
                            const textColor = '#6C7D93';

                            tbody.innerHTML += `
                                <tr style="border-bottom: 1px solid #F0F2F5;">
                                    <td style="padding: 15px 10px; color: #1B2F5E; font-size: 0.95rem;">${type}</td>
                                    <td style="text-align: center; padding: 15px 10px; color: #1B2F5E; font-weight: 600; font-size: 0.95rem;">${bucks}</td>
                                    <td style="text-align: center; padding: 15px 10px; color: #6C7D93; font-size: 0.95rem;">${title}</td>
                                    <td style="text-align: center; padding: 15px 10px;">
                                        <span style="background: ${statusColor}; color: ${textColor}; padding: 5px 15px; border-radius: 6px; font-size: 0.8rem; font-weight: 600;">${status}</span>
                                    </td>
                                </tr>
                            `;
                        });
                    })
                    .catch(err => {
                        console.error('Error fetching tasks:', err);
                        tbody.innerHTML = `<tr><td colspan="4" style="text-align: center; padding: 20px; color: #D9534F;">Failed to load tasks.</td></tr>`;
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
    
    const titleEl = document.getElementById('um-tab-title');
    const colNo = document.getElementById('um-col-no');
    
    const searchBySelect = document.getElementById('um-search-by');
    let idOption = null;
    if (searchBySelect) {
        idOption = searchBySelect.querySelector('option[value="id"]');
    }

    if (role === 'STUDENT') {
        titleEl.textContent = 'Student';
        colNo.textContent = 'Student No.';
        if (idOption) idOption.textContent = 'Search by: Student No.';
    } else if (role === 'LECTURER') {
        titleEl.textContent = 'Lecturer';
        colNo.textContent = 'Staff No.';
        if (idOption) idOption.textContent = 'Search by: Staff No.';
    } else {
        titleEl.textContent = 'Administrator';
        colNo.textContent = 'User ID';
        if (idOption) idOption.textContent = 'Search by: User ID';
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
    
    const searchBySelect = document.getElementById('um-search-by');
    const searchBy = searchBySelect ? searchBySelect.value : 'id';
    
    if (searchTerm) {
        filteredUsers = filteredUsers.filter(u => {
            const fullName = `${u.name || ''} ${u.surname || ''}`.toLowerCase();
            const email = (u.email || '').toLowerCase();
            const no = String(u.studentNo || u.staffNo || u.userID || '').toLowerCase();
            
            if (searchBy === 'id') return no.includes(searchTerm);
            if (searchBy === 'name') return fullName.includes(searchTerm);
            if (searchBy === 'email') return email.includes(searchTerm);
            return false;
        });
    }

    // Pagination info
    const pageInfo = document.getElementById('um-pagination-info');
    if (pageInfo) {
        if (filteredUsers.length === 0) {
            pageInfo.innerHTML = `<span style="font-weight: 700; color: #1B2F5E;">0</span> of 0`;
        } else {
            pageInfo.innerHTML = `<span style="font-weight: 700; color: #1B2F5E;">1</span> of 1`;
        }
    }

    filteredUsers.forEach(u => {
        const no = u.userType === 'STUDENT' ? (u.studentNo || 'N/A') : (u.userType === 'LECTURER' ? (u.staffNo || 'N/A') : u.userID);
        let date = 'N/A';
        if (u.createdDate) {
            const match = u.createdDate.match(/^(\d{4}-\d{2}-\d{2})/);
            if (match) {
                // convert YYYY-MM-DD to DD/MM/YYYY for the table to match previous style
                const parts = match[1].split('-');
                date = `${parts[2]}/${parts[1]}/${parts[0]}`;
            } else {
                date = u.createdDate.split(' ')[0];
            }
        }

        tbody.innerHTML += `
            <tr>
                <td style="color: #A0B2D6;">#${no}</td>
                <td style="color: #1B2F5E; font-weight: 600;">${u.name} ${u.surname}</td>
                <td><a href="mailto:${u.email}" style="color: #6C7D93; text-decoration: underline;">${u.email}</a></td>
                <td style="color: #1B2F5E; font-weight: 600;">${date}</td>
                <td style="text-align: center;">
                    <button class="um-action-btn" onclick="currentSelectedUserId = ${u.userID}; openUserProfileModal()">View</button>
                </td>
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
let cachedFriendsList = [];
let cachedFriendsUserID = null;

async function loadFriendsList(userID) {
    const container = document.getElementById('friends-list-container');
    const countEl = document.getElementById('friends-count');
    if (!container) return;

    // Clear search when reloading
    const searchInput = document.getElementById('friends-search-input');
    if (searchInput) searchInput.value = '';

    cachedFriendsUserID = userID;

    try {
        const response = await fetch(`${API_BASE}/friends/${userID}`);
        const friends = await response.json();

        if (!response.ok) {
            container.innerHTML = `<p style="color: #D9534F; font-size: 0.95rem;">Failed to load friends.</p>`;
            return;
        }

        cachedFriendsList = friends;

        if (friends.length === 0) {
            container.innerHTML = `<p style="color: #A0B2D6; font-size: 0.95rem;">You haven't added any friends yet.</p>`;
            if (countEl) countEl.textContent = '0 friends';
            return;
        }

        if (countEl) countEl.textContent = `${friends.length} friend${friends.length !== 1 ? 's' : ''}`;
        renderFriendsList(friends, userID);
    } catch (err) {
        container.innerHTML = `<p style="color: #D9534F; font-size: 0.95rem;">Error loading friends.</p>`;
        console.error(err);
    }
}

function renderFriendsList(friends, userID) {
    const container = document.getElementById('friends-list-container');
    if (!container) return;

    if (friends.length === 0) {
        container.innerHTML = `<p style="color: #A0B2D6; font-size: 0.95rem;">No friends match your search.</p>`;
        return;
    }

    container.innerHTML = friends.map(f => {
        const friendName = f.requesterID === userID ? f.addresseName : f.requesterName;
        const friendStudentNo = f.requesterID === userID ? (f.addresseStudentNo || '') : (f.requesterStudentNo || '');
        const friendId = f.requesterID === userID ? f.addresseID : f.requesterID;
        const isOnline = Math.random() > 0.5;
        const statusDot = isOnline
            ? `<span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #28A745; margin-right: 5px;"></span><span style="color: #28A745; font-size: 0.75rem; font-weight: 600;">Online</span>`
            : `<span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #D9534F; margin-right: 5px;"></span><span style="color: #D9534F; font-size: 0.75rem; font-weight: 600;">Offline</span>`;
        return `
            <div style="display: flex; justify-content: space-between; align-items: center; padding: 15px 20px; background: #F8FAFC; border-radius: 12px;">
                <div style="display: flex; align-items: center; gap: 12px; cursor: pointer;" onclick="showUserPopup(${friendId})">
                    <div style="width: 40px; height: 40px; background: #1B2F5E; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#F5A623" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/>
                        </svg>
                    </div>
                    <div>
                        <span style="color: #1B2F5E; font-weight: 600; font-size: 0.95rem; display: block;">${friendName}</span>
                        ${friendStudentNo ? `<span style="color: #A0B2D6; font-size: 0.8rem;">${friendStudentNo}</span>` : ''}
                    </div>
                </div>
                <div style="display: flex; align-items: center; gap: 14px;">
                    <div style="display: flex; align-items: center;">${statusDot}</div>
                    <button onclick="removeFriend(${f.friendshipID})" style="background: none; border: 1px solid #D9534F; color: #D9534F; padding: 6px 16px; border-radius: 6px; font-size: 0.8rem; font-weight: 600; cursor: pointer;">Unfriend</button>
                </div>
            </div>
        `;
    }).join('');
}

function filterFriendsList() {
    const query = document.getElementById('friends-search-input').value.trim().toLowerCase();
    const userID = cachedFriendsUserID;

    if (!query) {
        renderFriendsList(cachedFriendsList, userID);
        return;
    }

    const filtered = cachedFriendsList.filter(f => {
        const friendName = (f.requesterID === userID ? f.addresseName : f.requesterName) || '';
        const friendStudentNo = (f.requesterID === userID ? (f.addresseStudentNo || '') : (f.requesterStudentNo || ''));
        return friendName.toLowerCase().includes(query) || friendStudentNo.includes(query);
    });

    renderFriendsList(filtered, userID);
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

        setFriendsRequestBadge(pending.length);

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

/** Red bubble on the Friends nav tab: number of incoming pending requests.
 *  Same pattern as the Query / Delete Request / Accounting badges — hidden at zero. */
function setFriendsRequestBadge(count) {
    const badge = document.getElementById('friends-request-badge');
    if (!badge) return;
    badge.textContent = count;
    badge.style.display = count > 0 ? 'inline-flex' : 'none';
}

/** Standalone refresh for login/init, when the Friends panel is not open. */
async function refreshFriendsRequestBadge(userID) {
    try {
        const res = await fetch(`${API_BASE}/friends/${userID}/pending`);
        const pending = await res.json();
        if (res.ok) setFriendsRequestBadge((pending || []).length);
    } catch (err) {
        console.error('Failed to refresh friends request badge:', err);
    }
}

// C200 — Browse & Add Friend (profile-based flow)
let selectedProfileUser = null;

function toggleBrowseStudents() {
    const section = document.getElementById('browse-students-section');
    const profileView = document.getElementById('student-profile-view');
    if (section.style.display === 'none') {
        section.style.display = 'block';
        profileView.style.display = 'none';
        document.getElementById('browse-search-input').focus();
    } else {
        section.style.display = 'none';
        profileView.style.display = 'none';
    }
}

let searchDebounce = null;
function searchStudents() {
    clearTimeout(searchDebounce);
    searchDebounce = setTimeout(async () => {
        const query = document.getElementById('browse-search-input').value.trim();
        const container = document.getElementById('browse-students-list');
        if (!container) return;

        if (query.length < 2) {
            container.innerHTML = '<p style="color: #A0B2D6; font-size: 0.9rem; text-align: center; padding: 10px;">Type at least 2 characters to search...</p>';
            return;
        }

        const user = JSON.parse(sessionStorage.getItem('user'));
        if (!user) return;

        try {
            const resp = await fetch(`${API_BASE}/users/all`);
            if (!resp.ok) { container.innerHTML = '<p style="color:#D9534F; text-align:center;">Failed to load users.</p>'; return; }
            const allUsers = await resp.json();

            // Filter by search term (name, surname, studentNo) and exclude self
            const q = query.toLowerCase();
            const results = allUsers.filter(u =>
                u.userID !== user.userID &&
                u.userType === 'STUDENT' &&
                (`${u.name} ${u.surname}`.toLowerCase().includes(q) ||
                 (u.studentNo && u.studentNo.includes(q)))
            );

            if (results.length === 0) {
                container.innerHTML = '<p style="color: #A0B2D6; font-size: 0.9rem; text-align: center; padding: 10px;">No students found.</p>';
                return;
            }

            container.innerHTML = results.slice(0, 10).map(u => `
                <div onclick="viewStudentProfile(${u.userID}, '${escapeHtml(u.name)} ${escapeHtml(u.surname)}', '${u.studentNo || 'N/A'}', '${escapeHtml(u.email)}')" style="display: flex; align-items: center; gap: 12px; padding: 12px 16px; background: #F8FAFC; border-radius: 10px; cursor: pointer; transition: background 0.15s;" onmouseover="this.style.background='#EEF2F7'" onmouseout="this.style.background='#F8FAFC'">
                    <div style="width: 36px; height: 36px; background: #1B2F5E; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#F5A623" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/></svg>
                    </div>
                    <div style="flex: 1;">
                        <div style="color: #1B2F5E; font-weight: 600; font-size: 0.9rem;">${escapeHtml(u.name)} ${escapeHtml(u.surname)}</div>
                        <div style="color: #A0B2D6; font-size: 0.8rem;">${u.studentNo || ''}</div>
                    </div>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#A0B2D6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
                </div>
            `).join('');
        } catch (err) {
            container.innerHTML = '<p style="color:#D9534F; text-align:center;">Error searching users.</p>';
            console.error(err);
        }
    }, 300);
}

function viewStudentProfile(userID, name, studentNo, email) {
    selectedProfileUser = { userID, name, studentNo, email };
    document.getElementById('profile-view-name').textContent = name;
    document.getElementById('profile-view-studentno').textContent = 'Student No: ' + studentNo;
    document.getElementById('profile-view-email').textContent = email;
    document.getElementById('browse-students-section').style.display = 'none';
    document.getElementById('student-profile-view').style.display = 'block';
}

function closeProfileView() {
    document.getElementById('student-profile-view').style.display = 'none';
    document.getElementById('browse-students-section').style.display = 'block';
    selectedProfileUser = null;
}

async function sendFriendRequestFromProfile() {
    if (!selectedProfileUser) return;
    const user = JSON.parse(sessionStorage.getItem('user'));
    if (!user) return;

    const btn = document.getElementById('btn-profile-send-request');
    setButtonLoading(btn, true);

    try {
        const response = await fetch(`${API_BASE}/friends/request`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ requesterID: user.userID, addresseID: selectedProfileUser.userID })
        });

        const data = await response.json();

        if (response.ok) {
            showToast(data.message || 'Friend request sent!', 'success');
            closeProfileView();
            document.getElementById('browse-students-section').style.display = 'none';
            loadFriendsData();
        } else {
            showToast(data.error || 'Failed to send request.', 'error');
        }
    } catch (err) {
        showToast('Network error.', 'error');
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

// ── Login notifications (B400: users are informed of graded bet outcomes) ──
let notifUserID = null;

async function checkLoginNotifications(userID) {
    try {
        const res = await fetch(`${API_BASE}/notifications/${userID}`);
        const data = await res.json();
        if (!res.ok) return;
        const notifs = data.notifications || [];
        if (notifs.length === 0) return;

        notifUserID = userID;
        const list = document.getElementById('notif-list');
        list.innerHTML = notifs.map(n => {
            const won = n.message.startsWith('You won');
            const refund = n.message.startsWith('Bet cancelled');
            const accent = won ? '#28A745' : refund ? '#B8860B' : '#D9534F';
            const when = n.createdDate ? String(n.createdDate).slice(0, 16).replace('T', ' ') : '';
            return `
                <div style="border-left: 3px solid ${accent}; background: #F8FAFC; border-radius: 0 8px 8px 0; padding: 10px 14px;">
                    <div style="color: #2A3B50; font-size: 0.9rem;">${escapeHtml(n.message)}</div>
                    <div style="color: #A0B2D6; font-size: 0.75rem; margin-top: 3px;">${when}</div>
                </div>`;
        }).join('');
        document.getElementById('notif-modal').classList.remove('hidden');
    } catch (err) {
        console.error('Failed to load notifications:', err);
    }
}

function closeNotifModal() {
    document.getElementById('notif-modal').classList.add('hidden');
    // Flag them seen only once the user has actually had them on screen.
    if (notifUserID !== null) {
        fetch(`${API_BASE}/notifications/${notifUserID}/seen`, { method: 'POST' })
            .catch(err => console.error('Failed to mark notifications seen:', err));
        notifUserID = null;
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

    await populateLeaderboardGroupOptions(user.userID);

    await Promise.all([
        loadUserStats(user.userID),
        loadRankings(),
        loadBetHistory(user.userID)
    ]);
}

// Fill the "Show" dropdown with the user's groups, listed after Everyone/My Friends.
// Each group option carries value "group:<groupID>".
async function populateLeaderboardGroupOptions(userID) {
    const select = document.getElementById('lb-scope-select');
    if (!select) return;

    // Remove any previously-added group options (keep the first two fixed ones).
    Array.from(select.querySelectorAll('option[data-group="1"]')).forEach(o => o.remove());

    try {
        const res = await fetch(`${API_BASE}/groups?userId=${userID}`);
        const groups = await res.json();
        if (!res.ok || !Array.isArray(groups) || groups.length === 0) return;

        // Optional visual separator (disabled option) before the groups.
        const sep = document.createElement('option');
        sep.disabled = true;
        sep.textContent = '── My Groups ──';
        sep.setAttribute('data-group', '1');
        select.appendChild(sep);

        groups.forEach(g => {
            const opt = document.createElement('option');
            opt.value = `group:${g.groupID}`;
            opt.textContent = g.groupName || `Group ${g.groupID}`;
            opt.setAttribute('data-group', '1');
            select.appendChild(opt);
        });
    } catch (err) {
        console.error('Failed to load groups for leaderboard filter:', err);
    }
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

        // Fetch lecturer's created groups
        const groupsRes = await fetch(`${API_BASE}/groups?createdBy=${user.userID}`);
        if (groupsRes.ok) {
            const groups = await groupsRes.json();
            const container = document.getElementById('lecturer-groups-container');
            if (container) {
                container.innerHTML = '';
                if (groups.length === 0) {
                    container.innerHTML = '<p style="color: #6C7D93; font-style: italic;">No groups created yet.</p>';
                } else {
                    groups.forEach(g => {
                        const badge = g.groupName ? g.groupName.substring(0, 4).toUpperCase() : 'GRP';
                        const name = g.groupName || 'Unnamed Group';
                        const desc = g.description || 'No description';
                        const dateStr = g.createdDate ? new Date(g.createdDate).toLocaleDateString() : 'recently';
                        
                        container.innerHTML += `
                            <div class="lect-group-card">
                                <div class="lect-group-header">
                                    <div class="lect-group-badge">${badge}</div>
                                    <div>
                                        <p class="lect-group-name">${name}</p>
                                        <p class="lect-group-meta">${desc}</p>
                                    </div>
                                    <div class="lect-group-pill lect-pill-green">Active</div>
                                </div>
                                <ul class="lect-group-updates">
                                    <li>🟢 Created on: <strong>${dateStr}</strong></li>
                                    <li>💬 Ready for new activities</li>
                                </ul>
                                <button class="lect-group-btn" onclick="switchPanel('panel-groups'); return false;">View Group
                                    →</button>
                            </div>
                        `;
                    });
                }
            }
        }

    } catch (e) {
        console.error("Failed to load lecturer stats", e);
    }
}

// C500 — Load top rankings table
let currentLeaderboardSort = 'balance';
let currentLeaderboardScope = 'all';   // 'all' | 'friends' | 'group'
let currentLeaderboardGroupID = 0;

async function loadRankings(sortBy) {
    if (sortBy) currentLeaderboardSort = sortBy;
    const tbody = document.getElementById('leaderboard-table-body');
    if (!tbody) return;

    const user = JSON.parse(sessionStorage.getItem('user'));
    const uid = user ? user.userID : 0;

    let url = `${API_BASE}/leaderboard/rankings?limit=20&sortBy=${currentLeaderboardSort}`
            + `&scope=${currentLeaderboardScope}&userID=${uid}`;
    if (currentLeaderboardScope === 'group') {
        url += `&groupID=${currentLeaderboardGroupID}`;
    }

    try {
        const response = await fetch(url);
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
                    <td style="padding: 14px 10px; color: #1B2F5E; font-weight: ${isCurrentUser ? '700' : '500'}; cursor: pointer;" onclick="showUserPopup(${r.userID})">${r.name}${isCurrentUser ? ' (You)' : ''}</td>
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
let cachedBetHistory = [];

async function loadBetHistory(userID) {
    const tbody = document.getElementById('bet-history-table-body');
    if (!tbody) return;

    // Reset filters
    const typeFilter = document.getElementById('bet-history-type-filter');
    const statusFilter = document.getElementById('bet-history-status-filter');
    if (typeFilter) typeFilter.value = 'all';
    if (statusFilter) statusFilter.value = 'all';

    try {
        const response = await fetch(`${API_BASE}/leaderboard/history/${userID}`);
        const bets = await response.json();

        if (!response.ok) {
            tbody.innerHTML = `<tr><td colspan="5" style="padding: 20px; text-align: center; color: #D9534F;">Failed to load bet history.</td></tr>`;
            return;
        }

        cachedBetHistory = bets;
        renderBetHistory(bets);
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="5" style="padding: 20px; text-align: center; color: #D9534F;">Error loading bet history.</td></tr>`;
        console.error(err);
    }
}

function filterBetHistory() {
    const typeVal = document.getElementById('bet-history-type-filter').value;
    const statusVal = document.getElementById('bet-history-status-filter').value;

    let filtered = cachedBetHistory;

    if (typeVal !== 'all') {
        filtered = filtered.filter(b => {
            const desc = (b.description || b.eventDescription || '').toLowerCase();
            const type = typeVal.toLowerCase();
            return desc.includes(type);
        });
    }

    if (statusVal !== 'all') {
        if (statusVal === 'PENDING') {
            filtered = filtered.filter(b => b.outcome === 'PENDING' || b.outcome === null);
        } else if (statusVal === 'WON') {
            filtered = filtered.filter(b => b.outcome === 'YES');
        } else if (statusVal === 'LOST') {
            filtered = filtered.filter(b => b.outcome === 'NO');
        }
    }

    renderBetHistory(filtered);
}

function renderBetHistory(bets) {
    const tbody = document.getElementById('bet-history-table-body');
    if (!tbody) return;

    if (bets.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" style="padding: 20px; text-align: center; color: #A0B2D6;">No bets match the selected filters.</td></tr>`;
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
}

// Read both leaderboard dropdowns (Sort by + Show) and reload the rankings.
function applyLeaderboardControls() {
    const sortSelect  = document.getElementById('lb-sort-select');
    const scopeSelect = document.getElementById('lb-scope-select');

    if (sortSelect) currentLeaderboardSort = sortSelect.value;

    const scopeVal = scopeSelect ? scopeSelect.value : 'all';
    if (scopeVal.startsWith('group:')) {
        currentLeaderboardScope = 'group';
        currentLeaderboardGroupID = parseInt(scopeVal.split(':')[1], 10) || 0;
    } else {
        currentLeaderboardScope = scopeVal;   // 'all' or 'friends'
        currentLeaderboardGroupID = 0;
    }

    loadRankings();
}


let adminQueriesCache = [];

// ── Admin Query Management ──

// Client-side filter state layered on top of adminQueriesCache
let queryStatusFilter = 'ALL';   // 'ALL' | 'OPEN' | 'RESOLVED'
let querySearchTerm = '';

async function loadAdminQueries() {
    initQueryFilters();
    try {
        const response = await fetch(`${API_BASE}/queries`);
        const queries = await response.json();
        
        if (response.ok) {
            adminQueriesCache = queries;
            // Badge always reflects ALL open queries, not the filtered view
            const openQueries = queries.filter(q => q.resolvedStatus === 'OPEN');
            const badge = document.getElementById('query-badge');
            if (badge) {
                badge.textContent = openQueries.length;
                badge.style.display = openQueries.length > 0 ? 'inline-flex' : 'none';
            }
            applyQueryFilters();
        } else {
            console.error('Failed to load queries');
        }
    } catch (error) {
        console.error('Error loading queries:', error);
    }
}

// Apply the active search term + status filter to the cache and render
function applyQueryFilters() {
    const term = querySearchTerm.trim().toLowerCase();
    const filtered = adminQueriesCache.filter(q => {
        const matchesStatus = queryStatusFilter === 'ALL' || q.resolvedStatus === queryStatusFilter;
        if (!matchesStatus) return false;
        if (!term) return true;
        const title = (q.title || '').toLowerCase();
        const email = (q.email || '').toLowerCase();
        return title.includes(term) || email.includes(term);
    });
    renderQueryTable(filtered);
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
            <td><button class="admin-view-query-btn" onclick="viewQuery(${q.queryID})">View</button></td>
            <td>
                <div class="admin-option-dropdown">
                    <button class="admin-option-trigger" onclick="toggleQueryOptions(event, ${q.queryID})">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="1"/><circle cx="12" cy="5" r="1"/><circle cx="12" cy="19" r="1"/></svg>
                    </button>
                    <div id="query-options-${q.queryID}" class="admin-dropdown-menu">
                        ${q.resolvedStatus === 'OPEN'
                            ? `<div class="admin-dropdown-item" onclick="setQueryStatus(${q.queryID}, 'RESOLVED')">Mark as Resolved</div>`
                            : `<div class="admin-dropdown-item" onclick="setQueryStatus(${q.queryID}, 'OPEN')">Reopen (Change to Open)</div>`}
                    </div>
                </div>
            </td>
            <td><button class="admin-delete-query-btn" onclick="deleteQuery(${q.queryID})">Delete</button></td>
        `;
        list.appendChild(row);
    });

    if (paginationInfo) {
        const count = queries.length;
        paginationInfo.innerHTML = count === 0
            ? `<span style="font-weight: 700; color: #1B2F5E;">0</span> of 0`
            : `<span style="font-weight: 700; color: #1B2F5E;">${count}</span> of ${count}`;
    }
}

function viewQuery(queryID) {
    const q = adminQueriesCache.find(item => item.queryID === queryID);
    if (!q) {
        showToast('Query not found.', 'error');
        return;
    }

    const titleEl = document.getElementById('query-view-title');
    const metaEl = document.getElementById('query-view-meta');
    const bodyEl = document.getElementById('query-view-body');
    const closeBtn = document.getElementById('query-view-close');

    titleEl.textContent = q.title || 'Query';

    const date = q.queryDate ? new Date(q.queryDate) : null;
    const dateStr = date && !isNaN(date) ? date.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : '';
    const fromStr = q.email ? `From: ${q.email}` : '';
    metaEl.textContent = [dateStr, fromStr].filter(Boolean).join(' · ');

    bodyEl.textContent = q.description || 'No description provided.';

    if (closeBtn) closeBtn.focus();
    document.getElementById('query-view-modal').classList.remove('hidden');
}

function closeQueryViewModal() {
    const modal = document.getElementById('query-view-modal');
    if (modal) modal.classList.add('hidden');
}

function toggleQueryOptions(event, queryID) {
    event.stopPropagation();
    // Close all other dropdowns (including the filter menu)
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

// ── Query search & status filter ──

function initQueryFilters() {
    const searchInput = document.getElementById('query-search-input');
    if (searchInput && !searchInput.dataset.bound) {
        searchInput.addEventListener('input', (e) => {
            querySearchTerm = e.target.value || '';
            applyQueryFilters();
        });
        searchInput.dataset.bound = 'true';
    }
}

function toggleFilterMenu(event) {
    event.stopPropagation();
    const menu = document.getElementById('query-filter-menu');
    if (!menu) return;
    // Close row option dropdowns when opening the filter menu
    document.querySelectorAll('.admin-dropdown-menu').forEach(m => {
        if (m.id !== 'query-filter-menu') m.classList.remove('show');
    });
    menu.classList.toggle('show');
}

function setQueryStatusFilter(value) {
    queryStatusFilter = value;

    // Update the Filters button label to reflect the active filter
    const labelEl = document.getElementById('query-filter-label');
    if (labelEl) {
        labelEl.textContent = value === 'ALL' ? 'Filters' : (value === 'OPEN' ? 'Open' : 'Resolved');
    }

    // Highlight the active option
    document.querySelectorAll('#query-filter-menu .admin-dropdown-item').forEach(item => {
        item.classList.toggle('active', item.dataset.value === value);
    });

    const menu = document.getElementById('query-filter-menu');
    if (menu) menu.classList.remove('show');

    applyQueryFilters();
}

async function setQueryStatus(queryID, status) {
    try {
        const response = await fetch(`${API_BASE}/queries/${queryID}/status`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status })
        });

        // Close any open dropdown menu
        document.querySelectorAll('.admin-dropdown-menu').forEach(menu => menu.classList.remove('show'));

        if (response.ok) {
            showToast(status === 'RESOLVED' ? 'Query marked as resolved' : 'Query reopened', 'success');
            loadAdminQueries();
        } else {
            const data = await response.json().catch(() => ({}));
            showToast(data.error || 'Failed to update query status', 'error');
        }
    } catch (error) {
        showToast('Error updating query status', 'error');
        console.error(error);
    }
}

async function deleteQuery(queryID) {
    const confirmed = await showConfirmModal('Delete Query', 'Are you sure you want to delete this query? This action cannot be undone.');
    if (!confirmed) return;

    try {
        const response = await fetch(`${API_BASE}/queries/${queryID}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showToast('Query deleted', 'success');
            loadAdminQueries();
        } else {
            const data = await response.json().catch(() => ({}));
            showToast(data.error || 'Failed to delete query', 'error');
        }
    } catch (error) {
        showToast('Error deleting query', 'error');
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

window.allDeleteRequests = [];

async function loadDeleteRequests() {
    try {
        const response = await fetch(`${API_BASE}/users/delete-requests`);
        if (!response.ok) return;
        const requests = await response.json();

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
        
        const adminDeleteStat = document.getElementById('admin-delete-requests');
        if (adminDeleteStat) {
            adminDeleteStat.textContent = newCount;
        }
        
        window.allDeleteRequests = uniqueRequests;
        filterDeleteRequests();
    } catch (err) {
        console.error('Failed to load delete requests:', err);
    }
}

window.filterDeleteRequests = function() {
    const searchInput = document.getElementById('admin-delete-search-input');
    const searchTerm = searchInput ? searchInput.value.toLowerCase() : '';
    
    const searchBySelect = document.getElementById('admin-delete-search-by');
    const searchBy = searchBySelect ? searchBySelect.value : 'name';
    
    let filtered = window.allDeleteRequests;
    if (searchTerm) {
        filtered = filtered.filter(req => {
            const name = (req.userName || '').toLowerCase();
            const email = (req.userEmail || '').toLowerCase();
            
            if (searchBy === 'name') return name.includes(searchTerm);
            if (searchBy === 'email') return email.includes(searchTerm);
            return false;
        });
    }
    
    renderDeleteRequests(filtered);
}

function renderDeleteRequests(requestsToRender) {
    const tbody = document.getElementById('delete-request-tbody');
    if (!tbody) return;
    
    const count = requestsToRender.length;
    
    const paginationInfo = document.getElementById('delete-request-pagination-info');
    if (paginationInfo) {
        if (count === 0) {
            paginationInfo.innerHTML = `<span style="font-weight: 700; color: #1B2F5E;">0</span> of 0`;
        } else {
            paginationInfo.innerHTML = `<span style="font-weight: 700; color: #1B2F5E;">1</span> of 1`;
        }
    }

    tbody.innerHTML = '';
    requestsToRender.forEach(req => {
        const dateStr = new Date(req.requestDate).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
        
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td style="padding: 12px 15px; color: #6C7A9C; font-size: 0.9rem;">${dateStr}</td>
            <td style="padding: 12px 15px; font-weight: 600; color: #1B2F5E; font-size: 0.95rem;">${escapeHtml(req.userName || '')}</td>
            <td style="padding: 12px 15px;"><a href="mailto:${req.userEmail}" style="color: #6C7A9C; text-decoration: underline; font-size: 0.9rem;">${escapeHtml(req.userEmail || '')}</a></td>
            <td style="padding: 12px 15px; text-align: center;">
                <div style="display: flex; gap: 8px; justify-content: center;">
                    <button onclick="reinstateUser(${req.requestID})" style="background: white; border: 1px solid #1B2F5E; color: #1B2F5E; padding: 4px 12px; border-radius: 20px; font-size: 0.75rem; font-weight: 600; cursor: pointer; transition: all 0.2s;">Reinstate</button>
                    <button onclick="deleteUserPermanently(${req.userID})" style="background: #D9534F; border: none; color: white; padding: 4px 12px; border-radius: 20px; font-size: 0.75rem; font-weight: 600; cursor: pointer; transition: all 0.2s;">Delete</button>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
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
    const confirmed = await showConfirmModal(
        'Delete User?', 
        'Are you sure you want to delete this user? This action cannot be undone and will remove all associated bets and account data.'
    );
    if (!confirmed) return;
    
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
    const setStat = (id, value) => {
        const el = document.getElementById(id);
        if (el) el.textContent = value;
    };

    // 1. Dashboard stat cards
    try {
        const res = await fetch(`${API_BASE}/admin/dashboard-stats`);
        if (res.ok) {
            const stats = await res.json();
            
            setStat('stat-bets-proposed', stats.betsProposedToday);
            setStat('stat-bets-placed', stats.betsPlacedToday);
            setStat('stat-bets-pending', stats.betsPendingReview);
            setStat('stat-upcoming-events', stats.upcomingEvents);
            
            setStat('stat-users-total', stats.totalUsers);
            setStat('stat-users-joined-today', stats.usersJoinedToday);
            setStat('stat-support-queries', stats.openSupportQueries);
            setStat('stat-bonus-awarded', stats.usersRewardedToday);
            
            setStat('admin-total-users', stats.totalUsers);
            setStat('admin-new-proposals', stats.betsPendingReview);
            setStat('admin-weekly-growth', stats.usersJoinedToday);
        }
    } catch (err) {
        console.error('Failed to load admin dashboard stats:', err);
    }
    
    // 2. Populate dashboard Delete Requests card
    try {
        const res = await fetch(`${API_BASE}/users/delete-requests`);
        if (res.ok) {
            const requests = await res.json();
            const newRequests = requests.filter(r => r.status === 'NEW');
            const uniqueRequests = [];
            const seenUsers = new Set();
            for (const req of newRequests) {
                if (!seenUsers.has(req.userID)) {
                    seenUsers.add(req.userID);
                    uniqueRequests.push(req);
                }
            }
            const tbody = document.getElementById('dashboard-delete-requests-tbody');
            if (tbody) {
                if (uniqueRequests.length === 0) {
                    tbody.innerHTML = '<tr><td style="color:#A0B2D6;">No pending requests</td></tr>';
                } else {
                    tbody.innerHTML = uniqueRequests.slice(0, 6).map(r =>
                        `<tr><td>${r.userName || ''}</td></tr>`
                    ).join('');
                }
            }
        }
    } catch (err) {
        console.error('Failed to load dashboard delete requests:', err);
    }
    
    // 3. Populate dashboard Groups card
    try {
        const res = await fetch(`${API_BASE}/groups`);
        if (res.ok) {
            const groups = await res.json();
            const container = document.getElementById('dashboard-groups-list');
            if (container) {
                if (groups.length === 0) {
                    container.innerHTML = '<div class="admin-league-row"><span style="color:#A0B2D6;">No groups yet</span></div>';
                } else {
                    container.innerHTML = groups.slice(0, 6).map(g =>
                        `<div class="admin-league-row"><span>${g.groupName}</span><button class="admin-league-btn" onclick="switchPanel('panel-groups')">View</button></div>`
                    ).join('');
                }
            }
        }
    } catch (err) {
        console.error('Failed to load dashboard groups:', err);
    }
    
    // 4. Populate Bet Types pie chart from active bets
    try {
        const res = await fetch(`${API_BASE}/bets/active`);
        if (res.ok) {
            const data = await res.json();
            const bets = data.bets || [];
            
            const typeColors = {
                'Academics': '#34D399',
                'Sports': '#3B82F6',
                'Social': '#F87171',
                'Class Room': '#F5A623',
                'Other': '#A78BFA'
            };
            
            // Count bets by type
            const counts = {};
            bets.forEach(b => {
                const idx = b.description.indexOf(':');
                let type = 'Other';
                if (idx > 0) {
                    const prefix = b.description.slice(0, idx).trim();
                    const known = Object.keys(typeColors);
                    const match = known.find(k => k.toLowerCase() === prefix.toLowerCase());
                    if (match) type = match;
                }
                counts[type] = (counts[type] || 0) + 1;
            });
            
            const total = bets.length;
            const pie = document.getElementById('admin-bet-pie');
            const legend = document.getElementById('admin-bet-legend');
            
            if (pie && legend && total > 0) {
                // Build conic-gradient
                let gradientParts = [];
                let cumulative = 0;
                const entries = Object.entries(counts).sort((a, b) => b[1] - a[1]);
                
                entries.forEach(([type, count]) => {
                    const pct = (count / total) * 100;
                    const color = typeColors[type] || '#A78BFA';
                    gradientParts.push(`${color} ${cumulative.toFixed(1)}% ${(cumulative + pct).toFixed(1)}%`);
                    cumulative += pct;
                });
                
                pie.style.background = `conic-gradient(${gradientParts.join(', ')})`;
                
                // Build legend
                legend.innerHTML = entries.map(([type, count]) => {
                    const color = typeColors[type] || '#A78BFA';
                    return `<span class="admin-legend-dot" style="background:${color}; margin-left:8px;"></span> ${type} (${count})`;
                }).join('');
            } else if (pie && total === 0) {
                pie.style.background = '#E2E8F0';
                legend.innerHTML = '<span style="color:#A0B2D6;">No active bets</span>';
            }
        }
    } catch (err) {
        console.error('Failed to load bet types chart:', err);
    }
    
    // 5. Populate New Users line graph
    try {
        const res = await fetch(`${API_BASE}/users/all`);
        if (res.ok) {
            const users = await res.json();
            // Filter to STUDENTS and LECTURERS
            const targetUsers = users.filter(u => u.userType === 'STUDENT' || u.userType === 'LECTURER');
            
            // Generate last 7 days array ['Mon', 'Tue', 'Wed', ...] and counts
            const daysMap = {};
            const labels = [];
            const counts = [];
            for (let i = 6; i >= 0; i--) {
                const d = new Date();
                d.setDate(d.getDate() - i);
                const year = d.getFullYear();
                const month = String(d.getMonth() + 1).padStart(2, '0');
                const day = String(d.getDate()).padStart(2, '0');
                const dateStr = `${year}-${month}-${day}`;
                
                const label = d.toLocaleDateString('en-US', { weekday: 'short' });
                daysMap[dateStr] = { count: 0, label: label };
                labels.push(label);
            }
            
            // Count users by date
            targetUsers.forEach(u => {
                if (u.createdDate) {
                    const match = u.createdDate.match(/^(\d{4}-\d{2}-\d{2})/);
                    if (match) {
                        const dateStr = match[1];
                        if (daysMap[dateStr]) {
                            daysMap[dateStr].count++;
                        }
                    }
                }
            });
            
            Object.values(daysMap).forEach(data => counts.push(data.count));
            
            let maxCount = Math.max(...counts);
            if (maxCount < 10) maxCount = 10; // Minimum scale of 10
            
            // Update Y-axis labels
            const yAxis = document.getElementById('admin-users-line-y-axis');
            if (yAxis) {
                yAxis.innerHTML = `
                    <span>${maxCount}</span>
                    <span>${Math.round(maxCount / 2)}</span>
                    <span>0</span>
                `;
            }
            
            // Draw SVG
            const pathEl = document.getElementById('admin-users-line-path');
            const pointsGroup = document.getElementById('admin-users-line-points');
            const labelsDiv = document.getElementById('admin-users-line-labels');
            const tooltip = document.getElementById('admin-users-line-tooltip');
            
            if (pathEl && pointsGroup && labelsDiv) {
                let dPath = '';
                pointsGroup.innerHTML = '';
                
                const width = 200;
                const chartHeight = 80;
                const topPadding = 10;
                const stepX = width / (counts.length - 1);
                
                counts.forEach((val, i) => {
                    const x = i * stepX;
                    const y = topPadding + chartHeight - (val / maxCount) * chartHeight;
                    
                    if (i === 0) dPath += `M ${x} ${y} `;
                    else dPath += `L ${x} ${y} `;
                    
                    // Add invisible larger circle for easier hovering
                    pointsGroup.innerHTML += `
                        <circle cx="${x}" cy="${y}" r="10" fill="transparent"
                            onmouseover="showAdminLineTooltip(event, '${labels[i]}', ${val})"
                            onmouseout="hideAdminLineTooltip()" />
                        <circle cx="${x}" cy="${y}" r="3" fill="#1B2F5E" style="pointer-events:none;" />
                    `;
                });
                
                pathEl.setAttribute('d', dPath);
                labelsDiv.innerHTML = labels.map(l => `<span>${l}</span>`).join('');
            }
        }
    } catch (err) {
        console.error('Failed to load new users chart:', err);
    }
}

window.showAdminLineTooltip = function(e, label, value) {
    const tooltip = document.getElementById('admin-users-line-tooltip');
    if (!tooltip) return;
    tooltip.innerHTML = `${label}: ${value} users`;
    tooltip.style.opacity = '1';
    
    const svgRect = document.getElementById('admin-users-line-chart').getBoundingClientRect();
    const x = e.clientX - svgRect.left;
    const y = e.clientY - svgRect.top;
    
    // Position tooltip above the point
    tooltip.style.left = x + 'px';
    tooltip.style.top = y + 'px';
}

window.hideAdminLineTooltip = function() {
    const tooltip = document.getElementById('admin-users-line-tooltip');
    if (tooltip) tooltip.style.opacity = '0';
}

/* =============================================================
 * USER PROFILE MODAL & DELETION LOGIC
 * ============================================================= */

let currentSelectedUserId = null;
let currentSelectedUserData = null;

window.openUserProfileModal = async function() {
    if (!currentSelectedUserId) return;
    
    const detailsContainer = document.getElementById('admin-user-profile-details');
    detailsContainer.innerHTML = '<div style="grid-column: span 2; text-align: center; padding: 20px;">Loading details...</div>';
    
    // Ensure we're showing the read-only view, not the edit form
    const viewDiv = document.getElementById('admin-user-profile-view');
    const editDiv = document.getElementById('admin-user-profile-edit');
    if (viewDiv) viewDiv.style.display = 'block';
    if (editDiv) editDiv.style.display = 'none';
    
    document.getElementById('admin-user-profile-modal').classList.remove('hidden');
    
    try {
        const res = await fetch(`${API_BASE}/users/${currentSelectedUserId}`);
        const data = await res.json();
        
        if (res.ok && data.user) {
            const u = data.user;
            currentSelectedUserData = u;
            const balance = typeof data.balance === 'number' ? data.balance.toFixed(2) : '0.00';
            const role = u.userType || 'N/A';
            const idField = role === 'STUDENT' ? 'Student No.' : (role === 'LECTURER' ? 'Staff No.' : 'User ID');
            const idVal = role === 'STUDENT' ? u.studentNo : (role === 'LECTURER' ? u.staffNo : u.userID);
            
            detailsContainer.innerHTML = `
                <div style="color: #6C7D93; font-weight: 500;">Name:</div>
                <div style="font-weight: 600;">${escapeHtml(u.name || '')} ${escapeHtml(u.surname || '')}</div>
                
                <div style="color: #6C7D93; font-weight: 500;">Email:</div>
                <div style="font-weight: 600;">${escapeHtml(u.email || 'N/A')}</div>
                
                <div style="color: #6C7D93; font-weight: 500;">${idField}:</div>
                <div style="font-weight: 600;">#${escapeHtml(idVal || 'N/A')}</div>
                
                <div style="color: #6C7D93; font-weight: 500;">Role:</div>
                <div style="font-weight: 600;">${escapeHtml(role)}</div>
                
                <div style="color: #6C7D93; font-weight: 500;">MadiBucks:</div>
                <div style="font-weight: 700; color: #F5A623;">${balance}</div>
            `;
        } else {
            detailsContainer.innerHTML = `<div style="grid-column: span 2; color: #D9534F; text-align: center; padding: 20px;">Failed to load profile.</div>`;
        }
    } catch (err) {
        console.error('Error fetching user profile:', err);
        detailsContainer.innerHTML = `<div style="grid-column: span 2; color: #D9534F; text-align: center; padding: 20px;">Network error.</div>`;
    }
}

window.closeAdminUserProfileModal = function() {
    document.getElementById('admin-user-profile-modal').classList.add('hidden');
    const viewDiv = document.getElementById('admin-user-profile-view');
    const editDiv = document.getElementById('admin-user-profile-edit');
    if (viewDiv) viewDiv.style.display = 'block';
    if (editDiv) editDiv.style.display = 'none';
}

window.closeUserProfileModal = function() {
    document.getElementById('user-profile-modal').classList.add('hidden');
}

window.confirmDeleteUser = async function() {
    if (!currentSelectedUserId) return;
    
    // Utilize the existing confirm modal which returns a Promise
    const confirmed = await showConfirmModal(
        'Delete User?', 
        'Are you sure you want to delete this user? This action cannot be undone and will remove all associated bets and account data.'
    );
    
    if (confirmed) {
        try {
            const res = await fetch(`${API_BASE}/users/${currentSelectedUserId}`, {
                method: 'DELETE'
            });
            if (res.ok) {
                showToast('User deleted successfully.', 'success');
                window.closeAdminUserProfileModal();
                // Refresh the table
                loadUserManagementData();
            } else {
                const errData = await res.json();
                showToast(errData.error || 'Failed to delete user.', 'error');
            }
        } catch (err) {
            console.error('Error deleting user:', err);
            showToast('Network error while deleting.', 'error');
        }
    }
}

window.showAdminUpdateForm = function() {
    if (!currentSelectedUserData) return;

    // Pre-fill the form with current values
    document.getElementById('admin-edit-name').value = currentSelectedUserData.name || '';
    document.getElementById('admin-edit-surname').value = currentSelectedUserData.surname || '';

    // Switch to edit mode
    document.getElementById('admin-user-profile-view').style.display = 'none';
    document.getElementById('admin-user-profile-edit').style.display = 'block';
}

window.cancelAdminUpdateForm = function() {
    // Switch back to view mode
    document.getElementById('admin-user-profile-view').style.display = 'block';
    document.getElementById('admin-user-profile-edit').style.display = 'none';
}

window.handleAdminUpdateUser = async function(event) {
    event.preventDefault();
    if (!currentSelectedUserId || !currentSelectedUserData) return;

    const name = document.getElementById('admin-edit-name').value.trim();
    const surname = document.getElementById('admin-edit-surname').value.trim();

    if (!name || !surname) {
        showToast('Name and surname are required.', 'error');
        return;
    }

    const btn = document.getElementById('btn-admin-save-user');
    if (btn) setButtonLoading(btn, true);

    try {
        const response = await fetch(`${API_BASE}/users/${currentSelectedUserId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                name,
                surname,
                email: currentSelectedUserData.email
            })
        });

        if (response.ok) {
            showToast('User details updated successfully!', 'success');
            // Refresh the modal with updated data
            await window.openUserProfileModal();
            // Also refresh the user management table
            loadUserManagementData();
        } else {
            const data = await response.json();
            showToast(data.error || 'Failed to update user details.', 'error');
        }
    } catch (err) {
        showToast('Network error while updating user.', 'error');
        console.error(err);
    } finally {
        if (btn) setButtonLoading(btn, false);
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
    // The search box only filters the open-bets list — hide it on the propose form.
    const search = document.getElementById('bets-search');
    if (search) search.classList.toggle('hidden', tab !== 'place');
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
                ? `<div class="bet-outcome-stack">` +
                    (b.outcomes || []).filter(o => o.odds != null).map(o =>
                    `<button class="bet-action-btn bet-outcome-btn"
                        onclick="openWagerModal(${b.betID}, ${o.outcomeID})">
                        <span class="bet-outcome-label">${escapeHtml(o.label)}</span>
                        <span class="bet-outcome-odds">${Number(o.odds).toFixed(2)}</span></button>`).join('') +
                  `</div>`
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
    'Academics':  ['e.g. Above 60%', 'e.g. Below 60%', 'e.g. Exactly 60%'],
    'Sports':     ['e.g. Madibaz win', 'e.g. Wits win', 'e.g. Draw'],
    'Social':     ['e.g. Over 100 attend', 'e.g. Under 100 attend', 'e.g. Exactly 100'],
    'Class Room': ['e.g. Lecture happens', 'e.g. Lecture cancelled', 'e.g. Moved online'],
};

function updateOutcomePlaceholders() {
    const type = document.getElementById('propose-type').value;
    const examples = OUTCOME_EXAMPLES[type] || [];
    document.querySelectorAll('.propose-outcome-input').forEach((input, i) => {
        input.placeholder = examples[i] || 'Another outcome';
    });
}

/** The add button disappears at the 4-outcome cap instead of erroring. */
function updateAddOutcomeBtn() {
    const count = document.querySelectorAll('.propose-outcome-input').length;
    document.getElementById('btn-add-outcome').classList.toggle('hidden', count >= 4);
}

function addOutcomeField() {
    const container = document.getElementById('propose-outcomes');
    if (container.querySelectorAll('.propose-outcome-input').length >= 4) return;
    // Added fields are optional, so each comes with its own remove (×) button.
    const row = document.createElement('div');
    row.className = 'propose-outcome-row';
    const input = document.createElement('input');
    input.type = 'text';
    input.className = 'propose-outcome-input';
    input.maxLength = 100;
    const remove = document.createElement('button');
    remove.type = 'button';
    remove.className = 'propose-outcome-remove';
    remove.title = 'Remove this outcome';
    remove.textContent = '×';
    remove.onclick = () => removeOutcomeField(remove);
    row.appendChild(input);
    row.appendChild(remove);
    container.appendChild(row);
    updateOutcomePlaceholders();
    updateAddOutcomeBtn();
    input.focus();
}

function removeOutcomeField(btn) {
    btn.closest('.propose-outcome-row').remove();
    updateOutcomePlaceholders();
    updateAddOutcomeBtn();
}

/** Back to the two base fields (used after a successful submission). */
function resetOutcomeFields() {
    document.querySelectorAll('.propose-outcome-row').forEach(r => r.remove());
    document.querySelectorAll('.propose-outcome-input').forEach(i => { i.value = ''; });
    updateOutcomePlaceholders();
    updateAddOutcomeBtn();
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
            resetOutcomeFields();
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

/** Sidebar bubble on "Accounting System": proposals awaiting review.
 *  Same pattern as the Delete Request badge — hidden at zero. */
function setAccountingBadge(count) {
    const badge = document.getElementById('accounting-badge');
    if (!badge) return;
    badge.textContent = count;
    badge.style.display = count > 0 ? 'inline-flex' : 'none';
}

/** Standalone refresh for login/init, when the accounting panel is not open. */
async function refreshAccountingBadge() {
    try {
        const res = await fetch(`${API_BASE}/bets/proposed`);
        const data = await res.json();
        if (res.ok) setAccountingBadge((data.bets || []).length);
    } catch (err) {
        console.error('Failed to refresh accounting badge:', err);
    }
}

function renderProposedTable(bets) {
    setAccountingBadge(bets.length);
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


async function showUserPopup(userId) {
    try {
        const res = await fetch(`${API_BASE}/users/${userId}`);
        if (res.ok) {
            const data = await res.json();
            const u = data.user;
            
            document.getElementById('user-profile-avatar').src = u.avatarPath || "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Crect fill='%231B2F5E' width='100' height='100' rx='50'/%3E%3Ccircle cx='50' cy='38' r='16' fill='%23F5A623'/%3E%3Cpath d='M20 85c0-16 13-28 30-28s30 12 30 28' fill='%23F5A623'/%3E%3C/svg%3E";
            document.getElementById('user-profile-name').textContent = `${u.name} ${u.surname}`;
            document.getElementById('user-profile-madibucks').textContent = `${parseFloat(data.balance).toFixed(2)} MB`;
            
            document.getElementById('user-profile-modal').classList.remove('hidden');
        } else {
            showToast('User not found', 'error');
        }
    } catch (e) {
        console.error(e);
        showToast('Error loading user profile', 'error');
    }
}

function closeUserProfileModal(event) {
    if (event && event.target !== event.currentTarget) return;
    document.getElementById('user-profile-modal').classList.add('hidden');
}


