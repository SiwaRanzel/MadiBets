# -*- coding: utf-8 -*-
import io

APP = r'd:\University\Third year Project\MadiBets\src\main\resources\static\app.js'
HTML = r'd:\University\Third year Project\MadiBets\src\main\resources\static\index.html'

with io.open(APP, 'r', encoding='utf-8') as f:
    text = f.read()

# 1) Replace searchGroups('') with refreshGroups()
text = text.replace("searchGroups('');", "refreshGroups();")

# 2) Replace top join button in loadGroupDetail
target_btn = """            if (!isMember) {
                const joinBtn = document.createElement('button');
                joinBtn.className = 'btn btn-gold-cta';
                joinBtn.style = 'padding:10px 16px;';
                joinBtn.textContent = 'Join Group';
                joinBtn.onclick = async () => {
                    await joinGroup(groupID);
                };
                btnContainer.appendChild(joinBtn);
            }"""

# Normal and CRLF replacements
text = text.replace(target_btn, "")
text = text.replace(target_btn.replace('\n', '\r\n'), "")

# 3) Guard detail.appendChild(btnContainer) on empty container
text = text.replace(
    "            detail.appendChild(btnContainer);",
    "            if (btnContainer.children.length > 0) {\n                detail.appendChild(btnContainer);\n            }"
)
text = text.replace(
    "            detail.appendChild(btnContainer);".replace('\n', '\r\n'),
    "            if (btnContainer.children.length > 0) {\r\n                detail.appendChild(btnContainer);\r\n            }"
)

with io.open(APP, 'w', encoding='utf-8', newline='') as f:
    f.write(text)
print('app.js edits done')

# ----------------- index.html edits -----------------
with io.open(HTML, 'r', encoding='utf-16') as f:
    html = f.read().replace('\r\n', '\n')

top_old = """                        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
                            <h3 style="margin:0; color:#1B2F5E;">Groups</h3>
                            <input id="group-search" type="text" placeholder="Search groups by name or id" style="width:100%; padding:8px 12px; border-radius:8px; border:1px solid #E6EDF7; box-sizing:border-box; font-size:0.9rem;" oninput="debouncedSearchGroups()">
                        </div>"""

top_new = """                        <div style="margin-bottom:12px;">
                            <h3 style="margin:0; color:#1B2F5E;">Groups</h3>
                        </div>"""

bottom_old = """                        <!-- Bottom left box: Search bar -->
                        <div style="padding: 16px; border-top: 1px solid #E6EDF7; border-bottom: 1px solid #E6EDF7;">
                            <h3 style="margin:0; color:#1B2F5E; font-size:1rem; margin-bottom:12px;">Search Groups</h3>
                            <input id="group-search" type="text" placeholder="Search groups by name or id" style="width:100%; padding:8px 12px; border-radius:8px; border:1px solid #E6EDF7; box-sizing:border-box; font-size:0.9rem;" oninput="debouncedSearchGroups()">
                        </div>"""

bottom_new = """                        <!-- Bottom left box: Search Groups -->
                        <div style="padding: 16px; border-top: 1px solid #E6EDF7; border-bottom: 1px solid #E6EDF7;">
                            <h3 style="margin:0; color:#1B2F5E; font-size:1rem; margin-bottom:12px;">Search Groups</h3>
                            <input id="group-search" type="text" placeholder="Search groups by name or id" style="width:100%; padding:8px 12px; border-radius:8px; border:1px solid #E6EDF7; box-sizing:border-box; font-size:0.9rem;" oninput="debouncedSearchGroups()">
                            <div id="search-results-list" style="min-height:60px; margin-top:12px; max-height:220px; overflow-y:auto;">
                                <p style="color:#6C7D93; text-align:center; padding:24px 8px; font-size:0.9rem;">Type a group name to find groups you haven't joined.</p>
                            </div>
                        </div>"""

html = html.replace(top_old, top_new).replace(bottom_old, bottom_new)

with io.open(HTML, 'w', encoding='utf-16') as f:
    f.write(html)
print('index.html edits done')
