import io
p = r'd:\University\Third year Project\MadiBets\src\main\resources\static\app.js'
lines = io.open(p, encoding='utf-8').read().split('\n')
out = io.open(r'd:\University\Third year Project\MadiBets\_sg.txt','w',encoding='utf-8')
for i,l in enumerate(lines,1):
    if 'searchGroups(' in l:
        out.write('%d: %s\n' % (i, l.strip()))
out.close()
print('done')